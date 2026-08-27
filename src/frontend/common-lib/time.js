/**
 * Unified time formatting by user IANA timezone.
 * Contract: backend returns Unix epoch millis; frontend formats with formatByUserTz.
 * See docs/specification/timezone_and_datetime.md
 *
 * Display timezone source (converged with multi-tenant):
 * GET /project/api/user/users/tenantInfoForDisplay → applyTenantDisplayInfo → getUserTimeZone
 */

const DEFAULT_FORMAT = 'YYYY-MM-DD HH:mm:ss'
const DAY_MS = 24 * 3600 * 1000
export const DEFAULT_USER_TIME_ZONE = 'Asia/Shanghai'

function pad (n) {
    return n < 10 ? `0${n}` : `${n}`
}

/**
 * Apply tenant display info from project template API (tenantInfoForDisplay).
 * Sets window.tenantInfoForDisplay and syncs window.userInfo.timeZone for all page formatters.
 * @param {{ tenantId?: string, apiBaseUrl?: string, timeZone?: string }} info
 * @returns {{ tenantId: string, apiBaseUrl: string, timeZone: string }}
 */
export function applyTenantDisplayInfo (info = {}) {
    const timeZone = info.timeZone || DEFAULT_USER_TIME_ZONE
    const tenantInfo = {
        tenantId: info.tenantId || '',
        apiBaseUrl: info.apiBaseUrl || '',
        timeZone
    }
    if (typeof window !== 'undefined') {
        window.tenantInfoForDisplay = tenantInfo
        window.userInfo = {
            ...(window.userInfo || {}),
            timeZone,
            tenantId: tenantInfo.tenantId
        }
    }
    return tenantInfo
}

/**
 * 蓝鲸用户展示名 / 选人接口仅在后端下发了真实用户网关地址时启用。
 * 单租户或未配 BK 用户网关时必须跳过，避免打到同源 /api/v3/open-web/tenant/... 404。
 */
export function hasTenantUserApi (info) {
    const src = info || (typeof window !== 'undefined' ? window.tenantInfoForDisplay : null)
    return !!(src && String(src.apiBaseUrl || '').trim())
}

export function hasTenantId (info) {
    const src = info || (typeof window !== 'undefined' ? window.tenantInfoForDisplay : null)
    return !!(src && String(src.tenantId || '').trim())
}

export function getTenantUserApiPrefix (info) {
    const src = info || (typeof window !== 'undefined' ? window.tenantInfoForDisplay : null)
    const base = src && String(src.apiBaseUrl || '').trim()
    if (!base) {
        return ''
    }
    return `${base.replace(/\/$/, '')}/api/v3/open-web/tenant/users/-`
}

/**
 * Resolve IANA timezone for display:
 * window.tenantInfoForDisplay.timeZone → window.userInfo.timeZone → browser → Asia/Shanghai
 */
export function getUserTimeZone () {
    if (typeof window !== 'undefined') {
        if (window.tenantInfoForDisplay && window.tenantInfoForDisplay.timeZone) {
            return window.tenantInfoForDisplay.timeZone
        }
        if (window.userInfo && window.userInfo.timeZone) {
            return window.userInfo.timeZone
        }
    }
    try {
        return Intl.DateTimeFormat().resolvedOptions().timeZone || DEFAULT_USER_TIME_ZONE
    } catch (e) {
        return DEFAULT_USER_TIME_ZONE
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
    if (value instanceof Date) {
        const t = value.getTime()
        return Number.isNaN(t) || t <= 0 ? null : t
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
 * @param {number|string|Date} value epoch millis (preferred), seconds, or date string
 * @param {string} [timeZone] IANA timezone; default getUserTimeZone()
 * @param {string} [pattern] YYYY-MM-DD HH:mm:ss | YYYY-MM-DD | MM-DD HH:mm | HH:mm:ss
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
    if (pattern === 'HH:mm:ss') {
        return `${h}:${mi}:${s}`
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
 * Legacy helper: empty → '' (not '--'); formats in user timezone.
 */
export function prettyDateTimeFormat (target) {
    if (target === null || target === undefined || target === '') return ''
    const result = formatByUserTz(target)
    return result === '--' ? '' : result
}

/**
 * Current instant formatted in user timezone.
 */
export function nowInUserTz (pattern = DEFAULT_FORMAT, timeZone) {
    return formatByUserTz(Date.now(), timeZone, pattern)
}

/**
 * Calendar date (YYYY-MM-DD) of an instant in user timezone.
 */
export function calendarDateInUserTz (value = Date.now(), timeZone) {
    return formatByUserTz(value || Date.now(), timeZone, 'YYYY-MM-DD')
}

/**
 * Format relative duration (elapsed ms). Units match previous moment.duration behavior (365d/y, 30d/mon).
 * Output e.g. "1y 2mon 3d 4h 5m 6s" (leading zero units omitted).
 */
export function formatDuration (durationMs, empty = '--') {
    if (durationMs === null || durationMs === undefined || durationMs === '') return empty
    const totalSec = Math.floor(Math.abs(Number(durationMs)) / 1000)
    if (!Number.isFinite(totalSec) || totalSec <= 0) return empty

    const years = Math.floor(totalSec / (365 * 24 * 3600))
    let rem = totalSec % (365 * 24 * 3600)
    const months = Math.floor(rem / (30 * 24 * 3600))
    rem = rem % (30 * 24 * 3600)
    const days = Math.floor(rem / (24 * 3600))
    rem = rem % (24 * 3600)
    const hours = Math.floor(rem / 3600)
    rem = rem % 3600
    const minutes = Math.floor(rem / 60)
    const seconds = rem % 60

    const timeMap = {
        y: years,
        mon: months,
        d: days,
        h: hours,
        m: minutes,
        s: seconds
    }
    const diffTime = []
    let hasFirstNum = false
    Object.keys(timeMap).forEach((key) => {
        const val = timeMap[key]
        if (val <= 0 && !hasFirstNum) return
        hasFirstNum = true
        diffTime.push(`${val}${key}`)
    })
    return diffTime.length ? diffTime.join(' ') : empty
}

/** @deprecated alias — prefer formatDuration */
export function preciseDiff (duration) {
    return formatDuration(duration)
}

/**
 * UTC offset label at the given instant in IANA timezone, e.g. UTC+08:00
 */
export function getUtcOffsetLabel (value, timeZone) {
    const ms = toEpochMilli(value)
    if (ms === null) return ''
    const date = new Date(ms)
    if (Number.isNaN(date.getTime())) return ''
    const tz = timeZone || getUserTimeZone()
    try {
        const parts = new Intl.DateTimeFormat('en-US', {
            timeZone: tz,
            timeZoneName: 'longOffset'
        }).formatToParts(date)
        const name = parts.find(p => p.type === 'timeZoneName')?.value || ''
        // GMT+08:00 / GMT-04:00 / GMT → UTC+08:00 / UTC-04:00 / UTC+00:00
        if (name === 'GMT' || name === 'UTC') return 'UTC+00:00'
        return name.replace(/^GMT/, 'UTC').replace(/^UTC([+-])(\d)(?::|$)/, (_, sign, h) => `UTC${sign}0${h}`)
            .replace(/^UTC([+-]\d{2})$/, 'UTC$1:00')
    } catch (e) {
        return ''
    }
}

/**
 * Region / locale label from IANA timezone, e.g. China / Japan / United Kingdom
 */
export function getTimezoneRegionLabel (value, timeZone) {
    const ms = toEpochMilli(value)
    if (ms === null) return ''
    const date = new Date(ms)
    if (Number.isNaN(date.getTime())) return ''
    const tz = timeZone || getUserTimeZone()
    if (tz === 'UTC' || tz === 'Etc/UTC' || tz === 'Etc/GMT') return 'UTC'
    try {
        const parts = new Intl.DateTimeFormat('en-US', {
            timeZone: tz,
            timeZoneName: 'longGeneric'
        }).formatToParts(date)
        const name = parts.find(p => p.type === 'timeZoneName')?.value || ''
        if (!name || /^GMT/i.test(name) || /^UTC/i.test(name)) {
            // fallback: city segment of IANA id
            const city = tz.split('/').pop() || tz
            return city.replace(/_/g, ' ')
        }
        return name
            .replace(/\s*(Standard|Daylight|Summer)\s+Time$/i, '')
            .replace(/\s+Time$/i, '')
            .trim()
    } catch (e) {
        const city = tz.split('/').pop() || tz
        return city.replace(/_/g, ' ')
    }
}

/**
 * Tooltip content matching product format:
 * 2026-07-13 17:24:06 Asia/Shanghai China UTC+08:00
 */
export function formatTimezoneTooltip (value, timeZone, pattern = DEFAULT_FORMAT) {
    const ms = toEpochMilli(value)
    if (ms === null) return ''
    const tz = timeZone || getUserTimeZone()
    const display = formatByUserTz(ms, tz, pattern)
    if (display === '--') return ''
    const region = getTimezoneRegionLabel(ms, tz)
    const offset = getUtcOffsetLabel(ms, tz)
    return [display, tz, region, offset].filter(Boolean).join(' ')
}

/**
 * Shift YYYY-MM-DD by N calendar days (date-only arithmetic).
 */
export function addCalendarDays (dateStr, days) {
    const [y, m, d] = dateStr.split('-').map(Number)
    const utc = new Date(Date.UTC(y, m - 1, d))
    utc.setUTCDate(utc.getUTCDate() + days)
    return `${utc.getUTCFullYear()}-${pad(utc.getUTCMonth() + 1)}-${pad(utc.getUTCDate())}`
}

/**
 * Day range helpers for query params: calendar date in user TZ → epoch millis.
 * @returns {{ startTime: number, endTime: number }} endTime is exclusive next-day start
 */
export function calendarDateRangeToEpochMilli (startDate, endDate, timeZone) {
    const tz = timeZone || getUserTimeZone()
    const startMs = zonedDayStartEpochMilli(startDate, tz)
    const endExclusiveMs = zonedDayStartEpochMilli(addCalendarDays(endDate, 1), tz)
    return { startTime: startMs, endTime: endExclusiveMs }
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

/**
 * Recent N calendar days as display/query strings in user TZ: [start, end] inclusive dates → datetime strings.
 * end is "now"; start is day-start of (today - daysBack).
 */
export function recentDaysRangeInUserTz (daysBack = 7, timeZone, pattern = DEFAULT_FORMAT) {
    const tz = timeZone || getUserTimeZone()
    const now = Date.now()
    const today = calendarDateInUserTz(now, tz)
    const startDate = addCalendarDays(today, -Math.abs(daysBack))
    const startMs = zonedDayStartEpochMilli(startDate, tz)
    return {
        startTime: formatByUserTz(startMs, tz, pattern),
        endTime: formatByUserTz(now, tz, pattern),
        startMs,
        endMs: now,
        startDate,
        endDate: today,
        timeZone: tz
    }
}

/**
 * Datepicker shortcut: today in user TZ → [dayStart, now]
 */
export function userTzTodayRange (timeZone) {
    const tz = timeZone || getUserTimeZone()
    const today = calendarDateInUserTz(Date.now(), tz)
    return [new Date(zonedDayStartEpochMilli(today, tz)), new Date()]
}

/**
 * Datepicker shortcut: yesterday in user TZ → [dayStart, dayEnd]
 */
export function userTzYesterdayRange (timeZone) {
    const tz = timeZone || getUserTimeZone()
    const today = calendarDateInUserTz(Date.now(), tz)
    const yesterday = addCalendarDays(today, -1)
    const start = zonedDayStartEpochMilli(yesterday, tz)
    const end = zonedDayStartEpochMilli(today, tz) - 1
    return [new Date(start), new Date(end)]
}

/**
 * Datepicker shortcut: last N days (rolling) → [now - N days, now]
 */
export function userTzLastDaysRange (days, timeZone) {
    const end = Date.now()
    const start = end - Math.abs(days) * DAY_MS
    return [new Date(start), new Date(end)]
}

/**
 * Trend-style range used by atomstore charts:
 * end = yesterday 00:00:00 in user TZ; start = end - 1 unit (+1 day for weeks).
 * @param {'weeks'|'months'|'years'} unit
 */
export function userTzTrendRange (unit, timeZone) {
    const tz = timeZone || getUserTimeZone()
    const today = calendarDateInUserTz(Date.now(), tz)
    const endDate = addCalendarDays(today, -1)
    let startDate
    if (unit === 'weeks') {
        startDate = addCalendarDays(endDate, -6)
    } else if (unit === 'months') {
        startDate = addCalendarDays(endDate, -30)
    } else {
        startDate = addCalendarDays(endDate, -365)
    }
    const startMs = zonedDayStartEpochMilli(startDate, tz)
    const endMs = zonedDayStartEpochMilli(endDate, tz)
    return {
        startTime: formatByUserTz(startMs, tz, DEFAULT_FORMAT),
        endTime: formatByUserTz(endMs, tz, DEFAULT_FORMAT),
        startMs,
        endMs,
        timeZone: tz
    }
}

export default {
    DEFAULT_USER_TIME_ZONE,
    applyTenantDisplayInfo,
    hasTenantUserApi,
    hasTenantId,
    getTenantUserApiPrefix,
    getUserTimeZone,
    toEpochMilli,
    formatByUserTz,
    convertTime,
    prettyDateTimeFormat,
    nowInUserTz,
    calendarDateInUserTz,
    formatDuration,
    preciseDiff,
    getUtcOffsetLabel,
    getTimezoneRegionLabel,
    formatTimezoneTooltip,
    addCalendarDays,
    calendarDateRangeToEpochMilli,
    zonedDayStartEpochMilli,
    recentDaysRangeInUserTz,
    userTzTodayRange,
    userTzYesterdayRange,
    userTzLastDaysRange,
    userTzTrendRange
}
