"""
LangGraph Workflow — Multi-Agent Text2SQL Pipeline.

Defines the state graph with conditional edges:
  guardrails → sql_generator → sql_executor → analyzer → visualizer → END
                                    ↓ (error)
                              error_handler → sql_generator (retry)
                                    ↓ (max retries)
                                   END
"""

import logging
from langgraph.graph import StateGraph, END

from utils.state import AgentState
from utils.sql_executor import execute_sql
from agents.guardrails import guardrails_agent
from agents.sql_agent import sql_agent
from agents.error_agent import error_agent
from agents.analysis_agent import analysis_agent
from agents.viz_agent import visualization_agent

logger = logging.getLogger(__name__)


def _route_after_guardrails(state: AgentState) -> str:
    """Route based on guardrails classification."""
    if state.get("is_in_scope"):
        return "sql_generator"
    return END


def _route_after_executor(state: AgentState) -> str:
    """Route based on SQL execution result."""
    if state.get("error"):
        return "error_handler"
    return "analyzer"


def _route_after_error_handler(state: AgentState) -> str:
    """Route based on error handler result."""
    # If final_answer is set, max retries were reached → end
    if state.get("final_answer"):
        return END
    # Otherwise retry SQL generation
    return "sql_generator"


def build_graph() -> StateGraph:
    """Build and compile the multi-agent Text2SQL workflow.

    Returns:
        Compiled LangGraph application ready for .invoke() or .ainvoke().
    """
    workflow = StateGraph(AgentState)

    # ── Register nodes ────────────────────────────────────────────────────
    workflow.add_node("guardrails", guardrails_agent)
    workflow.add_node("sql_generator", sql_agent)
    workflow.add_node("sql_executor", execute_sql)
    workflow.add_node("error_handler", error_agent)
    workflow.add_node("analyzer", analysis_agent)
    workflow.add_node("visualizer", visualization_agent)

    # ── Entry point ───────────────────────────────────────────────────────
    workflow.set_entry_point("guardrails")

    # ── Edges ─────────────────────────────────────────────────────────────
    workflow.add_conditional_edges("guardrails", _route_after_guardrails)
    workflow.add_edge("sql_generator", "sql_executor")
    workflow.add_conditional_edges("sql_executor", _route_after_executor)
    workflow.add_conditional_edges("error_handler", _route_after_error_handler)
    workflow.add_edge("analyzer", "visualizer")
    workflow.add_edge("visualizer", END)

    logger.info("LangGraph workflow built successfully.")
    return workflow.compile()


# Compile once at module level for reuse
app = build_graph()
