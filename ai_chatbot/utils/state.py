"""
AgentState definition for the LangGraph workflow.
Uses TypedDict for type safety across all agent nodes.
"""

from typing import TypedDict, Optional, Any
import pandas as pd


class AgentState(TypedDict):
    """Shared state passed between all agents in the LangGraph workflow.

    Attributes:
        question:           The user's natural language question.
        user_id:            Authenticated user's ID (from JWT).
        role_type:          User role: INDIVIDUAL | CORPORATE | ADMIN.
        jwt_token:          JWT bearer token for Spring Boot API calls.
        sql_query:          Generated SQL SELECT query (set by SQL Agent).
        query_result:       Query results as a Pandas DataFrame (set by SQL Executor).
        error:              Error message if SQL execution failed.
        final_answer:       Natural language answer (set by Analysis Agent or Guardrails).
        visualization_code: Plotly Python code string (set by Viz Agent).
        is_in_scope:        Whether the question is in-scope for analytics.
        iteration_count:    Number of error-recovery retries so far.
    """
    question: str
    user_id: int
    corp_id: int
    role_type: str
    jwt_token: str
    sql_query: Optional[str]
    query_result: Optional[Any]  # pd.DataFrame — Any for TypedDict compat
    error: Optional[str]
    final_answer: Optional[str]
    visualization_code: Optional[str]
    is_in_scope: bool
    iteration_count: int
