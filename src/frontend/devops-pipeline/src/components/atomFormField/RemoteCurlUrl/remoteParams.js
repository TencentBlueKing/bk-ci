import { FORM_LIST } from '@/store/modules/atom/paramsConfig'
import { isObject, normalizeFormListValue } from '@/utils/util'

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
        const defaultValue = param.type === FORM_LIST
            ? normalizeFormListValue(param.defaultValue, param.fields, {
                filterDefaultRows: false
            })
            : param.defaultValue
        map[param.id] = serializeRemoteTriggerParamValue(defaultValue)
        return map
    }, {})
}

export function stringifyRemoteTriggerParams (params = []) {
    return JSON.stringify(serializeRemoteTriggerParamMap(params)).replace(/\"/g, '\\"')
}
