const path = require('path')
module.exports = {
  publicPath: process.env.NODE_ENV === 'production' ? '/repo/' : '/',
  outputDir: path.join(__dirname, '../frontend/repo'),
  chainWebpack: config => {
    config
      .plugin('html')
      .tap(args => {
        const isProd = process.env.NODE_ENV === 'production'
        args[0].filename = isProd ? `frontend#ui#index.html` : `index.html`
        args[0].title = '制品管理'
        args[0].isCi = process.env.VUE_APP_MODE_CONFIG === 'ci'
        args[0].externalUrl = process.env.VUE_APP_EXTERNAL_URL

        return args
      })
  },
}
