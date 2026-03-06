import CodeEditor from '@/components/CodeEditor'
import EmptyPage from '@/components/EmptyPage/index'
import ModeSwitch from '@/components/ModeSwitch'
import { useFlowConfigCode } from '@/hooks/useFlowConfigCode'
import { useModeStore } from '@/stores/flowMode'
import layoutStyles from '@/styles/layout.module.css'
import { Loading } from 'bkui-vue'
import { defineComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import type { FlowSettings } from '../../../../api/flowModel'
import NoticeContent from './NoticeContent'

export default defineComponent({
  name: 'NoticeTab',
  components: {
    ModeSwitch,
    NoticeContent,
    EmptyPage,
    CodeEditor,
    Loading,
  },
  setup(props, { emit }) {
    const { t } = useI18n()
    const route = useRoute()
    const modeStore = useModeStore()
    const projectId = route.params.projectId as string
    const flowId = route.params.flowId as string

    // Use flow config code hook for Code mode
    const { loading, yamlContent, sectionHighlight, isEmpty, flowSetting, flowModel } =
      useFlowConfigCode({
        projectId,
        flowId,
        version: route.params.version as string,
        section: 'notice',
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
                <NoticeContent flowSetting={flowSetting.value as FlowSettings} />
              )}
            </>
          )}
        </div>
      </div>
    )
  },
})
