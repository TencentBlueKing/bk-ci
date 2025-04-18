import { getCookie } from '@/common/util';
import { createI18n } from 'vue-i18n';
import ZhCN from '../../../locale/platform/zh-CN.json';
import EnUS from '../../../locale/platform/en-US.json';

const LS_KEY = 'blueking_language';
export const ZH_LOCALE = 'zh-CN';
export const EN_LOCALE = 'en-US';
const DEFAULT_LANG = ZH_LOCALE;
const localeAliasMap = {
    'zh-cn': 'zh-CN',
    zh_cn: 'zh-CN',
    cn: 'zh-CN',
    'en-us': 'en-US',
    en: 'en-US',
    us: 'en-US',
    en_us: 'en-US',
};
function getLsLocale() {
    try {
        const cookieLocale = getCookie(LS_KEY) || DEFAULT_LANG;

        return localeAliasMap[cookieLocale.toLowerCase()] ?? DEFAULT_LANG;
    } catch (error) {
        return DEFAULT_LANG;
    }
}
export default createI18n({
    legacy: false,
    locale: getLsLocale(),
    silentTranslationWarn: false,
    messages: {
        'zh-CN': ZhCN,
        'en-US': EnUS,
    }
});