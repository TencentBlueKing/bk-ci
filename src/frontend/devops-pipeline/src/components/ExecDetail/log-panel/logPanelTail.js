/**
 * 日志 Tab 运行中跟随：ES 近实时可能导致后写行先可搜。
 * after 必须带 sinceTimestamp，客户端按 lineNo 合并，游标用 max(lineNo)。
 */
export const LOG_PANEL_LOOKBACK_MS = 15000

export function mergeLogPanelLines(current, incoming) {
    if (!incoming || !incoming.length) return current || []
    const map = new Map()
    ;(current || []).forEach((line) => {
        if (line && line.lineNo != null) map.set(Number(line.lineNo), line)
    })
    incoming.forEach((line) => {
        if (line && line.lineNo != null) map.set(Number(line.lineNo), line)
    })
    return Array.from(map.values()).sort((a, b) => {
        const ts = (a.timestamp || 0) - (b.timestamp || 0)
        return ts !== 0 ? ts : (a.lineNo || 0) - (b.lineNo || 0)
    })
}

export function nextAfterCursor(lines) {
    if (!lines || !lines.length) return { startLineNo: null, sinceTimestamp: null }
    let startLineNo = lines[0].lineNo
    let sinceTimestamp = lines[0].timestamp
    lines.forEach((line) => {
        if (line.lineNo > startLineNo) startLineNo = line.lineNo
        if (line.timestamp > sinceTimestamp) sinceTimestamp = line.timestamp
    })
    return { startLineNo, sinceTimestamp }
}
