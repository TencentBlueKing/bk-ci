/** 与后端 PIPELINE_VARIABLES_OVERFLOW_PREFIX 对齐 */
export const PIPELINE_VARIABLES_OVERFLOW_PREFIX = '__BK_OVF__:'

/** 超过该长度视为超长：不直接塞进列表 DOM / input，避免 OOM */
export const LONG_INPUT_DISPLAY_THRESHOLD = 4000

/** 详情侧栏最多渲染的字符数，避免单次把数 MB 文本挂到 DOM */
export const LONG_VALUE_DETAIL_RENDER_LIMIT = 100 * 1024

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
    return isLongInputValue(param?.value)
}

/**
 * 去掉对象上可能导致 OOM 的超长字段（value / defaultValue）
 */
export function sanitizeParamForMemory (param = {}) {
    const next = { ...param }
    if (isLongInputValue(next.value)) {
        // 保留引用串本身（很短）；真实大值一律丢掉，按需再加载
        next.value = isOverflowReference(next.value)
            ? next.value
            : `${PIPELINE_VARIABLES_OVERFLOW_PREFIX}${String(next.value).length}`
    }
    if (isLongInputValue(next.defaultValue)) {
        next.defaultValue = undefined
    }
    return next
}

/**
 * 启动参数 Tab / ParamSet 元数据：只保留必要短字段，杜绝大字符串进入组件树
 */
export function toStartupParamMeta (param = {}) {
    const sanitized = sanitizeParamForMemory(param)
    return {
        id: sanitized.id ?? sanitized.key,
        key: sanitized.key,
        value: sanitized.value,
        valueType: sanitized.valueType,
        // 构建详情返回 BuildParameters.valueType，参数组合接口接收 BuildFormProperty.type。
        type: sanitized.type ?? sanitized.valueType ?? 'STRING',
        required: sanitized.required,
        constant: sanitized.constant,
        readOnly: sanitized.readOnly,
        desc: typeof sanitized.desc === 'string' && sanitized.desc.length > LONG_INPUT_DISPLAY_THRESHOLD
            ? undefined
            : sanitized.desc,
        sensitive: sanitized.sensitive,
        category: sanitized.category
    }
}

export function formatBuildParamsForDisplay (params = []) {
    return params.map(param => {
        const sanitized = sanitizeParamForMemory(param)
        if (!isLongBuildParam(sanitized) && !isOverflowReference(param?.value) && !isLongInputValue(param?.value)) {
            return sanitized
        }

        const overflowLength = getDisplayValueLength(param?.value)
        return {
            ...sanitized,
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
 * 详情展示用：超大文本截断，避免 DOM OOM
 */
export function getDetailRenderValue (value, limit = LONG_VALUE_DETAIL_RENDER_LIMIT) {
    if (typeof value !== 'string') return value
    if (value.length <= limit) return value
    return `${value.slice(0, limit)}\n\n...(${value.length} chars total, truncated for display)`
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
