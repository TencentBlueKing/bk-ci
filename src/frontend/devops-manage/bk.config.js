const path = require('path')

module.exports = {
  host: process.env.BK_APP_HOST,
  port: process.env.BK_APP_PORT,
  publicPath: process.env.BK_STATIC_URL,
  outputDir: process.env.BK_OUTPUT_DIR,
  cache: true,
  open: false,
  replaceStatic: false,
  typescript: true,
  resource: {
    main: {
      entry: './src/main',
      html: {
        filename: 'frontend#manage#index.html',
        template: './index.html',
      },
    },
  },
  chainWebpack (config) {
    config.resolve
      .modules
      .clear()
      .add(path.join(__dirname, 'node_modules'))

    config.resolveLoader
      .modules
      .clear()
      .add(path.join(__dirname, 'node_modules'))

    return config
  }
};
