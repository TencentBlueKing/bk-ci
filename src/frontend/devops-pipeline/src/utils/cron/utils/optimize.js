import Node from './Node'

export const optimize = (fieldMap) => {
    const isAllValue = node => node.length === 1
    && node[0].type === Node.TYPE_ENUM
    && (node[0].value === '*' || node[0].value === '?')
    const prettyMap = {}

    prettyMap.month = isAllValue(fieldMap.month) ? [] : fieldMap.month

    if (isAllValue(fieldMap.dayOfMonth) && isAllValue(fieldMap.month) && isAllValue(fieldMap.dayOfWeek)) {
        prettyMap.dayOfMonth = []
        delete prettyMap.month
    } else {
        if (!isAllValue(fieldMap.dayOfWeek)) {
            prettyMap.dayOfWeek = fieldMap.dayOfWeek
        }
        if (!isAllValue(fieldMap.dayOfMonth)) {
            prettyMap.dayOfMonth = fieldMap.dayOfMonth
        }
        if (!prettyMap.dayOfMonth && !prettyMap.dayOfWeek && prettyMap.month.length > 0) {
            prettyMap.dayOfMonth = []
        }
    }
    prettyMap.hour = isAllValue(fieldMap.hour) ? [] : fieldMap.hour
    if (prettyMap.hour.length < 1 && prettyMap.dayOfMonth && prettyMap.dayOfMonth.length < 1) {
        delete prettyMap.dayOfMonth
    }
    prettyMap.minute = isAllValue(fieldMap.minute) ? [] : fieldMap.minute
    if (prettyMap.minute.length < 1 && prettyMap.hour.length < 1) {
        delete prettyMap.hour
    }
    return prettyMap
}
