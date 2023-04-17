import { createApp } from 'vue';
import { createPinia } from 'pinia';
import router from './router';
import App from './app.vue';
import './css/index.css';

// 全量引入 bkui-vue
import bkui from 'bkui-vue';
// 全量引入 bkui-vue 样式
import 'bkui-vue/dist/style.css';
import { bkTooltips } from 'bkui-vue/lib/directives';
import ellipsis from '@/directives/ellipsis'

// i18n
import { getCookies } from './common/util';
import { createI18n } from 'vue-i18n';
import ZhCN from '../../locale/permission/zh-CN.json';
import EnUS from '../../locale/permission/en-US.json';

const cookiesObj = getCookies('blueking_language') || '';
const i18n = createI18n({
  legacy: false,
  locale: ['en', 'en-us', 'en_us'].includes((cookiesObj.blueking_language || '').toLowerCase()) ? 'en-US' : 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': ZhCN,
    'en-US': EnUS,
  },
});

const app = createApp(App)
app
  .use(router)
  .use(createPinia())
  .use(bkui)
  .use(i18n)
  .mount('.app');
app.directive('bk-tooltips', bkTooltips);
app.directive('bk-ellipsis', ellipsis);
