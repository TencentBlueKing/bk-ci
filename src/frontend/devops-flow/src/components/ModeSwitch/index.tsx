import { computed, defineComponent, type PropType, h, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Button, InfoBox, Message } from 'bkui-vue'
import { useModeStore } from '@/stores/flowMode'
import { apiTransfer, type ImportContentParams } from '@/api/flowContentList'
import styles from './ModeSwitch.module.css'
import { modeList, CODE_MODE } from '@/utils/flowConst'

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
    modelAndSetting: {
      type: Object as PropType<ImportContentParams['modelAndSetting']>,
    },
  },
  emits: ['transferSuccess', 'transferError'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const modeStore = useModeStore()
    const btnLoading = ref(false)
    const flowModelGroup = computed(() =>
      modeList.map((mode) => ({
        label: t(`flow.${mode}`),
        id: mode,
      })),
    )

    async function updatedMode(mode: string) {
      const previousMode = modeStore.currentMode
      if (mode === previousMode) {
        return
      }

      if (mode === CODE_MODE) {
        try {
          btnLoading.value = true
          const transferParams: ImportContentParams = {
            projectId: props.projectId,
            pipelineId: props.pipelineId,
            actionType: mode === CODE_MODE ? 'FULL_MODEL2YAML' : 'FULL_YAML2MODEL',
          }
          if (props.modelAndSetting) {
            transferParams.modelAndSetting = props.modelAndSetting
          }
          const response = await apiTransfer(transferParams)
          if (response.yamlSupported) {
            emit('transferSuccess', response.newYaml)
            modeStore.setMode(mode)
          } else {
            InfoBox({
              type: 'danger',
              title: t('flow.changeModeFail', [t(`flow.${mode}`)]),
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
                response.yamlInvalidMsg,
              ),
            })
          }
        } catch (error: any) {
          modeStore.setMode(previousMode)
          Message({ theme: 'error', message: error.message || error })
        } finally {
          btnLoading.value = false
        }
      } else {
        modeStore.setMode(mode)
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
