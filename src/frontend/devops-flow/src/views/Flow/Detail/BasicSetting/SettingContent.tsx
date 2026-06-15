import type { FlowSettings } from '@/api/flowModel'
import { RunLockType } from '@/types/flow'
import { convertTime } from '@/utils/util'
import { Collapse } from 'bkui-vue'
import { computed, defineComponent, ref, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import styles from './SettingContent.module.css'

export default defineComponent({
  name: 'SettingContent',
  props: {
    basicSettings: {
      type: Object as PropType<FlowSettings>,
      required: true,
    },
  },
  setup(props) {
    const { t } = useI18n()
    const route = useRoute()
    const flowId = route.params.flowId as string

    // 默认展开的面板
    const activeIndex = ref([0, 1])

    // 定义行类型
    type InfoRow = {
      key: string
      label: string
      value: string
      grayDesc?: string
      indent?: boolean
    }

    // 基础信息行
    const baseInfoRows = computed<InfoRow[]>(() => {
      const groupNames = (props.basicSettings as any)?.groupNames || []
      const groupDisplay = groupNames.length > 0 ? groupNames.join(', ') : '--'

      return [
        {
          key: 'workflowName',
          label: t('flow.content.workflowName'),
          value: props.basicSettings?.pipelineName || '--',
        },
        {
          key: 'flowGroup',
          label: t('flow.content.flowGroup'),
          value: groupDisplay,
        },
        {
          key: 'creatorDetail',
          label: t('flow.content.creator'),
          value: props.basicSettings?.creator || '--',
          grayDesc: props.basicSettings?.createTime
            ? ` | ${convertTime(props.basicSettings.createTime)}`
            : '',
        },
        {
          key: 'modificationDetail',
          label: t('flow.content.lastModify'),
          value: props.basicSettings?.updateTime
            ? convertTime(props.basicSettings.updateTime)
            : '--',
        },
        {
          key: 'description',
          label: t('flow.content.description'),
          value: props.basicSettings?.desc || '--',
        },
      ]
    })

    // 执行配置行
    const executeConfRows = computed<InfoRow[]>(() => {
      if (!props.basicSettings) return []

      const runLockType = props.basicSettings.runLockType?.toLowerCase()
      const isMultiple = runLockType === RunLockType.MULTIPLE.toLowerCase()
      const isGroupLock = runLockType === RunLockType.GROUP_LOCK.toLowerCase()

      const rows: InfoRow[] = [
        {
          key: 'concurrencySettings',
          label: t('flow.content.concurrencySettings'),
          value: isMultiple
            ? t('flow.content.concurrentExecution')
            : isGroupLock
              ? t('flow.content.groupOnlyOneBuildTaskCanRunAtSameTime')
              : '--',
        },
      ]

      // 并发执行配置
      if (isMultiple) {
        rows.push(
          {
            key: 'maxConcurrentExecutions',
            label: t('flow.content.maxConcurrentExecutions'),
            value: String(props.basicSettings.maxConRunningQueueSize ?? '--'),
            indent: true,
          } as InfoRow,
          {
            key: 'queueTimeoutTime',
            label: t('flow.content.queueTimeoutTime'),
            value: props.basicSettings.waitQueueTimeMinute
              ? `${props.basicSettings.waitQueueTimeMinute}${t('flow.content.minutes')}`
              : '--',
            indent: true,
          } as InfoRow,
        )
      }

      // 分组互斥配置
      if (isGroupLock) {
        rows.push(
          {
            key: 'groupName',
            label: t('flow.content.groupName'),
            value: props.basicSettings.concurrencyGroup || '--',
            indent: true,
          } as InfoRow,
          {
            key: 'stopWhenNewCome',
            label: t('flow.content.stopWhenNewCome'),
            value: props.basicSettings.concurrencyCancelInProgress
              ? t('flow.common.success')
              : t('flow.common.failed'),
            indent: true,
          } as InfoRow,
        )

        // 如果未启用"新任务到来时取消正在运行的构建"，显示排队配置
        if (!props.basicSettings.concurrencyCancelInProgress) {
          rows.push(
            {
              key: 'maxQueueSize',
              label: t('flow.content.maxQueueSize'),
              value: String(props.basicSettings.maxQueueSize ?? '--'),
              indent: true,
            } as InfoRow,
            {
              key: 'maxQueueTime',
              label: t('flow.content.maxQueueTime'),
              value: props.basicSettings.waitQueueTimeMinute
                ? `${props.basicSettings.waitQueueTimeMinute}${t('flow.content.minutes')}`
                : '--',
              indent: true,
            } as InfoRow,
          )
        }
      }

      return rows
    })

    // Collapse 列表数据
    const collapseList = computed(() => [
      {
        name: t('flow.content.basicInfo'),
        rows: baseInfoRows.value,
      },
      {
        name: t('flow.content.executionSettings'),
        rows: executeConfRows.value,
      },
    ])

    return () => (
      <div class={styles.settingContent}>
        <Collapse v-model={activeIndex.value} list={collapseList.value} class={styles.collapse}>
          {{
            content: (item: { rows: InfoRow[] }) => {
              const normalRows: InfoRow[] = []
              const indentRows: InfoRow[] = []

              item.rows.forEach((row) => {
                if (row.indent) {
                  indentRows.push(row)
                } else {
                  normalRows.push(row)
                }
              })

              return (
                <div class={styles.panelContent}>
                  {normalRows.map((row) => (
                    <div key={row.key} class={styles.infoRow}>
                      <label class={styles.label}>{row.label}</label>
                      <span class={styles.value}>
                        {row.value}
                        {row.grayDesc && <span class={styles.grayDesc}>{row.grayDesc}</span>}
                      </span>
                    </div>
                  ))}
                  {indentRows.length > 0 && (
                    <div class={styles.indentRowContainer}>
                      {indentRows.map((row) => (
                        <div key={row.key} class={styles.infoRow}>
                          <label class={styles.label}>{row.label}</label>
                          <span class={styles.value}>
                            {row.value}
                            {row.grayDesc && <span class={styles.grayDesc}>{row.grayDesc}</span>}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              )
            },
          }}
        </Collapse>
      </div>
    )
  },
})
