export function copyText (value, t) {
    const input = document.createElement('input')
    document.body.appendChild(input)
    input.setAttribute('value', value)
    input.select()
    if (document.execCommand('copy')) {
        document.execCommand('copy')
        t.$bkMessage({ theme: 'success', message: t.$t('turbo.已成功复制到粘贴板') })
    }
    document.body.removeChild(input)
}
