import { bkMessage } from 'bk-magic-vue'

function copyTxt (value) {
    const input = document.createElement('input')
    document.body.appendChild(input)
    input.setAttribute('value', value)
    input.select()
    if (document.execCommand('copy')) {
        document.execCommand('copy')
        bkMessage({ theme: 'success', message: 'Copy successfully' })
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
        return <i class="bk-icon icon-clipboard" style={ style } onClick={ () => copyTxt(value) }></i>
    }
}
