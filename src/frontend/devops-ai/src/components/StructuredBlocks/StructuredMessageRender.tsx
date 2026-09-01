import { ContentRender, MessageRender, MessageToolsStatus } from '@blueking/chat-x'
import { Component, computed, defineComponent, h, PropType } from 'vue'
import DataTableActivity from '../ActivityMessage/DataTableActivity'
import KeyValueActivity from '../ActivityMessage/KeyValueActivity'
import OperationResultActivity from '../ActivityMessage/OperationResultActivity'
import {
  containsBkBlocks,
  ContentSegment,
  parseStructuredContent,
} from './parser'
import SelectionFormBlock from './SelectionFormBlock'
import styles from './StructuredBlocks.module.css'

const blockComponentMap: Record<string, Component> = {
  'bk-table': DataTableActivity,
  'bk-kv': KeyValueActivity,
  'bk-status': OperationResultActivity,
  'bk-form': SelectionFormBlock,
}

export default defineComponent({
  name: 'StructuredMessageRender',
  props: {
    message: { type: Object, required: true },
    messageToolsStatus: { type: Object as PropType<MessageToolsStatus> },
    onSendMessage: { type: Function as PropType<(msg: string) => void> },
  },
  setup(props) {
    const hasBkBlocks = computed(() =>
      typeof props.message.content === 'string' &&
      containsBkBlocks(props.message.content),
    )

    const blockDataCache = new Map<string, unknown>()

    const segments = computed<ContentSegment[]>(() => {
      if (!hasBkBlocks.value) return []
      const parsed = parseStructuredContent(props.message.content as string)
      return parsed.map((seg) => {
        if (seg.type !== 'bk-block') return seg
        const cached = blockDataCache.get(seg.raw)
        if (cached) return { ...seg, data: cached }
        blockDataCache.set(seg.raw, seg.data)
        return seg
      })
    })

    function renderSegment(segment: ContentSegment, index: number) {
      if (segment.type === 'text') {
        return (
          <ContentRender
            key={`text-${index}`}
            content={segment.content}
            status={props.message.status}
          />
        )
      }

      if (segment.type === 'bk-block') {
        const Comp = blockComponentMap[segment.blockType]
        if (!Comp) {
          return (
            <ContentRender
              key={`fallback-${index}`}
              content={`\`\`\`${segment.blockType}\n${segment.raw}\n\`\`\``}
              status={props.message.status}
            />
          )
        }

        const extraProps =
          segment.blockType === 'bk-form'
            ? { onSendMessage: props.onSendMessage }
            : {}
        return h(Comp, {
          key: `block-${index}`,
          content: segment.data,
          ...extraProps,
        })
      }

      if (segment.type === 'bk-block-pending') {
        return (
          <div key={`pending-${index}`} class={styles.blockLoading}>
            <div class={styles.skeleton}>
              <div class={styles.skeletonLine} />
              <div class={styles.skeletonLine} />
              <div class={styles.skeletonLine} />
            </div>
          </div>
        )
      }

      return null
    }

    return () => {
      if (!hasBkBlocks.value) {
        return (
          <MessageRender
            message={props.message}
            messageToolsStatus={props.messageToolsStatus}
          />
        )
      }

      return segments.value.map(renderSegment)
    }
  },
})
