export function hasAnyMatrixRuleValue (values) {
    return Object.values(values || {}).some((item) => {
        if (typeof item === 'string') {
            return item.trim() !== ''
        }
        if (Array.isArray(item)) {
            return item.length > 0
        }
        if (item && typeof item === 'object') {
            return Object.keys(item).length > 0
        }
        return !!item
    })
}
