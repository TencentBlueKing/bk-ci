const path = require('path')

module.exports = {
  host: process.env.BK_APP_HOST,
  port: process.env.BK_APP_PORT,
  publicPath: process.env.BK_STATIC_URL,
  outputDir: process.env.BK_OUTPUT_DIR,
  cache: true,
  open: false,
  replaceStatic: {
    key: process.env.BK_STATIC_URL
  },
  typescript: true,
  parseNodeModules: false,
  resource: {
    main: {
      entry: './src/main',
      html: {
        filename: process.env.BK_HTML_NAME,
        template: './index.html',
      },
    },
  },
  chainWebpack (config) {

    config.resolve
      .alias
      .clear()
      .set('@', path.join(__dirname, 'src'))

    config.resolve
      .modules
      .clear()
      .add('node_modules')
      .add(path.join(__dirname, 'node_modules'))

    config.resolveLoader
      .modules
      .clear()
      .add('node_modules')
      .add(path.join(__dirname, 'node_modules'))

    return config
  }
};
