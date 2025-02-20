import Node from './utils/Node'

const weekDayMap = {
    0: '日',
    1: '一',
    2: '二',
    3: '三',
    4: '四',
    5: '五',
    6: '六',
    7: '日'
}
const weekDesDayMap = {
    sun: '日',
    mon: '一',
    tue: '二',
    wed: '三',
    thu: '四',
    fri: '五',
    sat: '六'
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

const getHourValue = (value) => {
    const num = ~~value
    if (num < 5) {
        return `凌晨${num}点`
    }
    if (num < 12) {
        return `上午${num}点`
    }
    if (num === 12) {
        return `中午${num}点`
    }
    if (num < 18) {
        return `下午${num}点`
    }
    return `晚上${num}点`
}

const getMinuteValue = (value) => {
    const num = ~~value
    if (num < 10) {
        return `0${num}`
    }
    return num
}

const translateMap = {
    minute: {
        genAll: () => '每分钟',
        [Node.TYPE_ENUM]: node => `${getMinuteValue(node.value)}分`,
        [Node.TYPE_RANG]: node => `${getMinuteValue(node.min)}分到${getMinuteValue(node.max)}分`,
        [Node.TYPE_REPEAT]: (node) => {
            if (node.value === '*') {
                return `每隔${node.repeatInterval}分钟`
            }
            return `从${getMinuteValue(node.value)}分开始每隔${node.repeatInterval}分钟`
        },
        // eslint-disable-next-line max-len
        [Node.TYPE_RANG_REPEAT]: node => `从${getMinuteValue(node.min)}分开始到${getMinuteValue(node.max)}分的每${node.repeatInterval}分钟`
    },
    hour: {
        genAll: () => '每小时',
        [Node.TYPE_ENUM]: node => `${getHourValue(node.value)}`,
        [Node.TYPE_RANG]: node => `${getHourValue(node.min)}到${getHourValue(node.max)}`,
        [Node.TYPE_REPEAT]: (node) => {
            if (node.value === '*') {
                return `每隔${node.repeatInterval}个小时`
            }
            return `从${getHourValue(node.value)}开始每隔${node.repeatInterval}个小时`
        },
        // eslint-disable-next-line max-len
        [Node.TYPE_RANG_REPEAT]: node => `从${getHourValue(node.min)}开始到${getHourValue(node.max)}的每${node.repeatInterval}个小时`
    },
    dayOfMonth: {
        genAll: () => '每天',
        [Node.TYPE_ENUM]: node => `${node.value}号`,
        [Node.TYPE_RANG]: node => `${node.min}号到${node.max}号`,
        [Node.TYPE_REPEAT]: (node) => {
            if (node.value === '*') {
                return `每隔${node.repeatInterval}天`
            }
            return `从${node.value}号开始每隔${node.repeatInterval}天`
        },
        // eslint-disable-next-line max-len
        [Node.TYPE_RANG_REPEAT]: node => `从${node.min}号开始到${node.max}号的每${node.repeatInterval}天`
    },
    month: {
        genAll: () => '每月',
        [Node.TYPE_ENUM]: node => `${node.value}月`,
        [Node.TYPE_RANG]: node => `${node.min}月到${node.max}月`,
        [Node.TYPE_REPEAT]: (node) => {
            if (node.value === '*') {
                return `每隔${node.repeatInterval}个月`
            }
            return `从${node.value}月开始每隔${node.repeatInterval}个月`
        },
        // eslint-disable-next-line max-len
        [Node.TYPE_RANG_REPEAT]: node => `从${node.min}月开始到${node.max}月的每${node.repeatInterval}个月`
    },
    dayOfWeek: {
        genAll: () => '每天',
        [Node.TYPE_ENUM]: node => `每周${getWeekDayValue(node.value)}`,
        [Node.TYPE_RANG]: node => `每周${getWeekDayValue(node.min)}到周${getWeekDayValue(node.max)}`,
        [Node.TYPE_REPEAT]: (node) => {
            if (node.value === '*') {
                return `每个星期内的每隔${node.repeatInterval}天`
            }
            return `从每周${getWeekDayValue(node.value)}开始每隔${node.repeatInterval}天`
        },
        // eslint-disable-next-line max-len
        [Node.TYPE_RANG_REPEAT]: node => `从每周${getWeekDayValue(node.min)}开始到周${getWeekDayValue(node.max)}的每隔${node.repeatInterval}天`
    }
}

export default (ast) => {
    const concatTextNew = (ast, field) => {
        if (!Object.prototype.hasOwnProperty.call(ast, field)) {
            return ''
        }
        const sequence = ast[field]
        const translate = translateMap[field]
        if (sequence.length < 1) {
            return translate.genAll()
        }
        const stack = sequence.map(node => translate[node.type](node))
        if (stack.length < 2) {
            return stack.join('')
        }
        const pre = stack.slice(0, -1)
        const last = stack.slice(-1)
        return `${pre.join('，')}和${last[0]}`
    }

    return [
        concatTextNew(ast, 'minute'),
        concatTextNew(ast, 'hour'),
        concatTextNew(ast, 'dayOfMonth'),
        concatTextNew(ast, 'dayOfWeek'),
        concatTextNew(ast, 'month')
    ]
}
