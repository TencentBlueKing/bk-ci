import CodeEditor from '@/components/CodeEditor'
import EmptyPage from '@/components/EmptyPage/index'
import ModeSwitch from '@/components/ModeSwitch'
import { useFlowConfigCode } from '@/hooks/useFlowConfigCode'
import { useModeStore } from '@/stores/flowMode'
import layoutStyles from '@/styles/layout.module.css'
import { Loading } from 'bkui-vue'
import { computed, defineComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import SettingContent from './SettingContent'
import { useFlowInfo } from '@/hooks/useFlowInfo'

export default defineComponent({
  name: 'SettingTab',
  components: {
    ModeSwitch,
    SettingContent,
    EmptyPage,
    CodeEditor,
    Loading,
  },
  setup(props, { emit }) {
    const { t } = useI18n()
    const route = useRoute()
    const modeStore = useModeStore()
    const flowId = route.params.flowId as string

    // Use flow config code hook for Code mode
    const { loading, yamlContent, sectionHighlight, isEmpty, flowSetting, flowModel } =
      useFlowConfigCode({
        projectId: route.params.projectId as string,
        flowId,
        version: route.params.version as string,
        section: 'basic-setting',
      })
    const { flowInfo } = useFlowInfo()

    // Combine flowSetting with group info from flowModel
    const basicSettingsWithGroup = computed(() => {
      if (!flowSetting.value) return null

      return {
        ...flowSetting.value,
        ...flowInfo.value,
        // Get group names from flowModel.staticViews
        groupNames: flowModel.value?.staticViews || [],
      }
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
                      height="calc(100% - 16px)"
                      highlightRanges={sectionHighlight.value}
                    />
                  ) : (
                    <EmptyPage
                      title={t('flow.content.noConfigurationFound')}
                      desc={t('flow.content.pleaseConfigureFirst')}
                    />
                  )}
                </div>
              ) : basicSettingsWithGroup.value ? (
                <SettingContent basicSettings={basicSettingsWithGroup.value} />
              ) : (
                <EmptyPage />
              )}
            </>
          )}
        </div>
      </div>
    )
  },
})
