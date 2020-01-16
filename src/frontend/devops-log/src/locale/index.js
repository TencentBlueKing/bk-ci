import en from './en-US'
import zh from './zh-CN'

const cookie = document.cookie || ''
const res = /blueking_language=([^;]+);/.exec(cookie) || []
const language = res[1] || 'zh-CN'

let curLoc = language === 'zh-CN' ? zh : en

export default function (key) {
    return curLoc[key]
}