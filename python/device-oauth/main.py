import json
from datetime import datetime

from cozepy import DeviceOAuthApp, load_oauth_app_from_config, Coze, TokenAuth, User

COZE_OAUTH_CONFIG_PATH = "coze_oauth_config.json"


def load_app_config(config_path) -> dict:
    with open(config_path, "r") as file:
        config = file.read()
    return json.loads(config)


def load_coze_oauth_app(config_path) -> DeviceOAuthApp:
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


app_config = load_app_config(COZE_OAUTH_CONFIG_PATH)
coze_oauth_app = load_coze_oauth_app(COZE_OAUTH_CONFIG_PATH)


def users_me(access_token) -> User:
    coze = Coze(auth=TokenAuth(access_token), base_url=app_config["coze_api_base"])

    return coze.users.me()


if __name__ == "__main__":
    coze_oauth_app = load_coze_oauth_app(COZE_OAUTH_CONFIG_PATH)

    device_code = coze_oauth_app.get_device_code()
    print("Please visit the following url to authorize the app:")
    print(f"    URL: {device_code.verification_url}")
    print("")

    oauth_token = coze_oauth_app.get_access_token(
        device_code=device_code.device_code, poll=True
    )
    print(f"[device-oauth] token_type: {oauth_token.token_type}")
    print(f"[device-oauth] access_token: {oauth_token.access_token}")
    print(f"[device-oauth] refresh_token: {oauth_token.refresh_token}")
    expires_str = timestamp_to_datetime(oauth_token.expires_in)
    print(f"[device-oauth] expires_in: {oauth_token.expires_in} ({expires_str})")

    user = users_me(oauth_token.access_token)
    print(f"[user_info] user_id: {user.user_id}")
    print(f"[user_info] user_name: {user.user_name}")
    print(f"[user_info] nick_name: {user.nick_name}")
