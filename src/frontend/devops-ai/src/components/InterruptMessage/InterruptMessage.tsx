import { Button as BkButton } from 'bkui-vue'
import { defineComponent, type PropType } from 'vue'
import { MessageStatus } from '@blueking/chat-x'
import styles from './InterruptMessage.module.css'

const REASON_LABELS: Record<string, string> = {
  human_approval: '需要审批',
  upload_required: '需要上传',
  policy_hold: '策略限制',
  database_modification: '数据变更确认',
}

export default defineComponent({
  name: 'InterruptMessage',
  props: {
    message: { type: Object, required: true },
  },
  emits: ['approve', 'reject'],
  setup(props, { emit }) {
    return () => {
      const { message } = props
      const interrupt = message.interrupt || {}
      const isPending = message.status === MessageStatus.Pending
      const reason = interrupt.reason
      const payload = interrupt.payload
      const reasonLabel = (reason && REASON_LABELS[reason]) || reason || ''

      const hasStructuredPayload = payload && typeof payload === 'object'
      const displayPayload = hasStructuredPayload
        ? { ...payload }
        : null
      if (displayPayload) delete displayPayload.message

      const hasPayloadDetails = displayPayload && Object.keys(displayPayload).length > 0

      return (
        <div class={styles.root}>
          <div class={styles.header}>
            <span class={styles.icon}>
              <svg viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" />
              </svg>
            </span>
            <span>等待确认</span>
            {reasonLabel && <span class={styles.reason}>{reasonLabel}</span>}
          </div>

          <div class={styles.body}>{message.content}</div>

          {hasPayloadDetails && (
            <div class={styles.details}>
              <pre>{JSON.stringify(displayPayload, null, 2)}</pre>
            </div>
          )}

          {isPending ? (
            <div class={styles.actions}>
              <BkButton
                theme="primary"
                size="small"
                onClick={() => emit('approve', { approved: true })}
              >
                确认继续
              </BkButton>
              <BkButton
                size="small"
                onClick={() => emit('reject')}
              >
                拒绝
              </BkButton>
            </div>
          ) : (
            <div class={styles.resolved}>已处理</div>
          )}
        </div>
      )
    }
  },
})
