
import './styles.css'

export default {
    props: {
        headBackgroundColor: {
            type: String,
            default: '#FAFBFD'
        },
        tailBackgroundColor: String
    },
    render (h) {
        const childrenArr = this.$slots.default

        if (childrenArr.length > 1) {
            const childrenLength = childrenArr.length
            let startIndex = 0
            let headChildren = null
            while (startIndex < childrenLength) {
                headChildren = childrenArr[startIndex]
                if (headChildren.tag && headChildren.componentOptions) {
                    break
                }
                startIndex += 1
            }
            let tailChildren = null
            let endIndex = childrenLength - 1
            while (endIndex >= 0 && endIndex) {
                tailChildren = childrenArr[endIndex]
                if (tailChildren.tag && tailChildren.componentOptions) {
                    break
                }
                endIndex -= 1
            }

            if (headChildren && tailChildren && headChildren !== tailChildren) {
                let firstChildStaticClass = 'compose-form-item-first'
                if (headChildren.data.staticClass) {
                    firstChildStaticClass += ` ${headChildren.data.staticClass}`
                }
                if (this.headBackgroundColor) {
                    headChildren.data.style = Object.assign(headChildren.data.style || {}, {
                        'background-color': this.headBackgroundColor
                    })
                }
                headChildren.data.staticClass = firstChildStaticClass

                let lastChildStaticClass = 'compose-form-item-last'
                if (tailChildren.data.staticClass) {
                    lastChildStaticClass += ` ${tailChildren.data.staticClass}`
                }
                if (this.tailBackgroundColor) {
                    tailChildren.data.style = Object.assign(tailChildren.data.style || {}, {
                        'background-color': this.tailBackgroundColor
                    })
                }
                tailChildren.data.staticClass = lastChildStaticClass
            }
        }
        return h('div', {
            staticClass: 'compose-form-item'
        }, childrenArr)
    }
}
