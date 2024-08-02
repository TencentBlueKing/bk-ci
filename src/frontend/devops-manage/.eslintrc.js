module.exports = {
  root: true,
  extends: ['@blueking/eslint-config-bk/tsvue3'],
  parserOptions: {
    tsconfigRootDir: __dirname,
    project: [
      './tsconfig.json',
    ],
  },
  ignorePatterns: [
    'dist/',
    'postcss.config.js',
    'bk.config.js',
    'index.html',
  ],
};
