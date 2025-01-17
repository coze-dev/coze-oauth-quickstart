# Coze OAuth Examples

This repository contains examples of different OAuth flows for Coze API authentication.

## Prerequisites

- Java 11 or higher
- Gradle
- A Coze API account with client credentials

## Project Structure

The repository contains 4 OAuth example implementations:

- `web-oauth` - Standard OAuth 2.0 flow for web applications
- `pkce-oauth` - OAuth with PKCE for mobile/native applications 
- `client-oauth` - Client credentials flow for server-to-server applications
- `device-oauth` - Device authorization flow for limited input devices

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
cd web-oauth
sh bootstrap.sh
```

### Device OAuth

### Set Environment Variables

To run the Device OAuth example, set the following config file:

The configuration file should be a JSON file, named coze_oauth_config.json with the following format:
```json
{
  "client_type": "device",
  "client_id": "{client_id}",
  "coze_www_base": "https://www.coze.cn",
  "coze_api_base": "https://api.coze.cn"
}
```
This file should be placed in the device-auth directory.

#### Running the Examples

After configuring the config file, you can run the Device OAuth example using:

```bash
cd device-oauth
sh bootstrap.sh
```

### PKCE OAuth

### Set Environment Variables

To run the PKCE OAuth example, set the following config file:

The configuration file should be a JSON file, named coze_oauth_config.json with the following format:
```json
{
  "client_type": "single_page",
  "client_id": "{client_id}",
  "redirect_uris": [
    "http://127.0.0.1:8080/redirect"
  ],
  "coze_www_base": "https://www.coze.cn",
  "coze_api_base": "https://api.coze.cn"
}
```
This file should be placed in the pkce-auth directory.

#### Running the Examples

After configuring the config file, you can run the PKCE OAuth example using:

```bash
cd pkce-oauth
sh bootstrap.sh
```

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
cd jwt-oauth
sh bootstrap.sh
```