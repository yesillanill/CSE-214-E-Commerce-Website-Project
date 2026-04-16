"""
Error Agent — Error Recovery Specialist.

Diagnoses failed SQL queries and generates corrected versions.
Hard-stops after MAX_ERROR_RETRIES attempts.
"""

import re
import logging
from utils.state import AgentState
from utils.gemini_client import call_gemini
from config import DB_SCHEMA, MAX_ERROR_RETRIES

logger = logging.getLogger(__name__)

ERROR_AGENT_SYSTEM_PROMPT = """You are an SQL error recovery specialist for a PostgreSQL e-commerce database.

DATABASE SCHEMA:
{schema}

You are given a SQL query that failed and its error message. Your job:
1. Diagnose the error (wrong column name, missing JOIN, syntax error, etc.)
2. Return ONLY the corrected SQL query.
3. No explanation, no markdown, no backticks, no semicolons at the end.
4. The corrected query must be a valid PostgreSQL SELECT statement.
5. Keep the same intent as the original query.
6. Use proper table aliases.
"""


def _clean_sql(raw: str) -> str:
    """Strip markdown fencing, backticks, and trailing semicolons."""
    sql = raw.strip()
    sql = re.sub(r"^```(?:sql)?\s*", "", sql)
    sql = re.sub(r"\s*```$", "", sql)
    sql = sql.replace("`", "")
    sql = sql.rstrip(";").strip()
    return sql


def error_agent(state: AgentState) -> AgentState:
    """Attempt to fix a failed SQL query based on the error message.

    Increments iteration_count. If at or above MAX_ERROR_RETRIES,
    sets final_answer with an error message and signals the graph to end.

    Args:
        state: Current state with sql_query, error, and iteration_count.

    Returns:
        Updated state with corrected sql_query or final_answer on max retries.
    """
    iteration = state.get("iteration_count", 0) + 1
    failed_sql = state.get("sql_query", "")
    error_msg = state.get("error", "Unknown error")

    logger.warning(
        "Error Agent attempt %d/%d | error=%s | sql=%s",
        iteration,
        MAX_ERROR_RETRIES,
        error_msg[:200],
        failed_sql[:200],
    )

    # Hard-stop after max retries
    if iteration >= MAX_ERROR_RETRIES:
        logger.error("Error Agent exceeded max retries (%d). Giving up.", MAX_ERROR_RETRIES)
        return {
            **state,
            "iteration_count": iteration,
            "final_answer": (
                "⚠️ Sorgunuz birkaç denemeden sonra çalıştırılamadı. "
                "Lütfen sorunuzu farklı şekilde ifade etmeyi deneyin."
            ),
            "error": None,
        }

    # Ask Gemini to fix the query
    system_prompt = ERROR_AGENT_SYSTEM_PROMPT.format(schema=DB_SCHEMA)
    user_message = (
        f"FAILED SQL QUERY:\n{failed_sql}\n\n"
        f"ERROR MESSAGE:\n{error_msg}\n\n"
        "Please provide the corrected SQL query."
    )

    raw_fixed = call_gemini(
        system_prompt=system_prompt,
        user_message=user_message,
        agent_name="error_agent",
        max_output_tokens=2048,
    )

    fixed_sql = _clean_sql(raw_fixed)
    logger.info("Error Agent corrected SQL: %s", fixed_sql[:500])

    return {
        **state,
        "sql_query": fixed_sql,
        "error": None,
        "iteration_count": iteration,
    }
