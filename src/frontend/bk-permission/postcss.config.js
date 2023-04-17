const fs = require('fs');
const path = require('path');
const { CachedInputFileSystem, ResolverFactory } = require('enhanced-resolve');

const myResolver = ResolverFactory.createResolver({
  alias: {
    '@': path.resolve(__dirname, './lib/client/src'),
  },
  preferRelative: true,
  fileSystem: new CachedInputFileSystem(fs, 4000),
  useSyncFileSystemCalls: true,
  extensions: ['.css'],
});

module.exports = {
  plugins: [
    [
      'postcss-import',
      {
        resolve(id, baseDir) {
          return myResolver.resolveSync({}, baseDir, id);
        },
      },
    ],
    'postcss-simple-vars',
    'postcss-mixins',
    'postcss-nested-ancestors',
    'postcss-nested',
    'postcss-preset-env',
    'postcss-url',
  ],
};
