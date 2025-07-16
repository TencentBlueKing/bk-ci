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

const app = createApp(App);
app.use(router);
app.use(i18n);
app.use(bkui);
app.use(createPinia());
app.directive('bk-tooltips', bkTooltips);
app.mount('#app');
