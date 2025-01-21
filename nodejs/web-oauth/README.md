# Coze OAuth Examples

This repository contains examples of different OAuth flows for Coze API authentication.

## Prerequisites

- Node.js 14 or higher
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
  "coze_api_base": "https://api.coze.cn"
}
```

This file should be placed in the web-auth directory.

#### Running the Examples

After configuring the config file, you can run the WEB OAuth example using:

```bash
# for mac/linux
sh bootstrap.sh

# for window
bootstrap.bat
```
