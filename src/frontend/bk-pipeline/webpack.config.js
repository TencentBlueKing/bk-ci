const path = require("path");
// const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin
const TerserPlugin = require("terser-webpack-plugin");

// 临时禁用 vue-template-compiler 的版本检查
// 因为我们需要在 Vue 3 环境中构建 Vue 2.7 兼容的组件
// 在 require vue-loader 之前，先 mock Vue 的版本，让 vue-loader 认为支持 <script setup>
const Module = require("module");
const originalRequire = Module.prototype.require;

// 先 mock Vue，让后续的 require 都能获取到 mock 的版本
let vueMocked = false;
if (!vueMocked) {
  try {
    const vue = require("vue");
    if (vue && vue.version && vue.version.startsWith("3.")) {
      // 修改 Vue 的 version 属性
      Object.defineProperty(vue, "version", {
        value: "2.7.16",
        writable: false,
        configurable: true,
      });
      vueMocked = true;
    }
  } catch (e) {
    // 忽略错误
  }
}

Module.prototype.require = function (id) {
  // Mock Vue 版本，让 vue-loader 认为支持 <script setup>
  if (
    id === "vue" ||
    id.endsWith("/vue") ||
    (id.includes("vue") &&
      !id.includes("vue-template-compiler") &&
      !id.includes("@vue/compiler"))
  ) {
    const vue = originalRequire.apply(this, arguments);
    // 如果 Vue 版本是 3.x，临时修改为 2.7，让 vue-loader 认为支持 <script setup>
    if (vue && vue.version && vue.version.startsWith("3.")) {
      Object.defineProperty(vue, "version", {
        value: "2.7.16",
        writable: false,
        configurable: true,
      });
    }
    return vue;
  }
  // 阻止 vue-loader 使用 @vue/compiler-sfc，强制使用 vue-template-compiler
  if (
    id.includes("@vue/compiler-sfc") ||
    id.includes("@vue/compiler-dom") ||
    id.includes("@vue/compiler-core")
  ) {
    // 返回 vue-template-compiler 的兼容接口
    return require("vue-template-compiler");
  }
  return originalRequire.apply(this, arguments);
};

const { VueLoaderPlugin } = require("vue-loader");

module.exports = (env = {}, argv) => {
  const isDev = argv.mode === "development";
  return {
    cache: {
      type: "filesystem",
      buildDependencies: {
        config: [__filename],
      },
    },
    entry: "./index.js",
    output: {
      library: {
        type: "umd",
        name: "bkPipeline",
      },
      filename: "bk-pipeline.min.js",
      path: path.resolve(__dirname, "dist"),
      clean: true,
    },
    externals: {
      vue: {
        commonjs: "vue",
        commonjs2: "vue",
        amd: "vue",
        root: "Vue",
      },
      "bk-magic-vue": {
        commonjs: "bk-magic-vue",
        commonjs2: "bk-magic-vue",
        amd: "bk-magic-vue",
        root: "bkMagic",
      },
      "vue-draggable-plus": {
        commonjs: "vue-draggable-plus",
        commonjs2: "vue-draggable-plus",
        amd: "vue-draggable-plus",
        root: "VueDraggablePlus",
      },
    },
    module: {
      rules: [
        {
          test: /\.vue$/,
          include: path.resolve("src"),
          loader: "vue-loader",
        },
        {
          test: /\.js$/,
          include: path.resolve("src"),
          loader: "babel-loader",
        },

        {
          test: /\.s?css$/,
          use: ["style-loader", "css-loader", "sass-loader"],
        },
        {
          test: /\.(js|vue)$/,
          loader: "eslint-loader",
          enforce: "pre",
          include: [path.resolve("src")],
          exclude: /node_modules/,
          options: {
            fix: true,
            formatter: require("eslint-friendly-formatter"),
          },
        },
      ],
    },
    plugins: [
      // new BundleAnalyzerPlugin(),
      new VueLoaderPlugin(),
      // new MiniCssExtractPlugin({
      //     filename: 'bk-pipeline.css',
      //     chunkFilename: '[id].css',
      //     ignoreOrder: true
      // })
    ],
    optimization: {
      chunkIds: isDev ? "named" : "deterministic",
      moduleIds: "deterministic",
      minimize: !isDev,
      removeEmptyChunks: true,
      minimizer: [
        new TerserPlugin({
          terserOptions: {
            format: {
              comments: false,
            },
          },
          extractComments: false,
        }),
      ],
    },
    resolve: {
      extensions: [".js", ".vue", ".json", ".ts", ".scss", ".css"],
      fallback: {
        path: false,
      },
      alias: {
        "@": path.resolve("src"),
        // 确保使用 vue-template-compiler 而不是 @vue/compiler-sfc
        "vue-template-compiler": path.resolve(
          __dirname,
          "node_modules/vue-template-compiler"
        ),
      },
    },
  };
};
