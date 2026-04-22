import { defineComponent, type PropType } from 'vue'
import { Alert, Table } from 'bkui-vue'
import type { OperationResultContent } from './types'
import styles from './ActivityMessage.module.css'

const ALERT_THEME: Record<string, string> = {
  success: 'success',
  error: 'danger',
  partial: 'warning',
}

const DETAIL_COLUMNS = [
  { field: 'id', label: 'ID', width: 120 },
  { field: 'status', label: '状态', width: 80 },
  { field: 'message', label: '详情' },
]

export default defineComponent({
  name: 'OperationResultActivity',
  props: {
    content: { type: Object as PropType<OperationResultContent>, required: true },
  },
  setup(props) {
    return () => {
      const { title, status, message, details } = props.content
      const theme = ALERT_THEME[status] || 'info'

      return (
        <div class={styles.activityCard}>
          <div class={styles.cardTitle}>{title}</div>
          <Alert theme={theme} title={message} />
          {details?.length ? (
            <div class={styles.detailTable}>
              <Table
                data={details}
                columns={DETAIL_COLUMNS}
                border={['row']}
                stripe
                showOverflowTooltip
              />
            </div>
          ) : null}
        </div>
      )
    }
  },
})
