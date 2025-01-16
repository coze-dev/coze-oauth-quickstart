import json
import secrets
from datetime import datetime

from cozepy import load_oauth_app_from_config, JWTOAuthApp
from flask import Flask, redirect, session, request

app = Flask(__name__,
            static_folder='assets',  # use shared/assets as static directory
            static_url_path='/assets')  # URL path for static files
app.secret_key = secrets.token_hex(16)  # for Flask session encryption

COZE_OAUTH_CONFIG_PATH = "coze_oauth_config.json"
REDIRECT_URI = "http://127.0.0.1:8080/callback"


def load_app_config(config_path) -> dict:
    with open(config_path, "r") as file:
        config = file.read()
    return json.loads(config)


def load_coze_oauth_app(config_path) -> JWTOAuthApp:
    try:
        with open(config_path, "r") as file:
            config = file.read()
        return load_oauth_app_from_config(config)
    except FileNotFoundError:
        raise Exception(
            f"Configuration file not found: {config_path}. Please make sure you have created the OAuth configuration file.")
    except Exception as e:
        raise Exception(f"Failed to load OAuth configuration: {str(e)}")


def timestamp_to_datetime(timestamp: int) -> str:
    return datetime.fromtimestamp(timestamp).strftime("%Y-%m-%d %H:%M:%S")


app_config = load_app_config(COZE_OAUTH_CONFIG_PATH)
coze_oauth_app = load_coze_oauth_app(COZE_OAUTH_CONFIG_PATH)


def read_html_template(file_path: str) -> str:
    with open(file_path, 'r', encoding='utf-8') as file:
        return file.read()


def render_template(template: str, kwargs: dict) -> str:
    if not kwargs:
        kwargs = {}
    kwargs['coze_www_base'] = app_config['coze_www_base']
    template = read_html_template(template)
    for key, value in kwargs.items():
        template = template.replace(f'{{{{{key}}}}}', str(value))
    return template


@app.errorhandler(Exception)
def handle_error(error):
    error_message = str(error)
    print(f"Error occurred: {error_message}")
    return render_template('websites/error.html', {
        'error': error_message
    })


@app.route('/')
def index():
    if not coze_oauth_app:
        return render_template('websites/error.html', {
            'error': "OAuth application is not properly configured. Please check your configuration file."
        })
    return render_template('websites/index.html', app_config)


@app.route('/login')
def login():
    return redirect('/callback')


@app.route('/callback')
def callback():
    if not coze_oauth_app:
        return render_template('websites/error.html', {
            'error': "OAuth application is not properly configured. Please check your configuration file."
        })

    try:
        oauth_token = coze_oauth_app.get_access_token()
        # 将 OAuth token 保存到 session 中，以便后续使用
        session[f'oauth_token_{app_config["client_id"]}'] = {
            'token_type': oauth_token.token_type,
            'access_token': oauth_token.access_token,
            'expires_in': oauth_token.expires_in
        }

        expires_str = timestamp_to_datetime(oauth_token.expires_in)

        # 如果是 AJAX 请求，返回 JSON 格式
        if request.headers.get('X-Requested-With') == 'XMLHttpRequest':
            return {
                'token_type': oauth_token.token_type,
                'access_token': oauth_token.access_token,
                'refresh_token': '',
                'expires_in': f"{oauth_token.expires_in} ({expires_str})"
            }

        # 否则返回 HTML 页面
        return render_template('websites/callback.html', {
            'token_type': oauth_token.token_type,
            'access_token': oauth_token.access_token,
            'refresh_token': '',
            'expires_in': f"{oauth_token.expires_in} ({expires_str})"
        })
    except Exception as e:
        error_message = f"Failed to get access token: {str(e)}"
        if request.headers.get('X-Requested-With') == 'XMLHttpRequest':
            return {'error': error_message}, 500
        return render_template('websites/error.html', {
            'error': error_message
        })


if __name__ == "__main__":
    app.run(debug=False, use_reloader=False, port=8080)
