import { computed, defineComponent, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { Dialog, Loading, Form, Input, Select, Message } from 'bkui-vue'
import { SvgIcon } from '@/components/SvgIcon'
import FlowLableSelector from '@/components/FlowLableSelector'
import {
  type CopyFlowParams,
  type DynamicParamLables,
  type MatchDynamicViewParams,
} from '@/api/flowContentList'
import { useFlowGroupData } from '@/hooks/useFlowGroupData'
import { useFlowListData } from '@/hooks/useFlowListData'
import styles from './CopyFlowPopup.module.css'
import { useRoute } from 'vue-router'

interface FormFieldConfig {
  property: keyof CopyFlowParams
  label: string
  maxlength?: number
  component: string
  props?: Record<string, any>
}

export default defineComponent({
  name: 'CopyFlowPopup',
  components: {
    SvgIcon,
    FlowLableSelector,
  },
  props: {
    isShow: {
      type: Boolean,
      default: false,
    },
    data: {
      type: Object,
      default: () => { },
    },
    loading: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['update:isShow', 'confirm'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const route = useRoute()
    const { getMatchDynamicData, getProjectTagList } = useFlowListData()
    const {
      flowGroups: dynamicGroupList,
      personalFlowGroups,
      projectFlowGroups,
    } = useFlowGroupData()
    const initFormData = () => {
      return {
        name: '',
        desc: '',
        labels: [],
        dynamicGroup: [],
        staticView: [],
      }
    }
    const formData = ref<CopyFlowParams>(initFormData())
    const dynamicLoading = ref(false)
    const staticViewList = computed(() => {
      const personalList = personalFlowGroups.value || []
      const projectList = projectFlowGroups.value.filter((item) => item.id !== 'unclassified') || []
      return [
        {
          id: 'personal',
          groupName: t('flow.content.personalFlowGroup'),
          count: personalList.length || 0,
          children: personalList,
        },
        {
          id: 'project',
          groupName: t('flow.content.projectFlowGroup'),
          count: projectList.length || 0,
          children: projectList,
        },
      ]
    })
    const tagGroupList = ref()
    const tagsLoading = ref(false)
    const labelSelectorRef = ref()
    // 保存当前选中的标签映射，用于刷新时保留选择状态
    const currentLabelMap = ref<Record<string, string[]>>({})

    const formFields = computed((): FormFieldConfig[] => [
      {
        property: 'name',
        label: t('flow.dialog.copyCreation.name'),
        maxlength: 128,
        component: 'Input',
        props: {
          placeholder: t('flow.content.inputFlowName'),
        },
      },
      {
        property: 'desc',
        label: t('flow.dialog.copyCreation.desc'),
        maxlength: 30,
        component: 'Input',
        props: {
          placeholder: t('flow.dialog.copyCreation.desc'),
        },
      },
      // TODO: 暂时注释掉标签、动态创作流组、静态创作流组，后续需要时再启用
      // {
      //   property: 'labels',
      //   label: t('flow.dialog.copyCreation.labels'),
      //   component: 'Custom',
      // },
      // {
      //   property: 'dynamicGroup',
      //   label: t('flow.dialog.copyCreation.dynamicFlowGroup'),
      //   component: 'Select',
      //   props: {
      //     disabled: true,
      //     multiple: true,
      //     loading: dynamicLoading.value,
      //     placeholder: t('flow.dialog.copyCreation.dynamicMatchPlaceholder'),
      //   },
      // },
      // {
      //   property: 'staticView',
      //   label: t('flow.dialog.copyCreation.staticFlowGroup'),
      //   component: 'Select',
      //   props: {
      //     multiple: true,
      //   },
      // },
    ])

    watch(
      () => props.isShow,
      async (newVal) => {
        if (newVal) {
          const name = props.data?.pipelineName ? `${props.data.pipelineName}_copy` : ''
          formData.value.name = name
          await handleRefresh()
        }
      },
      {
        immediate: true,
      },
    )

    async function getDynamicGroup(labelIds: DynamicParamLables[]) {
      dynamicLoading.value = true
      try {
        const params: MatchDynamicViewParams = {
          labels: labelIds,
          pipelineName: formData.value.name,
        }
        const res = await getMatchDynamicData(params)
        formData.value.dynamicGroup = res
      } catch (error: any) {
        Message({ theme: 'error', message: error.message || error })
      } finally {
        dynamicLoading.value = false
      }
    }

    const onClose = () => {
      formData.value = initFormData()
      currentLabelMap.value = {} // 清空保存的标签映射
      emit('update:isShow', false)
    }

    const onConfirm = async () => {
      emit('confirm', props.data.pipelineId, formData.value)
    }

    const handleAddLabel = () => {
      window.open(
        `${window.location.origin}/creative-stream/${route.params.projectId}/group`,
        '_blank',
      )
    }

    async function handleRefresh() {
      // 保留当前选中的标签状态，不清空
      tagsLoading.value = true
      try {
        const res = await getProjectTagList()
        tagGroupList.value = res

        // 如果有已选择的标签，使用当前选择的标签；否则使用空数组
        const currentLabels =
          Object.keys(currentLabelMap.value).length > 0
            ? Object.entries(currentLabelMap.value).map(([groupId, labelIds]) => ({
              groupId,
              labelIds,
            }))
            : res.map((item) => ({ groupId: item.id, labelIds: [] }))

        getDynamicGroup(currentLabels)
      } catch (error: any) {
        Message({ theme: 'error', message: error.message || error })
      } finally {
        tagsLoading.value = false
      }
    }

    const updateDynamicGroup = (labelMap: Record<string, string[]>) => {
      // 保存当前选中的标签映射
      currentLabelMap.value = labelMap

      // 将 labelMap 转换为 labels 数组格式
      const labels = Object.values(labelMap).flat()
      formData.value.labels = labels

      // 构建动态分组查询参数
      const params: DynamicParamLables[] = Object.entries(labelMap).map(([groupId, labelIds]) => ({
        groupId,
        labelIds,
      }))
      getDynamicGroup(params)
    }

    const renderFormField = (field: FormFieldConfig) => {
      if (field.component === 'Custom') {
        return (
          <Form.FormItem property={field.property} label={field.label}>
            {{
              label: () => (
                <div class={['flex-between', styles.labelTitle]}>
                  <p>{t('flow.dialog.copyCreation.labels')}</p>
                  <p class={['flex-between', styles.actions]}>
                    <span class={['flex-between', styles.newLabel]} onClick={handleAddLabel}>
                      <SvgIcon name="add-small" size={24} />
                      {t('flow.dialog.copyCreation.addLabel')}
                    </span>
                    <span class={['flex-between', styles.refresh]} onClick={handleRefresh}>
                      <SvgIcon name="refresh-line" size={14} />
                      {t('flow.actions.refresh')}
                    </span>
                  </p>
                </div>
              ),
              default: () => (
                <FlowLableSelector
                  ref={labelSelectorRef}
                  class={styles.labelContent}
                  loading={tagsLoading.value}
                  tagGroupList={tagGroupList.value}
                  editable={true}
                  onChange={updateDynamicGroup}
                />
              ),
            }}
          </Form.FormItem>
        )
      }

      if (field.component === 'Select') {
        return (
          <Form.FormItem property={field.property} label={field.label}>
            <Select v-model={formData.value[field.property]} {...field.props}>
              {field.property === 'dynamicGroup'
                ? dynamicGroupList.value.map((i: any) => (
                  <Select.Option key={i.id} id={i.id} name={i.name}></Select.Option>
                ))
                : staticViewList.value.map((item: any) => (
                  <Select.Group key={item.id} label={item.groupName} collapsible>
                    {item.children.map((flow: any) => (
                      <Select.Option key={flow.id} id={flow.id} name={flow.name}></Select.Option>
                    ))}
                  </Select.Group>
                ))}
            </Select>
          </Form.FormItem>
        )
      }

      return (
        <Form.FormItem property={field.property} label={field.label} maxlength={field.maxlength}>
          <Input v-model={formData.value[field.property]} {...field.props}></Input>
        </Form.FormItem>
      )
    }

    return () => (
      <Dialog
        is-show={props.isShow}
        title={t('flow.content.copyCreationFlow')}
        quick-close={false}
        class={styles.copyFlowPopup}
        isLoading={props.loading}
        zIndex={1000}
        onClosed={onClose}
        onHidden={onClose}
        onConfirm={onConfirm}
      >
        <Loading loading={props.loading} size="small">
          <Form model={formData.value} form-type="vertical">
            {formFields.value.map((field) => renderFormField(field))}
          </Form>
        </Loading>
      </Dialog>
    )
  },
})
