import Vue from 'vue'
let install = Vue => {
    Vue.directive('bk-focus', {
        inserted: function (el) {
            el.focus()
        },
        update (el, binding) {
            binding.value && el.focus()
        }
    })
}

export default install
