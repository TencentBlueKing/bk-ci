import { createApp } from 'vue';
import { createPinia } from 'pinia';
import router from './router';
import App from './app.vue';
import './css/index.css';
import './css/iconcool.js'

// 全量引入 bkui-vue
import bkui from 'bkui-vue';
// 全量引入 bkui-vue 样式
import 'bkui-vue/dist/style.css';
import { bkTooltips } from 'bkui-vue/lib/directives';
import bkuiZhCn from 'bkui-vue/dist/locale/zh-cn.esm';
import bkuiEn from 'bkui-vue/dist/locale/en.esm';

// 引入权限指令相关资源
import { handleProjectManageNoPermission } from './utils/permission';
import { AuthorityDirectiveV3 } from 'bk-permission';
import 'bk-permission/dist/main.css';

// i18n
import { getCookies } from './common/util';
import { createI18n } from 'vue-i18n';
import ZhCN from '../../locale/manage/zh-CN.json';
import EnUS from '../../locale/manage/en-US.json';

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

const app = createApp(App)
app
  .use(router)
  .use(createPinia())
  .use(bkui, {
    locale: ['en', 'en-us', 'en_us'].includes((cookiesObj.blueking_language || '').toLowerCase()) ? bkuiEn : bkuiZhCn
  })
  .use(i18n)
  .use(AuthorityDirectiveV3(handleProjectManageNoPermission))
  .mount('.app');
app.directive('bk-tooltips', bkTooltips)
