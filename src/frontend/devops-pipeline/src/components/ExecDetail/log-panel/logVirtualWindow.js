export const DEFAULT_LINE_HEIGHT = 20
export const VIRTUAL_THRESHOLD = 800
export const VIRTUAL_OVERSCAN = 30
export const MAX_VIRTUAL_OVERSCAN_PX = 2400
export const USER_SCROLL_IDLE_MS = 180

const LOG_SCROLL_KEYS = new Set(['ArrowDown', 'ArrowUp', 'PageDown', 'PageUp', 'Home', 'End', ' '])

export function isLogScrollKey (key) {
    return LOG_SCROLL_KEYS.has(String(key || ''))
}

export function resolveFollowIntent ({ currentFollow = true, atBottom = false, userScrollPending = false } = {}) {
    return userScrollPending ? Boolean(atBottom) : Boolean(currentFollow)
}

export function isCurrentLayoutUpdate ({ token, currentToken, interactionToken, currentInteractionToken, destroyed = false } = {}) {
    return !destroyed && token === currentToken && interactionToken === currentInteractionToken
}

export function shouldInterceptLogWheel ({
    virtualized = false,
    cancelable = false,
    ctrlKey = false,
    metaKey = false,
    shiftKey = false,
    deltaX = 0,
    deltaY = 0
} = {}) {
    return virtualized && cancelable && !ctrlKey && !metaKey && !shiftKey
        && deltaY !== 0 && Math.abs(deltaX) <= Math.abs(deltaY)
}

export function resolveLogWheelDeltas ({ deltaX = 0, deltaY = 0, deltaMode = 0, lineHeight = DEFAULT_LINE_HEIGHT, pageHeight = 0 } = {}) {
    const multiplier = deltaMode === 1 ? lineHeight : deltaMode === 2 ? pageHeight : 1
    return { deltaLeft: deltaX * multiplier, deltaTop: deltaY * multiplier }
}

export function resolveLogWheelTarget ({
    scrollTop = 0,
    scrollLeft = 0,
    deltaTop = 0,
    deltaLeft = 0,
    scrollHeight = 0,
    clientHeight = 0,
    scrollWidth = 0,
    clientWidth = 0
} = {}) {
    const top = Math.max(0, Math.min(Math.max(scrollHeight - clientHeight, 0), scrollTop + deltaTop))
    const left = Math.max(0, Math.min(Math.max(scrollWidth - clientWidth, 0), scrollLeft + deltaLeft))
    return { top, left, changed: top !== scrollTop || left !== scrollLeft }
}

export function isCurrentWheelTransaction (current, transaction) {
    return Boolean(transaction && current === transaction)
}

export function shouldYieldLogWheelToNative ({ hasPending = false, targetTop = 0, scrollTop = 0 } = {}) {
    return !hasPending && targetTop === scrollTop
}

export function shouldUseVirtualScroll (lineCount) {
    return lineCount > VIRTUAL_THRESHOLD
}

export function estimateExplicitLineHeight (text, lineHeight = DEFAULT_LINE_HEIGHT) {
    const lineCount = String(text ?? '').replace(/\r\n?/g, '\n').split('\n').length
    return Math.max(1, lineCount) * (Number(lineHeight) > 0 ? Number(lineHeight) : DEFAULT_LINE_HEIGHT)
}

export function resolveLineKey (line, index = 0) {
    if (line == null) return `__idx:${index}`
    const lineNo = line.lineNo ?? line.line
    if (lineNo != null && lineNo !== '') return String(lineNo)
    if (line.timestamp != null) return `ts:${line.timestamp}:${index}`
    return `__idx:${index}`
}

export function resolveLineIndexByKey (logs = [], lineKey) {
    const target = String(lineKey ?? '')
    return logs.findIndex((line, index) => resolveLineKey(line, index) === target)
}

export function preserveHeightMapForLogs (heightMap = {}, logs = []) {
    const next = {}
    logs.forEach((log, index) => {
        const key = resolveLineKey(log, index)
        const cached = heightMap[key]
        if (Number.isFinite(cached) && cached > 0) next[key] = cached
    })
    return next
}

export function parseScrollBoxPaddingTop (computedStyle) {
    if (!computedStyle) return 0
    const value = parseFloat(computedStyle.paddingTop)
    return Number.isFinite(value) ? value : 0
}

export function resolveScrollToBottomState ({
    pendingAnchor = null,
    scrollHeight = 0,
    clientHeight = 0
} = {}) {
    return {
        pendingAnchor: null,
        scrollTop: Math.max(scrollHeight - clientHeight, 0)
    }
}

export function shouldAcceptMeasuredHeight (cached, measured, tolerance = 1) {
    if (!Number.isFinite(measured) || measured <= 0) return false
    if (!Number.isFinite(cached) || cached <= 0) return true
    return Math.abs(measured - cached) > tolerance
}

export function captureAnchorFromDom ({ rowTop, boxTop, lineKey, lineNo }) {
    return {
        lineKey: String(lineKey),
        lineNo: lineNo != null ? String(lineNo) : undefined,
        viewportTop: rowTop - boxTop
    }
}

export function captureAnchorFromIndex (index, {
    scrollTop = 0,
    paddingTop = 0,
    lineKey,
    lineNo,
    lineIndex
} = {}) {
    const idx = Number.isFinite(lineIndex) && lineIndex >= 0
        ? lineIndex
        : index.indexOfKey(lineKey)
    if (idx < 0) return null
    const lineOffset = index.prefix(idx)
    return {
        lineKey: String(lineKey ?? index.keys[idx]),
        lineNo: lineNo != null ? String(lineNo) : undefined,
        viewportTop: (paddingTop || 0) + lineOffset - scrollTop
    }
}

export function resolveAnchorLineIndex (index, anchor, logs = []) {
    if (!anchor) return -1
    const key = anchor.lineKey != null
        ? String(anchor.lineKey)
        : (anchor.lineNo != null ? String(anchor.lineNo) : '')
    let lineIndex = index.indexOfKey(key)
    if (lineIndex < 0 && anchor.lineNo != null && logs.length) {
        lineIndex = logs.findIndex((log, idx) => resolveLineKey(log, idx) === String(anchor.lineNo)
            || String(log.lineNo ?? log.line ?? '') === String(anchor.lineNo))
    }
    return lineIndex
}

export function scrollTopForAnchor (index, anchor, { paddingTop = 0, logs = [] } = {}) {
    if (!index || !anchor) return 0
    const lineIndex = resolveAnchorLineIndex(index, anchor, logs)
    if (lineIndex < 0) return 0
    const viewportTop = Number(anchor.viewportTop) || 0
    return Math.max(0, (paddingTop || 0) + index.prefix(lineIndex) - viewportTop)
}

export function scrollTopForLineKey (index, lineKey, { clientHeight = 400 } = {}) {
    const lineIndex = index.indexOfKey(lineKey)
    if (lineIndex < 0) return 0
    return Math.max(0, index.prefix(lineIndex) - clientHeight / 2)
}

export function resolveLineCenterAdjustment ({ rowTop, rowHeight, boxTop, boxHeight } = {}) {
    const values = [rowTop, rowHeight, boxTop, boxHeight].map(Number)
    if (!values.every(Number.isFinite)) return 0
    return values[0] + values[1] / 2 - (values[2] + values[3] / 2)
}

class FenwickTree {
    constructor (n = 0) {
        this.n = n
        this.tree = new Array(n + 1).fill(0)
        Object.defineProperty(this, '_opCount', {
            value: 0,
            writable: true,
            configurable: true,
            enumerable: false
        })
    }

    build (values = []) {
        this.n = values.length
        this.tree = new Array(this.n + 1).fill(0)
        this._opCount = 0
        for (let i = 0; i < this.n; i++) {
            this.tree[i + 1] = values[i]
            this._opCount += 1
        }
        for (let i = 1; i <= this.n; i++) {
            const j = i + (i & -i)
            if (j <= this.n) {
                this.tree[j] += this.tree[i]
                this._opCount += 1
            }
        }
    }

    add (index, delta) {
        if (index < 0 || index >= this.n || !delta) return
        for (let i = index + 1; i <= this.n; i += i & -i) {
            this.tree[i] += delta
            this._opCount += 1
        }
    }

    prefix (endIndex) {
        let sum = 0
        for (let i = endIndex; i > 0; i -= i & -i) {
            sum += this.tree[i]
            this._opCount += 1
        }
        return sum
    }

    point (index) {
        if (index < 0 || index >= this.n) return 0
        return this.prefix(index + 1) - this.prefix(index)
    }

    set (index, value) {
        const current = this.point(index)
        this.add(index, value - current)
    }

    findLargestPrefixLte (target) {
        let idx = 0
        let bitMask = 1
        while (bitMask <= this.n) bitMask <<= 1
        bitMask >>= 1
        let sum = 0
        while (bitMask > 0) {
            const next = idx + bitMask
            if (next <= this.n && sum + this.tree[next] <= target) {
                sum += this.tree[next]
                idx = next
                this._opCount += 1
            }
            bitMask >>= 1
        }
        return idx
    }

    findSmallestPrefixGte (target) {
        let idx = 0
        let bitMask = 1
        while (bitMask <= this.n) bitMask <<= 1
        bitMask >>= 1
        let sum = 0
        while (bitMask > 0) {
            const next = idx + bitMask
            if (next <= this.n && sum + this.tree[next] < target) {
                sum += this.tree[next]
                idx = next
                this._opCount += 1
            }
            bitMask >>= 1
        }
        return Math.min(this.n, idx + 1)
    }

    getOpCount () {
        return this._opCount
    }

    resetOpCount () {
        this._opCount = 0
    }
}

export class HeightIndex {
    constructor (defaultHeight = DEFAULT_LINE_HEIGHT) {
        this.defaultHeight = defaultHeight
        this.keys = []
        this.keyToIndex = new Map()
        this.heightMap = {}
        this.heights = []
        this.tree = new FenwickTree(0)
        Object.defineProperty(this, '_opCount', {
            value: 0,
            writable: true,
            configurable: true,
            enumerable: false
        })
    }

    get size () {
        return this.keys.length
    }

    rebuild (logs = [], heightMap = {}) {
        this._opCount = 0
        this.heightMap = { ...heightMap }
        this.keys = logs.map((log, index) => resolveLineKey(log, index))
        this.keyToIndex = new Map()
        this.heights = this.keys.map((key, index) => {
            this.keyToIndex.set(key, index)
            const cached = heightMap[key]
            return Number.isFinite(cached) && cached > 0 ? cached : this.defaultHeight
        })
        this.tree.build(this.heights)
        return this
    }

    prefix (endIndex) {
        this._opCount += 1
        return this.tree.prefix(endIndex)
    }

    total () {
        this._opCount += 1
        return this.tree.prefix(this.size)
    }

    findIndexByOffset (offset) {
        if (!this.size) return 0
        this._opCount += 1
        const target = Math.max(0, offset) + 1e-6
        const idx = this.tree.findSmallestPrefixGte(target)
        return Math.max(0, Math.min(this.size - 1, idx - 1))
    }

    findEndIndex (visibleTarget) {
        if (!this.size) return 0
        this._opCount += 1
        const end = this.tree.findSmallestPrefixGte(visibleTarget)
        return Math.min(this.size, Math.max(end, 1))
    }

    indexOfKey (key) {
        const index = this.keyToIndex.get(String(key))
        return Number.isFinite(index) ? index : -1
    }

    setHeight (key, height) {
        const index = this.indexOfKey(key)
        if (index < 0) return false
        const current = this.heights[index]
        if (!shouldAcceptMeasuredHeight(current, height)) return false
        this.heights[index] = height
        this.heightMap[String(key)] = height
        this.tree.set(index, height)
        this._opCount += 1
        return true
    }

    getOpCount () {
        return this._opCount + this.tree.getOpCount()
    }

    resetOpCount () {
        this._opCount = 0
        this.tree.resetOpCount()
    }
}

export function createHeightIndex (defaultHeight = DEFAULT_LINE_HEIGHT) {
    return new HeightIndex(defaultHeight)
}

export function resolveVirtualOverscanPx ({
    clientHeight = 400,
    scrollDelta = 0,
    overscan = VIRTUAL_OVERSCAN,
    defaultHeight = DEFAULT_LINE_HEIGHT,
    maxOverscanPx = MAX_VIRTUAL_OVERSCAN_PX,
    dynamicOverscan = false
} = {}) {
    const base = Math.max(0, Number(overscan) || 0) * defaultHeight
    if (!dynamicOverscan) return base
    const viewportBuffer = Math.max(0, Number(clientHeight) || 0) * 1.5
    const movementBuffer = Math.abs(Number(scrollDelta) || 0) * 1.25
    const limit = Math.max(defaultHeight, Number(maxOverscanPx) || MAX_VIRTUAL_OVERSCAN_PX)
    return Math.min(limit, Math.max(defaultHeight, base, viewportBuffer, movementBuffer))
}

export function resolveVirtualWindowFromIndex (index, {
    scrollTop = 0,
    clientHeight = 400,
    overscan = VIRTUAL_OVERSCAN,
    defaultHeight = DEFAULT_LINE_HEIGHT,
    scrollDelta = 0,
    maxOverscanPx = MAX_VIRTUAL_OVERSCAN_PX,
    dynamicOverscan = false
} = {}) {
    if (!index || !index.size) {
        return { start: 0, end: 0, padTop: 0, padBottom: 0, total: 0 }
    }
    const overscanPx = resolveVirtualOverscanPx({ clientHeight, scrollDelta, overscan, defaultHeight, maxOverscanPx, dynamicOverscan })
    const scrollTarget = Math.max(0, scrollTop - overscanPx)
    const visibleTarget = scrollTop + clientHeight + overscanPx
    const start = index.findIndexByOffset(scrollTarget)
    let end = index.findEndIndex(visibleTarget)
    end = Math.min(index.size, Math.max(end, start + 1))
    const padTop = index.prefix(start)
    const total = index.total()
    const padBottom = total - index.prefix(end)
    return {
        start,
        end,
        padTop,
        padBottom,
        total
    }
}
