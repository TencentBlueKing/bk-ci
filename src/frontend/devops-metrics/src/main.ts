import { createApp } from 'vue';
import { createPinia } from 'pinia';
import router from './router';
import App from './app.vue';
import './css/index.css';

// 全量引入 bkui-vue
import bkui from 'bkui-vue';
// 全量引入 bkui-vue 样式
import 'bkui-vue/dist/cli.css';

// i18n
import { getCookie, getCookies } from '@/common/util'
import { createI18n } from 'vue-i18n'
import ZhCN from '../../locale/metrics/zh-CN.json'
import EnUS from '../../locale/metrics/en-US.json'
import JaJp from '../../locale/metrics/ja-JP.json'
import bkuiZhCn from 'bkui-vue/dist/locale/zh-cn.esm'
import bkuiEn from 'bkui-vue/dist/locale/en.esm'
import bkuiJp from 'bkui-vue/dist/locale/ja-jp.esm'

const cookiesObj = getCookies() || {};
const localeAliasMap = {
  'zh-CN': 'zh-CN',
  'zh-cn': 'zh-CN',
  'ja-JP': 'ja-JP',
  ja: 'ja-JP',
  zh_CN: 'zh-CN',
  zh_cn: 'zh-CN',
  cn: 'zh-CN',
  'en-US': 'en-US',
  'en-us': 'en-US',
  en: 'en-US',
  us: 'en-US',
  en_US: 'en-US',
  en_us: 'en-US'
}
const bkUiLocaleAliasMap = {
  'zh-CN': bkuiZhCn,
  'zh-cn': bkuiZhCn,
  'ja-JP': bkuiJp,
  ja: bkuiJp,
  zh_CN: bkuiZhCn,
  zh_cn: bkuiZhCn,
  cn: bkuiZhCn,
  'en-US': bkuiEn,
  'en-us': bkuiEn,
  en: bkuiEn,
  us: bkuiEn,
  en_US: bkuiEn,
  en_us: bkuiEn
}
const i18nLocale = getCookie('blueking_language') || ''
const i18n = createI18n({
  legacy: false,
  locale: localeAliasMap[i18nLocale.toLowerCase()] || 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': ZhCN,
    'en-US': EnUS,
    'ja-JP': JaJp,
  }
})

createApp(App)
  .use(router)
  .use(createPinia())
  .use(bkui, {
    locale: bkUiLocaleAliasMap[cookiesObj.blueking_language] || bkuiZhCn
  })
  .use(i18n)
  .mount('.app');
  