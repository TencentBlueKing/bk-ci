import { FLOW_GROUP_TYPES } from '@/constants/flowGroup'
import { Loading } from 'bkui-vue'
import { computed, defineComponent, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink, useRoute } from 'vue-router'
import { SvgIcon } from '../SvgIcon'
import styles from './CommonHeader.module.css'

export interface CommonHeaderProps {
  workflowName: string
  onWorkflowNameClick?: () => void
}

export const CommonHeader = defineComponent({
  name: 'CommonHeader',
  props: {
    loading: {
      type: Boolean,
      default: false,
    },
    workflowName: {
      type: String,
      required: true,
    },
    onWorkflowNameClick: {
      type: Function as PropType<() => void>,
    },
  },
  setup(props, { slots }) {
    const { t } = useI18n()
    const route = useRoute()

    const flowList = computed(() => ({
      name: 'flowList',
      params: {
        projectId: route.params.projectId,
        groupId: FLOW_GROUP_TYPES.ALL_FLOWS,
      },
    }))

    const handleWorkflowNameClick = () => {
      props.onWorkflowNameClick?.()
    }

    return () => (
      <header class={styles.header}>
        {props.loading ? (
          <Loading mode="spin" size="mini" theme="primary" />
        ) : (
          <>
            <div class={styles.headerLeft}>
              {/* Logo/图标 */}
              <RouterLink to={flowList.value} class={styles.logoLink}>
                <div class={styles.logo}>
                  <img
                    src={`${import.meta.env.BASE_URL}devops-flow-logo.svg`}
                    alt="flow"
                    class={styles.logoIcon}
                  />
                </div>
                {/* 创作流文本 */}
                <span class={styles.flowLabel}>{t('flow.title')}</span>
              </RouterLink>

              <SvgIcon name="angle-down" class={styles.separatorIcon} size={18} />

              {/* 工作流名称 */}
              {slots['workflow-selector'] ? (
                slots['workflow-selector']()
              ) : (
                <span
                  class={[styles.workflowName, props.onWorkflowNameClick && styles.clickable]}
                  onClick={handleWorkflowNameClick}
                >
                  {props.workflowName}
                </span>
              )}

              {/* 执行详情插槽 */}
              {slots['execution-detail'] && (
                <>
                  <SvgIcon name="angle-down" class={styles.separatorIcon} size={18} />
                  {slots['execution-detail']?.()}
                </>
              )}

              {/* 版本选择器插槽 */}
              {slots['version-selector']?.()}
            </div>

            {/* 中间区域插槽 (如 ModeSwitch) */}
            {slots.center && <div class={styles.headerCenter}>{slots.center?.()}</div>}

            {/* 右侧操作按钮插槽 */}
            <div class={styles.headerRight}>{slots.actions?.()}</div>
          </>
        )}
      </header>
    )
  },
})
