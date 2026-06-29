import { isObject } from '@/utils/util'

export const DEFAULT_DISPLAY_CONDITION_OPERATOR = '=='
export const DISPLAY_CONDITION_IN_OPERATOR = 'IN'

export function normalizeDisplayConditionOperator (operator) {
    const operatorMap = {
        STARTWITH: 'STARTS_WITH',
        ENDWITH: 'ENDS_WITH'
    }
    const value = String(operator || DEFAULT_DISPLAY_CONDITION_OPERATOR).trim().toUpperCase()
    return operatorMap[value] || value
}

export function parseDisplayConditionValue (rawValue, operator) {
    try {
        const condition = typeof rawValue === 'string' ? JSON.parse(rawValue) : rawValue
        if (isObject(condition) && condition.operator) {
            return {
                operator: normalizeDisplayConditionOperator(condition.operator),
                value: condition.value ?? ''
            }
        }
    } catch (error) {
        // 兼容旧版 displayCondition: { key: value }
    }

    return {
        operator: normalizeDisplayConditionOperator(operator),
        value: rawValue
    }
}

export function getDisplayConditionOptions (param = {}) {
    return param.options || param.list || []
}

export function supportDisplayConditionOperator (param = {}, operator) {
    if (normalizeDisplayConditionOperator(operator) === DISPLAY_CONDITION_IN_OPERATOR) {
        return getDisplayConditionOptions(param).length > 0
    }
    return true
}

export function hasDisplayConditionOperatorSupportChanged (prevParam = {}, nextParam = {}) {
    return supportDisplayConditionOperator(prevParam, DISPLAY_CONDITION_IN_OPERATOR) !== supportDisplayConditionOperator(nextParam, DISPLAY_CONDITION_IN_OPERATOR)
}

export function getInvalidDisplayConditionDependents (params = [], sourceParam = {}) {
    const sourceId = sourceParam.id
    if (!sourceId || !Array.isArray(params)) return []

    return params.reduce((invalidList, param) => {
        const conditionMap = param?.displayCondition
        if (!isObject(conditionMap) || param.id === sourceId || !Object.prototype.hasOwnProperty.call(conditionMap, sourceId)) {
            return invalidList
        }

        const condition = parseDisplayConditionValue(conditionMap[sourceId])
        if (!supportDisplayConditionOperator(sourceParam, condition.operator)) {
            invalidList.push({
                param,
                operator: condition.operator
            })
        }

        return invalidList
    }, [])
}

export function getInvalidDisplayConditionDependentInfos (params = [], sourceParam = {}) {
    return getInvalidDisplayConditionDependents(params, sourceParam).map(({ param, operator }) => ({
        varName: param.id,
        varAlias: param.name || '',
        category: param.category || param.varGroupName || '',
        operator
    }))
}
