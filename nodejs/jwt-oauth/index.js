const Koa = require("koa");
const Router = require("koa-router");
const bodyParser = require("koa-bodyparser");
const fs = require("fs");
const path = require("path");
const { getJWTToken } = require("@coze/api");
const cors = require('@koa/cors');

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
    "public_key_id",
    "private_key",
    "coze_www_base",
    "coze_api_base",
  ];

  for (const field of requiredFields) {
    if (!config[field]) {
      throw new Error(`Configuration file missing required field: ${field}`);
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

// Add CORS middleware
app.use(cors());

// Use bodyParser middleware to parse POST request body
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
    console.error("Server Error:", error);
    ctx.status = 500;
    ctx.body = "Server Error: " + error.message;
  }
});

// Login route
router.get("/callback", async (ctx) => {
  try {
    // Get JWT OAuth token directly instead of redirecting
    const oauthToken = await getJWTToken({
      baseURL: config.coze_api_base,
      appId: config.client_id,
      aud: new URL(config.coze_api_base).host,
      keyid: config.public_key_id,
      privateKey: config.private_key,
    });

    // Check if it's an AJAX request
    if (ctx.get("X-Requested-With") === "XMLHttpRequest") {
      ctx.body = {
        token_type: oauthToken.token_type,
        access_token: oauthToken.access_token,
        refresh_token: "",
        expires_in: `${oauthToken.expires_in} (${timestampToDatetime(
          oauthToken.expires_in
        )})`,
      };
      return;
    }

    // Render callback page directly with token info
    const expiresStr = timestampToDatetime(oauthToken.expires_in);
    ctx.body = renderTemplate(
      path.join(__dirname, "websites", "callback.html"),
      {
        token_type: config.client_type,
        access_token: oauthToken.access_token,
        refresh_token: "", // JWT OAuth doesn't use refresh tokens
        expires_in: `${oauthToken.expires_in} (${expiresStr})`,
      }
    );
  } catch (error) {
    console.error("Failed to get JWT OAuth token:", error);
    ctx.status = 500;
    ctx.body = renderTemplate(path.join(__dirname, "websites", "error.html"), {
      error: `Failed to get JWT OAuth token: ${error.message}`,
    });
  }
});

router.get("/login", async (ctx) => {
  ctx.redirect("/callback");
});

// Register routes
app.use(router.routes()).use(router.allowedMethods());

// Start server
const port = process.env.PORT || 8080;
app.listen(port, () => {
  console.log(`Server running on port http://127.0.0.1:${port}`);
});
