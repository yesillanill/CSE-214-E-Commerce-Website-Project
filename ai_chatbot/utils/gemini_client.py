"""
Gemini API client wrapper.
Mirrors the retry/backoff pattern from the existing Java GeminiService.java,
but uses the google-generativeai Python SDK.
"""

import os
import time
import logging
from datetime import datetime
from typing import Optional

import google.generativeai as genai
from google.generativeai.types import HarmCategory, HarmBlockThreshold

from config import (
    GEMINI_API_KEY,
    GEMINI_MODEL,
    GEMINI_MAX_RETRIES,
    GEMINI_INITIAL_BACKOFF_S,
)

logger = logging.getLogger(__name__)

# ── Configure Gemini SDK ──────────────────────────────────────────────────────
genai.configure(api_key=GEMINI_API_KEY)

# Safety settings — BLOCK_ONLY_HIGH avoids false positives on business data
# (same philosophy as the Java GeminiService)
_SAFETY_SETTINGS = {
    HarmCategory.HARM_CATEGORY_HATE_SPEECH: HarmBlockThreshold.BLOCK_ONLY_HIGH,
    HarmCategory.HARM_CATEGORY_HARASSMENT: HarmBlockThreshold.BLOCK_ONLY_HIGH,
    HarmCategory.HARM_CATEGORY_SEXUALLY_EXPLICIT: HarmBlockThreshold.BLOCK_ONLY_HIGH,
    HarmCategory.HARM_CATEGORY_DANGEROUS_CONTENT: HarmBlockThreshold.BLOCK_ONLY_HIGH,
}

_model = genai.GenerativeModel(
    model_name=GEMINI_MODEL,
    safety_settings=_SAFETY_SETTINGS,
)


def call_gemini(
    system_prompt: str,
    user_message: str,
    agent_name: str = "unknown",
    temperature: float = 0.1,
    max_output_tokens: int = 1024,
) -> str:
    """Call Gemini with exponential backoff (mirrors Java GeminiService pattern).

    Args:
        system_prompt:    System-level instruction for the model.
        user_message:     The user's input/question.
        agent_name:       Name of the calling agent (for logging).
        temperature:      Sampling temperature (low = deterministic).
        max_output_tokens: Maximum response length.

    Returns:
        The model's text response, or "BLOCKED" / error messages.
    """
    full_prompt = f"{system_prompt}\n\nUser: {user_message}"
    input_len = len(full_prompt)

    for attempt in range(GEMINI_MAX_RETRIES + 1):
        start_ts = datetime.now()
        try:
            response = _model.generate_content(
                full_prompt,
                generation_config=genai.GenerationConfig(
                    temperature=temperature,
                    max_output_tokens=max_output_tokens,
                ),
            )

            latency_ms = (datetime.now() - start_ts).total_seconds() * 1000
            output_text = ""

            # Check for blocked response
            if response.prompt_feedback and response.prompt_feedback.block_reason:
                logger.warning(
                    "[%s] Gemini blocked response | reason=%s | latency=%.0fms",
                    agent_name,
                    response.prompt_feedback.block_reason,
                    latency_ms,
                )
                return "BLOCKED"

            output_text = response.text.strip() if response.text else ""
            output_len = len(output_text)

            logger.info(
                "[%s] Gemini call OK | input=%d chars | output=%d chars | latency=%.0fms",
                agent_name,
                input_len,
                output_len,
                latency_ms,
            )
            return output_text

        except Exception as e:
            latency_ms = (datetime.now() - start_ts).total_seconds() * 1000
            error_str = str(e)

            # Retry on quota / server errors (503, 429, etc.)
            is_retryable = any(
                code in error_str for code in ["503", "429", "RESOURCE_EXHAUSTED", "UNAVAILABLE"]
            )

            if is_retryable and attempt < GEMINI_MAX_RETRIES:
                backoff = GEMINI_INITIAL_BACKOFF_S * (2 ** attempt)
                logger.warning(
                    "[%s] Gemini error (attempt %d/%d). Retrying in %ds... | error=%s",
                    agent_name,
                    attempt + 1,
                    GEMINI_MAX_RETRIES,
                    backoff,
                    error_str[:200],
                )
                time.sleep(backoff)
            else:
                logger.error(
                    "[%s] Gemini call FAILED | attempt=%d | latency=%.0fms | error=%s",
                    agent_name,
                    attempt + 1,
                    latency_ms,
                    error_str[:300],
                )
                return "I couldn't generate a response. Please try again."

    return "The AI service is currently unavailable. Please try again later."
