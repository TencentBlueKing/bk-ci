import { computed, defineComponent, ref, watch } from 'vue'
import styles from './VuexTextarea.module.css'

export default defineComponent({
  name: 'VuexTextarea',
  props: {
    value: {
      type: [String, Number],
      default: '',
    },
    name: {
      type: String,
      required: true,
    },
    handleChange: {
      type: Function,
      default: () => () => {},
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    readOnly: {
      type: Boolean,
      default: false,
    },
    readOnlyCheck: {
      type: Boolean,
      default: true,
    },
    placeholder: {
      type: String,
      default: '',
    },
    clickUnfold: {
      type: Boolean,
      default: false,
    },
    hoverUnfold: {
      type: Boolean,
      default: false,
    },
    maxlength: {
      type: Number,
      default: undefined,
    },
    rows: {
      type: Number,
      default: 3,
    },
    descTooltips: {
      type: String,
      default: '',
    },
  },
  emits: ['change', 'update:value'],
  setup(props, { emit }) {
    const innerValue = ref(String(props.value ?? ''))

    watch(
      () => props.value,
      (value) => {
        innerValue.value = String(value ?? '')
      },
      { immediate: true },
    )

    const isReadonly = computed(() => (props.readOnlyCheck && props.readOnly) || props.disabled)
    const title = computed(() => {
      if (props.descTooltips && props.disabled) {
        return props.descTooltips
      }
      return isReadonly.value ? innerValue.value : ''
    })

    const emitValue = (value: string) => {
      emit('update:value', value)
      emit('change', value)
      props.handleChange(props.name, value)
    }

    const handleInput = (event: Event) => {
      const value = (event.target as HTMLTextAreaElement).value
      innerValue.value = value
      if (value !== String(props.value ?? '')) {
        emitValue(value)
      }
    }

    const handleBlur = (event: Event) => {
      const value = (event.target as HTMLTextAreaElement).value.trim()
      innerValue.value = value
      if (value !== String(props.value ?? '').trim() || value !== '') {
        emitValue(value)
      }
    }

    return () => (
      <textarea
        name={props.name}
        value={innerValue.value}
        rows={props.rows}
        placeholder={props.placeholder}
        disabled={isReadonly.value}
        title={title.value}
        maxlength={props.maxlength}
        class={[
          styles.textarea,
          props.clickUnfold && styles.clickUnfold,
          props.hoverUnfold && isReadonly.value && styles.hoverUnfold,
        ]}
        onInput={handleInput}
        onBlur={handleBlur}
      />
    )
  },
})
