import { isObject } from '@/utils/util'

function shouldStringifyArray (value) {
    return value.some(item => isObject(item) || Array.isArray(item))
}

export function serializeRemoteTriggerParamValue (value) {
    if (Array.isArray(value)) {
        return shouldStringifyArray(value)
            ? JSON.stringify(value)
            : value.join(',')
    }
    if (isObject(value)) {
        return JSON.stringify(value)
    }
    return value
}

export function serializeRemoteTriggerParamMap (params = []) {
    if (!Array.isArray(params)) return {}
    return params.filter(param => param.required).reduce((map, param) => {
        if (!param.id) return map
        map[param.id] = serializeRemoteTriggerParamValue(param.defaultValue)
        return map
    }, {})
}

export function stringifyRemoteTriggerParams (params = []) {
    return JSON.stringify(serializeRemoteTriggerParamMap(params)).replace(/\"/g, '\\"')
}
