"""
Analysis Agent — Data Analyst.

Takes SQL query results and produces a clear, concise natural language explanation
with key insights. Keeps responses under 150 words.
"""

import logging
import pandas as pd
from utils.state import AgentState
from utils.gemini_client import call_gemini

logger = logging.getLogger(__name__)

ANALYSIS_SYSTEM_PROMPT = """You are a data analyst for an e-commerce platform.

Given query results in JSON format, provide a brief natural language explanation with key insights.

RULES:
1. Keep your response under 150 words.
2. Highlight the most important findings: top values, trends, anomalies, totals.
3. Use bullet points for multiple insights.
4. If the data is empty, say "Sonuç bulunamadı" (No results found) and suggest the user rephrase.
5. Use numbers and percentages where relevant.
6. Respond in Turkish if the original question appears Turkish, otherwise in English.
7. Format with markdown (bold, bullet points) for readability.
8. Do NOT reveal raw SQL or table/column names.
"""


def analysis_agent(state: AgentState) -> AgentState:
    """Analyze query results and produce a natural language summary.

    Args:
        state: Current state with query_result (DataFrame) and question.

    Returns:
        Updated state with final_answer containing the analysis.
    """
    question = state.get("question", "")
    df = state.get("query_result")

    # Handle empty / missing results
    if df is None or (isinstance(df, pd.DataFrame) and df.empty):
        logger.info("Analysis Agent: no results to analyze.")
        return {
            **state,
            "final_answer": (
                "📭 Sorgunuz için sonuç bulunamadı.\n\n"
                "Bu durum aşağıdaki nedenlerden kaynaklanabilir:\n"
                "• Aradığınız ürün veya veri veritabanında mevcut olmayabilir\n"
                "• Ürün adı farklı bir şekilde kayıtlı olabilir\n"
                "• Arama kriterleri çok spesifik olabilir\n\n"
                "💡 **Öneriler:**\n"
                "• Daha genel terimlerle aramayı deneyin\n"
                "• Ürün adının yazımını kontrol edin\n"
                "• Tüm ürünleri listelemek için \"tüm ürünleri göster\" deneyin"
            ),
        }

    # Convert DataFrame to a concise JSON string for the prompt
    if isinstance(df, pd.DataFrame):
        # Limit to first 50 rows to keep prompt manageable
        sample = df.head(50)
        data_json = sample.to_json(orient="records", force_ascii=False, date_format="iso")
        row_count = len(df)
        col_names = list(df.columns)
    else:
        data_json = str(df)
        row_count = 0
        col_names = []

    user_message = (
        f"ORIGINAL QUESTION: {question}\n\n"
        f"QUERY RESULTS ({row_count} rows, columns: {col_names}):\n{data_json}"
    )

    analysis = call_gemini(
        system_prompt=ANALYSIS_SYSTEM_PROMPT,
        user_message=user_message,
        agent_name="analysis_agent",
        max_output_tokens=1024,
    )

    logger.info("Analysis Agent produced %d char response.", len(analysis))

    role_type = state.get("role_type", "GUEST").upper()
    if role_type == "GUEST":
         analysis += "\n\n💡 *Daha detaylı analizler, sipariş takibi ve kişiselleştirilmiş verileriniz için lütfen giriş yapın veya üye olun!*"

    return {**state, "final_answer": analysis}
