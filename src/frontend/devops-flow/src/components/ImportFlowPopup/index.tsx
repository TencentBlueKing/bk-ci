import { defineComponent, nextTick, ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { Dialog, Upload, Message } from 'bkui-vue'
import { useRouter, useRoute } from 'vue-router'
import styles from './ImportFlowPopup.module.css'
import { CODE_MODE, UI_MODE } from '@/utils/flowConst'
import { ROUTE_NAMES } from '@/constants/routes'
import { useModeStore } from '@/stores/flowMode'
import {
  apiTransfer,
  getPluginProperties,
  type ImportContentParams,
  type PluginProperty,
} from '@/api/flowContentList'
import type { FlowModel, FlowSettings, Stage } from '@/types/flow'

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
    pipelineId: {
      type: String,
    },
    pipelineName: {
      type: String,
    },
    handleImportSuccess: {
      type: Function,
    },
  },
  emits: ['update:isShow'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const router = useRouter()
    const route = useRoute()
    const modeStore = useModeStore()

    // 从路由中获取参数
    const projectId = computed(() => route.params.projectId as string)

    // 暂存文件内容
    const fileContent = ref<any>(null)
    const isFileValid = ref(false)
    const loading = ref(false)

    // TODO： 存放导入后的数据, 传递给编辑页使用数据
    const importedData = ref<{
      pipeline?: FlowModel
      pipelineYaml?: string
      pipelineSetting?: FlowSettings
      pipelineWithoutTrigger?: FlowModel
      editFrom?: boolean
      pipelineEditing?: boolean
    }>({})

    // 获取所有 element 元素
    function getAllElements(stages: Stage[]): Stage[] {
      const elements: Stage[] = []
      if (!stages) return elements

      stages.forEach((stage) => {
        if (stage.containers) {
          stage.containers.forEach((container: any) => {
            if (container.elements) {
              elements.push(...container.elements)
            }
          })
        }
      })
      return elements
    }

    async function transfer({ projectId, pipelineId, actionType, ...params }: ImportContentParams) {
      const apis = [apiTransfer({ projectId, pipelineId, actionType, ...params })]

      if (actionType === 'FULL_YAML2MODEL' && !importedData.value.editFrom) {
        apis.push(getPluginProperties({ projectId, pipelineId, ...params }))
      }
      const [data, atomPropRes] = await Promise.all(apis)

      if (!data) {
        throw new Error(t('flow.content.importFailed'))
      }

      if (data.yamlInvalidMsg) {
        throw new Error(data.yamlInvalidMsg)
      }
      if (actionType === 'FULL_YAML2MODEL' && atomPropRes) {
        const atomProp = atomPropRes as PluginProperty
        const elements = getAllElements(data.modelAndSetting?.model.stages ?? [])
        elements.forEach((element) => {
          // 将os属性设置到model内
          const atomCode = element.atomCode as string
          if (atomCode && atomProp[atomCode]) {
            Object.assign(element, {
              ...atomProp[atomCode],
            })
          }
        })
      }
      return data
    }

    async function transferPipeline({
      projectId,
      pipelineId,
      actionType,
      ...params
    }: ImportContentParams) {
      const data = await transfer({ projectId, pipelineId, actionType, ...params })

      switch (actionType) {
        case 'FULL_YAML2MODEL':
          if (data?.modelAndSetting?.model) {
            importedData.value.pipeline = data.modelAndSetting.model
            importedData.value.pipelineWithoutTrigger = {
              ...(data?.modelAndSetting?.model ?? {}),
              stages: data?.modelAndSetting?.model.stages.slice(1),
            }
            importedData.value.pipelineSetting = data?.modelAndSetting?.setting
          }
          break
        case 'FULL_MODEL2YAML':
          if (data?.newYaml) {
            importedData.value.pipelineYaml = data?.newYaml
          }
          break
      }
      return data
    }

    function checkJsonValid(json: any) {
      try {
        return (json.model?.stages && json.setting?.pipelineName) || json.stages
      } catch (e) {
        return false
      }
    }

    // 更新 CODE_MODE 的流水线
    async function updateCodeModePipeline(result: string) {
      importedData.value.pipelineYaml = result
      importedData.value.editFrom = true

      try {
        const { modelAndSetting } = await transferPipeline({
          projectId: projectId.value,
          actionType: 'FULL_YAML2MODEL',
          oldYaml: result,
        })

        if (!modelAndSetting?.model || !modelAndSetting?.setting) {
          throw new Error(t('flow.content.importFailed'))
        }

        const newPipelineName = modelAndSetting.model.name

        const pipeline = {
          ...modelAndSetting.model,
          name: newPipelineName,
        }

        importedData.value.pipelineSetting = {
          ...modelAndSetting.setting,
          pipelineId: props.pipelineId ?? modelAndSetting.setting.pipelineId,
          pipelineName: newPipelineName,
        } as FlowSettings
        importedData.value.pipeline = pipeline
        importedData.value.pipelineWithoutTrigger = {
          ...pipeline,
          stages: modelAndSetting.model.stages.slice(1),
        }
        importedData.value.pipelineEditing = true

        return true
      } catch (error: any) {
        Message({
          theme: 'error',
          message: error.message,
        })
        return false
      }
    }

    // 更新 UI_MODE 的流水线
    async function updatePipeline(result: any, newPipelineName: string) {
      const { templateId, instanceFromTemplate, ...restModel } = result.model
      const pipeline = {
        ...restModel,
        name: newPipelineName,
      }

      try {
        await transferPipeline({
          projectId: projectId.value,
          actionType: 'FULL_MODEL2YAML',
          modelAndSetting: {
            model: {
              ...result.model,
              name: newPipelineName,
            },
            setting: {
              ...result.setting,
              pipelineName: newPipelineName,
            },
          },
          oldYaml: '',
        })
      } catch (error: any) {
        Message({
          theme: 'error',
          message: error.message,
        })
      }

      importedData.value.pipelineSetting = {
        ...result.setting,
        pipelineId: props.pipelineId ?? result.setting.pipelineId,
        pipelineName: newPipelineName,
      }
      importedData.value.pipeline = pipeline
      importedData.value.pipelineWithoutTrigger = {
        ...pipeline,
        stages: result.model.stages.slice(1),
      }
      importedData.value.pipelineEditing = true
      importedData.value.editFrom = true

      return true
    }

    async function handleSuccess(result: any, type: string = UI_MODE) {
      loading.value = true

      try {
        let res
        if (type === UI_MODE) {
          const newPipelineName = result.model.name
          res = await updatePipeline(result, newPipelineName)
          modeStore.setMode(UI_MODE)
        } else if (type === CODE_MODE) {
          res = await updateCodeModePipeline(result)
          modeStore.setMode(CODE_MODE)
        }

        if (res) {
          if (typeof props.handleImportSuccess === 'function') {
            props.handleImportSuccess(importedData.value)
            handleClose()
            return
          }

          // 跳转到编辑页面
          nextTick(() => {
            router.push({
              name: ROUTE_NAMES.FLOW_EDIT_WORKFLOW_ORCHESTRATION,
              params: {
                flowId: '1234', // TODO 这里实际应该不需要flowId，需等编辑页调整
              },
            })
          })

          handleClose()
        }
      } catch (error: any) {
        console.error('Import flow error:', error)
        Message({
          theme: 'error',
          message: error.message || t('flow.content.importFailed'),
        })
      } finally {
        loading.value = false
      }
    }

    function handleSelect({ file, onProgress, onSuccess, onComplete }: any) {
      const reader = new FileReader()
      reader.readAsText(file)
      reader.addEventListener('loadend', async (e) => {
        try {
          if (file.type === 'application/json' || file.name.endsWith('.json')) {
            const jsonResult = JSON.parse(reader.result as string)
            const isValid = checkJsonValid(jsonResult)
            const code = isValid ? 0 : 1
            const message = isValid ? null : t('flow.content.invalidFlowJson')

            // 暂存文件内容,不立即处理
            if (isValid) {
              fileContent.value = { data: jsonResult, type: UI_MODE }
              isFileValid.value = true
            } else {
              fileContent.value = null
              isFileValid.value = false
              Message({
                theme: 'error',
                message: t('flow.content.invalidFlowJson'),
              })
            }

            onSuccess(
              {
                code,
                message,
                result: jsonResult,
              },
              file,
            )
          } else if (
            file.type === 'application/x-yaml' ||
            file.name.endsWith('.yaml') ||
            file.name.endsWith('.yml')
          ) {
            // YAML 文件处理
            const yaml = e.target?.result as string
            const isValid = !!yaml
            const code = isValid ? 0 : 1
            const message = isValid ? null : t('flow.content.invalidFlowJson')

            // 暂存文件内容
            if (isValid) {
              fileContent.value = { data: yaml, type: CODE_MODE }
              isFileValid.value = true
            } else {
              fileContent.value = null
              isFileValid.value = false
              Message({
                theme: 'error',
                message: t('flow.content.invalidFlowJson'),
              })
            }

            onSuccess(
              {
                code,
                message,
                result: yaml,
              },
              file,
            )
          }
        } catch (e) {
          console.error(e)
          fileContent.value = null
          isFileValid.value = false
          Message({
            theme: 'error',
            message: t('flow.content.invalidFlowJson'),
          })

          onSuccess(
            {
              code: 1,
              message: t('flow.content.invalidFlowJson'),
              result: '',
            },
            file,
          )
        } finally {
          onComplete(file)
        }
      })
      reader.addEventListener('progress', onProgress)
    }

    function handleConfirm() {
      // 点击确认时才处理文件内容
      if (!isFileValid.value || !fileContent.value) {
        Message({
          theme: 'warning',
          message: t('flow.content.pleaseSelectFile'),
        })
        return
      }

      // 处理文件内容并调用接口
      const { data, type } = fileContent.value
      handleSuccess(data, type)
    }

    function handleClose() {
      // 关闭时清空暂存的文件内容
      fileContent.value = null
      isFileValid.value = false
      emit('update:isShow', false)
    }

    return () => (
      <Dialog
        is-show={props.isShow}
        theme="primary"
        zIndex={1000}
        width={640}
        title={props.title || t('flow.content.importFlow')}
        confirm-text={t('flow.content.import')}
        quick-close={false}
        isLoading={loading.value}
        onClosed={handleClose}
        onConfirm={handleConfirm}
      >
        {{
          default: () => (
            <>
              <span class={`${styles.label} ${styles.desc}`}>
                {t('flow.content.importFlowLabel')}
              </span>
              <Upload
                v-if={props.isShow}
                accept=".json, .yaml, .yml, application/json, application/x-yaml"
                with-credentials={true}
                custom-request={handleSelect}
                class={styles.upload}
              ></Upload>
              <span class={styles.desc}>{t('flow.content.importFlowTip')}</span>
            </>
          ),
        }}
      </Dialog>
    )
  },
})
