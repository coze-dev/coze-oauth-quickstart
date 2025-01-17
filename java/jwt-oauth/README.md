# Coze OAuth Examples

This repository contains examples of different OAuth flows for Coze API authentication.

## Prerequisites

- Java 11 or higher
- Gradle
- A Coze API account with client credentials

## Configuration

Each example requires config file to be set with your Coze API credentials:

### JWT OAuth

### Set Environment Variables

To run the JWT OAuth example, set the following config file:

The configuration file should be a JSON file, named coze_oauth_config.json with the following format:
```json
{
  "client_type": "server",
  "client_id": "{client_id}",
  "private_key": "{private_key}",
  "public_key_id": "{public_key_id}",
  "coze_www_base": "https://www.coze.cn",
  "coze_api_base": "https://api.coze.cn"
}
```
This file should be placed in the jwt-auth directory.

#### Running the Examples

After configuring the config file, you can run the JWT OAuth example using:

```bash
sh bootstrap.sh
```