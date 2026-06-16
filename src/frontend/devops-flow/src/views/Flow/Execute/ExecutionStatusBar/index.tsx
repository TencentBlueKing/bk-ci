import Summary from '@/components/Summary'
import { SvgIcon } from '@/components/SvgIcon'
import { useExecuteDetail } from '@/hooks/useExecuteDetail'
import { STATUS, type StatusType } from '@/types/flow'
import { isRunning, mapThemeOfStatus } from '@/utils/flowStatus'
import { convertTime } from '@/utils/util'
import { Tag } from 'bkui-vue'
import { computed, defineComponent, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './ExecutionStatusBar.module.css'

export default defineComponent({
  name: 'ExecutionStatusBar',
  setup() {
    const { t } = useI18n()
    // 从 store 获取执行详情数据（全局唯一）
    const { executeDetail } = useExecuteDetail()

    const summaryVisible = ref(true)
    const show = ref(false)

    // 状态标签主题映射
    const statusTagTheme = computed(() => {
      return mapThemeOfStatus(executeDetail.value?.status as StatusType)
    })

    // 状态标签文本
    const statusLabel = computed(() => {
      return executeDetail.value?.status ? t(`flow.statusMap.${executeDetail.value.status}`) : ''
    })

    const isRunningOrNot = computed(() => isRunning(executeDetail.value?.status))

    // 格式化开始时间
    const execFormatStartTime = computed(() => {
      return convertTime(executeDetail.value?.queueTime || 0)
    })

    const recordList = computed(() => {
      const list = [...(executeDetail.value?.recordList || [])]
      return (
        list.reverse().map((record, index) => ({
          id: index + 1,
          user: record.startUser,
        })) ?? []
      )
    })

    const executeCount = computed(() => {
      return executeDetail.value?.executeCount || 0
    })

    // 触发用户
    const startUser = computed(() => {
      return recordList.value.find((i) => i.id === executeCount.value)?.user || ''
    })

    // 折叠/展开
    const collapseSummary = () => {
      summaryVisible.value = !summaryVisible.value
    }

    // 渲染状态图标
    const renderStatusIcon = () => {
      if (executeDetail.value?.status === STATUS.QUEUE) {
        return <SvgIcon class="hourglass-queue" name="hourglass" />
      }
      if (executeDetail.value?.status === STATUS.RUNNING) {
        return <SvgIcon class="spinIcon" name="circle-2-1" />
      }
      return null
    }

    const handlerScroll = (e: any) => {
      show.value = e.target.scrollTop > 88
    }

    return () => {
      if (!executeDetail.value) {
        return null
      }

      return (
        <div class={styles.statusBarWrapper}>
          <div class={styles.execDetailSummaryHeader} onScroll={handlerScroll}>
            <span
              class={[
                styles.execDetailBuildSummaryAnchor,
                executeDetail.value?.status ? styles[executeDetail.value.status] : null,
              ]}
            ></span>
            <aside class={styles.execDetailSummaryHeaderTitle}>
              <Tag class={styles.execStatusTag} type="stroke" theme={statusTagTheme.value}>
                <span class={styles.execStatusLabel}>
                  {isRunningOrNot.value ? renderStatusIcon() : null}

                  {statusLabel.value}

                  {executeDetail.value?.status === STATUS.CANCELED && (
                    <SvgIcon
                      name="info-circle"
                      class={styles.infoIcon}
                      v-bk-tooltips={`${t('flow.execute.canceller')}：${executeDetail.value?.cancelUserId || '--'}`}
                    />
                  )}
                </span>
              </Tag>
              <span v-bk-overflow-tips class={styles.execDetailSummaryHeaderBuildMsg}>
                {executeDetail.value?.buildMsg}
              </span>
            </aside>

            <aside class={styles.execDetailSummaryHeaderTrigger}>
              {executeDetail.value?.triggerUserProfile ? (
                <img class={styles.execTriggerProfile} />
              ) : null}

              <SvgIcon class={styles.execTriggerProfile} name="default-user" size={24} />

              {startUser.value ? (
                <span>
                  {t('flow.execute.executorInfo', [
                    startUser.value,
                    executeDetail.value?.trigger,
                    execFormatStartTime.value,
                  ])}
                </span>
              ) : null}
            </aside>
          </div>
          <p class={show.value ? styles.summaryHeaderShadow : ''}></p>

          <Summary visible={summaryVisible.value} execDetail={executeDetail.value}></Summary>

          <p class={styles.pipelineExecGap}>
            <span
              onClick={collapseSummary}
              class={[
                styles.summaryCollapsedHandler,
                !summaryVisible.value ? styles.isCollapsed : '',
              ]}
            >
              <SvgIcon name="arrows-up" size={18} />
            </span>
          </p>
        </div>
      )
    }
  },
})
