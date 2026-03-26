import { defineComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import { Dialog, Upload } from 'bkui-vue'
import { useRoute, useRouter } from 'vue-router'
import { ROUTE_NAMES } from '@/constants/routes'
import { useFlowModelStore } from '@/stores/flowModel'
import type { FlowModel, FlowSettings } from '@/types/flow'
import { randomLenString } from '@/utils/util'

export default defineComponent({
  name: 'ImportFlowPopup',
  props: {
    isShow: {
      type: Boolean,
      default: false,
    },
    title: {
      type: String,
    },
    handleImportSuccess: {
      type: Function,
    },
  },
  emits: ['update:isShow'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const route = useRoute()
    const router = useRouter()
    const flowModelStore = useFlowModelStore()

    function checkJsonValid(json: any) {
      try {
        return (json.model?.stages && json.setting?.pipelineName) || json.stages
      } catch (e) {
        return false
      }
    }

    function handleSelect({ file, onProgress, onSuccess, onComplete }: any) {
      const reader = new FileReader()
      reader.readAsText(file)
      reader.addEventListener('loadend', async () => {
        try {
          if (file.type === 'application/json' || file.name.endsWith('.json')) {
            const jsonResult = JSON.parse(reader.result as string)
            const isValid = checkJsonValid(jsonResult)
            const code = isValid ? 0 : 1
            const message = isValid ? null : t('flow.content.invalidFlowJson')

            onSuccess({ code, message, result: jsonResult }, file)

            if (isValid) {
              handleImport(jsonResult)
            }
          } else {
            onSuccess(
              { code: 1, message: t('flow.content.unsupportedFileType'), result: '' },
              file,
            )
          }
        } catch (e) {
          console.error(e)
          onSuccess(
            { code: 1, message: t('flow.content.invalidFlowJson'), result: '' },
            file,
          )
        } finally {
          onComplete(file)
        }
      })
      reader.addEventListener('progress', onProgress)
    }

    function handleImport(json: any) {
      const { templateId, instanceFromTemplate, ...restModel } = json.model
      const importName = `${restModel.name}_${randomLenString(6)}`
      const pipeline: FlowModel = {
        ...restModel,
        name: importName,
      }
      const setting: FlowSettings = {
        ...json.setting,
        pipelineName: importName,
      }

      if (typeof props.handleImportSuccess === 'function') {
        props.handleImportSuccess({ pipeline, pipelineSetting: setting })
        handleClose()
        return
      }

      flowModelStore.setImportedFlowModel(pipeline, setting)

      router.push({
        name: ROUTE_NAMES.FLOW_IMPORT_EDIT_WORKFLOW_ORCHESTRATION,
        params: { projectId: route.params.projectId },
      })

      handleClose()
    }

    function handleClose() {
      emit('update:isShow', false)
    }

    return () => (
      <Dialog
        isShow={props.isShow}
        theme="primary"
        width={600}
        title={props.title || t('flow.content.importFlow')}
        maskClose={false}
        showFooter={false}
        onCancel={handleClose}
        onClosed={handleClose}
      >
        {t('flow.content.importFlowLabel')}
        <Upload
          v-if={props.isShow}
          accept=".json, application/json"
          with-credentials={true}
          custom-request={handleSelect}
        />
      </Dialog>
    )
  },
})
