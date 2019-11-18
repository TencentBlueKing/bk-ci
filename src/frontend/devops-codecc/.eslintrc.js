module.exports = {
    // vue 相关的规则，加载 vue 规则会自动加载 es6 的规则
    // 如果不需要 vue 仅仅只是检测 es6 那么配置 @tencent/eslint-config-bk/index 即可
    extends: [
        // 'eslint-config-bk/vue'
    ],

    // vue plugin，不需要自行安装 eslint-plugin-vue，安装 @tencent/eslint-config-bk 时会把 eslint 相关的包全部安装
    // 如果不需要 vue 仅仅只是检测 es6 那么这里可以不用配置 eslint-plugin-vue
    plugins: [
        'vue'
    ],

    // 代码中的全局变量，key 为全局变量名称，value 为 true 允许被重写，为 false 不允许被重写
    globals: {
        // value 为 true 允许被重写，为 false 不允许被重写
        NODE_ENV: true,
        SITE_URL: true,
        DEVOPS_SITE_URL: true,
        PAAS_SERVICE_URL: true,
        AJAX_URL_PREFIX: true,
        MAIN_SITE_URL: true,
        RELEASE_VERSION: true,
        CodeMirror: false
    },

    // 添加自定义的规则以及覆盖 @tencent/eslint-config-bk 里的规则
    // @tencent/eslint-config-bk 里所有的规则在这里
    rules: {
    },

    // vue/script-indent 规则只会检测 .vue 文件里的 script 而不会去检测 .js 文件，
    // 但是 eslint 默认的 indent 规则会检测 .js 文件和 .vue 文件，为了不让 vue/script-indent 和 indent 相互干扰，
    // 所以这里需要配置 overrides，让 eslint indent 的规则不用检测 .vue 文件
    overrides: [
        {
            files: ['*.vue'],
            rules: {
                indent: 'off'
            }
        }
    ]
}
