!(function () {
    const svgCode
    = '<div style="position:absolute;width:0;height:0;">__SVG_SPRITES_SYMBOLS__</div>'
    if (document.body) {
        document.body.insertAdjacentHTML('afterbegin', svgCode)
    } else {
        document.addEventListener('DOMContentLoaded', function () {
            document.body.insertAdjacentHTML('afterbegin', svgCode)
        })
    }
})()
