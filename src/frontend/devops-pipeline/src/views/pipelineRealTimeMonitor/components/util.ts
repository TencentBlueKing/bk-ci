/**
 * 将时间字符串数组转换为时间戳数组（秒级，10位）
 * @param {Array<string>} timeStrArr - 时间字符串数组，格式：['YYYY-MM-DD HH:mm:ss', 'YYYY-MM-DD HH:mm:ss']
 * @returns {Array<number>} 时间戳数组（秒）
 * @example
 * convertTimeToTimestamp(['2025-12-22 16:07:51', '2025-12-22 10:07:51'])
 * // 返回: [1734855671, 1734834471]
 */
   export const convertTimeToTimestamp = (timeStrArr) => {
    if (!Array.isArray(timeStrArr) || timeStrArr.length === 0) {
        return []
     }
                
    return timeStrArr.map(timeStr => {
     // 将时间字符串转换为时间戳（毫秒）
    const timestamp = new Date(timeStr).getTime()
                 
    // 检查是否为有效时间
    if (isNaN(timestamp)) {
        console.error(`Invalid time string: ${timeStr}`)
        return null
     }
 
    // 转换为秒级时间戳（10位）
    return Math.floor(timestamp / 1000)
    }).filter(item => item !== null) // 过滤掉无效的时间
    }

/**
 * 获取当前时间和24小时前的时间戳（秒级，10位）
 * @returns {Array<number>} [24小时前时间戳, 当前时间戳]
 */
    export const getTimeRange24h = () => {
        const now = Math.floor(Date.now() / 1000)
        const before24h = now - 24 * 60 * 60
        return [before24h, now]
    }
