import BkPipeline from '@/index.vue'
import '@/icon'
import { loadI18nMessages, useLang } from '@/locale'
import './src/index.scss'

BkPipeline.install = function (Vue, opts = {}) {
    loadI18nMessages(opts.i18n)
    Vue.component('bk-pipeline', BkPipeline)
}

export default BkPipeline
export { loadI18nMessages, useLang }
