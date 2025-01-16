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

Each example requires environment variables to be set with your Coze API credentials:

### Web OAuth

### Set Environment Variables

To run the Web OAuth example, set the following environment variables:

- `WEB_OAUTH_CONFIG_PATH` - Path to the Web OAuth configuration file
The configuration file should be a YAML file with the following format:
```yaml
app:
  client_id: "857890086xxxxxx3.app.coze"
  client_secret: "2N0YlVDT5j44dr22Nx8mKBIxxxxx"
  coze_api_base: "https://api.coze.cn"
```

#### Running the Examples

After configuring the environment variables, you can run the WEB OAuth example using:

```bash
./gradlew :web-oauth:run
```

### Device OAuth

### Set Environment Variables

To run the Device OAuth example, set the following environment variables:

- `DEVICE_OAUTH_CONFIG_PATH` - Path to the Device OAuth configuration file
The configuration file should be a YAML file with the following format:
```yaml
app:
  client_id: "857890086xxxxxx3.app.coze"
  coze_api_base: "https://api.coze.cn"
```

#### Running the Examples

After configuring the environment variables, you can run the Device OAuth example using:

```bash
./gradlew :device-oauth:run
```

### PKCE OAuth

### Set Environment Variables

To run the PKCE OAuth example, set the following environment variables:

- `PKCE_OAUTH_CONFIG_PATH` - Path to the PKCE OAuth configuration file
The configuration file should be a YAML file with the following format:
```yaml
app:
  client_id: "857890086xxxxxx3.app.coze"
  coze_api_base: "https://api.coze.cn"
```

#### Running the Examples

After configuring the environment variables, you can run the PKCE OAuth example using:

```bash
./gradlew :pkce-oauth:run
```

### JWT OAuth

### Set Environment Variables

To run the JWT OAuth example, set the following environment variables:

- `JWT_OAUTH_CONFIG_PATH` - Path to the JWT OAuth configuration file
The configuration file should be a YAML file with the following format:
```yaml
app:
  client_id: "1145*****"
  private_key: "-----BEGIN PRIVATE KEY-----\n
    MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCzyhyJYD+pefuC\n
    7xaxlMC5ZiaRctfTW6o5TpZEvhiSEG46UFTlrfECgYA8aHcS1fpqW7VtNv12VplA\n
    ***************************************************************\n
    -----END PRIVATE KEY-----\n"
  public_key_id: "**************"
  coze_api_base: "https://api.coze.cn"
```

#### Running the Examples

After configuring the environment variables, you can run the JWT OAuth example using:

```bash
./gradlew :jwt-oauth:run
```