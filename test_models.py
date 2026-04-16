import google.generativeai as genai
genai.configure(api_key='AIzaSyCwEgvgr9WZ487SGERyPNFxoy_2ZIBWY3A')
models=['gemini-2.0-flash-lite', 'gemini-flash-lite-latest', 'gemini-pro-latest', 'gemini-2.0-flash', 'gemini-2.0-flash-001']
for model_name in models:
    try:
        m = genai.GenerativeModel(model_name)
        text = m.generate_content('hi').text[:10]
        print(f"{model_name}: SUCCESS ({text})")
    except Exception as e:
        print(f"{model_name}: ERROR {str(e)[:150]}")
