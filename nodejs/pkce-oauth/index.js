const Koa = require("koa");
const Router = require("koa-router");
const bodyParser = require("koa-bodyparser");
const fs = require("fs");
const path = require("path");
const crypto = require("crypto");
const session = require("koa-session");
const {
  getPKCEAuthenticationUrl,
  getPKCEOAuthToken,
  refreshOAuthToken,
} = require("@coze/api");

const REDIRECT_URI = "http://127.0.0.1:8080/callback";
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
    "redirect_uris",
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

// Read and process HTML template
function renderTemplate(templatePath, variables) {
  try {
    let template = fs.readFileSync(templatePath, "utf8");

    // Replace all variables in {{variable}} format
    Object.keys(variables).forEach((key) => {
      const regex = new RegExp(`{{${key}}}`, "g");
      template = template.replace(regex, variables[key]);
    });

    return template;
  } catch (error) {
    console.error("Template rendering error:", error);
    throw error;
  }
}

// Utility function: Convert timestamp to date string
function timestampToDatetime(timestamp) {
  return new Date(timestamp * 1000).toLocaleString();
}

// Load configuration
const config = loadConfig();

const app = new Koa();
const router = new Router();

// Session configuration
const SESSION_CONFIG = {
  key: "koa:sess",
  maxAge: 86400000,
  autoCommit: true,
  overwrite: true,
  httpOnly: true,
  signed: true,
  rolling: false,
  renew: false,
};

// Set the keys for cookie signing
app.keys = [crypto.randomBytes(32).toString("hex")];

// Apply session middleware
app.use(session.createSession(SESSION_CONFIG, app));

// Use bodyParser middleware
app.use(bodyParser());

// Static file service middleware
app.use(async (ctx, next) => {
  if (ctx.path.startsWith("/assets/")) {
    try {
      // Point to websites/assets directory for static resources
      const filePath = path.join(__dirname, "websites", ctx.path);
      ctx.type = path.extname(filePath);
      ctx.body = fs.createReadStream(filePath);
    } catch (error) {
      console.error("Static resource access error:", error);
      ctx.status = 404;
    }
  } else {
    await next();
  }
});

// Home route
router.get("/", async (ctx) => {
  try {
    const templatePath = path.join(__dirname, "websites", "index.html");
    const variables = {
      coze_www_base: config.coze_www_base,
      client_type: config.client_type,
      client_id: config.client_id,
    };

    ctx.type = "html";
    ctx.body = renderTemplate(templatePath, variables);
  } catch (error) {
    console.error("Failed to render template:", error);
    ctx.status = 500;
    ctx.body = "Server Error: " + error.message;
  }
});

// Login route
router.get("/login", async (ctx) => {
  try {
    const { codeVerifier, url } = await getPKCEAuthenticationUrl({
      baseURL: config.coze_api_base,
      clientId: config.client_id,
      redirectUrl: REDIRECT_URI,
    });

    // Store code_verifier in session
    ctx.session.codeVerifier = codeVerifier;

    ctx.redirect(url);
  } catch (error) {
    console.error("Failed to generate authorization URL:", error);
    ctx.status = 500;
    ctx.body = renderTemplate(path.join(__dirname, "websites", "error.html"), {
      error: `Failed to generate authorization URL: ${error.message}`,
    });
  }
});

// OAuth callback route
router.get("/callback", async (ctx) => {
  const { code } = ctx.query;
  const codeVerifier = ctx.session.codeVerifier;

  if (!code) {
    ctx.status = 400;
    return (ctx.body = renderTemplate(
      path.join(__dirname, "websites", "error.html"),
      { error: "Authorization failed: No authorization code received" }
    ));
  }

  if (!codeVerifier) {
    ctx.status = 400;
    return (ctx.body = renderTemplate(
      path.join(__dirname, "websites", "error.html"),
      { error: "Authorization failed: No code verifier found" }
    ));
  }

  try {
    // Get access token using PKCE
    const oauthToken = await getPKCEOAuthToken({
      baseURL: config.coze_api_base,
      clientId: config.client_id,
      code: code,
      redirectUrl: REDIRECT_URI,
      codeVerifier: codeVerifier,
    });

    // Render callback page
    const expiresStr = timestampToDatetime(oauthToken.expires_in);
    ctx.body = renderTemplate(
      path.join(__dirname, "websites", "callback.html"),
      {
        token_type: "pkce",
        access_token: oauthToken.access_token,
        refresh_token: oauthToken.refresh_token,
        expires_in: `${oauthToken.expires_in} (${expiresStr})`,
      }
    );
  } catch (error) {
    console.error("Failed to get access token:", error);
    ctx.status = 500;
    ctx.body = renderTemplate(path.join(__dirname, "websites", "error.html"), {
      error: `Failed to get access token: ${error.message}`,
    });
  }
});

// Refresh token route
router.post("/refresh_token", async (ctx) => {
  try {
    const { refresh_token } = ctx.request.body;

    if (!refresh_token) {
      ctx.status = 400;
      return (ctx.body = { error: "No refresh token provided" });
    }

    // Refresh access token
    const oauthToken = await refreshOAuthToken({
      baseURL: config.coze_api_base,
      clientId: config.client_id,
      refreshToken: refresh_token,
    });

    const expiresStr = timestampToDatetime(oauthToken.expires_in);
    ctx.body = {
      token_type: "pkce",
      access_token: oauthToken.access_token,
      refresh_token: oauthToken.refresh_token,
      expires_in: `${oauthToken.expires_in} (${expiresStr})`,
    };
  } catch (error) {
    console.error("Failed to refresh token:", error);
    ctx.status = 500;
    ctx.body = { error: `Failed to refresh token: ${error.message}` };
  }
});

// Register routes
app.use(router.routes()).use(router.allowedMethods());

// Start server
const port = process.env.PORT || 8080;
app.listen(port, () => {
  console.log(`Server running on port http://127.0.0.1:${port}`);
});
