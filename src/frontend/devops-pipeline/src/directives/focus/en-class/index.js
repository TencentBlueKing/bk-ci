import jsCookie from 'js-cookie'

export default {
    install (Vue) {
        Vue.directive('enClass', {
            bind (el, binding) {
                if ((typeof binding.value === 'string' && el?.classList.contains(binding.value))
          || jsCookie.get('blueking_language') !== 'en') {
                    return
                }
                if (typeof binding.value === 'string') {
                    binding.value && el.classList.add(binding.value)
                    return
                }
                let options = { class: '', styles: {} }
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
