"""
Gemini API client wrapper using the new google-genai SDK.
Implements exponential backoff with rate limiting for gemini-2.5-flash free tier.
"""

import time
import threading
import logging
from datetime import datetime
from typing import Optional

from google import genai
from google.genai import types

from config import (
    GEMINI_API_KEY,
    GEMINI_MODEL,
    GEMINI_MAX_RETRIES,
    GEMINI_INITIAL_BACKOFF_S,
)

logger = logging.getLogger(__name__)

# ── Configure Gemini SDK ──────────────────────────────────────────────────────
_client = genai.Client(api_key=GEMINI_API_KEY)

# ── Rate Limiter (gemini-2.5-flash free tier: ~10 RPM) ───────────────────────
_rate_lock = threading.Lock()
_request_timestamps: list[float] = []
_MAX_RPM = 8  # Stay slightly under limit to avoid 429s


def _wait_for_rate_limit():
    """Simple sliding-window rate limiter. Blocks if we're at capacity."""
    with _rate_lock:
        now = time.time()
        # Remove timestamps older than 60 seconds
        while _request_timestamps and _request_timestamps[0] < now - 60:
            _request_timestamps.pop(0)

        if len(_request_timestamps) >= _MAX_RPM:
            # Need to wait until the oldest request expires
            wait_time = 60 - (now - _request_timestamps[0]) + 0.5
            if wait_time > 0:
                logger.info("[rate_limiter] Throttling for %.1fs to stay under %d RPM", wait_time, _MAX_RPM)
                time.sleep(wait_time)
                # Clean again after waiting
                now = time.time()
                while _request_timestamps and _request_timestamps[0] < now - 60:
                    _request_timestamps.pop(0)

        _request_timestamps.append(time.time())


def call_gemini(
    system_prompt: str,
    user_message: str,
    agent_name: str = "unknown",
    temperature: float = 0.1,
    max_output_tokens: int = 1024,
) -> str:
    """Call Gemini with exponential backoff and rate limiting.

    Args:
        system_prompt:    System-level instruction for the model.
        user_message:     The user's input/question.
        agent_name:       Name of the calling agent (for logging).
        temperature:      Sampling temperature (low = deterministic).
        max_output_tokens: Maximum response length.

    Returns:
        The model's text response, or error messages.
    """
    input_len = len(system_prompt) + len(user_message)

    for attempt in range(GEMINI_MAX_RETRIES + 1):
        # Apply rate limiting before each request
        _wait_for_rate_limit()

        start_ts = datetime.now()
        try:
            response = _client.models.generate_content(
                model=GEMINI_MODEL,
                contents=user_message,
                config=types.GenerateContentConfig(
                    system_instruction=system_prompt,
                    temperature=temperature,
                    max_output_tokens=max_output_tokens,
                ),
            )

            latency_ms = (datetime.now() - start_ts).total_seconds() * 1000

            # Check for blocked response
            if response.candidates and response.candidates[0].finish_reason and \
               response.candidates[0].finish_reason.name == "SAFETY":
                logger.warning(
                    "[%s] Gemini blocked response | reason=SAFETY | latency=%.0fms",
                    agent_name, latency_ms,
                )
                return "BLOCKED"

            output_text = response.text.strip() if response.text else ""
            output_len = len(output_text)

            logger.info(
                "[%s] Gemini call OK | input=%d chars | output=%d chars | latency=%.0fms",
                agent_name, input_len, output_len, latency_ms,
            )
            return output_text

        except Exception as e:
            latency_ms = (datetime.now() - start_ts).total_seconds() * 1000
            error_str = str(e)

            # Retry on quota / server errors (503, 429, etc.)
            is_retryable = any(
                code in error_str for code in ["503", "429", "RESOURCE_EXHAUSTED", "UNAVAILABLE", "quota"]
            )

            if is_retryable and attempt < GEMINI_MAX_RETRIES:
                backoff = GEMINI_INITIAL_BACKOFF_S * (2 ** attempt)
                logger.warning(
                    "[%s] Gemini error (attempt %d/%d). Retrying in %ds... | error=%s",
                    agent_name, attempt + 1, GEMINI_MAX_RETRIES, backoff, error_str[:200],
                )
                time.sleep(backoff)
            else:
                logger.error(
                    "[%s] Gemini call FAILED | attempt=%d | latency=%.0fms | error=%s",
                    agent_name, attempt + 1, latency_ms, error_str[:300],
                )
                return "I couldn't generate a response. Please try again."

    return "The AI service is currently unavailable. Please try again later."
