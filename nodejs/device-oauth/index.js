const { getDeviceCode, getDeviceToken } = require("@coze/api");
const fs = require("fs");
const path = require("path");

const configPath = path.join(__dirname, "coze_oauth_config.json");

// Load configuration file
function loadConfig() {
  // Check if configuration file exists
  if (!fs.existsSync(configPath)) {
    throw new Error(
      "Configuration file coze_oauth_config.json does not exist!"
    );
  }

  // Read configuration file
  const config = JSON.parse(fs.readFileSync(configPath, "utf8"));

  // Validate required fields
  const requiredFields = [
    "client_type",
    "client_id",
    "coze_www_base",
    "coze_api_base",
  ];

  for (const field of requiredFields) {
    if (!config[field]) {
      throw new Error(`Configuration file missing required field: ${field}`);
    }
    if (Array.isArray(config[field]) && config[field].length === 0) {
      throw new Error(`Configuration field ${field} cannot be an empty array`);
    }
    if (typeof config[field] === "string" && !config[field].trim()) {
      throw new Error(`Configuration field ${field} cannot be an empty string`);
    }
  }

  return config;
}

function timestampToDateTime(timestamp) {
  return new Date(timestamp * 1000).toLocaleString();
}

async function main() {
  try {
    const config = loadConfig();

    const deviceCode = await getDeviceCode({
      baseURL: config.coze_api_base,
      clientId: config.client_id,
    });
    console.log("Please visit the following url to authorize the app:");
    console.log(
      `    URL: ${
        deviceCode.verification_uri + "?user_code=" + deviceCode.user_code
      }`
    );
    console.log("");

    let deviceToken = await getDeviceToken({
      baseURL: config.coze_api_base,
      clientId: config.client_id,
      deviceCode: deviceCode.device_code,
      poll: true,
    });

    console.log(`[device-oauth] token_type: ${config.client_type}`);
    console.log(`[device-oauth] access_token: ${deviceToken.access_token}`);
    console.log(`[device-oauth] refresh_token: ${deviceToken.refresh_token}`);
    const expiresStr = timestampToDateTime(deviceToken.expires_in);
    console.log(
      `[device-oauth] expires_in: ${deviceToken.expires_in} (${expiresStr})`
    );
  } catch (error) {
    console.error("Error:", error);
    process.exit(1);
  }
}

main();
