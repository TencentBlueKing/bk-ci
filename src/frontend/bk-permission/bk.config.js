const path = require('path');

module.exports = {
  host: process.env.BK_APP_HOST,
  port: process.env.BK_APP_PORT,
  publicPath: process.env.BK_STATIC_URL,
  cache: true,
  open: true,
  replaceStatic: true,
  target: 'lib',
  configureWebpack: {
    externals: {
      vue: 'vue'
    }
  },
  chainWebpack (config) {
    config.module
        .rule('svg')
        .clear();

    config.module
        .rule('svg-inline')
        .test(/\.(svg)(\?inline)?$/)
        .set('type', 'asset/inline');

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
    return config;
  }
};
