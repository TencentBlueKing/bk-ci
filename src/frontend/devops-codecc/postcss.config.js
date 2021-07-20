/**
 * @file postcss 基本配置
 * @author blueking
 */

// https://github.com/michael-ciniawsky/postcss-load-config
module.exports = {
    plugins: {
        // 把 import 的内容转换为 inline
        // @see https://github.com/postcss/postcss-import#postcss-import
        'postcss-import': {},

        // mixins，本插件需要放在 postcss-simple-vars 和 postcss-nested 插件前面
        // @see https://github.com/postcss/postcss-mixins#postcss-mixins-
        'postcss-mixins': {
        },

        // 用于在 URL ( )上重新定位、内嵌或复制。
        // @see https://github.com/postcss/postcss-url#postcss-url
        'postcss-url': {
            url: 'rebase'
        },

        // cssnext 已经不再维护，推荐使用 postcss-preset-env
        'postcss-preset-env': {
            // see https://github.com/csstools/postcss-preset-env#options
            stage: 0,
            autoprefixer: {
                browsers: ['last 2 versions'],
                grid: true
            },
            browsers: ['last 2 versions']
        },
        // 这个插件可以在写 nested 样式时省略开头的 &
        // @see https://github.com/postcss/postcss-nested#postcss-nested-
        'postcss-nested': {},

        // 将 @at-root 里的规则放入到根节点
        // @see https://github.com/OEvgeny/postcss-atroot#postcss-at-root-
        'postcss-atroot': {},

        // 提供 @extend 语法
        // @see https://github.com/jonathantneal/postcss-extend-rule#postcss-extend-rule-
        'postcss-extend-rule': {},

        // 变量相关
        // @see https://github.com/jonathantneal/postcss-advanced-variables#postcss-advanced-variables-
        'postcss-advanced-variables': {
            // variables 属性内的变量为全局变量
        },

        // 类似于 stylus，直接引用属性而不需要变量定义
        // @see https://github.com/simonsmith/postcss-property-lookup#postcss-property-lookup-
        'postcss-property-lookup': {}
    }
}
