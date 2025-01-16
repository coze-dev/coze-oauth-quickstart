from datetime import datetime

from cozepy import DeviceOAuthApp, load_oauth_app_from_config

COZE_OAUTH_CONFIG_PATH = "coze_oauth_config.json"


def load_coze_oauth_app(config_path) -> DeviceOAuthApp:
    with open(config_path, "r") as file:
        config = file.read()

    return load_oauth_app_from_config(config)


def timestamp_to_datetime(timestamp: int) -> str:
    return datetime.fromtimestamp(timestamp).strftime("%Y-%m-%d %H:%M:%S")


if __name__ == "__main__":
    coze_oauth_app = load_coze_oauth_app(COZE_OAUTH_CONFIG_PATH)

    device_code = coze_oauth_app.get_device_code()
    print(f'Please visit the following url to authorize the app:')
    print(f'    URL: {device_code.verification_url}')
    print(f'')

    oauth_token = coze_oauth_app.get_access_token(device_code=device_code.device_code, poll=True)
    print(f"[device-oauth] token_type: {oauth_token.token_type}")
    print(f"[device-oauth] access_token: {oauth_token.access_token}")
    print(f"[device-oauth] refresh_token: {oauth_token.refresh_token}")
    expires_str = timestamp_to_datetime(oauth_token.expires_in)
    print(f"[device-oauth] expires_in: {oauth_token.expires_in} ({expires_str})")
