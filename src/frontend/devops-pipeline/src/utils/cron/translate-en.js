import Node from './utils/Node'

const weekDayMap = {
    0: 'Sunday',
    1: 'Monday',
    2: 'Tuesday',
    3: 'Wednesday',
    4: 'Thursday',
    5: 'Friday',
    6: 'Saturday',
    7: 'Sunday'
}

const weekDesDayMap = {
    sun: 'Sunday',
    mon: 'Monday',
    tue: 'Tuesday',
    wed: 'Wednesday',
    thu: 'Thursday',
    fri: 'Friday',
    sat: 'Sunday'
}

const dayMap = {
    1: 'January',
    2: 'February',
    3: 'March',
    4: 'April',
    5: 'May',
    6: 'June',
    7: 'July',
    8: 'August',
    9: 'September',
    10: 'October',
    11: 'November',
    12: 'December'
}

const getWeekDayValue = (value) => {
    if (weekDayMap[value]) {
        return weekDayMap[value]
    }
    const text = value.toString().toLowerCase()
    if (weekDesDayMap[text]) {
        return weekDesDayMap[text]
    }
    return value
}

const getHourValue = value => value

const getMonthValue = value => dayMap[value]

const ordinalSuffixOf = (i) => {
    const j = i % 10
    const k = i % 100
    if (j === 1 && k !== 11) {
        return `${i}st`
    }
    if (j === 2 && k !== 12) {
        return `${i}nd`
    }
    if (j === 3 && k !== 13) {
        return `${i}rd`
    }
    return `${i}th`
}

const getRepeatIntervalueText = (value) => {
    if (parseInt(value, 10) === 1) {
        return ' '
    }
    return ` ${ordinalSuffixOf(value)} `
}

const formatNumber = (value) => {
    const num = ~~value
    if (num < 10) {
        return `0${num}`
    }
    return num
}

const translateMap = {
    minute: {
        genAll: () => 'every minute',
        [Node.TYPE_ENUM]: node => `${node.value}`,
        [Node.TYPE_RANG]: node => `every minute from ${node.min} through ${node.max}`,
        [Node.TYPE_REPEAT]: (node) => {
            if (node.value === '*') {
                return `every${getRepeatIntervalueText(node.repeatInterval)}minute`
            }
            return `every${getRepeatIntervalueText(node.repeatInterval)}minute from ${node.value} through 59`
        },
        // eslint-disable-next-line max-len
        [Node.TYPE_RANG_REPEAT]: node => `every${getRepeatIntervalueText(node.repeatInterval)}minute from ${node.min} through ${node.max}`
    },
    hour: {
        genAll: () => '',
        [Node.TYPE_ENUM]: node => `${getHourValue(node.value)}`,
        [Node.TYPE_RANG]: node => `every hour from ${getHourValue(node.min)} through ${getHourValue(node.max)}`,
        [Node.TYPE_REPEAT]: (node) => {
            if (node.value === '*') {
                return `every${getRepeatIntervalueText(node.repeatInterval)}hour`
            }
            return `every${getRepeatIntervalueText(node.repeatInterval)}hour from ${node.value} through 23`
        },
        // eslint-disable-next-line max-len
        [Node.TYPE_RANG_REPEAT]: node => `every${getRepeatIntervalueText(node.repeatInterval)}hour from ${node.min} through ${node.max}`
    },
    dayOfMonth: {
        genAll: () => '',
        [Node.TYPE_ENUM]: node => `${node.value}`,
        [Node.TYPE_RANG]: node => `every day-of-month ${node.min} from ${node.max}`,
        [Node.TYPE_REPEAT]: (node) => {
            if (node.value === '*') {
                return `every ${getRepeatIntervalueText(node.repeatInterval)}day-of-month`
            }
            return `every${getRepeatIntervalueText(node.repeatInterval)}day-of-month from ${node.value} through 31`
        },
        // eslint-disable-next-line max-len
        [Node.TYPE_RANG_REPEAT]: node => `every${getRepeatIntervalueText(node.repeatInterval)}day-of-month from ${node.min} through ${node.max}`
    },
    month: {
        genAll: () => '',
        [Node.TYPE_ENUM]: node => `${getMonthValue(node.value)}`,
        [Node.TYPE_RANG]: node => `every month from ${getMonthValue(node.min)} through ${getMonthValue(node.max)}`,
        [Node.TYPE_REPEAT]: (node) => {
            if (node.value === '*') {
                return `every${getRepeatIntervalueText(node.repeatInterval)}month`
            }
            return `every${getRepeatIntervalueText(node.repeatInterval)}month from ${getMonthValue(node.value)} through December`
        },
        // eslint-disable-next-line max-len
        [Node.TYPE_RANG_REPEAT]: node => `every${getRepeatIntervalueText(node.repeatInterval)}month from ${getMonthValue(node.min)} through ${getMonthValue(node.max)}`
    },
    dayOfWeek: {
        genAll: () => '',
        [Node.TYPE_ENUM]: node => `${getWeekDayValue(node.value)}`,
        [Node.TYPE_RANG]: node => `every day-of-week ${getWeekDayValue(node.min)} through ${getWeekDayValue(node.max)}`,
        [Node.TYPE_REPEAT]: (node) => {
            if (node.value === '*') {
                return `every${getRepeatIntervalueText(node.repeatInterval)}day-of-week`
            }
            return `every${getRepeatIntervalueText(node.repeatInterval)}day-of-week from ${getWeekDayValue(node.value)} through Sunday`
        },
        // eslint-disable-next-line max-len
        [Node.TYPE_RANG_REPEAT]: node => `every${getRepeatIntervalueText(node.repeatInterval)}day-of-week from ${getWeekDayValue(node.min)} through ${getWeekDayValue(node.max)}`
    }
}

export default (ast) => {
    const concatTextNew = (ast, field, prefix, unit) => {
        if (!Object.prototype.hasOwnProperty.call(ast, field)) {
            return ''
        }
        const sequence = ast[field]
        const translate = translateMap[field]
        if (sequence.length < 1) {
            const all = translate.genAll()
            return all ? `${prefix} ${all}` : all
        }

        let start = prefix
        if (sequence[0].type === Node.TYPE_ENUM && unit) {
            start = `${start} ${unit}`
        }

        const stack = sequence.map(node => translate[node.type](node))
        if (stack.length < 2) {
            return `${start} ${stack.join('')}`
        }
        const pre = stack.slice(0, -1)
        const last = stack.slice(-1)
        return `${start} ${pre.join(',')}, and ${last[0]}`
    }

    let textMinute = concatTextNew(ast, 'minute', 'At', 'minute')
    let textHour = concatTextNew(ast, 'hour', 'past', 'hour')
    if (ast.minute.length === 1 && ast.hour.length === 1) {
        const minuteNode = ast.minute[0]
        const hourNode = ast.hour[0]
        if (minuteNode.type === Node.TYPE_ENUM && hourNode.type === Node.TYPE_ENUM) {
            textMinute = `:${formatNumber(minuteNode.value)}`
            textHour = `At ${formatNumber(hourNode.value)}`
        }
    }
    const textDayOfMonth = concatTextNew(ast, 'dayOfMonth', 'on', 'day-of-month')
    const textDayOfWeek = concatTextNew(ast, 'dayOfWeek', 'on', '')

    return [
        textMinute,
        textHour,
        textDayOfMonth,
        textDayOfMonth && textDayOfWeek ? `and ${textDayOfWeek}` : textDayOfWeek,
        concatTextNew(ast, 'month', 'in', '')
    ]
}
