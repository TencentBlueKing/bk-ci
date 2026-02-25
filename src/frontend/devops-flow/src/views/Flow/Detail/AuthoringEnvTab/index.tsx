import AuthoringEnv from '@/components/AuthoringEnv'
import CodeEditor from '@/components/CodeEditor'
import EmptyPage from '@/components/EmptyPage/index'
import ModeSwitch from '@/components/ModeSwitch'
import { useAuthoringEnvironment } from '@/hooks/useAuthoringEnvironment.ts'
import { useFlowConfigCode } from '@/hooks/useFlowConfigCode'
import { useModeStore } from '@/stores/flowMode.ts'
import layoutStyles from '@/styles/layout.module.css'
import { Loading } from 'bkui-vue'
import { defineComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'

export default defineComponent({
  name: 'AuthoringEnv',
  components: {
    ModeSwitch,
    EmptyPage,
    CodeEditor,
    Loading,
  },
  setup(props, { emit }) {
    const { t } = useI18n()
    const route = useRoute()
    const modeStore = useModeStore()

    // Use authoring environment hook for UI mode
    const { envSelectList, envListLoading, nodeList, nodeListLoading } = useAuthoringEnvironment({
      projectId: route.params.projectId as string,
      flowId: route.params.flowId as string,
      autoLoadNodes: true,
      autoLoadEnvList: true,
    })

    // Use flow config code hook for Code mode
    const { loading, yamlContent, sectionHighlight, isEmpty, flowSetting } = useFlowConfigCode({
      projectId: route.params.projectId as string,
      flowId: route.params.flowId as string,
      version: route.params.version as string,
      section: 'authoring-env',
    })

    return () => (
      <div class={layoutStyles.detailContainer}>
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
                      title={t('flow.content.noEnvironmentConfigured')}
                      desc={t('flow.content.pleaseConfigureEnvironmentFirst')}
                    />
                  )}
                </div>
              ) : (
                <div>
                  {!isEmpty.value ? (
                    <AuthoringEnv
                      modelValue={flowSetting.value?.envHashId}
                      envList={envSelectList.value}
                      envLoading={envListLoading.value}
                      nodeList={nodeList.value}
                      nodeLoading={nodeListLoading.value}
                    />
                  ) : (
                    <EmptyPage
                      title={t('flow.content.noEnvironmentConfigured')}
                      desc={t('flow.content.pleaseConfigureEnvironmentFirst')}
                    />
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    )
  },
})
