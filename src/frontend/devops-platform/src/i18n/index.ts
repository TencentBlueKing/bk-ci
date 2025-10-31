import { getCookie } from '@/common/util';
import { createI18n } from 'vue-i18n';
import ZhCN from '../../../locale/platform/zh-CN.json';
import EnUS from '../../../locale/platform/en-US.json';
import JaJP from '../../../locale/platform/ja-JP.json';

const LS_KEY = 'blueking_language';
export const ZH_LOCALE = 'zh-CN';
export const EN_LOCALE = 'en-US';
export const JA_LOCALE = 'ja-JP';
const DEFAULT_LANG = ZH_LOCALE;
const localeAliasMap = {
    'zh-cn': 'zh-CN',
    zh_cn: 'zh-CN',
    cn: 'zh-CN',
    'en-us': 'en-US',
    en: 'en-US',
    us: 'en-US',
    en_us: 'en-US',
    'ja-jp': 'ja-JP',
    ja: 'ja-JP',
    jp: 'ja-JP',
    ja_jp: 'ja-JP'
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
        'ja-JP': JaJP
    }
});