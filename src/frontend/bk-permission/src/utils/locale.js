import bkPermissionLocales from './i18n.json'

const DEFAULT_LOCALE = 'zh-CN'
const I18N_SCOPE = 'bk-permission'
let lang = bkPermissionLocales[DEFAULT_LOCALE]

export function loadI18nMessages (i18n) {
    try {
        const locale = i18n?.locale || DEFAULT_LOCALE
        lang = bkPermissionLocales[locale]
        const ditc = {
            [I18N_SCOPE]: lang
        }
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
