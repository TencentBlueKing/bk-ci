/**
 * 获取单个 cookie 值
 * @param key cookie 键名
 * @returns cookie 值
 */
export function getCookie(key: string): string {
  const cookieStr = document.cookie || ''
  const cookieArr = cookieStr.split(';').filter(v => v)
  const cookieObj = cookieArr.reduce((res: Record<string, string>, cookieItem: string) => {
    const [key, value] = cookieItem.split('=')
    const cKey = (key || '').trim()
    const cVal = (value || '').trim()
    res[cKey] = cVal
    return res
  }, {})
  return cookieObj[key] || ''
}

/**
 * 获取所有 cookies 对象
 * @param strCookie cookie 字符串，默认为 document.cookie
 * @returns cookies 对象
 */
export function getCookies(strCookie: string = document.cookie): Record<string, string> {
  if (!strCookie) {
    return {}
  }
  const arrCookie = strCookie.split('; ')
  const cookiesObj: Record<string, string> = {}
  arrCookie.forEach((cookieStr) => {
    const arr = cookieStr.split('=')
    const [key, value] = arr
    if (key && value) {
      cookiesObj[key] = value
    }
  })
  return cookiesObj
}

