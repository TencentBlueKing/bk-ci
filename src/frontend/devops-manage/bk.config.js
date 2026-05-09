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

    config.resolveLoader
      .modules
      .clear()
      .add('node_modules')
      .add(path.join(__dirname, 'node_modules'))

      
    config.merge({
      resolve: {
        alias: {
          vue$: path.join(__dirname, 'node_modules/vue/index.js'),
        },
      },
      devServer: {
          server: { 
            type: 'https',
            options: {
              key: '../local.bk-tenant-dev.woa.com+3-key.pem',
              cert: '../local.bk-tenant-dev.woa.com+3.pem',
            }
          }
      }
    })

    return config
  }
};
