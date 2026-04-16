from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import pandas as pd
import plotly.graph_objects as go
from graph import app as langgraph_app

app = FastAPI()

class ChatRequest(BaseModel):
    question: str
    role_type: str = "INDIVIDUAL"
    jwt_token: str = ""
    user_id: int = 0

@app.post("/api/chat")
async def chat_endpoint(request: ChatRequest):
    initial_state = {
        "question": request.question,
        "user_id": request.user_id,
        "role_type": request.role_type,
        "jwt_token": request.jwt_token,
        "sql_query": None,
        "query_result": None,
        "error": None,
        "final_answer": None,
        "visualization_code": None,
        "is_in_scope": False,
        "iteration_count": 0,
    }
    
    try:
        result = langgraph_app.invoke(initial_state)
        
        status = "IN_SCOPE" if result.get("is_in_scope") else "OUT_OF_SCOPE"
        answer = result.get("final_answer") or "Bir sonuç üretilemedi."
        
        chart_json_str = None
        viz_code = result.get("visualization_code")
        if viz_code:
            try:
                df = result.get("query_result")
                safe_globals = {
                    "go": go,
                    "pd": pd,
                    "data": df if isinstance(df, pd.DataFrame) else pd.DataFrame(),
                    "__builtins__": __builtins__,
                }
                exec(viz_code, safe_globals)
                fig = safe_globals.get("fig")
                if fig:
                    chart_json_str = fig.to_json()
            except Exception as e:
                print(f"Viz rendering failed: {e}")
                
        return {
            "status": status,
            "text": answer,
            "chart_json": chart_json_str
        }

    except Exception as e:
        print(f"Error in graph execution: {e}")
        raise HTTPException(status_code=500, detail=str(e))
