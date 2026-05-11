import { downloadFlowJson, downloadFlowYaml } from '@/api/flowContentList'
import { useFlowModelStore } from '@/stores/flowModel'
import { Button, Dialog, Message } from 'bkui-vue'
import { computed, defineComponent, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { SvgIcon } from '../SvgIcon'
import styles from './ExportFlowDialog.module.css'

export interface ExportFlowDialogProps {
  isShow: boolean
  flowId: string
  flowName: string
}

type ExportFormat = 'json' | 'yaml'

interface ExportItem {
  format: ExportFormat
  title: string
  icon: string
  tip: string
  buttonText: string
  disabled: boolean
  run: () => Promise<void>
}

export const ExportFlowDialog = defineComponent({
  name: 'ExportFlowDialog',
  props: {
    isShow: {
      type: Boolean,
      default: false,
    },
    flowId: {
      type: String,
      required: true,
    },
    flowName: {
      type: String,
      required: true,
    },
  },
  emits: ['update:isShow'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const route = useRoute()
    const projectId = route.params.projectId as string
    const flowModelStore = useFlowModelStore()

    const exportingFormat = ref<ExportFormat | null>(null)

    const handleClose = () => {
      emit('update:isShow', false)
    }

    const exportJson = () =>
      downloadFlowJson(projectId, props.flowId, `${props.flowName}.json`)

    const exportYaml = () =>
      downloadFlowYaml(
        projectId,
        props.flowId,
        {
          model: flowModelStore.flowModel!,
          setting: flowModelStore.flowSetting!,
        },
        `${props.flowName}.yml`,
      )

    const exportList = computed<ExportItem[]>(() => [
      {
        format: 'json',
        title: 'Pipeline JSON',
        icon: 'pipeline',
        tip: t('flow.dialog.exportFlow.exportJsonTip'),
        buttonText: t('flow.dialog.exportFlow.exportJson'),
        disabled: false,
        run: exportJson,
      },
      {
        format: 'yaml',
        title: 'Pipeline YAML',
        icon: 'pipeline',
        tip: t('flow.dialog.exportFlow.exportYamlTip'),
        buttonText: t('flow.dialog.exportFlow.exportYaml'),
        disabled: !flowModelStore.flowModel || !flowModelStore.flowSetting,
        run: exportYaml,
      },
    ])

    const handleExport = async (item: ExportItem) => {
      if (item.disabled) return
      exportingFormat.value = item.format
      try {
        await item.run()
        Message({ theme: 'success', message: t('flow.content.exportSuccess') })
        handleClose()
      } catch (error: any) {
        Message({ theme: 'error', message: error?.message || t('flow.content.exportFailed') })
      } finally {
        exportingFormat.value = null
      }
    }

    return () => (
      <Dialog
        isShow={props.isShow}
        title={t('flow.dialog.exportFlow.title')}
        width={640}
        showFooter={false}
        onCancel={handleClose}
        onClosed={handleClose}
      >
        {{
          default: () => (
            <div class={styles.exportList}>
              {exportList.value.map((item) => (
                <div class={styles.exportItem} key={item.format}>
                  <SvgIcon name={item.icon} class={styles.exportIcon} />
                  <h5 class={styles.exportTitle}>{item.title}</h5>
                  <p class={styles.exportTip}>{item.tip}</p>
                  <Button
                    class={styles.exportButton}
                    onClick={() => handleExport(item)}
                    loading={exportingFormat.value === item.format}
                    disabled={
                      item.disabled
                      || (exportingFormat.value !== null && exportingFormat.value !== item.format)
                    }
                  >
                    {item.buttonText}
                  </Button>
                </div>
              ))}
            </div>
          ),
        }}
      </Dialog>
    )
  },
})
