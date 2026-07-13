/**
 * Unified time formatting by user IANA timezone.
 * Contract: backend returns Unix epoch millis; frontend formats with formatByUserTz.
 * See docs/specification/timezone_and_datetime.md
 */

const DEFAULT_FORMAT = 'YYYY-MM-DD HH:mm:ss'

function pad (n) {
    return n < 10 ? `0${n}` : `${n}`
}

/**
 * Resolve IANA timezone: window.userInfo.timeZone → browser → Asia/Shanghai
 */
export function getUserTimeZone () {
    if (typeof window !== 'undefined' && window.userInfo && window.userInfo.timeZone) {
        return window.userInfo.timeZone
    }
    try {
        return Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Shanghai'
    } catch (e) {
        return 'Asia/Shanghai'
    }
}

/**
 * Normalize input to epoch millis.
 * Accepts ms, seconds (auto *1000 when < 1e12), or parseable date string.
 */
export function toEpochMilli (value) {
    if (value === null || value === undefined || value === '') {
        return null
    }
    if (typeof value === 'number') {
        if (!Number.isFinite(value) || value <= 0) return null
        return value < 1e12 ? value * 1000 : value
    }
    if (typeof value === 'string') {
        const trimmed = value.trim()
        if (!trimmed) return null
        if (/^\d+$/.test(trimmed)) {
            const num = Number(trimmed)
            if (!Number.isFinite(num) || num <= 0) return null
            return num < 1e12 ? num * 1000 : num
        }
        const parsed = Date.parse(trimmed)
        return Number.isNaN(parsed) ? null : parsed
    }
    return null
}

function formatParts (date, timeZone) {
    const formatter = new Intl.DateTimeFormat('en-CA', {
        timeZone,
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hourCycle: 'h23'
    })
    const parts = formatter.formatToParts(date)
    const map = {}
    parts.forEach((p) => {
        if (p.type !== 'literal') {
            map[p.type] = p.value
        }
    })
    return map
}

/**
 * Format absolute time to user local display string.
 * @param {number|string} value epoch millis (preferred), seconds, or date string
 * @param {string} [timeZone] IANA timezone; default getUserTimeZone()
 * @param {string} [pattern] currently supports YYYY-MM-DD HH:mm:ss and YYYY-MM-DD
 */
export function formatByUserTz (value, timeZone, pattern = DEFAULT_FORMAT) {
    const ms = toEpochMilli(value)
    if (ms === null) return '--'
    const date = new Date(ms)
    if (Number.isNaN(date.getTime())) return '--'
    const tz = timeZone || getUserTimeZone()
    let map
    try {
        map = formatParts(date, tz)
    } catch (e) {
        map = formatParts(date, getUserTimeZone())
    }
    const y = map.year
    const m = map.month
    const d = map.day
    const h = map.hour === '24' ? '00' : map.hour
    const mi = map.minute
    const s = map.second
    if (pattern === 'YYYY-MM-DD') {
        return `${y}-${m}-${d}`
    }
    if (pattern === 'MM-DD HH:mm') {
        return `${m}-${d} ${h}:${mi}`
    }
    return `${y}-${m}-${d} ${h}:${mi}:${s}`
}

/**
 * Backward-compatible alias used by most sub-apps (expects ms; also accepts seconds).
 */
export function convertTime (ms) {
    return formatByUserTz(ms)
}

/**
 * Day range helpers for query params: calendar date in user TZ → epoch millis.
 * @returns {{ startTime: number, endTime: number }} endTime is exclusive next-day start
 */
export function calendarDateRangeToEpochMilli (startDate, endDate, timeZone) {
    const tz = timeZone || getUserTimeZone()
    // Interpret YYYY-MM-DD as local calendar date in tz via noon UTC trick + formatter is hard;
    // use Temporal-free approach: construct as UTC date components then adjust with offset at that day.
    const startMs = zonedDayStartEpochMilli(startDate, tz)
    const endExclusiveMs = zonedDayStartEpochMilli(addOneDay(endDate), tz)
    return { startTime: startMs, endTime: endExclusiveMs }
}

function addOneDay (dateStr) {
    const [y, m, d] = dateStr.split('-').map(Number)
    const utc = new Date(Date.UTC(y, m - 1, d))
    utc.setUTCDate(utc.getUTCDate() + 1)
    return `${utc.getUTCFullYear()}-${pad(utc.getUTCMonth() + 1)}-${pad(utc.getUTCDate())}`
}

/**
 * Start of calendar day in IANA timezone → epoch ms.
 */
export function zonedDayStartEpochMilli (dateStr, timeZone) {
    const tz = timeZone || getUserTimeZone()
    const target = `${dateStr} 00:00:00`
    let lo = Date.parse(`${dateStr}T00:00:00.000Z`) - 36 * 3600 * 1000
    let hi = Date.parse(`${dateStr}T00:00:00.000Z`) + 36 * 3600 * 1000
    while (lo < hi) {
        const mid = Math.floor((lo + hi) / 2)
        const map = formatParts(new Date(mid), tz)
        const h = map.hour === '24' ? '00' : map.hour
        const key = `${map.year}-${map.month}-${map.day} ${h}:${map.minute}:${map.second}`
        if (key < target) {
            lo = mid + 1
        } else {
            hi = mid
        }
    }
    return lo
}

export default {
    getUserTimeZone,
    toEpochMilli,
    formatByUserTz,
    convertTime,
    calendarDateRangeToEpochMilli,
    zonedDayStartEpochMilli
}
