import { createApp } from 'vue';
import { createPinia } from 'pinia';
import router from './router';
import App from './app.vue';
import './css/index.css';

// 全量引入 bkui-vue
import bkui from 'bkui-vue';
// 全量引入 bkui-vue 样式
import 'bkui-vue/dist/style.css';

// i18n
import { getCookie } from '@/common/util'
import { createI18n } from 'vue-i18n'
import ZhCN from '../../locale/metrics/zh-CN.json'
import EnUS from '../../locale/metrics/en-US.json'
import JaJp from '../../locale/metrics/ja-JP.json'

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
  .use(bkui)
  .use(i18n)
  .mount('.app');
