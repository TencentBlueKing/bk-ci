import { useAuthoringEnvironment } from '@/hooks/useAuthoringEnvironment'
import { useFlowModel } from '@/hooks/useFlowModel'
import { defineComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import AuthoringEnv from '../../../../components/AuthoringEnv'
import sharedStyles from '../shared.module.css'

export default defineComponent({
  name: 'EditWorkflowEnvironment',
  setup() {
    const { t } = useI18n()
    const route = useRoute()
    const flowId = route.params.flowId as string
    const projectId = route.params.projectId as string

    // Use flowModel to update settings
    const { flowSetting, updateFlowSetting } = useFlowModel()

    const { 
      envSelectList,
      nodeList,
      nodeListLoading,
      envListLoading
     } = useAuthoringEnvironment({ ...route.params, autoLoadEnvList: true })

    // Handle user environment selection change
    const handleEnvChange = (envHashId: string) => {
      // Only update if actually changed by user
      if (flowSetting.value && envHashId !== (flowSetting.value as any).envHashId) {
        updateFlowSetting({
          ...flowSetting.value,
          envHashId,
        })
      }
    }

    return () => (
      <div class={[sharedStyles.tabContainer, sharedStyles.tabPadding]}>
        <AuthoringEnv
          isEdit
          modelValue={flowSetting.value?.envHashId}
          envList={envSelectList.value}
          envLoading={envListLoading.value}
          nodeLoading={nodeListLoading.value}
          nodeList={nodeList.value}
          onUpdate:modelValue={handleEnvChange}
        />
      </div>
    )
  },
})
