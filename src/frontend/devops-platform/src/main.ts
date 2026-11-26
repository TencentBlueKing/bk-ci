import { createApp } from 'vue'
import { createPinia } from 'pinia';
import './css/index.css'
import App from './App'
import router from './router/index';
import i18n from './i18n/index';
import { bkTooltips } from 'bkui-vue/lib/directives';
// 全量引入 bkui-vue
import bkui from 'bkui-vue';
// 全量引入 bkui-vue 样式
import 'bkui-vue/dist/cli.css';
import { BkXssFilterDirective } from '@blueking/xss-filter';
import { CONVENTION_XSS_OPTIONS } from '@/common/convention-xss';

const app = createApp(App);
app.use(router);
app.use(i18n);
app.use(bkui);
app.use(createPinia());
app.use(BkXssFilterDirective, {
  defaultOptions: CONVENTION_XSS_OPTIONS,
});
app.directive('bk-tooltips', bkTooltips);
app.mount('#app');
