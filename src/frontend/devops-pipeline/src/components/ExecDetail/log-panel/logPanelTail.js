/**
 * 日志 Tab 运行中跟随：ES 近实时可能导致后写行先可搜。
 * after 必须带 sinceTimestamp，客户端按 lineNo 合并，游标用 max(lineNo)。
 */
export const LOG_PANEL_LOOKBACK_MS = 15000

function lineNumber (line) {
    const value = Number(line?.lineNo ?? line?.line)
    return Number.isFinite(value) ? value : null
}

function lineTimestamp (line) {
    const raw = line?.timestamp ?? line?.time
    if (raw === null || raw === undefined || raw === '') return null
    const value = Number(raw)
    return Number.isFinite(value) ? value : null
}

function compareLines (left, right) {
    const leftTime = lineTimestamp(left)
    const rightTime = lineTimestamp(right)
    if (leftTime !== null && rightTime !== null && leftTime !== rightTime) return leftTime - rightTime
    if (leftTime === null && rightTime !== null) return 1
    if (leftTime !== null && rightTime === null) return -1
    return (lineNumber(left) ?? 0) - (lineNumber(right) ?? 0)
}

/** Merge by storage line number; the newest occurrence wins. */
export function mergeLogPanelLines (current = [], incoming = []) {
    const lines = new Map()
    current.forEach(line => {
        const number = lineNumber(line)
        if (number !== null) lines.set(number, { ...line, lineNo: number })
    })
    incoming.forEach(line => {
        const number = lineNumber(line)
        if (number !== null) lines.set(number, { ...line, lineNo: number })
    })
    return Array.from(lines.values()).sort(compareLines)
}

export function countNewLogPanelLines (current = [], incoming = []) {
    const known = new Set(current.map(lineNumber).filter(number => number !== null))
    const incomingNumbers = new Set(incoming.map(lineNumber).filter(number => number !== null))
    const existingIncoming = Array.from(incomingNumbers).filter(number => known.has(number)).length
    return incomingNumbers.size - existingIncoming
}

export function nextAfterCursor (lines = []) {
    const cursor = { startLineNo: null, sinceTimestamp: null }
    for (const line of lines) {
        const number = lineNumber(line)
        if (number !== null && (cursor.startLineNo === null || number > cursor.startLineNo)) cursor.startLineNo = number
        const timestamp = lineTimestamp(line)
        if (timestamp !== null && (cursor.sinceTimestamp === null || timestamp > cursor.sinceTimestamp)) cursor.sinceTimestamp = timestamp
    }
    return cursor
}
