"""
Visualization Agent — Visualization Specialist.

Decides whether a chart would add value, and if so, generates
executable Plotly code in a sandboxed environment.
"""

import re
import logging
import pandas as pd
import plotly.graph_objects as go

from utils.state import AgentState
from utils.gemini_client import call_gemini

logger = logging.getLogger(__name__)

VIZ_SYSTEM_PROMPT = """You are a data visualization expert for an e-commerce analytics platform.

You are given query results in JSON format and the original question.

STEP 1: Decide if a chart would add value.
- YES for: comparisons, trends over time, distributions, rankings, proportions, breakdowns.
- NO for: single scalar values, simple counts, yes/no answers, text results, or results with only 1 row and 1 column.

STEP 2: If YES, output a valid JSON object representing a Plotly chart configuration.

RULES:
1. First line of your response must be exactly YES or NO.
2. If YES, the remaining lines must be ONLY valid JSON — no markdown, no backticks, no python code, no explanations.
3. The JSON object must have a `data` array containing Plotly traces (e.g. type: 'bar', x: [...], y: [...]).
4. The JSON object can optionally have a `layout` object with title, axes configurations, etc.
5. Do NOT include JavaScript function calls or eval() or unquoted executable code in the JSON.
6. Use professional colors for the traces (#667eea, #764ba2, #f093fb, #4facfe).
7. Ensure all values in x and y correspond to the provided query results.
"""

import json

def _extract_json(response: str) -> str:
    """Extract JSON code from the Gemini response (after the YES line)."""
    lines = response.strip().split("\n")
    if len(lines) <= 1:
        return ""
    
    # Strip the YES line
    json_lines = []
    started = False
    for line in lines:
        if not started:
            if line.strip().upper() == "YES":
                started = True
            continue
        # Remove markdown if any
        if line.strip().startswith("```"):
            continue
        json_lines.append(line)
        
    extracted = "\n".join(json_lines).strip()
    return extracted


def visualization_agent(state: AgentState) -> AgentState:
    """Decide on visualization and optionally generate Plotly JSON.

    Args:
        state: Current state with query_result (DataFrame) and question.

    Returns:
        Updated state with visualization_code (JSON string) if chart was generated.
    """
    df = state.get("query_result")
    question = state.get("question", "")

    # No data → no visualization
    if df is None or (isinstance(df, pd.DataFrame) and df.empty):
        logger.info("Viz Agent: no data to visualize.")
        return {**state, "visualization_code": None}

    # Prepare data summary for the prompt
    if isinstance(df, pd.DataFrame):
        sample = df.head(30)
        data_json = sample.to_json(orient="records", force_ascii=False, date_format="iso")
        col_info = f"Columns: {list(df.columns)}, Rows: {len(df)}"
    else:
        data_json = str(df)
        col_info = "Unknown format"

    user_message = (
        f"ORIGINAL QUESTION: {question}\n\n"
        f"DATA INFO: {col_info}\n\n"
        f"DATA (JSON):\n{data_json}"
    )

    response = call_gemini(
        system_prompt=VIZ_SYSTEM_PROMPT,
        user_message=user_message,
        agent_name="viz_agent",
        max_output_tokens=2048,
    )

    if any(err in response.upper() for err in [
        "COULDN'T GENERATE", "UNAVAILABLE", "TRY AGAIN", "QUOTA", "RATE LIMIT"
    ]):
        logger.warning("Viz Agent: Gemini API is unavailable. Skipping visualization.")
        return state

    first_line = response.strip().split("\n")[0].strip().upper()

    if "YES" not in first_line:
        logger.info("Viz Agent decided: NO chart needed.")
        return {**state, "visualization_code": None}

    # Extract JSON safe payload
    json_str = _extract_json(response)
    if not json_str:
        logger.warning("Viz Agent said YES but no JSON was generated.")
        return {**state, "visualization_code": None}

    logger.info("Viz Agent generated %d chars of Plotly JSON.", len(json_str))

    # Validate JSON
    try:
        json_data = json.loads(json_str)
        if "data" not in json_data:
             logger.warning("Viz Agent JSON missing 'data' key.")
             return {**state, "visualization_code": None}
             
        # Secure execution: we just return the JSON string. No exec() and no JS.
        return {**state, "visualization_code": json_str}

    except Exception as e:
        logger.error("Viz Agent JSON parsing failed: %s", str(e)[:300])
        return {**state, "visualization_code": None}
