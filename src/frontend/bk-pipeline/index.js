import BkPipeline from '@/index.vue'
import '@/icon'
import { loadI18nMessages, useLang } from '@/locale'
import './src/index.scss'

function install (Vue, opts = {}) {
    loadI18nMessages(opts.i18n)
    Vue.component('bk-pipeline', BkPipeline)
}

BkPipeline.install = install
if (typeof window !== 'undefined' && window.Vue) {
    install(window.Vue)
}

export default BkPipeline
export { loadI18nMessages, useLang }
