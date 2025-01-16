from datetime import datetime

from cozepy import load_oauth_app_from_config, JWTOAuthApp

COZE_OAUTH_CONFIG_PATH = "coze_oauth_config.json"


def load_coze_oauth_app(config_path) -> JWTOAuthApp:
    with open(config_path, "r") as file:
        config = file.read()

    return load_oauth_app_from_config(config)


def timestamp_to_datetime(timestamp: int) -> str:
    return datetime.fromtimestamp(timestamp).strftime("%Y-%m-%d %H:%M:%S")


if __name__ == "__main__":
    coze_oauth_app = load_coze_oauth_app(COZE_OAUTH_CONFIG_PATH)

    oauth_token = coze_oauth_app.get_access_token(session_name="biz_user_id")
    print(f"[jwt-oauth] token_type: {oauth_token.token_type}")
    print(f"[jwt-oauth] access_token: {oauth_token.access_token}")
    expires_str = timestamp_to_datetime(oauth_token.expires_in)
    print(f"[jwt-oauth] expires_in: {oauth_token.expires_in} ({expires_str})")
