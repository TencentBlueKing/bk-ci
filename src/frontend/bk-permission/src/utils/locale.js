import bkPermissionLocales from './i18n.json'
import Vue from 'vue'

const DEFAULT_LOCALE = 'zh-CN'
const I18N_SCOPE = 'bk-permission'
let lang = bkPermissionLocales[DEFAULT_LOCALE]

export function loadI18nMessages (i18n) {
    try {
        const locale = i18n?.locale || DEFAULT_LOCALE
        const ditc = {
            [I18N_SCOPE]: bkPermissionLocales[locale]
        }
        lang = bkPermissionLocales[locale]
        if (i18n) {
            const { messages } = i18n
        
            i18n.setLocaleMessage(locale, {
                ...messages[locale],
                ...ditc
            })
        }
    } catch (error) {
        console.log(error)
    }
}

export function t (key, ...args) {
    const translate = Object.getPrototypeOf(this || Vue).$te
    if (typeof translate === 'function') {
        const result = translate.call(this, `${I18N_SCOPE}.${key}`, ...args)
        if (result && typeof result === 'string') return result
    }
    const keyPath = key.split('.')
    return keyPath.reduce((acc, key) => {
        acc = acc[key]
        return acc
    }, lang)
}

export const localeMixins = {
    methods: {
        t
    }
}
