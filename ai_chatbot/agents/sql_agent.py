"""
SQL Agent — SQL Expert.

Generates valid PostgreSQL SELECT queries from natural language questions.
Applies role-based data filtering and validates output is SELECT-only.
"""

import re
import logging
from utils.state import AgentState
from utils.gemini_client import call_gemini
from config import DB_SCHEMA

logger = logging.getLogger(__name__)

SQL_AGENT_SYSTEM_PROMPT = """You are a senior SQL developer specializing in e-commerce PostgreSQL databases.

DATABASE SCHEMA:
{schema}

ROLE-BASED ACCESS RULES:
- Current user_id: {user_id}
- Current role: {role_type}

{role_filter_instruction}

STRICT RULES:
1. Generate ONLY a valid PostgreSQL SELECT query.
2. Output raw SQL only — NO markdown, NO backticks, NO explanation, NO semicolons at the end.
3. ALWAYS use table aliases (e.g., u for users, o for orders, p for products).
4. Prefer JOINs over subqueries for performance.
5. NEVER generate INSERT, UPDATE, DELETE, DROP, CREATE, ALTER, or TRUNCATE statements.
6. NEVER include sensitive fields like password in SELECT.
7. For aggregate queries, always include meaningful column aliases.
8. Use appropriate PostgreSQL functions (e.g., DATE_TRUNC, EXTRACT, COALESCE).
9. Limit results to 100 rows unless the user specifies otherwise.
10. When asked about "store orders", join through order_items → products → stores.
"""

ROLE_FILTER_INSTRUCTIONS = {
    "INDIVIDUAL": (
        "IMPORTANT: This is an INDIVIDUAL user. You MUST always filter data to show ONLY "
        "this user's own data. Add WHERE clause: o.user_id = {user_id} for orders, "
        "r.user_id = {user_id} for reviews, etc. NEVER show other users' data."
    ),
    "CORPORATE": (
        "IMPORTANT: This is a CORPORATE (store owner) user. You MUST always filter data "
        "to show ONLY this user's store data. Use: s.owner_id = {user_id} for stores, "
        "or JOIN through stores to filter products, orders, inventory by this store only. "
        "NEVER show other stores' data."
    ),
    "ADMIN": (
        "This is an ADMIN user with full data access. No filtering restrictions apply. "
        "The admin can see all data across all stores and users."
    ),
}

# Dangerous SQL patterns that must never appear
DANGEROUS_PATTERNS = re.compile(
    r"\b(INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|TRUNCATE|EXEC|EXECUTE|GRANT|REVOKE)\b",
    re.IGNORECASE,
)


def _clean_sql(raw: str) -> str:
    """Strip markdown fencing, backticks, and trailing semicolons from Gemini output."""
    sql = raw.strip()
    # Remove markdown code blocks
    sql = re.sub(r"^```(?:sql)?\s*", "", sql)
    sql = re.sub(r"\s*```$", "", sql)
    # Remove stray backticks
    sql = sql.replace("`", "")
    # Remove trailing semicolons
    sql = sql.rstrip(";").strip()
    return sql


def _validate_sql(sql: str) -> bool:
    """Return True if the SQL is a safe SELECT statement."""
    if not sql:
        return False
    # Must start with SELECT (or WITH for CTEs)
    if not re.match(r"^\s*(SELECT|WITH)\b", sql, re.IGNORECASE):
        return False
    # Must not contain dangerous statements
    if DANGEROUS_PATTERNS.search(sql):
        return False
    return True


def sql_agent(state: AgentState) -> AgentState:
    """Generate a SQL query from the user's question with role-based filtering.

    Args:
        state: Current agent state with question, user_id, role_type.

    Returns:
        Updated state with sql_query populated (or final_answer on validation failure).
    """
    question = state.get("question", "")
    user_id = state.get("user_id", 0)
    role_type = state.get("role_type", "INDIVIDUAL").upper()

    logger.info("SQL Agent generating query | role=%s | user_id=%s", role_type, user_id)

    # Build role-specific filter instruction
    role_instruction = ROLE_FILTER_INSTRUCTIONS.get(role_type, ROLE_FILTER_INSTRUCTIONS["INDIVIDUAL"])
    role_instruction = role_instruction.format(user_id=user_id)

    # Build the full system prompt with schema and role info
    system_prompt = SQL_AGENT_SYSTEM_PROMPT.format(
        schema=DB_SCHEMA,
        user_id=user_id,
        role_type=role_type,
        role_filter_instruction=role_instruction,
    )

    raw_sql = call_gemini(
        system_prompt=system_prompt,
        user_message=question,
        agent_name="sql_agent",
        max_output_tokens=2048,
    )

    sql = _clean_sql(raw_sql)
    logger.info("SQL Agent raw output: %s", sql[:500])

    # Validate the generated SQL
    if not _validate_sql(sql):
        logger.warning("SQL Agent generated invalid/dangerous SQL: %s", sql[:200])
        return {
            **state,
            "sql_query": None,
            "error": "Generated query failed safety validation.",
        }

    return {**state, "sql_query": sql, "error": None}
