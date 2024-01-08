/**
 * 判断两个数组是否有交集
 * @param {Array} arr1
 * @param {Array} arr2
 */
export function hasIntersection (arr1, arr2) {
    try {
        return arr2.some(item => arr1.includes(item))
    } catch (e) {
        return false
    }
}

/**
 * 根据插件字段rely配置决定是否显示
 * @param {Object} fieldProps 插件字段配置
 * @param {Object} values   插件表单值
 */
export function rely (fieldProps, values) {
    try {
        const { rely: { expression = [], operation = 'AND' } } = fieldProps
        const cb = item => {
            const { key, value, regex } = item
            if (Array.isArray(value)) {
                if (Array.isArray(values[key])) {
                    return hasIntersection(value, values[key])
                }
                return typeof values[key] !== 'undefined' && value.includes(values[key])
            } else if (regex) {
                const reg = new RegExp(regex, 'i')
                return Array.isArray(values[key]) ? values[key].some(item => reg.test(item)) : reg.test(values[key])
            } else {
                return Array.isArray(values[key]) ? values[key].some(item => item === value) : values[key] === value
            }
        }
        switch (operation) {
            case 'AND':
                return expression.every(cb)
            case 'OR':
                return expression.length > 0 ? expression.some(cb) : true
            case 'NOT':
                return expression.length > 0 ? !expression.some(cb) : true
            default:
                return true
        }
    } catch (e) {
        return true
    }
}
