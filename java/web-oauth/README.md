# Coze OAuth Examples

This repository contains examples of different OAuth flows for Coze API authentication.

## Prerequisites

- Java 11 or higher
- Gradle
- A Coze API account with client credentials

## Configuration

Each example requires config file to be set with your Coze API credentials:

### Web OAuth

### Set Environment Variables

To run the Web OAuth example, set the following config file:

The configuration file should be a JSON file, named coze_oauth_config.json with the following format:
```json
{
  "client_type": "web",
  "client_id": "{client_id}",
  "client_secret": "{client_secret}",
  "redirect_uris": [
    "http://127.0.0.1:8080/redirect"
  ],
  "coze_www_base": "https://www.coze.cn",
  "coze_api_base": "https://api.coze.cn"
}
```

This file should be placed in the web-auth directory.

#### Running the Examples

After configuring the config file, you can run the WEB OAuth example using:

```bash
sh bootstrap.sh
```