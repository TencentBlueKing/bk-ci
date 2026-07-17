/** 与后端 PIPELINE_VARIABLES_OVERFLOW_PREFIX 对齐 */
export const PIPELINE_VARIABLES_OVERFLOW_PREFIX = '__BK_OVF__:'

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

export function mergeBuildParamValue (params = [], key, value) {
    return params.map(param => {
        if (param.key !== key) return param

        return {
            ...param,
            value,
            valueLoaded: true,
            valueLoading: false,
            valueError: false
        }
    })
}
