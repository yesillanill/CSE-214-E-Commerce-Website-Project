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

STEP 2: If YES, output executable Python code that creates a Plotly chart.

RULES:
1. First line of your response must be exactly YES or NO.
2. If YES, the remaining lines must be ONLY Python code — no markdown, no backticks, no explanation.
3. Use `plotly.graph_objects` (already imported as `go`) and `pandas` (already imported as `pd`).
4. Create a variable named `fig` using `go.Figure()`.
5. The `data` variable is already available as a pandas DataFrame.
6. Do NOT call fig.show() or fig.write_html() or any display/IO function.
7. Do NOT import anything — `go` and `pd` are already available.
8. Use a professional dark theme with these colors: #667eea, #764ba2, #f093fb, #4facfe, #00f2fe, #43e97b, #fa709a.
9. Set layout with: template='plotly_dark', paper_bgcolor='rgba(0,0,0,0)', plot_bgcolor='rgba(0,0,0,0)'.
10. Add proper title, axis labels, and legend.
11. For bar charts, use marker_color with gradient colors from the palette.
12. For pie charts, use the color palette and add hole=0.4 for donut style.
"""


def _extract_code(response: str) -> str:
    """Extract Python code from the Gemini response (after the YES line)."""
    lines = response.strip().split("\n")
    if not lines:
        return ""
    # Skip the YES line and any blank lines
    code_lines = []
    started = False
    for line in lines:
        if not started:
            if line.strip().upper() == "YES":
                started = True
            continue
        # Remove any markdown code fencing
        if line.strip().startswith("```"):
            continue
        code_lines.append(line)
    return "\n".join(code_lines).strip()


def visualization_agent(state: AgentState) -> AgentState:
    """Decide on visualization and optionally generate Plotly chart code.

    The generated code is executed in a sandboxed namespace with only
    `go` (plotly.graph_objects) and `pd` (pandas) available.

    Args:
        state: Current state with query_result (DataFrame) and question.

    Returns:
        Updated state with visualization_code (if chart was generated).
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

    first_line = response.strip().split("\n")[0].strip().upper()

    if "YES" not in first_line:
        logger.info("Viz Agent decided: NO chart needed.")
        return {**state, "visualization_code": None}

    # Extract and execute the code safely
    code = _extract_code(response)
    if not code:
        logger.warning("Viz Agent said YES but no code was generated.")
        return {**state, "visualization_code": None}

    logger.info("Viz Agent generated %d chars of Plotly code.", len(code))

    # Execute in sandboxed namespace
    try:
        safe_globals = {
            "go": go,
            "pd": pd,
            "data": df,
            "__builtins__": __builtins__,
        }
        exec(code, safe_globals)
        fig = safe_globals.get("fig")

        if fig is None:
            logger.warning("Viz Agent code executed but no 'fig' variable was created.")
            return {**state, "visualization_code": None}

        # Store the code and the figure will be re-created in main.py
        return {**state, "visualization_code": code}

    except Exception as e:
        logger.error("Viz Agent code execution failed: %s", str(e)[:300])
        return {**state, "visualization_code": None}
