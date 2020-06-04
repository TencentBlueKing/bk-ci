import en from './en-US'
import zh from './zh-CN'

const cookie = document.cookie || ''
const res = /blueking_language=([^;]+);/.exec(cookie) || []
const language = res[1] || 'zh-cn'

const curLoc = language === 'zh-cn' ? zh : en

export default function (key) {
    return curLoc[key]
}
