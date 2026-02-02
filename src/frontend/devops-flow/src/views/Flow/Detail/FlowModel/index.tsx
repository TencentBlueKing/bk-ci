import CodeEditor from '@/components/CodeEditor'
import EmptyPage from '@/components/EmptyPage/index'
import ModeSwitch from '@/components/ModeSwitch'
import { useFlowModel } from '@/hooks/useFlowModel'
import { useModeStore } from '@/stores/flowMode'
import layoutStyles from '@/styles/layout.module.css'
import { Loading } from 'bkui-vue'
import { defineComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import styles from './FlowModel.module.css'

import 'bkui-pipeline/dist/bk-pipeline.css'
import BkPipeline from 'bkui-pipeline/vue3'

export default defineComponent({
  name: 'FlowModel',
  setup(props, { emit }) {
    const { t } = useI18n()
    const route = useRoute()
    const modeStore = useModeStore()

    // 使用 useFlowModel hook 管理数据
    const {
      flowModelWithoutTriggerStage,
      yamlContent,
      loading,
      isFlowEmpty,
      flowModel,
      flowSetting,
    } = useFlowModel()

    return () => (
      <div class={styles.flowModel}>
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

        <div class={layoutStyles.flexDetailContent}>
          {loading.value ? (
            <div class={layoutStyles.loadingWrapper}>
              <Loading loading size="small" mode="spin" theme="primary" />
            </div>
          ) : (
            <>
              {modeStore.isCodeMode ? (
                <div class={styles.codeEditorWrapper}>
                  <CodeEditor
                    modelValue={yamlContent.value}
                    readOnly={true}
                    height="100%"
                    codeLensTitle={t('flow.content.editStep')}
                  />
                </div>
              ) : (
                <div class={styles.uiModeWrapper}>
                  {!isFlowEmpty.value && flowModelWithoutTriggerStage.value ? (
                    <BkPipeline
                      editable={false}
                      isCreativeStream={true}
                      pipeline={flowModelWithoutTriggerStage.value}
                    />
                  ) : (
                    <EmptyPage
                      title={t('flow.content.blankTemplateNoOrchestration')}
                      desc={t('flow.content.createToAddFirstStage')}
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
