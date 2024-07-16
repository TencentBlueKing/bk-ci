import { copyToClipboard } from '@/utils/util'
import { bkMessage } from 'bk-magic-vue'
import createLocale from '../../../locale'
const { i18n } = createLocale(require.context('@locale/pipeline/', false, /\.json$/))
const locale = i18n.locale
const messages = i18n.messages[locale]
const message = messages.copySuc

async function copyTxt (value) {
    copyToClipboard(value)
    bkMessage({ theme: 'success', message })
}

export default {
    name: 'copy-icon',

    functional: true,

    props: ['value'],

    render (h, ctx) {
        const props = ctx.props || {}
        const value = props.value ?? ''
        const style = {
            cursor: 'pointer'
        }
        return <i class="bk-icon icon-clipboard pointer-events-auto" style={ style } onClick={ () => copyTxt(value) }></i>
    }
}
