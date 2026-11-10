import { bkTooltips } from 'bkui-vue/lib/directives';
import { createPinia } from 'pinia';
import { createApp } from 'vue';
import App from './App';
import './css/index.css';
import i18n from './i18n/index';
import router from './router/index';
// 全量引入 bkui-vue
import bkui from 'bkui-vue';
// 全量引入 bkui-vue 样式
import 'bkui-vue/dist/cli.css';
import TenantSingleton from './utils/tenant';
async function initializeApp() {
    const data = await new TenantSingleton().init()
    
    const app = createApp(App, {
        tenantId: data.tenantId,
        apiBaseUrl: data.apiBaseUrl,   
    });
    app.use(router);
    app.use(i18n);
    app.use(bkui);
    app.use(createPinia());
    app.directive('bk-tooltips', bkTooltips);
    app.mount('#app');
}


initializeApp()