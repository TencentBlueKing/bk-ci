import '@/icon'
import BkPipeline from '@/index.vue'
import { loadI18nMessages, useLang } from '@/locale'
import './src/index.scss'

function install (Vue, opts = {}) {
    loadI18nMessages(opts.i18n)
    Vue.component('bk-pipeline', BkPipeline)
}

BkPipeline.install = install

export default BkPipeline
export { loadI18nMessages, useLang }
