import { createPinia } from 'pinia';
import { createApp } from 'vue';
import App from './app.vue';
import './css/iconcool.js';
import './css/index.css';
import router from './router';

// 全量引入 bkui-vue
import bkui from 'bkui-vue';
// 全量引入 bkui-vue 样式
import 'bkui-vue/dist/style.css';
import { bkTooltips } from 'bkui-vue/lib/directives';

// 引入权限指令相关资源
import { AuthorityDirectiveV3 } from 'bk-permission';
import 'bk-permission/dist/main.css';
import { handleProjectManageNoPermission } from './utils/permission';

// i18n
import { createI18n } from 'vue-i18n';
import EnUS from '../../locale/manage/en-US.json';
import ZhCN from '../../locale/manage/zh-CN.json';
import { getCookies } from './common/util';

const cookiesObj = getCookies() || {};
const i18n = createI18n({
  legacy: false,
  locale: ['en', 'en-us', 'en_us'].includes((cookiesObj.blueking_language || '').toLowerCase()) ? 'en-US' : 'zh-CN',
  fallbackLocale: 'zh-CN',
  messages: {
    'zh-CN': ZhCN,
    'en-US': EnUS,
  },
});
const app = createApp(App);
app
  .use(router)
  .use(createPinia())
  .use(bkui)
  .use(i18n)
  .use(AuthorityDirectiveV3(handleProjectManageNoPermission))
  .mount('.app');
app.directive('bk-tooltips', bkTooltips);
