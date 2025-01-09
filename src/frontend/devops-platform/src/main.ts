import { createApp } from 'vue'
import './css/index.css'
import App from './App.tsx'
import router from './router/index.ts';
import i18n from './i18n/index.ts';
import { bkTooltips } from 'bkui-vue/lib/directives';

const app = createApp(App);
console.log(123123)
app.use(router);
app.use(i18n);
app.directive('bk-tooltips', bkTooltips);
app.mount('#app');
