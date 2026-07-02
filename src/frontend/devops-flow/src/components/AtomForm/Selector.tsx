import { useDataSource, type SelectDataConf } from '@/hooks/useDataSource'
import { Select } from 'bkui-vue'
import { computed, defineComponent, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'

export default defineComponent({
  name: 'flow-selector',
  props: {
    value: {
      type: [String, Number, Array, Boolean],
      default: '',
    },
    name: {
      type: String,
      required: true,
    },
    // Static list (backward compatible)
    list: {
      type: Array as PropType<Array<{ id: string | number; name: string; disabled?: boolean }>>,
      default: () => [],
    },
    // API request URL
    optionsConf: {
      type: Object as PropType<SelectDataConf>,
      default: () => ({}),
    },
    handleChange: {
      type: Function,
      default: () => () => {},
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    placeholder: {
      type: String,
      default: '',
    },
    // Atom/element value for URL parsing
    atomValue: {
      type: Object as PropType<Record<string, unknown>>,
      default: () => ({}),
    },
    multiSelect: {
      type: Boolean,
      default: false
    }
  },
  emits: ['change', 'update:value'],
  setup(props, { emit, attrs }) {
    const { t } = useI18n()
    const mergedConf: SelectDataConf = {
      ...(attrs as SelectDataConf),
      ...props.optionsConf,
      options: props.list ?? props.optionsConf.options,
      atomValue: props.atomValue,
      multiSelect: props.multiSelect,
    }
    const { list, isLoading, refreshList, isApiMode, selectConf } = useDataSource(mergedConf)

    // Placeholder with loading state
    const displayPlaceholder = computed(() =>
      isLoading.value ? t('common.loading', 'Loading...') : props.placeholder,
    )

    const handleChange = (value: string | number | Array<string | number>) => {
      emit('update:value', value)
      emit('change', value)
      props.handleChange(props.name, value)
    }

    // Handle dropdown toggle for API data source
    const handleToggleVisible = (open: boolean) => {
      if (open && isApiMode()) {
        refreshList()
      }
    }

    return () => (
      <Select
        loading={isLoading.value}
        modelValue={props.value}
        disabled={props.disabled || isLoading.value}
        placeholder={displayPlaceholder.value}
        onChange={handleChange}
        onToggle={handleToggleVisible}
        list={list.value}
        {...selectConf.value}
      ></Select>
    )
  },
})
