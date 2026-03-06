import { downloadFlowJson } from '@/api/flowContentList'
import { Button, Dialog, Message } from 'bkui-vue'
import { defineComponent, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { SvgIcon } from '../SvgIcon'
import styles from './ExportFlowDialog.module.css'

export interface ExportFlowDialogProps {
  isShow: boolean
  flowId: string
  flowName: string
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
    const isExporting = ref(false)

    const handleClose = () => {
      emit('update:isShow', false)
    }

    // 下载 JSON 格式
    const handleExportJson = async () => {
      isExporting.value = true
      try {
        await downloadFlowJson(projectId, props.flowId, `${props.flowName}.json`)
        Message({ theme: 'success', message: t('flow.content.exportSuccess') })
        handleClose()
      } catch (error: any) {
        Message({ theme: 'error', message: error?.message || t('flow.content.exportFailed') })
      } finally {
        isExporting.value = false
      }
    }

    return () => (
      <Dialog
        isShow={props.isShow}
        title={t('flow.dialog.exportFlow.title')}
        width={400}
        showFooter={false}
        onCancel={handleClose}
        onClosed={handleClose}
      >
        {{
          default: () => (
            <div class={styles.exportList}>
              {/* JSON 导出 */}
              <div class={styles.exportItem}>
                <SvgIcon name="pipeline" class={styles.exportIcon} />
                <h5 class={styles.exportTitle}>Pipeline JSON</h5>
                <p class={styles.exportTip}>{t('flow.dialog.exportFlow.exportJsonTip')}</p>
                <Button
                  class={styles.exportButton}
                  onClick={handleExportJson}
                  loading={isExporting.value}
                >
                  {t('flow.dialog.exportFlow.exportJson')}
                </Button>
              </div>
            </div>
          ),
        }}
      </Dialog>
    )
  },
})
