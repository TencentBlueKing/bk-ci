import { Select } from 'bkui-vue'
import { computed, defineComponent, type PropType } from 'vue'
import { COMPONENT_MAP } from './AtomForm'
import styles from './ConditionalInputSelector.module.css'

// Props that belong to Selector's optionsConf, not top-level props
const SELECTOR_OPTIONS_CONF_KEYS = new Set([
  'url', 'dataPath', 'paramId', 'paramName',
  'searchable', 'clearable', 'multiple', 'multiSelect',
  'initRequest', 'options', 'hasAddItem',
])

export default defineComponent({
  name: 'ConditionalInputSelector',
  props: {
    value: {
      type: String,
      default: '',
    },
    name: {
      type: String,
      required: true,
    },
    element: {
      type: Object as PropType<Record<string, any>>,
      default: () => ({}),
    },
    atomValue: {
      type: Object as PropType<Record<string, any>>,
      default: () => ({}),
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    placeholder: {
      type: String,
      default: '',
    },
    searchable: {
      type: Boolean,
      default: false,
    },
    clearable: {
      type: Boolean,
      default: false,
    },
    displayKey: {
      type: String,
      default: 'label',
    },
    settingKey: {
      type: String,
      default: 'value',
    },
    list: {
      type: Array as PropType<Array<Record<string, any>>>,
      default: () => [],
    },
    handleChange: {
      type: Function,
      default: () => () => {},
    },
  },
  emits: ['change', 'update:value'],
  setup(props, { emit }) {
    const curComponent = computed(() => {
      return (
        props.list.find((i) => i.value === props.value) || {
          type: 'vuex-input',
          key: 'repositoryHashId',
          required: false,
        }
      )
    })

    const handleSelectChange = (val: string | number) => {
      emit('update:value', val)
      emit('change', val)
      props.handleChange(props.name, val)
    }

    // Keys that belong to the select option itself, not the child form component
    const SELECT_OPTION_KEYS = new Set(['value', 'label', 'hidden'])

    return () => {
      const { type, key, required, ...rest } = curComponent.value
      const ChildComponent = COMPONENT_MAP[type] || COMPONENT_MAP['vuex-input']
      const childValue = props.atomValue?.[key] ?? ''

      // Remove select-option-specific keys (value, label, hidden) that don't belong to child component
      const filteredRest: Record<string, any> = {}
      for (const [k, v] of Object.entries(rest)) {
        if (!SELECT_OPTION_KEYS.has(k)) {
          filteredRest[k] = v
        }
      }

      // For request-selector type, extract API-related props into optionsConf
      let childProps: Record<string, any> = {}
      if (type === 'request-selector' || type === 'selector') {
        const optionsConf: Record<string, any> = {}
        const otherProps: Record<string, any> = {}
        for (const [k, v] of Object.entries(filteredRest)) {
          if (SELECTOR_OPTIONS_CONF_KEYS.has(k)) {
            optionsConf[k] = v
          } else {
            otherProps[k] = v
          }
        }
        childProps = { ...otherProps, optionsConf, searchable: filteredRest.searchable, clearable: filteredRest.clearable }
      } else {
        childProps = filteredRest
      }

      return (
        <div class={styles.conditionalInputSelector}>
          <Select
            class={styles.groupBox}
            modelValue={props.value}
            disabled={props.disabled}
            clearable={props.clearable}
            searchable={props.searchable}
            placeholder={props.placeholder}
            onChange={handleSelectChange}
          >
            {props.list.map((item) => (
              <Select.Option
                key={item[props.settingKey]}
                value={item[props.settingKey]}
                label={item[props.displayKey]}
              />
            ))}
          </Select>
          <ChildComponent
            class={styles.inputSelector}
            name={key}
            value={childValue}
            disabled={props.disabled}
            required={required}
            handleChange={props.handleChange}
            atomValue={props.atomValue}
            {...childProps}
          />
        </div>
      )
    }
  },
})
