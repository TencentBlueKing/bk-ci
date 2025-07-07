import axios from 'axios'
import { lang, locale } from 'bk-magic-vue'
import cookies from 'js-cookie'
import VueI18n from 'vue-i18n'
const DEFAULT_LOCALE = window.INIT_LOCALE || 'zh-CN'
const LS_KEY = 'blueking_language'
const loadedModule = {}
const localeLabelMap = {
    'zh-CN': '中文',
    'zh-cn': '中文',
    'ja-JP': '日本語',
    cn: '中文',
    'en-US': 'English',
    'en-us': 'English',
    en: 'English',
    us: 'English'
}
const localeAliasMap = {
    'zh-CN': 'zh-CN',
    'zh-cn': 'zh-CN',
    'ja-JP': 'ja-JP',
    ja: 'ja-JP',
    zh_CN: 'zh-CN',
    cn: 'zh-CN',
    'en-US': 'en-US',
    'en-us': 'en-US',
    en: 'en-US',
    us: 'en-US',
    en_US: 'en-US'
}

const backendLocalEnum = {
    'zh-CN': 'zh_CN', // 简体中文
    'en-US': 'en_US', // 英文
    'zh-TW': 'zh_TW', // 繁体中文
    'ja-JP': 'ja_JP', // 日文
    'ko-KR': 'ko_KR', // 韩文
    'it-IT': 'it_IT', // 意大利文
    'de-DE': 'de_DE', // 德文
    'fr-FR': 'fr_FR' // 法文
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
            return acc
        }, []).slice(1)
    } catch (error) {
        return []
    }
}

function getLsLocale () {
    try {
        const cookieLocale = cookies.get(LS_KEY) || DEFAULT_LOCALE
        
        return localeAliasMap[cookieLocale.toLowerCase()] ?? DEFAULT_LOCALE
    } catch (error) {
        return DEFAULT_LOCALE
    }
}

function setLsLocale (locale) {
    let formateLocale = 'zh-cn'
    switch (localeAliasMap[locale]) {
        case 'en-US':
            formateLocale = 'en'
            break
        case 'ja-JP':
            formateLocale = 'ja'
            break
        default:
            formateLocale = 'zh-cn'
            break
    }
    if (typeof cookies.set === 'function') {
        const subDomains = getSubDoamin()
        subDomains.forEach(domain => {
            cookies.remove(LS_KEY, { domain, path: '/' })
        })
        const domain = window.LOCALE_DOMAIN || (subDomains[0] ?? location.hostname)
        cookies.set(LS_KEY, formateLocale, { domain, path: '/', expires: 365 })
    }
}

function getLanguageCode (lang) {
    const languageCodeMatch = lang.match(/^[A-Za-z]{2}/)
    
    if (languageCodeMatch) {
        return languageCodeMatch[0].toUpperCase()
    }

    return 'ZH'
}

export default (r, initSetLocale = false) => {
    const { messages, localeList } = importAll(r)
    
    const initLocale = getLsLocale()
    const lang = getLanguageCode(initLocale.split('_')[0].toLocaleUpperCase())
    
    const i18n = new VueI18n({
        locale: initLocale,
        fallbackLocale: initLocale,
        messages: localeList.reduce((acc, { key }) => {
            acc[key] = {
                ...lang[initLocale.replace('-', '')],
                ...messages[key]
            }
            return acc
        }, {})
    })
    locale.i18n((...args) => i18n.t(...args))
    setLocale(initLocale, initSetLocale)

    function dynamicLoadModule (module, locale = DEFAULT_LOCALE) {
        const localeModuleId = getLocalModuleId(module, locale)

        if (loadedModule[localeModuleId]) {
            return Promise.resolve()
        }
        return axios.get(`${window.PUBLIC_URL_PREFIX}/${module}/${locale}.json?t=${+new Date()}`, {
            crossdomain: true
        }).then(response => {
            const messages = response.data
            
            i18n.setLocaleMessage(locale, {
                ...i18n.messages[locale],
                [module]: messages
            })
            loadedModule[localeModuleId] = true
        })
    }

    async function setLocale (localeLang, initSetLocale) {
        Object.keys(loadedModule).forEach(mod => {
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
        
        if (initSetLocale && localeLang !== localeAliasMap[window.INIT_LOCALE]) {
            await syncLocaleBackend(localeLang)
        }
        
        return localeLang
    }

    async function syncLocaleBackend (localeLang) {
        try {
            const bkLocalEnum = {
                'zh-CN': 'zh-cn', // 简体中文
                'en-US': 'en' // 英文
            }
            console.log('sync backendLocalEnum', backendLocalEnum[localeLang], localeLang, bkLocalEnum[localeLang])
            await Promise.any([
                axios.put('/ms/project/api/user/locales/update', {
                    language: backendLocalEnum[localeLang] ?? localeLang
                }),
                jsonpLocale(bkLocalEnum[localeLang])
            ])
        } catch (error) {
            console.error('sync locale to backend error', error)
        }
    }
     
    return {
        lang,
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

function jsonpLocale (language) {
    if (!window.BK_PAAS_PRIVATE_URL) return
    return new Promise((resolve) => {
        try {
            const callbackName = `jsonp_callback_${Math.round(100000 * Math.random())}`
            window[callbackName] = function (data) {
                delete window[callbackName]
                document.body.removeChild(script)
                resolve(data)
            }

            const script = document.createElement('script')
            script.src = `${window.BK_PAAS_PRIVATE_URL}/api/c/compapi/v2/usermanage/fe_update_user_language?language=${language}&callback=${callbackName}`
            document.body.appendChild(script)
        } catch (e) {
            console.error('jsonp locale error', e)
            resolve(false)
        }
    })
}
