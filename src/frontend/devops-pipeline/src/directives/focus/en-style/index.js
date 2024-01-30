import jsCookie from 'js-cookie'

export default {
    install (Vue) {
        Vue.directive('enStyle', {
            bind (el, binding) {
                console.log(0, el.style.cssText, 1, binding.value, 1111, jsCookie.get('blueking_language'))
                if (jsCookie.get('blueking_language') !== 'en' || (typeof binding.value === 'string' && el.style.cssText?.includes(binding.value))) {
                    return
                }
                let options = { class: '', styles: {} }
                if (typeof binding.value === 'string') {
                    el.style.cssText += binding.value
                    return
                }
                options = { ...options, ...binding.value }
                options.class && el.classList.add(options.class)
                let cssText = ''
                if (typeof options.styles === 'string') {
                    cssText += options.styles
                } else {
                    Object.keys(options.styles).forEach((key) => {
                        cssText += `${key}: ${options.styles[key]};`
                    })
                }
                el.style.cssText += cssText
            }
        })
    }
}
