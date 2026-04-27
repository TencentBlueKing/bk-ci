export interface TextSegment {
  type: 'text'
  content: string
}

export interface BkBlockSegment {
  type: 'bk-block'
  blockType: string
  data: unknown
  raw: string
}

export interface BkBlockPendingSegment {
  type: 'bk-block-pending'
  blockType: string
}

export type ContentSegment =
  | TextSegment
  | BkBlockSegment
  | BkBlockPendingSegment

const SUPPORTED_TYPES = new Set(['bk-table', 'bk-kv', 'bk-status', 'bk-form'])

// Matches closed XML-style tags: <bk-table>...</bk-table>
const BK_TAG_RE = /<bk-(table|kv|status|form)>([\s\S]*?)<\/bk-\1>/g

// Matches an unclosed opening tag at the end of content (streaming state)
const BK_TAG_OPEN_RE = /<bk-(table|kv|status|form)>[^]*$/

export function containsBkBlocks(content: string): boolean {
  if (typeof content !== 'string') return false
  BK_TAG_RE.lastIndex = 0
  return BK_TAG_RE.test(content) || BK_TAG_OPEN_RE.test(content)
}

export function parseStructuredContent(content: string): ContentSegment[] {
  const segments: ContentSegment[] = []
  let lastIndex = 0

  BK_TAG_RE.lastIndex = 0
  let match: RegExpExecArray | null

  while ((match = BK_TAG_RE.exec(content)) !== null) {
    if (match.index > lastIndex) {
      const text = content.slice(lastIndex, match.index)
      if (text.trim()) {
        segments.push({ type: 'text', content: text })
      }
    }

    const tagName = match[1]
    const blockType = `bk-${tagName}`
    const rawJson = match[2].trim()

    if (!SUPPORTED_TYPES.has(blockType)) {
      segments.push({ type: 'text', content: match[0] })
    } else {
      try {
        const data = JSON.parse(rawJson)
        segments.push({ type: 'bk-block', blockType, data, raw: rawJson })
      } catch {
        segments.push({ type: 'text', content: match[0] })
      }
    }

    lastIndex = match.index + match[0].length
  }

  const remaining = content.slice(lastIndex)
  if (remaining) {
    const openMatch = remaining.match(BK_TAG_OPEN_RE)
    if (openMatch) {
      const blockType = `bk-${openMatch[1]}`
      const textBefore = remaining.slice(0, openMatch.index)
      if (textBefore.trim()) {
        segments.push({ type: 'text', content: textBefore })
      }
      segments.push({ type: 'bk-block-pending', blockType })
    } else if (remaining.trim()) {
      segments.push({ type: 'text', content: remaining })
    }
  }

  return segments
}
