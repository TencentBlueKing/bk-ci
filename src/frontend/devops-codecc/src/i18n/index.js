import Vue from 'vue'
import VueI18n from 'vue-i18n'
import Cookies from 'js-cookie'
import langMessages from './lang/messages'
import { locale, lang } from 'bk-magic-vue'

Vue.use(VueI18n)

const languageMaps = {
    'zh_cn': 'zh-CN',
    'zh-cn': 'zh-CN',
    'zh': 'zh-CN'
}

const messages = {
    'zh-CN': Object.assign(lang.zhCN, langMessages['zh-CN']),
    'en': Object.assign(lang.enUS, langMessages['en'])
}

let curLocale = Cookies.get('blueking_language') || 'zh-CN'
curLocale = languageMaps.hasOwnProperty(curLocale) ? languageMaps[curLocale] : curLocale

// console.log(locale, messages)

const i18n = new VueI18n({
    locale: curLocale,
    fallbackLocale: 'zh-CN',
    messages,
    silentTranslationWarn: true, // 取消本地化失败时输出的警告
    missing (locale, path) {
        const parsedPath = i18n._path.parsePath(path)
        return parsedPath[parsedPath.length - 1]
    }
})

locale.i18n((key, value) => i18n.t(key, value))

export const toggleLang = () => {
    i18n.locale = i18n.locale === 'zh-CN' ? 'en' : 'zh-CN'

    Cookies.set('blueking_language', i18n.locale)
}

export const language = i18n.locale

export default i18n
