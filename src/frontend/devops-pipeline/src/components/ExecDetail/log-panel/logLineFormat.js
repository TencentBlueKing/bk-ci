const ANSI_COLORS = {
    30: '#000000', 31: '#ff6666', 32: '#7fca54', 33: '#ff9c01',
    34: '#5fb4e1', 35: '#ff00ff', 36: '#00ffff', 37: '#ffffff',
    90: '#83828c'
}
const FORMAT_CACHE = new WeakMap()
const VALID_MARKER = /^##\[(command|info|warning|warn|error|debug|group|endgroup)(?:\s+([^\]]*))?\]\s*/i
const MARKER_COLORS = { command: '#5fb4e1', info: '#f0f1f5', warning: '#e18732', warn: '#e18732', error: '#d25050', debug: '#83828c' }

function escapeHtml (value) {
    return String(value).replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char])
}

function hasControlChar (value, { includeC1 = false, extra = '' } = {}) {
    const text = String(value)
    for (let index = 0; index < text.length; index++) {
        const code = text.charCodeAt(index)
        if (code <= 0x1f || code === 0x7f || (includeC1 && code >= 0x80 && code <= 0x9f) || extra.includes(text[index])) return true
    }
    return false
}

function decodeEntitiesOnce (value) {
    let valid = true
    const decoded = String(value).replace(/&([^;\s]{1,32});/g, (entity, name) => {
        const key = name.toLowerCase()
        const named = { amp: '&', lt: '<', gt: '>', quot: '"', apos: "'", nbsp: '\u00a0' }
        if (Object.prototype.hasOwnProperty.call(named, key)) return named[key]
        const isHex = /^#x[\da-f]+$/i.test(key)
        const isDecimal = /^#\d+$/.test(key)
        const number = isHex ? parseInt(key.slice(2), 16) : isDecimal ? parseInt(key.slice(1), 10) : NaN
        if (Number.isInteger(number) && number > 0 && number <= 0x10ffff && !(number >= 0xd800 && number <= 0xdfff)) return String.fromCodePoint(number)
        valid = false
        return entity
    })
    return { value: decoded, valid }
}

function safeHref (source) {
    const raw = String(source || '').replace(/^[\t\n\f\r ]+|[\t\n\f\r ]+$/g, '')
    if (hasControlChar(raw)) return ''
    const decoded = decodeEntitiesOnce(raw)
    if (!decoded.valid || hasControlChar(decoded.value)) return ''
    if (/^https?:\/\//i.test(decoded.value)) {
        try {
            const url = new URL(decoded.value)
            return /^https?:$/.test(url.protocol) && url.hostname ? url.href : ''
        } catch (error) {
            return ''
        }
    }
    if (/[\u0080-\u009f]/.test(decoded.value) || !decoded.value.startsWith('/') || decoded.value.startsWith('//') || decoded.value.includes('\\')) return ''
    const pathname = decoded.value.split(/[?#]/, 1)[0]
    if (!/^\/console(?:\/|$)/.test(pathname) || /%(?:2f|5c|25)/i.test(pathname)) return ''
    for (const segment of pathname.split('/')) {
        let decodedSegment
        try {
            decodedSegment = decodeURIComponent(segment)
        } catch (error) {
            return ''
        }
        if (decodedSegment === '.' || decodedSegment === '..' || hasControlChar(decodedSegment, { includeC1: true, extra: '/\\' })) return ''
    }
    try {
        const trustedOrigin = 'https://console-link.invalid'
        const url = new URL(decoded.value, trustedOrigin)
        if (url.origin !== trustedOrigin || !/^\/console(?:\/|$)/.test(url.pathname)) return ''
        const canonicalHref = url.href.slice(trustedOrigin.length)
        if (typeof window === 'undefined' || typeof window.__prefixSameOriginUrl !== 'function') return canonicalHref

        const prefixRoot = window.__prefixSameOriginUrl('/console')
        if (typeof prefixRoot !== 'string' || !prefixRoot.startsWith('/') || prefixRoot.startsWith('//') || hasControlChar(prefixRoot, { includeC1: true, extra: '?#\\' })) return ''
        const rawRootPath = prefixRoot.split(/[?#]/, 1)[0]
        if (/%(?:2f|5c|25)/i.test(rawRootPath)) return ''
        for (const segment of rawRootPath.split('/')) {
            let decodedSegment
            try {
                decodedSegment = decodeURIComponent(segment)
            } catch (error) {
                return ''
            }
            if (decodedSegment === '.' || decodedSegment === '..' || hasControlChar(decodedSegment, { includeC1: true, extra: '/\\' })) return ''
        }
        const rootUrl = new URL(prefixRoot, trustedOrigin)
        if (rootUrl.origin !== trustedOrigin || !/(?:^|\/)console$/.test(rootUrl.pathname)) return ''
        for (const segment of rootUrl.pathname.split('/')) {
            let decodedSegment
            try {
                decodedSegment = decodeURIComponent(segment)
            } catch (error) {
                return ''
            }
            if (decodedSegment === '.' || decodedSegment === '..' || hasControlChar(decodedSegment, { includeC1: true, extra: '/\\' })) return ''
        }

        const consoleSuffix = url.pathname.slice('/console'.length)
        const suffixAndFragment = canonicalHref.slice(url.pathname.length)
        const expectedRawHref = `${prefixRoot}${consoleSuffix}${suffixAndFragment}`
        const expectedCanonicalHref = `${rootUrl.pathname}${consoleSuffix}${suffixAndFragment}`
        const prefixedHref = window.__prefixSameOriginUrl(canonicalHref)
        if (typeof prefixedHref !== 'string' || !prefixedHref.startsWith('/') || prefixedHref.startsWith('//') || hasControlChar(prefixedHref, { includeC1: true, extra: '\\' })) return ''
        if (prefixedHref !== expectedRawHref) return ''
        const prefixedUrl = new URL(prefixedHref, trustedOrigin)
        if (prefixedUrl.origin !== trustedOrigin || prefixedUrl.pathname !== `${rootUrl.pathname}${consoleSuffix}`) return ''
        if (prefixedUrl.href.slice(trustedOrigin.length) !== expectedCanonicalHref) return ''
        return prefixedHref
    } catch (error) {
        return ''
    }
}

function parseAnchorAttributes (source) {
    const attrs = []
    const input = String(source || '')
    let index = 0
    while (index < input.length) {
        while (/\s/.test(input[index] || '')) index += 1
        if (index >= input.length) break
        const name = input.slice(index).match(/^([a-zA-Z_:][\w:.-]*)/)
        if (!name) return null
        index += name[0].length
        while (/\s/.test(input[index] || '')) index += 1
        if (input[index] !== '=') {
            attrs.push({ name: name[0].toLowerCase(), value: '' })
            continue
        }
        index += 1
        while (/\s/.test(input[index] || '')) index += 1
        const quote = input[index]
        if (quote !== '"' && quote !== "'") {
            const unquoted = input.slice(index).match(/^[^\s"'=<>`]+/)
            if (!unquoted) return null
            attrs.push({ name: name[0].toLowerCase(), value: unquoted[0] })
            index += unquoted[0].length
            continue
        }
        index += 1
        const end = input.indexOf(quote, index)
        if (end < 0) return null
        attrs.push({ name: name[0].toLowerCase(), value: input.slice(index, end) })
        index = end + 1
    }
    return attrs
}

function parseAnsi (source, links = []) {
    const text = String(source ?? '')
    const segments = []
    let color = ''
    let href = ''
    let visible = ''
    const flush = () => {
        if (visible) segments.push({ text: visible, color, href })
        visible = ''
    }
    const sourceHref = position => links.find(link => position >= link.start && position < link.end)?.href || ''
    let index = 0
    while (index < text.length) {
        if (text[index] === '\u001b' && text[index + 1] === ']') {
            const start = index
            let end = index + 2
            while (end < text.length && text.charCodeAt(end) !== 0x07 && text.charCodeAt(end) !== 0x9c && !(text[end] === '\u001b' && text[end + 1] === '\\')) end += 1
            if (end < text.length) {
                flush(); index = text[end] === '\u001b' ? end + 2 : end + 1
            } else {
                flush()
                visible = text.slice(start + 1)
                index = text.length
            }
            continue
        }
        if (text[index] === '\u009d') {
            const start = index
            let end = index + 1
            while (end < text.length && text.charCodeAt(end) !== 0x07 && text.charCodeAt(end) !== 0x9c) end += 1
            if (end < text.length) {
                flush(); index = end + 1
            } else {
                flush()
                visible = text.slice(start + 1)
                index = text.length
            }
            continue
        }
        const esc = text[index] === '\u001b' || text[index] === '\u009b'
        const bare = text[index] === '[' && /^\[(?:\d+(?:;\d+)*)?m/.test(text.slice(index))
        if (esc || bare) {
            const match = esc
                ? text.slice(index + 1).match(text[index] === '\u009b' ? /^[0-?]*[ -/]*[@-~]/ : /^\[[0-?]*[ -/]*[@-~]/)
                : text.slice(index).match(/^\[(?:\d+(?:;\d+)*)?m/)
            if (match) {
                const sequence = esc ? text[index] + match[0] : match[0]
                if (/m$/.test(sequence)) {
                    flush()
                    const codes = sequence.slice(sequence.indexOf('[') + 1, -1).split(';')
                    for (let codeIndex = 0; codeIndex < codes.length; codeIndex++) {
                        const code = Number(codes[codeIndex] || 0)
                        if (code === 38 || code === 48) {
                            const mode = Number(codes[codeIndex + 1])
                            codeIndex += mode === 2 ? 4 : mode === 5 ? 2 : 0
                        } else if (code === 0 || code === 39) color = ''
                        else if (ANSI_COLORS[code]) color = ANSI_COLORS[code]
                    }
                }
                index += sequence.length
                continue
            }
            // A malformed/incomplete control marker must not consume the rest of the record.
            if (esc) {
                index += 1; continue
            }
        }
        const code = text.charCodeAt(index)
        if (code < 0x20 && code !== 0x09 && code !== 0x0a && code !== 0x0d && code !== 0x08) {
            flush()
            index += 1
            continue
        }
        if (code === 0x1b || (code >= 0x80 && code <= 0x9f)) {
            flush()
            index += 1; continue
        }
        const nextHref = sourceHref(index)
        if (nextHref !== href) {
            flush(); href = nextHref
        }
        visible += text[index]
        index += 1
    }
    flush()
    if (!segments.length) segments.push({ text: '', color: '', href: '' })
    return segments
}

function flattenSegments (segments) {
    return segments.map(segment => segment.text).join('')
}

function prepareAnchors (source) {
    const links = []
    let output = ''
    let sourceOffset = 0
    const anchor = /<a\b([^>]*)>([\s\S]*?)<\/a\s*>/gi
    let match
    while ((match = anchor.exec(source))) {
        output += source.slice(sourceOffset, match.index)
        const attrs = parseAnchorAttributes(match[1])
        const hrefAttrs = attrs?.filter(attr => attr.name === 'href') || []
        const href = hrefAttrs.length === 1 ? safeHref(hrefAttrs[0].value) : ''
        const body = match[2]
        if (/<[^>]*>/.test(body)) {
            output += match[0]
        } else {
            // 不可信或无法识别的 href 只降级为纯文本，不把 <a> 标签源码暴露在日志里
            const text = decodeEntitiesOnce(body).value || href
            const start = output.length
            output += text
            if (href) links.push({ start, end: output.length, href })
        }
        sourceOffset = match.index + match[0].length
    }
    output += source.slice(sourceOffset)
    return { text: output, links }
}

function normalizeControls (segments) {
    const input = []
    segments.forEach(segment => Array.from(segment.text).forEach(char => input.push({ ...segment, text: char })))
    const output = []
    const columns = []
    let column = 0
    let marker = null
    const text = input.map(item => item.text).join('')
    const markerMatch = text.match(VALID_MARKER)
    const markerLength = markerMatch ? markerMatch[0].length : 0
    if (markerMatch) {
        marker = { type: markerMatch[1].toLowerCase(), value: markerMatch[2] || '' }
        input.splice(0, Array.from(text.slice(0, markerLength)).length)
        const markerColor = MARKER_COLORS[marker.type]
        if (markerColor) input.forEach(item => {
            if (!item.color) item.color = markerColor
        })
    }
    const pushOutput = (item, width) => {
        column += width
        output.push(item)
        columns.push(column)
    }
    for (let index = 0; index < input.length; index++) {
        const item = input[index]
        if (item.text === '\r') {
            output.push({ ...item, text: '\n' }); column = 0; columns.push(column)
            if (input[index + 1]?.text === '\n') index += 1
        } else if (item.text === '\n') {
            output.push(item); column = 0; columns.push(column)
        } else if (item.text === '\t') {
            const count = 4 - (column % 4)
            for (let i = 0; i < count; i++) pushOutput({ ...item, text: ' ' }, 1)
        } else if (item.text === '\b') {
            if (output.length && output[output.length - 1].text !== '\n') {
                output.pop()
                columns.pop()
                column = columns[columns.length - 1] ?? 0
            }
        } else {
            pushOutput(item, visualColumnWidth(item.text))
        }
    }
    return { marker, segments: mergeCharacterSegments(output) }
}

function visualColumnWidth (value) {
    return Array.from(value).reduce((width, char) => width + (/^[\u1100-\u11ff\u2e80-\u9fff\uf900-\ufaff\uff01-\uff60\uac00-\ud7af]$/u.test(char) ? 2 : 1), 0)
}

function mergeCharacterSegments (tokens) {
    const segments = []
    tokens.forEach(token => {
        const previous = segments[segments.length - 1]
        if (previous && previous.color === token.color && previous.href === token.href) previous.text += token.text
        else segments.push({ text: token.text, color: token.color || '', href: token.href || '' })
    })
    return segments.length ? segments : [{ text: '', color: '', href: '' }]
}

export function parseLogLine (source) {
    const input = typeof source === 'object' && source !== null ? source : null
    const raw = input ? String(input.text ?? input.content ?? input.message ?? '') : String(source ?? '')
    const sourceKey = input ? `${input.lineNo ?? input.line ?? ''}:${input.timestamp ?? ''}` : ''
    if (input && FORMAT_CACHE.has(input)) {
        const cached = FORMAT_CACHE.get(input)
        if (cached.raw === raw && cached.sourceKey === sourceKey) return cached.value
    }
    if (!hasControlChar(raw, { includeC1: true, extra: '<>&[' })) {
        const value = { plainText: raw, segments: [{ text: raw, color: '', href: '' }], marker: null, sourceKey }
        if (input) FORMAT_CACHE.set(input, { raw, sourceKey, value })
        return value
    }
    const prepared = prepareAnchors(raw)
    const ansi = parseAnsi(prepared.text, prepared.links)
    const { marker, segments } = normalizeControls(ansi)
    const plainText = flattenSegments(segments)
    const value = {
        plainText,
        segments,
        marker,
        sourceKey
    }
    if (input) FORMAT_CACHE.set(input, { raw, sourceKey, value })
    return value
}

export function renderFormattedLogHtml (parsed, keyword = '', allowLinks = true) {
    const segments = parsed?.segments || []
    const fullText = parsed?.plainText || ''
    const search = String(keyword || '').toLowerCase()
    const ranges = []
    if (search) {
        let from = 0
        const lower = fullText.toLowerCase()
        while ((from = lower.indexOf(search, from)) !== -1) {
            ranges.push({ start: from, end: from + search.length })
            from += Math.max(search.length, 1)
        }
    }
    let offset = 0
    return segments.map(segment => {
        const start = offset
        const end = start + segment.text.length
        offset = end
        const chunks = []
        let cursor = 0
        ranges.filter(range => range.start < end && range.end > start).forEach(range => {
            const left = Math.max(range.start, start) - start
            const right = Math.min(range.end, end) - start
            if (left > cursor) chunks.push(escapeHtml(segment.text.slice(cursor, left)))
            chunks.push(`<mark class="lp-hl">${escapeHtml(segment.text.slice(left, right))}</mark>`)
            cursor = right
        })
        if (cursor < segment.text.length) chunks.push(escapeHtml(segment.text.slice(cursor)))
        let body = chunks.join('')
        if (segment.color) body = `<span style="color:${segment.color}">${body}</span>`
        if (allowLinks && segment.href) body = `<a href="${escapeHtml(segment.href)}" target="_blank" rel="noopener noreferrer">${body}</a>`
        return body
    }).join('')
}

export function clearLogFormatCache (log) {
    if (log && typeof log === 'object') FORMAT_CACHE.delete(log)
}
