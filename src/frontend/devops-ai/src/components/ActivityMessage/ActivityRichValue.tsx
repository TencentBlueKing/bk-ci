import { Button } from 'bkui-vue'
import { defineComponent, PropType, VNode } from 'vue'
import styles from './ActivityMessage.module.css'

const DOWNLOAD_RE = /<download>([\s\S]*?)<\/download>/gi

function isSafeDownloadHref(href: string): boolean {
  const t = href.trim()
  if (!t) return false
  try {
    const base =
      typeof window !== 'undefined' && window.location?.href
        ? window.location.href
        : 'http://localhost/'
    const u = new URL(t, base)
    return u.protocol === 'http:' || u.protocol === 'https:'
  } catch {
    return false
  }
}

function openSafeDownload(href: string) {
  const url = href.trim()
  const a = document.createElement('a')
  a.href = url
  a.target = '_blank'
  a.rel = 'noopener noreferrer'
  a.click()
}

/**
 * Table / KV 单元格：将 `<download>url</download>` 渲染为下载按钮，其余文本原样展示。
 */
export default defineComponent({
  name: 'ActivityRichValue',
  props: {
    cell: {
      type: null as unknown as PropType<unknown>,
      required: true,
    },
  },
  setup(props) {
    return () => {
      const str = props.cell == null ? '' : String(props.cell)
      if (!str || !/<download>/i.test(str)) {
        return str
      }

      const parts: Array<string | VNode> = []
      let last = 0
      let m: RegExpExecArray | null
      const re = new RegExp(DOWNLOAD_RE.source, DOWNLOAD_RE.flags)
      while ((m = re.exec(str)) !== null) {
        if (m.index > last) {
          parts.push(str.slice(last, m.index))
        }
        const inner = m[1].trim()
        last = m.index + m[0].length
        if (inner && isSafeDownloadHref(inner)) {
          parts.push(
            <Button
              key={`dl-${m.index}`}
              class={styles.activityDownloadBtn}
              theme="primary"
              size="small"
              title={inner}
              onClick={() => openSafeDownload(inner)}
            >
              下载
            </Button>,
          )
        } else if (inner) {
          parts.push(inner)
        }
      }
      if (last < str.length) {
        parts.push(str.slice(last))
      }

      return <span class={styles.activityRichCell}>{parts}</span>
    }
  },
})
