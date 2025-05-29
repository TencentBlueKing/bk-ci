module.exports = {
  root: true,
  extends: ['@blueking/eslint-config-bk/tsvue3'],
  parserOptions: {
    "ecmaVersion": "latest",
    project: [
      './tsconfig.json',
    ],
  },
  rules: {
    "indent": [
      "warn",
      2
    ]
  }
};