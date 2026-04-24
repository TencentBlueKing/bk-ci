import { ContentRender, MessageStatus } from '@blueking/chat-x'
import { Tag as BkTag, Loading } from 'bkui-vue'
import { computed, defineComponent, ref, watch } from 'vue'
import styles from './SubAgentMessage.module.css'

const TOOL_STATUS_CONFIG: Record<string, { theme: string; label: string; icon: string }> = {
  [MessageStatus.Streaming]: { theme: 'info', label: '执行中', icon: '⏳' },
  [MessageStatus.Complete]: { theme: 'success', label: '已完成', icon: '✅' },
  [MessageStatus.Error]: { theme: 'danger', label: '失败', icon: '❌' },
}

const STATUS_CONFIG = {
  reasoning: { theme: 'warning', label: '思考中' },
  streaming: { theme: 'info', label: '回复中' },
  complete: { theme: 'success', label: '已完成' },
  error: { theme: 'danger', label: '出错' },
} as const

function getStatus(message: any) {
  if (message.status === MessageStatus.Error) return STATUS_CONFIG.error
  if (message.status === MessageStatus.Complete) return STATUS_CONFIG.complete
  if (Array.isArray(message.content)) return STATUS_CONFIG.reasoning
  return STATUS_CONFIG.streaming
}

function buildBlocks(message: any): any[] {
  if (message.blocks?.length) {
    return message.blocks.map((b: any) =>
      b.type === 'content'
        ? { type: 'content', content: b.content }
        : { type: 'toolCall', toolCallId: b.toolCallId },
    )
  }

  const result: any[] = []
  const { content, toolCalls = [] } = message
  const isReasoning = Array.isArray(content)
  const hasContent = isReasoning
    ? content.length > 0 && content.some((t: string) => t)
    : !!content

  if (hasContent) result.push({ type: 'content', content })
  for (const tc of toolCalls) {
    result.push({ type: 'toolCall', toolCallId: tc.id })
  }
  return result
}

export default defineComponent({
  name: 'SubAgentMessage',
  props: {
    message: { type: Object, required: true },
  },
  setup(props) {
    const blocks = computed(() => buildBlocks(props.message))
    const collapsed = ref(false)
    const isComplete = computed(() =>
      props.message.status === MessageStatus.Complete || props.message.status === MessageStatus.Error,
    )

    watch(isComplete, (val) => {
      if (val) collapsed.value = true
    }, { immediate: true })

    return () => {
      const { message } = props
      const status = getStatus(message)
      const toolCalls = message.toolCalls || []

      return (
        <div class={[styles.root, collapsed.value && styles.collapsed]}>
          <div
            class={[styles.header, isComplete.value && styles.headerClickable]}
            onClick={() => { if (isComplete.value) collapsed.value = !collapsed.value }}
          >
            {isComplete.value && (
              <span class={[styles.arrow, !collapsed.value && styles.arrowExpanded]}>▶</span>
            )}
            <span class={styles.agentName}>{message.agentName}</span>
            <BkTag theme={status.theme} size="small">{status.label}</BkTag>
          </div>

          {!collapsed.value && blocks.value.map((block: any, i: number) => {
            if (block.type === 'content' && block.content) {
              const isReasoning = Array.isArray(block.content)
              return (
                <div class={styles.body} key={`content-${i}`}>
                  {isReasoning
                    ? (block.content as string[]).map((text: string, j: number) => (
                        <ContentRender key={j} content={text} status={message.status} />
                      ))
                    : <ContentRender content={block.content} status={message.status} />}
                </div>
              )
            }
            if (block.type === 'toolCall') {
              const tc = toolCalls.find((t: any) => t.id === block.toolCallId)
              if (!tc) return null
              const tcStatus = tc.toolMessage?.status ?? MessageStatus.Streaming
              const cfg = TOOL_STATUS_CONFIG[tcStatus] ?? TOOL_STATUS_CONFIG[MessageStatus.Streaming]
              return (
                <div class={styles.toolCallItem} key={`tc-${block.toolCallId}`}>
                  <span class={styles.toolCallNameContainer}>
                    {
                      tcStatus === MessageStatus.Streaming ? (
                        <Loading size="mini" mode="spin" theme="primary" class={styles.toolCallIcon}></Loading>
                      ) : (
                        <span class={styles.toolCallIcon}>{cfg.icon}</span>
                      )
                    }
                    <span class={styles.toolCallName}>{tc.function?.name ?? 'unknown'}</span>
                  </span>
                  <BkTag theme={cfg.theme} size="small">{cfg.label}</BkTag>
                </div>
              )
            }
            return null
          })}
        </div>
      )
    }
  },
})
