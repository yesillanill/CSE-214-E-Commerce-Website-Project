"""
Guardrails Agent — Security and Scope Manager.

Classifies user questions as GREETING, OUT_OF_SCOPE, or IN_SCOPE.
Never reveals the database schema in any response.
Blocks raw SQL commands before they reach the pipeline.
"""

import re
import logging
from utils.state import AgentState
from utils.gemini_client import call_gemini

logger = logging.getLogger(__name__)

# ── SQL Injection Detection ──────────────────────────────────────────────────
# If ANY of these patterns match the user's raw question, it is rejected
# immediately — no LLM call, no SQL generation.
SQL_INJECTION_PATTERNS = re.compile(
    r"""
    # Dangerous DML / DDL statements
    \b(INSERT\s+INTO|UPDATE\s+\w+\s+SET|DELETE\s+FROM
       |DROP\s+TABLE|DROP\s+DATABASE|ALTER\s+TABLE|TRUNCATE\s+TABLE
       |CREATE\s+TABLE|CREATE\s+DATABASE)\b
    # UNION-based injection
    | \bUNION\s+(ALL\s+)?SELECT\b
    # Raw SELECT with FROM (users trying to execute SQL directly)
    | \bSELECT\s+.+\s+FROM\s+\w+
    # Schema probing
    | \b(INFORMATION_SCHEMA|PG_CATALOG|PG_TABLES|PG_CLASS|SYSOBJECTS)\b
    # Statement terminators + keywords
    | ;\s*(DROP|ALTER|CREATE|TRUNCATE|INSERT|UPDATE|DELETE|EXEC|SELECT)\b
    # SQL comments used for injection
    | --\s
    | /\*
    # Time-based blind injection
    | \b(PG_SLEEP|SLEEP|BENCHMARK|WAITFOR\s+DELAY)\s*\(
    # Hex-encoded payloads
    | 0x[0-9a-fA-F]{8,}
    # Classic tautology injection
    | '\s*(OR|AND)\s+\d+\s*=\s*\d+
    # xp_ extended procedures
    | \bxp_\w+
    # File operations
    | \b(LOAD_FILE|INTO\s+OUTFILE|INTO\s+DUMPFILE)\b
    # Function obfuscation
    | \b(CHAR|CHR|UNHEX|CONV)\s*\(
    """,
    re.IGNORECASE | re.VERBOSE,
)

SQL_BLOCKED_RESPONSE = (
    "🚫 Güvenlik uyarısı: Mesajınızda SQL komutları tespit edildi.\n\n"
    "SQL sorguları doğrudan yazılamaz. Lütfen sorunuzu doğal dilde ifade edin.\n\n"
    "Örneğin:\n"
    '• "En çok satan ürünler hangileri?"\n'
    '• "Bu ayın geliri ne kadar?"\n'
    '• "Toplam sipariş sayısı kaç?"'
)

GUARDRAILS_SYSTEM_PROMPT = """You are a strict guardrails system for an e-commerce analytics chatbot.

Your job is to classify the user question into EXACTLY ONE of these categories:
- GREETING: If the user is saying hello, hi, hey, merhaba, selam, hoşgeldin, good morning, etc.
- OUT_OF_SCOPE: If the question is NOT related to e-commerce data analysis (e.g., weather, coding help, personal questions, math problems, general knowledge, recipes, etc.)
- IN_SCOPE: If the question is about e-commerce analytics, sales data, orders, products, customers, revenue, inventory, shipments, payments, reviews, categories, brands, stores, or any business metrics.

CRITICAL RULES:
1. Respond with ONLY the classification word: GREETING, OUT_OF_SCOPE, or IN_SCOPE
2. No explanation, no extra text — just the single word.
3. NEVER reveal any database schema, table names, or column names.
4. Questions about shopping behavior, spending patterns, top products, revenue breakdowns, customer demographics — these are all IN_SCOPE.
5. If the user message contains raw SQL commands (SELECT, INSERT, DROP, etc.), classify as OUT_OF_SCOPE.
"""

# Friendly welcome messages
GREETING_RESPONSE = (
    "👋 Merhaba! E-ticaret analitik asistanınıza hoş geldiniz!\n\n"
    "Size satış verileri, ürün performansı, müşteri analizleri ve daha fazlası "
    "hakkında sorular sorabilirsiniz. Örneğin:\n\n"
    '• "En çok satan ürünler hangileri?"\n'
    '• "Bu ayın toplam geliri ne kadar?"\n'
    '• "Kategorilere göre satış dağılımı"\n'
    '• "Müşteri memnuniyet oranları"\n\n'
    "Nasıl yardımcı olabilirim? 📊"
)

OUT_OF_SCOPE_RESPONSE = (
    "⚠️ Üzgünüm, bu soru e-ticaret veri analizi kapsamı dışındadır.\n\n"
    "Ben yalnızca satış, sipariş, ürün, müşteri ve mağaza verileri hakkında "
    "sorulara yanıt verebilirim. Lütfen e-ticaret analitiği ile ilgili "
    "bir soru sorun."
)


def guardrails_agent(state: AgentState) -> AgentState:
    """Classify the user question and decide whether to proceed or end.

    Args:
        state: Current agent state with the user's question.

    Returns:
        Updated state with is_in_scope flag and optional final_answer.
    """
    question = state.get("question", "").strip()
    logger.info("Guardrails agent processing: '%s'", question[:100])

    if not question:
        return {
            **state,
            "is_in_scope": False,
            "final_answer": "Lütfen bir soru sorun.",
        }

    # ── SQL Injection Check (BEFORE any LLM call) ────────────────────────
    if SQL_INJECTION_PATTERNS.search(question):
        logger.warning(
            "Guardrails BLOCKED — SQL injection detected in user question: '%s'",
            question[:200],
        )
        return {
            **state,
            "is_in_scope": False,
            "final_answer": SQL_BLOCKED_RESPONSE,
        }

    # Call Gemini for classification
    classification = call_gemini(
        system_prompt=GUARDRAILS_SYSTEM_PROMPT,
        user_message=question,
        agent_name="guardrails",
    ).strip().upper()

    logger.info("Guardrails classification: %s", classification)

    if "GREETING" in classification:
        return {
            **state,
            "is_in_scope": False,
            "final_answer": GREETING_RESPONSE,
        }
    elif "OUT_OF_SCOPE" in classification or "OUT" in classification:
        return {
            **state,
            "is_in_scope": False,
            "final_answer": OUT_OF_SCOPE_RESPONSE,
        }
    else:
        # IN_SCOPE — continue to SQL Agent
        return {
            **state,
            "is_in_scope": True,
        }

