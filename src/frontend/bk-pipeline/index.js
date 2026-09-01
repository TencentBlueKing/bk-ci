import "@/icon"
import BkPipeline from "@/index.vue"
import { loadI18nMessages, useLang } from "@/locale"
import "./src/index.scss"

// Vue 2.7 和 Vue 3 兼容的安装函数
function install (appOrVue, opts = {}) {
    loadI18nMessages(opts.i18n)

    // 检测是 Vue 3 (app) 还是 Vue 2.7 (Vue)
    // Vue 3: app.component()
    // Vue 2.7: Vue.component()
    if (appOrVue && typeof appOrVue === "object" && "component" in appOrVue) {
    // Vue 3: app.component()
        appOrVue.component("bk-pipeline", BkPipeline)
    } else if (
        appOrVue
    && typeof appOrVue === "function"
    && appOrVue.prototype
    && appOrVue.component
    ) {
    // Vue 2.7: Vue.component()
        appOrVue.component("bk-pipeline", BkPipeline)
    } else {
        console.warn(
            "[bk-pipeline] Unsupported Vue version. Please use Vue 2.7+ or Vue 3."
        )
    }
}

BkPipeline.install = install

export default BkPipeline
export { loadI18nMessages, useLang }
