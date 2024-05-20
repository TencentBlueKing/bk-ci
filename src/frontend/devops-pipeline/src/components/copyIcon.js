import { bkMessage } from 'bk-magic-vue'
import createLocale from '../../../locale'
const { i18n } = createLocale(require.context('@locale/pipeline/', false, /\.json$/))
const locale = i18n.locale
const messages = i18n.messages[locale]
const message = messages.copySuc

async function copyTxt (value) {
    try {
        if (navigator.clipboard.writeText) {
            const res = await navigator.clipboard.writeText(value)
            console.log(res, 'cpy')
            bkMessage({ theme: 'success', message })
        } else {
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
    } catch (error) {
        console.log(error)
    }
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
