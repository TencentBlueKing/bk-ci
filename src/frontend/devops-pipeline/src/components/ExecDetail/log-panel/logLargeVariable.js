export const LARGE_VARIABLE_PREFIX = '__BK_OVF__:'
export const SENSITIVE_VALUE = '******'

const OUTPUT_PATTERN = /^output\((overflow|except)\):\s*([^=]+)=__BK_OVF__:(\d+)\s*$/
const INPUT_PATTERN = /^input\((normal)\):\s*(\([^)]*\))?\s*([^=]+)=__BK_OVF__:(\d+)\s*$/

export function isLargeVariableRef (value) {
    return typeof value === 'string' && value.startsWith(LARGE_VARIABLE_PREFIX)
}

export function parseLargeVariableLength (value) {
    if (!isLargeVariableRef(value)) return null
    const length = Number(value.slice(LARGE_VARIABLE_PREFIX.length).split(':')[0])
    return Number.isFinite(length) && length >= 0 ? length : null
}

export function parseLargeVariableLogLine (plainText) {
    const text = String(plainText ?? '')
    const output = text.match(OUTPUT_PATTERN)
    if (output) {
        const key = output[2].trim()
        if (!key) return null
        return { kind: output[1], key, length: Number(output[3]), label: '', queryable: true }
    }
    const input = text.match(INPUT_PATTERN)
    if (input) {
        const key = input[3].trim()
        if (!key) return null
        return { kind: input[1], key, length: Number(input[4]), label: input[2] || '', queryable: false }
    }
    return null
}

export function resolveLargeVariableResult (data) {
    if (data === null || data === undefined) return { state: 'notFound', value: '' }
    const value = typeof data === 'string' ? data : JSON.stringify(data)
    if (value === SENSITIVE_VALUE) return { state: 'sensitive', value }
    if (isLargeVariableRef(value)) return { state: 'expired', value: '' }
    return { state: 'ready', value }
}
