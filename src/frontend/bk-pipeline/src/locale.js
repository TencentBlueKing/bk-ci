// Vue 2.7 和 Vue 3 兼容的导入
// 在 ES 模块环境中，不直接导入 Vue，而是通过运行时检测
let Vue = null
try {
    // 尝试从全局获取 Vue（UMD 环境）
    if (typeof window !== "undefined" && window.Vue) {
        Vue = window.Vue
    } else if (typeof global !== "undefined" && global.Vue) {
        Vue = global.Vue
    }
} catch (e) {
    // 忽略错误
}

import bkPipelineLocales from "./i18n.json"

const DEFAULT_LOCALE = "zh-CN"
const I18N_SCOPE = "bk-pipeline"
let lang = bkPipelineLocales[DEFAULT_LOCALE]

// 存储 i18n 实例的引用（用于 Vue 3）
let i18nInstance = null

export function loadI18nMessages (i18n) {
    try {
        i18nInstance = i18n
        // Vue 3: i18n.global.locale.value
        // Vue 2.7: i18n.locale
        const locale
      = i18n?.global?.locale?.value
      || i18n?.locale?.value
      || i18n?.locale
      || DEFAULT_LOCALE
        const ditc = {
            [I18N_SCOPE]:
        bkPipelineLocales[locale] || bkPipelineLocales[DEFAULT_LOCALE],
        }
        lang = bkPipelineLocales[locale] || bkPipelineLocales[DEFAULT_LOCALE]

        if (i18n) {
            // Vue 3: i18n.global.setLocaleMessage()
            // Vue 2.7: i18n.setLocaleMessage()
            if (i18n.global) {
                // Vue 3
                const currentMessages = i18n.global.getLocaleMessage(locale) || {}
                i18n.global.setLocaleMessage(locale, {
                    ...currentMessages,
                    ...ditc,
                })
            } else if (i18n.setLocaleMessage) {
                // Vue 2.7
                const currentMessages = i18n.messages?.[locale] || {}
                i18n.setLocaleMessage(locale, {
                    ...currentMessages,
                    ...ditc,
                })
            }
        }
    } catch (error) {
        console.log("[bk-pipeline] loadI18nMessages error:", error)
    }
}

export function t (key, ...args) {
    // 优先使用传入的 i18n 实例（Vue 3）
    if (i18nInstance) {
        if (i18nInstance.global) {
            // Vue 3: i18n.global.t()
            const result = i18nInstance.global.t(`${I18N_SCOPE}.${key}`, ...args)
            if (
                result
        && typeof result === "string"
        && result !== `${I18N_SCOPE}.${key}`
            ) {
                return result
            }
        } else if (i18nInstance.t) {
            // Vue 2.7: i18n.t()
            const result = i18nInstance.t(`${I18N_SCOPE}.${key}`, ...args)
            if (
                result
        && typeof result === "string"
        && result !== `${I18N_SCOPE}.${key}`
            ) {
                return result
            }
        }
    }

    // 回退到 Vue 实例的 $t (Vue 2.7)
    try {
        const translate = this?.$t || (Vue && Vue.prototype && Vue.prototype.$t)
        if (typeof translate === "function") {
            const result = translate.call(
                this || {},
                `${I18N_SCOPE}.${key}`,
                ...args
            )
            if (
                result
        && typeof result === "string"
        && result !== `${I18N_SCOPE}.${key}`
            ) {
                return result
            }
        }
    } catch (e) {
    // 忽略错误，继续使用 fallback
    }

    // 最后的 fallback: 从本地语言包获取
    const keyPath = key.split(".")
    let result = lang
    for (const k of keyPath) {
        if (result && typeof result === "object" && k in result) {
            result = result[k]
        } else {
            return key // 如果找不到，返回原始 key
        }
    }
    return result || key
}

export function useLang (l) {
    lang = l || lang
}

export const localeMixins = {
    methods: {
        t,
    },
}
