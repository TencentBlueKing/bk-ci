/**
 * StageDetail Component
 * Stage 详情侧边栏 - 显示 Stage 配置信息
 * 功能：
 * - Stage 配置只读展示
 * - 使用 StagePropertyContent 组件复用配置展示逻辑
 */
import StatusIcon from '@/components/StatusIcon'
import { StagePropertyContent } from '@/components/WorkflowOrchestration'
import type { ExecuteDetailData, Stage, StatusType } from '@/types/flow'
import { computed, defineComponent, nextTick, onBeforeUnmount, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './ExecDetail.module.css'

export default defineComponent({
  name: 'StageDetail',
  props: {
    isShow: {
      type: Boolean,
      default: false,
    },
    execDetail: {
      type: Object as PropType<ExecuteDetailData>,
      required: true,
    },
    stage: {
      type: Object as PropType<Stage>,
      required: true,
    },
    executeCount: {
      type: Number,
      default: 1,
    },
  },
  emits: ['close'],
  setup(props, { emit }) {
    const { t } = useI18n()

    // Computed
    const stageName = computed(() => props.stage?.name || 'Stage')
    const stageStatus = computed(() => (props.stage?.status || 'QUEUE') as StatusType)

    // Close panel
    const closePanel = () => {
      emit('close')
    }

    // Handle click outside
    const handleClickOutside = (event: MouseEvent) => {
      const target = event.target as HTMLElement
      const logMain = document.querySelector(`.${styles.logMain}`)
      if (logMain && !logMain.contains(target)) {
        closePanel()
      }
    }

    // Watch for show changes
    watch(
      () => props.isShow,
      (newVal) => {
        if (newVal) {
          nextTick(() => {
            document.addEventListener('click', handleClickOutside)
          })
        } else {
          document.removeEventListener('click', handleClickOutside)
        }
      },
      { immediate: true },
    )

    onBeforeUnmount(() => {
      document.removeEventListener('click', handleClickOutside)
    })

    return () => {
      if (!props.isShow) return null

      return (
        <article class={styles.logHome}>
          <section class={[styles.logMain, styles.whiteTheme, styles.overHidden]}>
            <header class={styles.logHead}>
              <span class={styles.logTitle}>
                <StatusIcon status={stageStatus.value} />
                {stageName.value}
              </span>
            </header>

            <div class={styles.logContentArea}>
              <div class={styles.settingContent}>
                <StagePropertyContent
                  stage={props.stage}
                  editable={false}
                  isNew={false}
                  showNameField={false}
                />
              </div>
            </div>
          </section>
        </article>
      )
    }
  },
})
