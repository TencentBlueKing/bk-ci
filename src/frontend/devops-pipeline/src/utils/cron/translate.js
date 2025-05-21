import translateCN from './translate-cn'
import translateEN from './translate-en'
import {
    optimize,
    parsetext
} from './utils'

const fieldList = [
    'minute',
    'hour',
    'dayOfMonth',
    'month',
    'dayOfWeek'
]

const print = (expression, locale) => {
    const atoms = (`${expression}`).trim().split(/\s+/)
    const fieldMap = {}
    atoms.forEach((item, index) => {
        fieldMap[fieldList[index]] = parsetext(item)
    })
    const ast = optimize(fieldMap)
    return locale === 'zh-CN' ? translateCN(ast) : translateEN(ast)
}

export default print
