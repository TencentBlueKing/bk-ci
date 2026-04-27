import { defineComponent, ref, computed, type PropType } from 'vue'
import { Button, Checkbox, Tag as BkTag } from 'bkui-vue'
import styles from './StructuredBlocks.module.css'

export interface BkFormOption {
  value: string
  label: string
  description?: string
  tags?: string[]
  disabled?: boolean
}

export interface BkFormData {
  title: string
  description?: string
  options: BkFormOption[]
  submitLabel: string
  multiple?: boolean
}

const TAG_THEME: Record<string, string> = {
  推荐: 'success',
  最小权限: 'success',
  权限较大: 'warning',
  危险: 'danger',
}

function getTagTheme(tag: string): string {
  return TAG_THEME[tag] || 'info'
}

export default defineComponent({
  name: 'SelectionFormBlock',
  props: {
    content: { type: Object as PropType<BkFormData>, required: true },
    onSendMessage: { type: Function as PropType<(msg: string) => void> },
  },
  setup(props) {
    const selected = ref<string[]>([])
    const submitted = ref(false)

    const isMultiple = computed(() => props.content.multiple !== false)

    function toggleOption(value: string) {
      if (submitted.value) return
      if (isMultiple.value) {
        const idx = selected.value.indexOf(value)
        if (idx >= 0) {
          selected.value.splice(idx, 1)
        } else {
          selected.value.push(value)
        }
      } else {
        selected.value = [value]
      }
    }

    function handleSubmit() {
      if (!selected.value.length || submitted.value) return
      submitted.value = true

      const { options, title } = props.content
      const labels = selected.value.map((v) => {
        const opt = options.find((o) => o.value === v)
        return opt ? `${opt.label}(${v})` : v
      })

      const message = `我选择了以下${title}：${labels.join('、')}，请帮我处理。`
      props.onSendMessage?.(message)
    }

    return () => {
      const { title, description, options, submitLabel } = props.content

      return (
        <div class={styles.activityCard}>
          <div class={styles.cardTitle}>{title}</div>
          {description && (
            <div class={styles.formDescription}>{description}</div>
          )}
          <div class={styles.optionList}>
            {options.map((opt) => {
              const isSelected = selected.value.includes(opt.value)
              const isDisabled = opt.disabled || submitted.value
              return (
                <div
                  key={opt.value}
                  class={[
                    styles.optionItem,
                    isSelected && styles.optionItemSelected,
                    isDisabled && styles.optionItemDisabled,
                  ]}
                  onClick={() => !isDisabled && toggleOption(opt.value)}
                >
                  <div class={styles.optionHeader}>
                    <Checkbox
                      modelValue={isSelected}
                      disabled={isDisabled}
                      class={styles.optionCheckbox}
                    />
                    <span class={styles.optionLabel}>{opt.label}</span>
                    {opt.tags?.map((tag) => (
                      <BkTag
                        key={tag}
                        theme={getTagTheme(tag)}
                        size="small"
                        class={styles.optionTag}
                      >
                        {tag}
                      </BkTag>
                    ))}
                  </div>
                  {opt.description && (
                    <div class={styles.optionDescription}>
                      {opt.description}
                    </div>
                  )}
                </div>
              )
            })}
          </div>
          <div class={styles.formFooter}>
            <Button
              theme="primary"
              size="small"
              disabled={!selected.value.length || submitted.value}
              onClick={handleSubmit}
            >
              {submitted.value ? '已提交' : submitLabel}
            </Button>
          </div>
        </div>
      )
    }
  },
})
