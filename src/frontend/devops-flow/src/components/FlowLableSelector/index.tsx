import { computed, defineComponent, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { Select, Loading } from 'bkui-vue'
import styles from './FlowLableSelector.module.css'

export default defineComponent({
  name: 'FlowLableSelector',
  props: {
    editable: {
      type: Boolean,
      default: true,
    },
    loading: {
      type: Boolean,
      default: false,
    },
    tagGroupList: {
      type: Array,
      default: () => [],
    },
  },
  emits: ['change'],
  setup(props, { emit, expose }) {
    const { t } = useI18n()
    const labelMap = ref<any>({})
    const tagSelectModelList = computed(() =>
      props.tagGroupList.map((item: any) => ({
        ...item,
        handleChange: (value: string[]) => handleChange(item.id, value),
      })),
    )

    const handleChange = (groupId: string, labelIds: string[]) => {
      labelMap.value[groupId] = labelIds
      emit('change', labelMap.value)
    }

    const clearLabels = () => {
      labelMap.value = {}
      emit('change', [])
    }

    expose({
      clearLabels,
    })

    return () => (
      <ul class={styles.flowLabelSelector}>
        <Loading loading={props.loading}>
          {tagSelectModelList.value.length > 0 ? (
            tagSelectModelList.value.map((item: any) => (
              <li key={item.id}>
                <label class={styles.flowLabelSelectorLabel}>{item.name}</label>
                <Select
                  disabled={!props.editable}
                  class={styles.subLabelSelect}
                  modelValue={labelMap.value[item.id]}
                  onUpdate:modelValue={item.handleChange}
                  multiple
                >
                  {item.labels.map((label: any) => (
                    <Select.Option key={label.id} id={label.id} name={label.name} />
                  ))}
                </Select>
              </li>
            ))
          ) : (
            <span class={styles.noLabelPlaceholder}>{t('flow.dialog.copyCreation.noLabels')}</span>
          )}
        </Loading>
      </ul>
    )
  },
})
