module.exports = {
  presets: [
    '@vue/cli-plugin-babel/preset',
    ['@babel/preset-typescript', { isTSX: true, allExtensions: true }]
  ],
  plugins: [
    '@vue/babel-plugin-jsx',
    '@babel/plugin-transform-class-static-block'
  ]
}
