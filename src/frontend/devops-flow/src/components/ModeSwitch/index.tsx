import { computed, defineComponent, h, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Button, InfoBox, Message } from 'bkui-vue'
import { useModeStore } from '@/stores/flowMode'
import { useFlowModelStore } from '@/stores/flowModel'
import { apiTransfer, type ImportContentParams } from '@/api/flowContentList'
import styles from './ModeSwitch.module.css'
import { modeList, CODE_MODE, UI_MODE } from '@/utils/flowConst'

export default defineComponent({
  name: 'ModeSwitch',
  props: {
    projectId: {
      type: String,
      required: true,
    },
    pipelineId: {
      type: String,
      required: true,
    },
  },
  setup(props) {
    const { t } = useI18n()
    const modeStore = useModeStore()
    const flowModelStore = useFlowModelStore()
    const btnLoading = ref(false)
    const flowModelGroup = computed(() =>
      modeList.map((mode) => ({
        label: t(`flow.${mode}`),
        id: mode,
      })),
    )

    function showTransferError(targetMode: string, errMsg?: string) {
      InfoBox({
        type: 'danger',
        title: t('flow.changeModeFail', [t(`flow.${targetMode}`)]),
        content: h(
          'pre',
          {
            style: {
              padding: '16px',
              background: '#f5f5f5',
              textAlign: 'left',
              lineHeight: '24px',
              whiteSpace: 'pre-wrap',
              wordBreak: 'break-all',
            },
          },
          errMsg ?? '',
        ),
      })
    }

    /**
     * 切换模式时仅在“源端有未同步编辑”时调用后端 transfer，
     * 否则 yamlContent 与 flowModel 已一致，直接切模式即可，避免不必要的请求。
     */
    async function updatedMode(mode: string) {
      const previousMode = modeStore.currentMode
      if (mode === previousMode || btnLoading.value) {
        return
      }

      const needModelToYaml = mode === CODE_MODE && flowModelStore.modelDirty
      const needYamlToModel = mode === UI_MODE && flowModelStore.yamlDirty

      if (!needModelToYaml && !needYamlToModel) {
        modeStore.setMode(mode)
        return
      }

      try {
        btnLoading.value = true
        const transferParams: ImportContentParams = {
          projectId: props.projectId,
          pipelineId: props.pipelineId || undefined,
          actionType: needModelToYaml ? 'FULL_MODEL2YAML' : 'FULL_YAML2MODEL',
        }

        if (needModelToYaml) {
          if (flowModelStore.flowModel && flowModelStore.flowSetting) {
            transferParams.modelAndSetting = {
              model: flowModelStore.flowModel,
              setting: flowModelStore.flowSetting,
            }
          }
        } else {
          transferParams.oldYaml = flowModelStore.yamlContent
        }

        const response = await apiTransfer(transferParams)

        if (response.yamlSupported === false) {
          showTransferError(mode, response.yamlInvalidMsg)
          return
        }

        if (needModelToYaml) {
          flowModelStore.applyTransferResult({ yaml: response.newYaml ?? '' })
        } else {
          flowModelStore.applyTransferResult({
            model: response.modelAndSetting?.model,
            setting: response.modelAndSetting?.setting,
          })
        }
        modeStore.setMode(mode)
      } catch (error: any) {
        modeStore.setMode(previousMode)
        Message({ theme: 'error', message: error?.message || String(error) })
      } finally {
        btnLoading.value = false
      }
    }

    return () => (
      <div class={styles.modelSwitch}>
        <Button.ButtonGroup size="small">
          {flowModelGroup.value.map((mode) => (
            <Button
              selected={mode.id === modeStore.currentMode}
              onClick={() => updatedMode(mode.id)}
              loading={btnLoading.value}
            >
              {mode.label}
            </Button>
          ))}
        </Button.ButtonGroup>
      </div>
    )
  },
})
