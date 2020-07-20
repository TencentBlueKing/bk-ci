import Vue from 'vue'
import Rules from './rules'

/**
 *  解析type
 */
function parseType (type) {
    if (typeof type !== 'string') {
        return {
            el: 'input',
            type: 'text'
        }
    }

    let $type = type.split(':')

    return {
        el: $type[0],
        type: $type[1]
    }
}

/**
 *  解析rule
 */
function parseRule (rule) {
    if (typeof rule !== 'string') {
        return {
            rule: 'not_empty'
        }
    }

    let $rule = rule.split(':')

    return {
        rule: $rule[0],
        ext: $rule[1]
    }
}

/**
 *  错误控制
 *  @param {Element} el - 当前绑定了指令的DOM节点
 *  @param {Boolean} valid - 当前的值是否通过检测
 */
function ErrorHandler (el, valid) {
    if (!valid) {
        el.classList.add('has-error')
        el.setAttribute('data-bk-valid', false)
    } else {
        el.classList.remove('has-error')
        el.setAttribute('data-bk-valid', true)
    }
}

let install = Vue => {
    Vue.directive('bk-validation', {
        inserted: function (el) {
            // el.focus()
        },
        update (el, binding) {
            let {
                value,
                oldValue
            } = binding

            // 避免不必要的更新
            if (value.val === oldValue.val) return

            let parsedType = parseType(value.type)
            let parsedRule = parseRule(value.rule)
            let result

            switch (parsedRule.rule) {
                case 'not_empty':
                    result = Rules.notEmpty(value.val)
                    break
                case 'limit':
                    result = Rules.limit(value.val, parsedRule.ext)
                    break
            }

            ErrorHandler(el, result)
        }
    })
}

export default install
