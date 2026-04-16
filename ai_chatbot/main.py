"""
Chainlit entry point for the Multi-Agent Text2SQL Chatbot.

Features:
- Login authentication via Spring Boot /api/auth/login/email
- Chat interface that invokes the LangGraph pipeline
- Plotly chart rendering as inline HTML elements
"""

import logging
import os

import chainlit as cl
import pandas as pd
import plotly.graph_objects as go
import plotly.io as pio
import requests

from dotenv import load_dotenv

load_dotenv()

from config import LOGIN_URL
from graph import app
from utils.state import AgentState

# ── Logging setup ─────────────────────────────────────────────────────────────
logging.basicConfig(
    level=os.getenv("LOG_LEVEL", "INFO"),
    format="%(asctime)s [%(name)s] %(levelname)s: %(message)s",
)
logger = logging.getLogger(__name__)


# ── Chainlit Auth via Spring Boot login ───────────────────────────────────────
@cl.password_auth_callback
def auth_callback(username: str, password: str):
    """Authenticate against Spring Boot /api/auth/login/email.

    On success, returns a cl.User with the JWT token and role stored in metadata.
    On failure, returns None (Chainlit shows error).
    """
    try:
        resp = requests.post(
            LOGIN_URL,
            json={"email": username, "password": password},
            timeout=10,
        )
        resp.raise_for_status()
        data = resp.json()

        token = data.get("token", "")
        user_id = data.get("id")
        role = data.get("role", "INDIVIDUAL")
        name = data.get("name", "User")
        surname = data.get("surname", "")

        logger.info("User authenticated: %s %s (id=%s, role=%s)", name, surname, user_id, role)

        return cl.User(
            identifier=username,
            metadata={
                "token": token,
                "user_id": user_id,
                "corp_id": data.get("storeId", 0),  # Example of extracting if available
                "role": role,
                "name": name,
                "surname": surname,
            },
        )
    except requests.exceptions.HTTPError as e:
        logger.warning("Login failed for %s: %s", username, str(e)[:200])
        return None
    except requests.exceptions.ConnectionError:
        logger.error("Cannot connect to Spring Boot at %s", LOGIN_URL)
        return None
    except Exception as e:
        logger.error("Login error: %s", str(e)[:200])
        return None


# ── Chat Start ────────────────────────────────────────────────────────────────
@cl.on_chat_start
async def on_chat_start():
    """Send a welcome message when the chat session starts."""
    user = cl.user_session.get("user")
    name = user.metadata.get("name", "User") if user else "User"

    await cl.Message(
        content=(
            f"👋 Hoş geldiniz, **{name}**!\n\n"
            "Ben e-ticaret analitik asistanınızım. Veritabanınızdaki veriler hakkında "
            "doğal dilde sorular sorabilirsiniz. Örneğin:\n\n"
            '• *"En çok satan 10 ürünü göster"*\n'
            '• *"Bu ayın toplam geliri ne kadar?"*\n'
            '• *"Kategorilere göre sipariş dağılımı"*\n'
            '• *"Son 30 günde ortalama sipariş tutarı"*\n\n'
            "Sorularınızı bekliyorum! 📊"
        ),
    ).send()


# ── Message Handler ───────────────────────────────────────────────────────────
@cl.on_message
async def on_message(message: cl.Message):
    """Process a user message through the multi-agent Text2SQL pipeline."""
    user = cl.user_session.get("user")

    if not user:
        await cl.Message(content="⚠️ Lütfen önce giriş yapın.").send()
        return

    user_id = user.metadata.get("user_id", 0)
    role_type = user.metadata.get("role", "INDIVIDUAL")
    jwt_token = user.metadata.get("token", "")

    # Build initial state
    initial_state: AgentState = {
        "question": message.content,
        "user_id": user_id,
        "corp_id": user.metadata.get("corp_id", 0),
        "role_type": role_type,
        "jwt_token": jwt_token,
        "sql_query": None,
        "query_result": None,
        "error": None,
        "final_answer": None,
        "visualization_code": None,
        "is_in_scope": False,
        "iteration_count": 0,
    }

    # Show a thinking indicator
    thinking_msg = cl.Message(content="🔄 Sorgunuz işleniyor...")
    await thinking_msg.send()

    try:
        # Invoke the LangGraph pipeline
        result = app.invoke(initial_state)

        # Remove the thinking message
        await thinking_msg.remove()

        # Build response
        answer = result.get("final_answer") or "Bir sonuç üretilemedi."
        elements = []

        # If there's visualization code (JSON), parse and render it
        viz_code = result.get("visualization_code")
        if viz_code:
            try:
                fig = pio.from_json(viz_code)
                if fig:
                    fig_html = pio.to_html(fig, full_html=False, include_plotlyjs="cdn")
                    elements.append(
                        cl.Html(name="chart", display="inline", content=fig_html)
                    )
                    logger.info("Plotly chart rendered successfully.")
            except Exception as ve:
                logger.error("Visualization rendering failed: %s", str(ve)[:300])
                # Don't fail the whole response — just skip the chart

        await cl.Message(content=answer, elements=elements).send()

    except Exception as e:
        await thinking_msg.remove()
        logger.error("Pipeline error: %s", str(e)[:500])
        await cl.Message(
            content="⚠️ Bir hata oluştu. Lütfen tekrar deneyin."
        ).send()
