import type { Element } from '@/api/flowModel'
import CodeEditor from '@/components/CodeEditor'
import EmptyPage from '@/components/EmptyPage/index'
import ModeSwitch from '@/components/ModeSwitch'
import { useFlowConfigCode } from '@/hooks/useFlowConfigCode'
import { useFlowModel } from '@/hooks/useFlowModel'
import { useModeStore } from '@/stores/flowMode'
import layoutStyles from '@/styles/layout.module.css'
import { Loading } from 'bkui-vue'
import { computed, defineComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import TriggerEventContent from './TriggerEventContent'

interface TriggerEvent {
  name: string
  icon: string
  version: string
  enabled: boolean
  type: string
}

export default defineComponent({
  name: 'TriggerEvent',
  components: {
    ModeSwitch,
    EmptyPage,
    CodeEditor,
    Loading,
    TriggerEventContent,
  },
  setup(props, { emit }) {
    const { t } = useI18n()
    const route = useRoute()
    const modeStore = useModeStore()
    const flowId = route.params.flowId as string

    // Use flow model hook to get trigger events
    const { triggerEvents: triggerElements } = useFlowModel()

    // Use flow config code hook for Code mode
    const { loading, yamlContent, sectionHighlight, isEmpty, flowSetting, flowModel } =
      useFlowConfigCode({
        projectId: route.params.projectId as string,
        flowId,
        version: route.params.version as string,
        section: 'trigger-event',
      })

    // 将 Element[] 转换为 TriggerEvent[] 格式
    const triggerEvents = computed<TriggerEvent[]>(() => {
      return triggerElements.value.map((element: Element) => {
        // 获取启用状态
        const enabled = element.additionalOptions?.enable ?? true

        // 根据 atomCode 确定图标和类型
        const getIconAndType = (atomCode?: string) => {
          if (!atomCode) return { icon: '', type: '' }

          // 手动触发
          if (atomCode === 'manualTrigger' || atomCode.includes('manual')) {
            return { icon: 'play-circle', type: 'manual' }
          }

          // 云桌面关机
          if (atomCode.includes('cloud-desktop') || atomCode.includes('desktop')) {
            return { icon: 'desktop', type: 'cloud-desktop-shutdown' }
          }

          // 默认
          return { icon: 'trigger', type: atomCode }
        }

        const { icon, type } = getIconAndType(element.atomCode)

        return {
          name: element.name || element.atomCode || t('flow.content.unknownTrigger'),
          icon,
          version: element.version || '1 latest',
          enabled,
          type,
        }
      })
    })

    return () => (
      <div class={layoutStyles.detailContainerWithRightPadding}>
        {/* <ModeSwitch
          projectId={route.params.projectId as string}
          pipelineId={route.params.flowId as string}
          modelAndSetting={
            flowModel.value && flowSetting.value
              ? {
                  model: flowModel.value,
                  setting: flowSetting.value,
                }
              : undefined
          }
        /> */}

        <div class={layoutStyles.detailContent}>
          {loading.value ? (
            <div class={layoutStyles.loadingWrapper}>
              <Loading loading size="small" mode="spin" theme="primary" />
            </div>
          ) : (
            <>
              {modeStore.isCodeMode ? (
                <div class={layoutStyles.codeEditorWrapper}>
                  {!isEmpty.value ? (
                    <CodeEditor
                      modelValue={yamlContent.value}
                      readOnly={true}
                      height="calc(100vh - 160px)"
                      highlightRanges={sectionHighlight.value}
                    />
                  ) : (
                    <EmptyPage
                      title={t('flow.content.noConfigurationFound')}
                      desc={t('flow.content.pleaseConfigureFirst')}
                    />
                  )}
                </div>
              ) : (
                <TriggerEventContent triggerEvents={triggerEvents.value} />
              )}
            </>
          )}
        </div>
      </div>
    )
  },
})
