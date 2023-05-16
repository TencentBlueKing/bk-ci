import { createApp } from 'vue';
import { createPinia } from 'pinia';
import router from './router';
import App from './app.vue';
import './css/index.css';
import zhCn from 'bkui-vue/dist/locale/zh-cn.esm'
import en from 'bkui-vue/dist/locale/en.esm'

// 全量引入 bkui-vue
import bkui from 'bkui-vue';
// 全量引入 bkui-vue 样式
import 'bkui-vue/dist/style.css';
import eventBus from '@/utils/bus.js'

// i18n
import { getCookie } from '@/common/util'
import { createI18n } from 'vue-i18n'
import ZhCN from '../../locale/metrics/zh-CN.json'
import EnUS from '../../locale/metrics/en-US.json'

const i18nLocale = getCookie('blueking_language') || ''
const i18n = createI18n({
  legacy: false,
  locale: ['en', 'en-us', 'en_us'].includes(i18nLocale.toLowerCase()) ? 'en-US' : 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': ZhCN,
    'en-US': EnUS
  }
})

window.Bus = eventBus;

createApp(App)
  .use(router)
  .use(createPinia())
  .use(bkui, {
    locale: ['en', 'en-us', 'en_us'].includes(i18nLocale.toLowerCase()) ? en : zhCn
  })
  .use(i18n)
  .mount('.app');
