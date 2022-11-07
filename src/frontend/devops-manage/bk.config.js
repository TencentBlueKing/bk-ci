
module.exports = {
  host: process.env.BK_APP_HOST,
  port: process.env.BK_APP_PORT,
  publicPath: process.env.BK_STATIC_URL,
  outputDir: process.env.BK_OUTPUT_DIR,
  cache: true,
  open: false,
  replaceStatic: false,
  typescript: true,
};
