/** 与后端 PIPELINE_VARIABLES_OVERFLOW_PREFIX 对齐 */
export const PIPELINE_VARIABLES_OVERFLOW_PREFIX = '__BK_OVF__:'

/** 执行预览入参：超过该长度不再直接塞进 input/textarea，避免浏览器渲染空白 */
export const LONG_INPUT_DISPLAY_THRESHOLD = 4000

/**
 * 判断主表 VALUE 是否为大变量引用串：`__BK_OVF__:{length}`
 */
export function isOverflowReference (value) {
    return typeof value === 'string' && value.startsWith(PIPELINE_VARIABLES_OVERFLOW_PREFIX)
}

/**
 * 从引用串提取原始字符长度；非引用串返回 null
 */
export function getOverflowLength (value) {
    if (!isOverflowReference(value)) return null
    const lengthText = value.slice(PIPELINE_VARIABLES_OVERFLOW_PREFIX.length)
    if (!/^\d+$/.test(lengthText)) return null
    return Number(lengthText)
}

export function getDisplayValueLength (value) {
    const overflowLength = getOverflowLength(value)
    if (overflowLength != null) return overflowLength
    if (typeof value === 'string') return value.length
    return 0
}

export function isLongInputValue (value) {
    return isOverflowReference(value)
        || (typeof value === 'string' && value.length > LONG_INPUT_DISPLAY_THRESHOLD)
}

export function isLongBuildParam (param) {
    return isOverflowReference(param?.value)
}

export function formatBuildParamsForDisplay (params = []) {
    return params.map(param => {
        if (!isLongBuildParam(param)) return param

        const overflowLength = getOverflowLength(param.value)
        return {
            ...param,
            isLongValue: true,
            overflowLength,
            value: undefined,
            valueLoaded: false,
            valueLoading: false,
            valueError: false
        }
    })
}

export function mergeBuildParamValue (params = [], key, value, extra = {}) {
    return params.map(param => {
        if (param.key !== key) return param

        return {
            ...param,
            value,
            valueLoaded: true,
            valueLoading: false,
            valueError: false,
            ...extra
        }
    })
}

/**
 * 用已解析的参数列表覆盖 values 中仍是引用串的项
 */
export function replaceOverflowValues (values = {}, resolvedParams = []) {
    if (!values || typeof values !== 'object') return values
    const resolvedMap = (resolvedParams || []).reduce((acc, param) => {
        const id = param.id ?? param.key
        if (id != null && !isOverflowReference(param.value) && typeof param.value !== 'undefined') {
            acc[id] = param.value
        }
        return acc
    }, {})

    return Object.keys(values).reduce((acc, key) => {
        const current = values[key]
        if (isOverflowReference(current) && Object.prototype.hasOwnProperty.call(resolvedMap, key)) {
            acc[key] = resolvedMap[key]
        } else if (isOverflowReference(current)) {
            // 无法解析时清空，避免把引用串当真实值启动
            acc[key] = ''
        } else {
            acc[key] = current
        }
        return acc
    }, {})
}

export function hasOverflowReferenceValues (values = {}) {
    return Object.values(values || {}).some(isOverflowReference)
}
