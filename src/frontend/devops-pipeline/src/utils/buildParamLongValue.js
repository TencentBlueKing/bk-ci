// Runtime references may contain either just a length (logs) or length:executeCount:sha256.
export {
    PIPELINE_VARIABLES_OVERFLOW_PREFIX,
    isOverflowReference,
    getOverflowLength
} from '../../../common-lib/variableValue'

export const LONG_VALUE_DETAIL_RENDER_LIMIT = 100 * 1024

export function getDetailRenderValue (value, limit = LONG_VALUE_DETAIL_RENDER_LIMIT) {
    if (typeof value !== 'string' || value.length <= limit) return value
    return `${value.slice(0, limit)}\n\n...(${value.length} chars total, truncated for display)`
}
