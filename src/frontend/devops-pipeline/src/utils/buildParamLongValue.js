export const LONG_BUILD_PARAM_VALUE_LENGTH = 4096

const LONG_VALUE_FIELDS = [
    'isLongValue',
    'longValue',
    'isLongVariable',
    'longVariable',
    'valueTooLong',
    'valueTruncated',
    'needFetchValue',
    'needFetch',
    'valueRef',
    'valueReference',
    'variableValueRef'
]

function hasLongValueFlag (param) {
    return LONG_VALUE_FIELDS.some(field => {
        const value = param?.[field]
        return value === true || (typeof value === 'string' && value.length > 0)
    })
}

function getValueLength (value) {
    if (typeof value === 'undefined' || value === null) return 0
    return String(value).length
}

export function isLongBuildParam (param, maxLength = LONG_BUILD_PARAM_VALUE_LENGTH) {
    return hasLongValueFlag(param) || getValueLength(param?.value) > maxLength
}

export function formatBuildParamsForDisplay (params = []) {
    return params.map(param => {
        if (!isLongBuildParam(param)) return param

        return {
            ...param,
            isLongValue: true,
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
