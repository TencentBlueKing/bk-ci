import { apiTransfer } from '@/api/flowContentList'
import { ROUTE_NAMES } from '@/constants/routes'
import { useModeStore } from '@/stores/flowMode'
import { useFlowModelStore } from '@/stores/flowModel'
import type { FlowModel, FlowSettings } from '@/types/flow'
import { CODE_MODE, UI_MODE } from '@/utils/flowConst'
import { randomLenString } from '@/utils/util'
import { Dialog, Message, Upload } from 'bkui-vue'
import { defineComponent } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'

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
    const modeStore = useModeStore()

    function checkJsonValid(json: any) {
      try {
        return (json.model?.stages && json.setting?.pipelineName) || json.stages
      } catch (e) {
        return false
      }
    }

    function isJsonFile(file: File) {
      return file.type === 'application/json' || file.name.endsWith('.json')
    }

    function isYamlFile(file: File) {
      return (
        file.type === 'application/x-yaml'
        || file.type === 'text/yaml'
        || file.name.endsWith('.yaml')
        || file.name.endsWith('.yml')
      )
    }

    function handleSelect({ file, onProgress, onSuccess, onComplete }: any) {
      const reader = new FileReader()
      reader.readAsText(file)
      reader.addEventListener('loadend', async () => {
        try {
          if (isJsonFile(file)) {
            const jsonResult = JSON.parse(reader.result as string)
            const isValid = checkJsonValid(jsonResult)
            const code = isValid ? 0 : 1
            const message = isValid ? null : t('flow.content.invalidFlowJsonOrYaml')

            onSuccess({ code, message, result: jsonResult }, file)

            if (isValid) {
              handleImportJson(jsonResult)
            }
          } else if (isYamlFile(file)) {
            const yaml = reader.result as string
            const isValid = !!yaml && yaml.trim().length > 0
            const code = isValid ? 0 : 1
            const message = isValid ? null : t('flow.content.invalidFlowJsonOrYaml')

            onSuccess({ code, message, result: yaml }, file)

            if (isValid) {
              await handleImportYaml(yaml)
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
            { code: 1, message: t('flow.content.invalidFlowJsonOrYaml'), result: '' },
            file,
          )
        } finally {
          onComplete(file)
        }
      })
      reader.addEventListener('progress', onProgress)
    }

    function handleImportJson(json: any) {
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

      modeStore.setMode(UI_MODE)
      flowModelStore.setImportedFlowModel(pipeline, setting)

      router.push({
        name: ROUTE_NAMES.FLOW_IMPORT_EDIT_WORKFLOW_ORCHESTRATION,
        params: { projectId: route.params.projectId },
      })

      handleClose()
    }

    async function handleImportYaml(yaml: string) {
      try {
        const projectId = route.params.projectId as string
        const res = await apiTransfer({
          projectId,
          actionType: 'FULL_YAML2MODEL',
          oldYaml: yaml,
        })

        if (res.yamlInvalidMsg) {
          Message({ theme: 'error', message: res.yamlInvalidMsg })
          return
        }

        const modelAndSetting = res.modelAndSetting
        if (!modelAndSetting?.model || !modelAndSetting?.setting) {
          Message({ theme: 'error', message: t('flow.content.invalidFlowJsonOrYaml') })
          return
        }

        const { templateId, instanceFromTemplate, ...restModel } = modelAndSetting.model as any
        const pipeline: FlowModel = { ...(restModel as FlowModel) }
        const setting: FlowSettings = { ...modelAndSetting.setting }

        if (typeof props.handleImportSuccess === 'function') {
          props.handleImportSuccess({ pipeline, pipelineSetting: setting, yaml })
          handleClose()
          return
        }

        modeStore.setMode(CODE_MODE)
        flowModelStore.setImportedFlowModel(pipeline, setting, yaml)

        router.push({
          name: ROUTE_NAMES.FLOW_IMPORT_EDIT_WORKFLOW_ORCHESTRATION,
          params: { projectId: route.params.projectId },
        })

        handleClose()
      } catch (error: any) {
        console.error('Failed to transfer yaml to model:', error)
        Message({
          theme: 'error',
          message: error?.message || t('flow.content.invalidFlowJsonOrYaml'),
        })
      }
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
          accept=".json, .yaml, .yml, application/json, application/x-yaml"
          with-credentials={true}
          custom-request={handleSelect}
        />
      </Dialog>
    )
  },
})
