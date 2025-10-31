import translateCN from './translate-cn'
import translateEN from './translate-en'
import translateJP from './translate-jp'
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
    const localeMap = {
        'zh-CN': translateCN,
        'zh-cn': translateCN,
        'ja-JP': translateJP,
        ja: translateJP,
        zh_CN: translateCN,
        zh_cn: translateCN,
        cn: translateCN,
        'en-US': translateEN,
        'en-us': translateEN,
        en: translateEN,
        us: translateEN,
        en_US: translateEN,
        en_us: translateEN
    }
    return localeMap[locale](ast) || translateCN(ast)
}

export default print
