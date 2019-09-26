/**
 * @file checkoutside
 * @author ielgnaw <wuji0223@gmail.com>
 */

const nodes = []
const CLICK_CTX = '$clickoutsideCtx'

// 确保鼠标按下和松开时是同一个目标
let beginClick = ''
document.addEventListener('mousedown', event => {
    beginClick = event
    return beginClick
}, false)
document.addEventListener('mouseup', event => {
    for (const node of nodes) {
        node[CLICK_CTX].clickoutsideHandler(event, beginClick)
    }
}, false)

export default {
    bind (el, binding, vnode) {
        const id = nodes.push(el) - 1
        const clickoutsideHandler = (mouseup = {}, mousedown = {}) => {
            if (!vnode.context // 点击在vue实例之外的DOM上
                || !mouseup.target
                || !mousedown.target
                // 鼠标按下时的DOM节点是当前展开的组件的子元素
                || el.contains(mouseup.target)
                // 鼠标松开时的DOM节点是当前展开的组件的子元素
                || el.contains(mousedown.target)
                // 鼠标松开时的DOM节点是当前展开的组件的根元素
                || el === mouseup.target
                || (
                    // 当前点击元素是有弹出层的
                    vnode.context.popup
                        && (
                            // 鼠标按下时的DOM节点是当前有弹出层元素的子节点
                            vnode.context.popup.contains(mouseup.target)
                                // 鼠标松开时的DOM节点是当前有弹出层元素的子节点
                                || vnode.context.popup.contains(mousedown.target)
                        )
                )
            ) {
                return
            }
            
            // 传入了指令绑定的表达式
            if (binding.expression
                // 当前元素的clickoutside对象中有回调函数名
                && el[CLICK_CTX].callbackName
                // vnode中存在回调函数
                && vnode.context[el[CLICK_CTX].callbackName]
            ) {
                vnode.context[el[CLICK_CTX].callbackName]()
            } else {
                el[CLICK_CTX].bindingFn && el[CLICK_CTX].bindingFn()
            }
        }

        el[CLICK_CTX] = {
            id,
            clickoutsideHandler,
            callbackName: binding.expression,
            callbackFn: binding.value
        }
    },

    update (el, binding) {
        el[CLICK_CTX].callbackName = binding.expression
        el[CLICK_CTX].callbackFn = binding.value
    },

    unbind (el) {
        for (let i = 0, len = nodes.length; i < len; i++) {
            if (nodes[i][CLICK_CTX].id === el[CLICK_CTX].id) {
                nodes.splice(i, 1)
                break
            }
        }
    }
}
