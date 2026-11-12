// Startup values and defaults use characters (UTF-16, as on the JVM), not UTF-8 bytes.
export const VAR_MAX_LENGTH = 4000
export const PIPELINE_VARIABLES_OVERFLOW_PREFIX = '__BK_OVF__:'

export function isOverflowReference (value) {
    return typeof value === 'string' && value.startsWith(PIPELINE_VARIABLES_OVERFLOW_PREFIX)
}

export function getOverflowLength (value) {
    if (!isOverflowReference(value)) return null
    const length = value.slice(PIPELINE_VARIABLES_OVERFLOW_PREFIX.length).split(':')[0]
    const number = Number(length)
    return /^\d+$/.test(length) && Number.isSafeInteger(number) ? number : null
}

// Match the wire representation: multi-selects are comma-separated, objects are JSON.
export function getVariableValueText (value) {
    if (value == null) return ''
    if (Array.isArray(value)) return value.join(',')
    return typeof value === 'object' ? JSON.stringify(value) : String(value)
}

export function getVariableValueIssue (value) {
    const text = getVariableValueText(value)
    if (isOverflowReference(text)) return { reference: true, length: getOverflowLength(text) }
    return text.length > VAR_MAX_LENGTH ? { reference: false, length: text.length } : null
}
