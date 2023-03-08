import VueI18n from 'vue-i18n'
import Vue from 'vue'
import { lang, locale } from 'bk-magic-vue'
import axios from 'axios'
import cookies from 'js-cookie'
const DEFAULT_LOCALE = window.INIT_LOCALE ?? 'zh-CN'
const LS_KEY = 'blueking_language'
const loadedModule = {}
const localeLabelMap = {
    'zh-CN': '中文',
    'zh-cn': '中文',
    'cn': '中文',
    'en-US': 'English',
    'en-us': 'English',
    'en': 'English',
    'us': 'English'
}
const localeAliasMap = {
    'zh-CN': 'zh-CN',
    'zh-cn': 'zh-CN',
    'cn': 'zh-CN',
    'en-US': 'en-US',
    'en-us': 'en-US',
    'en': 'en-US',
    'us': 'en-US'
}

function getSubDoamin () {
    try {
        return location.hostname.split('.').reduce((acc, _, index, list) => {
            const last = list.length - 1
            const item = list[last - index]
            if (index > 0) {
                acc.push([item, acc[index - 1]].join('.'))
            } else {
                acc.push(item)
            }
            console.log(acc)
            return acc
        }, []).slice(1)
    } catch (error) {
        return []
    }
}

function getLsLocale () {
    try {
        const cookieLcale = cookies.get(LS_KEY) || DEFAULT_LOCALE
        return localeAliasMap[cookieLcale.toLowerCase()] || DEFAULT_LOCALE
    } catch (error) {
        return DEFAULT_LOCALE
    }
}

function setLsLocale (locale) {
    const formateLocale = localeAliasMap[locale] === 'zh-CN' ? 'zh-cn' : 'en'
    if (typeof cookies.set === 'function') {
        const subDomains = getSubDoamin()
        subDomains.forEach(domain => {
            cookies.remove(LS_KEY, { domain, path: '/' })
        })
        cookies.set(LS_KEY, formateLocale, { domain: location.hostname, path: '/' })
    }
}

export default (r, initSetLocale = false) => {
    Vue.use(VueI18n)
    const { messages, localeList } = importAll(r)
    const initLocale = getLsLocale()
    // export localeList
    const i18n = new VueI18n({
        locale: initLocale,
        fallbackLocale: initLocale,
        messages
    })
    if (initSetLocale) {
        setLocale(initLocale)
    }

    locale.i18n((key, value) => i18n.t(key, value))

    function dynamicLoadModule (module, locale = DEFAULT_LOCALE) {
        const localeModuleId = getLocalModuleId(module, locale)
        if (loadedModule[localeModuleId]) {
            return Promise.resolve()
        }
        return axios.get(`/${module}/${locale}.json?t=${+new Date()}`, {
            crossdomain: true
        }).then(response => {
            const messages = response.data
            
            i18n.setLocaleMessage(locale, {
                ...i18n.messages[locale],
                [ module ]: messages
            })
            loadedModule[localeModuleId] = true
        })
    }

    function setLocale (localeLang) {
        Object.keys(loadedModule).map(mod => {
            const [, module] = mod.split('_')
            if (!loadedModule[getLocalModuleId(module, localeLang)]) {
                dynamicLoadModule(module, localeLang)
            }
        })
        i18n.locale = localeLang
        setLsLocale(localeLang)
        locale.use(lang[localeLang.replace('-', '')])
        axios.defaults.headers.common['Accept-Language'] = localeLang
        document.querySelector('html').setAttribute('lang', localeLang)
        
        return localeLang
    }
     
    return {
        i18n,
        setLocale,
        localeList,
        dynamicLoadModule
    }
}

function getLocalModuleId (module, locale) {
    return `${locale}_${module}`
}

function importAll (r) {
    const localeList = []
    const messages = r.keys().reduce((acc, key) => {
        const mod = r(key)
        
        const matchLocaleKey = key.match(/\/([\w-]+)?\.json$/)
        const localeKey = (matchLocaleKey ? matchLocaleKey[1] : '')
        if (localeKey) {
            acc[localeKey] = {
                ...lang[localeKey.replace('-', '')],
                ...mod
            }
            localeList.push({
                key: localeKey,
                label: localeLabelMap[localeKey]
            })
        }
        return acc
    }, {})

    return {
        localeList,
        messages
    }
}
