"""
SQL Executor — sends generated SQL to the Spring Boot backend for execution.
Mirrors the approach from ChatController.java: POST to /api/chat/execute.
"""

import logging
from typing import Any, Dict

import pandas as pd
import requests

from utils.state import AgentState
from config import SQL_EXECUTE_URL

logger = logging.getLogger(__name__)


def execute_sql(state: AgentState) -> AgentState:
    """Execute SQL via the Spring Boot backend and return updated state.

    Calls POST /api/chat/execute with the generated SQL query.
    On success, populates query_result as a DataFrame.
    On failure, populates error with a safe message.

    Args:
        state: Current agent state containing sql_query, user_id, role_type, jwt_token.

    Returns:
        New state dict with query_result or error populated.
    """
    sql = state.get("sql_query", "")
    if not sql:
        return {**state, "error": "No SQL query was generated.", "query_result": None}

    try:
        headers = {"Content-Type": "application/json"}
        token = state.get("jwt_token", "")
        if token:
            headers["Authorization"] = f"Bearer {token}"

        payload = {
            "sql": sql,
            "userId": state.get("user_id"),
            "corpId": state.get("corp_id", 0),
            "roleType": state.get("role_type", ""),
        }

        logger.info(
            "Executing SQL via Spring Boot | url=%s | sql_length=%d",
            SQL_EXECUTE_URL,
            len(sql),
        )

        resp = requests.post(
            SQL_EXECUTE_URL,
            json=payload,
            headers=headers,
            timeout=15,
        )
        resp.raise_for_status()

        data = resp.json()
        results = data.get("results", [])
        df = pd.DataFrame(results) if results else pd.DataFrame()

        logger.info("SQL execution OK | rows=%d", len(df))

        return {**state, "query_result": df, "error": None}

    except requests.exceptions.HTTPError as e:
        # Try to get error message from response body
        error_msg = "SQL execution failed."
        try:
            err_body = e.response.json()
            error_msg = err_body.get("error", str(e))
        except Exception:
            error_msg = str(e)

        logger.error("SQL execution HTTP error | status=%s | error=%s", e.response.status_code, error_msg[:300])
        return {**state, "error": error_msg, "query_result": None}

    except requests.exceptions.ConnectionError:
        logger.error("SQL execution failed — cannot connect to Spring Boot at %s", SQL_EXECUTE_URL)
        return {
            **state,
            "error": "Backend service is not available. Please ensure the server is running.",
            "query_result": None,
        }

    except Exception as e:
        logger.error("SQL execution unexpected error | %s", str(e)[:300])
        return {
            **state,
            "error": "An unexpected error occurred while executing the query.",
            "query_result": None,
        }
