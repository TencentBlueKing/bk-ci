import VueI18n from 'vue-i18n'
import Vue from 'vue'
import { lang, locale } from 'bk-magic-vue'
import axios from 'axios'
const DEFAULT_LOCALE = 'zh-CN'
const loadedModule = {}

export default (r) => {
    Vue.use(VueI18n)
    const { messages, localeList } = importAll(r)

    // export localeList
    const i18n = new VueI18n({
        locale: DEFAULT_LOCALE,
        fallbackLocale: DEFAULT_LOCALE,
        messages
    })

    locale.i18n((key, value) => i18n.t(key, value))


    function dynamicLoadModule (module, locale = DEFAULT_LOCALE) {
        
        const localeModuleId = getLocalModuleId(module, locale)
        if (loadedModule[localeModuleId]) {
            return
        }
        console.log(`@locale/${module}/${locale}.js`)
        return axios.get(`${WEBSITE_URL}/${module}/${locale}.json`, {
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

    function setLocale (locale) {
        Object.keys(loadedModule).map(mod => {
            const [ , module ] = mod.split('_')
            if (!loadedModule[getLocalModuleId(module, locale)]) {
                dynamicLoadModule(module, locale)
            }
        })
        i18n.locale = locale
        axios.defaults.headers.common['Accept-Language'] = locale
        document.querySelector('html').setAttribute('lang', locale)
        return locale
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
    let localeList = []
    const messages = r.keys().reduce((acc, key) => {
        const mod = r(key)
        
        const matchLocaleKey = key.match(/\/([\w-]+)?\.json$/)
        const localeKey = (matchLocaleKey ? matchLocaleKey[1] : '')
        if (localeKey) {
            acc[localeKey] = {
                ...lang[localeKey.replace('-', '')],
                ...mod
            }
            
            localeList.push(localeKey)
        }
        return acc
    }, {});

    return {
        localeList,
        messages
    }
}