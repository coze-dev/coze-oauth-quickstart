import json
import secrets
from datetime import datetime

from cozepy import load_oauth_app_from_config, WebOAuthApp, Coze, TokenAuth
from flask import Flask, redirect, request, session

app = Flask(
    __name__,
    static_folder="assets",  # use shared/assets as static directory
    static_url_path="/assets",
)  # URL path for static files
app.secret_key = secrets.token_hex(16)  # for Flask session encryption

COZE_OAUTH_CONFIG_PATH = "coze_oauth_config.json"
REDIRECT_URI = "http://127.0.0.1:8080/callback"


def load_app_config(config_path) -> dict:
    with open(config_path, "r") as file:
        config = file.read()
    return json.loads(config)


def load_coze_oauth_app(config_path) -> WebOAuthApp:
    try:
        with open(config_path, "r") as file:
            config = json.loads(file.read())
        return load_oauth_app_from_config(config)  # type: ignore
    except FileNotFoundError:
        raise Exception(
            f"Configuration file not found: {config_path}. Please make sure you have created the OAuth configuration file."
        )
    except Exception as e:
        raise Exception(f"Failed to load OAuth configuration: {str(e)}")


def timestamp_to_datetime(timestamp: int) -> str:
    return datetime.fromtimestamp(timestamp).strftime("%Y-%m-%d %H:%M:%S")


def read_html_template(file_path: str) -> str:
    with open(file_path, "r", encoding="utf-8") as file:
        return file.read()


def render_template(template: str, kwargs: dict) -> str:
    if not kwargs:
        kwargs = {}
    kwargs["coze_www_base"] = app_config["coze_www_base"]
    kwargs["coze_api_base"] = app_config["coze_api_base"]
    template = read_html_template(template)
    for key, value in kwargs.items():
        template = template.replace(f"{{{{{key}}}}}", str(value))
    return template


app_config = load_app_config(COZE_OAUTH_CONFIG_PATH)
coze_oauth_app = load_coze_oauth_app(COZE_OAUTH_CONFIG_PATH)


@app.errorhandler(Exception)
def handle_error(error):
    error_message = str(error)
    print(f"Error occurred: {error_message}")
    return render_template("websites/error.html", {"error": error_message})


@app.route("/")
def index():
    if not coze_oauth_app:
        return render_template(
            "websites/error.html",
            {
                "error": "OAuth application is not properly configured. Please check your configuration file.",
            },
        )
    return render_template("websites/index.html", app_config)


@app.route("/login")
def login():
    if not coze_oauth_app:
        return render_template(
            "websites/error.html",
            {
                "error": "OAuth application is not properly configured. Please check your configuration file.",
            },
        )
    try:
        auth_url = coze_oauth_app.get_oauth_url(redirect_uri=REDIRECT_URI)
        return redirect(auth_url)
    except Exception as e:
        return render_template(
            "websites/error.html",
            {
                "error": f"Failed to generate authorization URL: {str(e)}",
            },
        )


@app.route("/callback")
def callback():
    if not coze_oauth_app:
        return render_template(
            "websites/error.html",
            {
                "error": "OAuth application is not properly configured. Please check your configuration file.",
            },
        )

    code = request.args.get("code")
    if not code:
        return render_template(
            "websites/error.html",
            {
                "error": "Authorization failed: No authorization code received",
            },
        )

    try:
        oauth_token = coze_oauth_app.get_access_token(
            redirect_uri=REDIRECT_URI, code=code
        )
        # 将 OAuth token 保存到 session 中，以便后续使用
        session[f'oauth_token_{app_config["client_id"]}'] = {
            "token_type": oauth_token.token_type,
            "access_token": oauth_token.access_token,
            "refresh_token": oauth_token.refresh_token,
            "expires_in": oauth_token.expires_in,
        }

        expires_str = timestamp_to_datetime(oauth_token.expires_in)
        return render_template(
            "websites/callback.html",
            {
                "token_type": oauth_token.token_type,
                "access_token": oauth_token.access_token,
                "refresh_token": oauth_token.refresh_token,
                "expires_in": f"{oauth_token.expires_in} ({expires_str})",
            },
        )
    except Exception as e:
        return render_template(
            "websites/error.html",
            {
                "error": f"Failed to get access token: {str(e)}",
            },
        )


@app.route("/refresh_token", methods=["POST"])
def refresh_token():
    if not coze_oauth_app:
        return {
            "error": "OAuth application is not properly configured",
        }, 500

    try:
        data = request.get_json()
        refresh_token = data.get("refresh_token")
        if not refresh_token:
            return {"error": "No refresh token provided"}, 400

        oauth_token = coze_oauth_app.refresh_access_token(refresh_token=refresh_token)

        # 更新 session 中的 token
        session[f'oauth_token_{app_config["client_id"]}'] = {
            "token_type": oauth_token.token_type,
            "access_token": oauth_token.access_token,
            "refresh_token": oauth_token.refresh_token,
            "expires_in": oauth_token.expires_in,
        }

        expires_str = timestamp_to_datetime(oauth_token.expires_in)
        return {
            "token_type": oauth_token.token_type,
            "access_token": oauth_token.access_token,
            "refresh_token": oauth_token.refresh_token,
            "expires_in": f"{oauth_token.expires_in} ({expires_str})",
        }
    except Exception as e:
        return {"error": f"Failed to refresh token: {str(e)}"}, 500


@app.route("/users_me")
def users_me():
    access_token = request.args.get("access_token")
    if not access_token:
        return render_template(
            "websites/error.html", {"error": "Access token is required"}
        )
    coze = Coze(auth=TokenAuth(access_token), base_url=app_config["coze_api_base"])

    try:
        user = coze.users.me()
        return {
            "user_id": user.user_id,
            "user_name": user.user_name,
            "nick_name": user.nick_name,
            "avatar_url": user.avatar_url,
        }
    except Exception as e:
        return {"error": f"Failed to get user info: {str(e)}"}, 500


if __name__ == "__main__":
    app.run(debug=False, use_reloader=False, port=8080)
