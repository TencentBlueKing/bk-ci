import Node from './utils/Node'

const weekDayMap = {
    0: '日',
    1: '月',
    2: '火',
    3: '水',
    4: '木',
    5: '金',
    6: '土',
    7: '日'
}
const weekDesDayMap = {
    sun: '日',
    mon: '月',
    tue: '火',
    wed: '水',
    thu: '木',
    fri: '金',
    sat: '土'
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
        return `深夜${num}時`
    }
    if (num < 12) {
        return `午前${num}時`
    }
    if (num === 12) {
        return `正午${num}時`
    }
    if (num < 18) {
        return `午後${num}時`
    }
    return `夜${num}時`
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
        genAll: () => '毎分',
        [Node.TYPE_ENUM]: node => `${getMinuteValue(node.value)}分`,
        [Node.TYPE_RANG]: node => `${getMinuteValue(node.min)}分から${getMinuteValue(node.max)}分まで`,
        [Node.TYPE_REPEAT]: (node) => {
            if (node.value === '*') {
                return `${node.repeatInterval}分ごと`
            }
            return `${getMinuteValue(node.value)}分から${node.repeatInterval}分ごと`
        },
        [Node.TYPE_RANG_REPEAT]: node => `${getMinuteValue(node.min)}分から${getMinuteValue(node.max)}分までの${node.repeatInterval}分ごと`
    },
    hour: {
        genAll: () => '毎時',
        [Node.TYPE_ENUM]: node => `${getHourValue(node.value)}`,
        [Node.TYPE_RANG]: node => `${getHourValue(node.min)}から${getHourValue(node.max)}まで`,
        [Node.TYPE_REPEAT]: (node) => {
            if (node.value === '*') {
                return `${node.repeatInterval}時間ごと`
            }
            return `${getHourValue(node.value)}から${node.repeatInterval}時間ごと`
        },
        [Node.TYPE_RANG_REPEAT]: node => `${getHourValue(node.min)}から${getHourValue(node.max)}までの${node.repeatInterval}時間ごと`
    },
    dayOfMonth: {
        genAll: () => '毎日',
        [Node.TYPE_ENUM]: node => `${node.value}日`,
        [Node.TYPE_RANG]: node => `${node.min}日から${node.max}日まで`,
        [Node.TYPE_REPEAT]: (node) => {
            if (node.value === '*') {
                return `${node.repeatInterval}日ごと`
            }
            return `${node.value}日から${node.repeatInterval}日ごと`
        },
        [Node.TYPE_RANG_REPEAT]: node => `${node.min}日から${node.max}日までの${node.repeatInterval}日ごと`
    },
    month: {
        genAll: () => '毎月',
        [Node.TYPE_ENUM]: node => `${node.value}月`,
        [Node.TYPE_RANG]: node => `${node.min}月から${node.max}月まで`,
        [Node.TYPE_REPEAT]: (node) => {
            if (node.value === '*') {
                return `${node.repeatInterval}ヶ月ごと`
            }
            return `${node.value}月から${node.repeatInterval}ヶ月ごと`
        },
        [Node.TYPE_RANG_REPEAT]: node => `${node.min}月から${node.max}月までの${node.repeatInterval}ヶ月ごと`
    },
    dayOfWeek: {
        genAll: () => '毎日',
        [Node.TYPE_ENUM]: node => `毎週${getWeekDayValue(node.value)}`,
        [Node.TYPE_RANG]: node => `毎週${getWeekDayValue(node.min)}から${getWeekDayValue(node.max)}まで`,
        [Node.TYPE_REPEAT]: (node) => {
            if (node.value === '*') {
                return `毎週の${node.repeatInterval}日ごと`
            }
            return `毎週${getWeekDayValue(node.value)}から${node.repeatInterval}日ごと`
        },
        [Node.TYPE_RANG_REPEAT]: node => `毎週${getWeekDayValue(node.min)}から${getWeekDayValue(node.max)}までの${node.repeatInterval}日ごと`
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
        return `${pre.join('，')}と${last[0]}`
    }

    return [
        concatTextNew(ast, 'minute'),
        concatTextNew(ast, 'hour'),
        concatTextNew(ast, 'dayOfMonth'),
        concatTextNew(ast, 'dayOfWeek'),
        concatTextNew(ast, 'month')
    ]
}
