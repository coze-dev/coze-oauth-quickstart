import json
import secrets
from datetime import datetime

from cozepy import PKCEOAuthApp, load_oauth_app_from_config
from flask import Flask, redirect, request, session

app = Flask(__name__)
app.secret_key = secrets.token_hex(16)  # 用于 Flask session 加密

COZE_OAUTH_CONFIG_PATH = "coze_oauth_config.json"
REDIRECT_URI = "http://127.0.0.1:8080/callback"


def load_coze_oauth_app(config_path) -> PKCEOAuthApp:
    with open(config_path, "r") as file:
        config = file.read()
    return load_oauth_app_from_config(config)


def timestamp_to_datetime(timestamp: int) -> str:
    return datetime.fromtimestamp(timestamp).strftime("%Y-%m-%d %H:%M:%S")


coze_oauth_app = load_coze_oauth_app(COZE_OAUTH_CONFIG_PATH)


@app.route('/')
def index():
    return '''
        <h1>Coze PKCE OAuth Quickstart</h1>
        <p><a href="/login">Login</a></p>
    '''


@app.route('/login')
def login():
    code_verifier = secrets.token_urlsafe(16)
    session['code_verifier'] = code_verifier
    auth_url = coze_oauth_app.get_oauth_url(redirect_uri=REDIRECT_URI, code_verifier=code_verifier)
    return redirect(auth_url)


@app.route('/callback')
def callback():
    code = request.args.get('code')
    if not code:
        return 'Authorization failed: No authorization code received', 400
    
    code_verifier = session.get('code_verifier')
    if not code_verifier:
        return 'Authorization failed: No code verifier found', 400

    oauth_token = coze_oauth_app.get_access_token(redirect_uri=REDIRECT_URI, code=code,code_verifier=code_verifier)

    expires_str = timestamp_to_datetime(oauth_token.expires_in)
    token_info = {
        'token_type': oauth_token.token_type,
        'access_token': oauth_token.access_token,
        'expires_in': f"{oauth_token.expires_in} ({expires_str})"
    }

    return f'''
        <h2>Authorization successful!</h2>
        <pre>{json.dumps(token_info, indent=2, ensure_ascii=False)}</pre>
        <p><a href="/">Back to home</a></p>
    '''


if __name__ == "__main__":
    print("Starting pkce-oauth quickstart...")
    print("Please visit the following url to authorize the app:")
    print(f"    URL: http://127.0.0.1:8080")
    print("")
    print("")
    app.run(debug=True, port=8080)
