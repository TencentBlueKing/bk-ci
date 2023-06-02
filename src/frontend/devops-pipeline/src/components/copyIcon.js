import { bkMessage } from '@tencent/bk-magic-vue'
import createLocale from '../../../locale'
const { i18n } = createLocale(require.context('@locale/pipeline/', false, /\.json$/))
const locale = i18n.locale
const messages = i18n.messages[locale]
const message = messages.copySuc

function copyTxt (value) {
    const input = document.createElement('input')
    document.body.appendChild(input)
    input.setAttribute('value', value)
    input.select()
    if (document.execCommand('copy')) {
        document.execCommand('copy')
        bkMessage({ theme: 'success', message })
    }
    document.body.removeChild(input)
}

export default {
    name: 'copy-icon',

    functional: true,

    props: ['value'],

    render (h, ctx) {
        const props = ctx.props || {}
        const value = props.value || ''
        const style = {
            cursor: 'pointer'
        }
        return <i class="bk-icon icon-clipboard pointer-events-auto" style={ style } onClick={ () => copyTxt(value) }></i>
    }
}
