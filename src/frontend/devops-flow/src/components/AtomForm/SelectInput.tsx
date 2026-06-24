import { useDataSource, type SelectDataConf } from '@/hooks/useDataSource'
import { Input } from 'bkui-vue'
import { computed, defineComponent, nextTick, onBeforeUnmount, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './SelectInput.module.css'

interface OptionItem {
  id: string | number
  name: string
  disabled?: boolean
  [key: string]: unknown
}

interface GroupOption {
  id: string | number
  name: string
  disabled?: boolean
  children: OptionItem[]
}

export default defineComponent({
  name: 'SelectInput',
  props: {
    value: {
      type: [String, Number] as PropType<string | number>,
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
    placeholder: {
      type: String,
      default: '',
    },
    options: {
      type: Array as PropType<Array<OptionItem | GroupOption>>,
      default: () => [],
    },
    optionsConf: {
      type: Object as PropType<SelectDataConf & { hasGroup?: boolean }>,
      default: () => ({}),
    },
    atomValue: {
      type: Object as PropType<Record<string, unknown>>,
      default: () => ({}),
    },
    list: {
      type: Array as PropType<Array<{ id: string | number; name: string; disabled?: boolean }>>,
      default: () => [],
    },
  },
  emits: ['change', 'update:value'],
  setup(props, { emit }) {
    const { t } = useI18n()

    const inputRef = ref<HTMLElement | null>(null)
    const dropMenuRef = ref<HTMLElement | null>(null)
    const containerRef = ref<HTMLElement | null>(null)

    const displayName = ref('')
    const optionListVisible = ref(false)
    const selectedPointer = ref(0)
    const isFocused = ref(false)
    const focusTimerId = ref<ReturnType<typeof setTimeout> | null>(null)

    const hasGroup = computed(() => !!props.optionsConf?.hasGroup)

    const { list: dataSourceList, isLoading, refreshList, isApiMode } = useDataSource({
      ...props.optionsConf,
      options: props.list?.length ? props.list : props.options?.length ? props.options : props.optionsConf?.options,
      atomValue: props.atomValue,
    })

    const optionList = computed<OptionItem[]>(() => {
      if (dataSourceList.value.length) {
        return dataSourceList.value.map((item) => ({
          ...item,
          id: item.value,
          name: String(item.label),
        }))
      }
      if (Array.isArray(props.options) && props.options.length) {
        return props.options as OptionItem[]
      }
      return []
    })

    const filteredList = computed(() => {
      const searchStr = String(displayName.value || '').toLowerCase()
      if (!searchStr) return optionList.value

      if (hasGroup.value) {
        return (optionList.value as unknown as GroupOption[])
          .map((group) => ({
            ...group,
            children: (group.children || []).filter(
              (child) => String(child.name).toLowerCase().includes(searchStr),
            ),
          }))
          .filter((group) => group.children.length > 0)
      }

      return optionList.value.filter(
        (item) => String(item.name).toLowerCase().includes(searchStr),
      )
    })

    const hasOption = computed(() => filteredList.value.length > 0)

    const getDisplayName = (val: string | number) => {
      if (!val && val !== 0) return ''
      if (hasGroup.value) {
        for (const group of optionList.value as unknown as GroupOption[]) {
          const match = group.children?.find((child) => child.id === val)
          if (match) return match.name
        }
        return String(val)
      }
      const matched = optionList.value.find((item) => item.id === val)
      return matched ? String(matched.name) : String(val)
    }

    watch(
      () => props.value,
      (newVal) => {
        displayName.value = getDisplayName(newVal)
      },
      { immediate: true },
    )

    watch(optionList, () => {
      if (props.value) {
        displayName.value = getDisplayName(props.value)
      }
    })

    const emitChange = (val: string | number) => {
      emit('update:value', val)
      emit('change', val)
      props.handleChange(props.name, val)
    }

    const handleInputChange = (val: string) => {
      displayName.value = val
      optionListVisible.value = true
      if (!hasGroup.value) {
        emitChange(val.trim())
      }
    }

    const selectOption = (item: OptionItem) => {
      if (item.disabled) return
      emitChange(item.id)
      displayName.value = String(item.name)
      handleBlur()
    }

    const clearValue = () => {
      emitChange('')
      displayName.value = ''
      nextTick(() => {
        const inputEl = containerRef.value?.querySelector('input')
        inputEl?.focus()
      })
    }

    const handleFocus = () => {
      isFocused.value = true
      if (!optionListVisible.value) {
        focusTimerId.value = setTimeout(() => {
          optionListVisible.value = true
          if (isApiMode()) {
            refreshList()
          }
        }, 200)
      }
    }

    const handleBlur = () => {
      optionListVisible.value = false
      selectedPointer.value = 0
      isFocused.value = false

      if (hasGroup.value && !filteredList.value.length) {
        emitChange('')
        displayName.value = ''
      } else if (!hasGroup.value) {
        displayName.value = getDisplayName(props.value)
      }
    }

    const handleKeydown = (e: KeyboardEvent) => {
      const flatItems = hasGroup.value
        ? (filteredList.value as unknown as GroupOption[]).flatMap((g) => g.children || [])
        : filteredList.value

      switch (e.key) {
        case 'ArrowDown':
          e.preventDefault()
          if (!optionListVisible.value) optionListVisible.value = true
          if (selectedPointer.value < flatItems.length - 1) {
            selectedPointer.value++
            scrollToSelected()
          }
          break
        case 'ArrowUp':
          e.preventDefault()
          if (selectedPointer.value > 0) {
            selectedPointer.value--
            scrollToSelected()
          }
          break
        case 'Enter':
          e.preventDefault()
          if (optionListVisible.value && flatItems[selectedPointer.value]) {
            selectOption(flatItems[selectedPointer.value]!)
          }
          break
        case 'Tab':
        case 'Escape':
          handleBlur()
          break
      }
    }

    const scrollToSelected = () => {
      nextTick(() => {
        const menuEl = dropMenuRef.value
        if (!menuEl) return
        const items = menuEl.querySelectorAll(`.${styles.optionItem}, .${styles.groupItem}`)
        const target = items[selectedPointer.value]
        target?.scrollIntoView({ block: 'nearest' })
      })
    }

    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.value && !containerRef.value.contains(e.target as Node)) {
        handleBlur()
      }
    }

    // Click outside handling
    if (typeof document !== 'undefined') {
      document.addEventListener('mousedown', handleClickOutside)
    }

    onBeforeUnmount(() => {
      if (focusTimerId.value) {
        clearTimeout(focusTimerId.value)
      }
      document.removeEventListener('mousedown', handleClickOutside)
    })

    const renderGroupOptions = () => {
      let flatIndex = 0
      return (filteredList.value as unknown as GroupOption[]).map((group) => (
        <li key={group.id}>
          <div class={styles.groupName}>{group.name}</div>
          {group.children.map((child) => {
            const idx = flatIndex++
            return (
              <div
                key={child.id}
                class={[
                  styles.groupItem,
                  child.id === props.value && styles.active,
                  selectedPointer.value === idx && styles.highlighted,
                  child.disabled && styles.disabled,
                ]}
                title={String(child.name)}
                onMouseover={() => { selectedPointer.value = idx }}
                onClick={(e: MouseEvent) => { e.stopPropagation(); selectOption(child) }}
              >
                {child.name}
              </div>
            )
          })}
        </li>
      ))
    }

    const renderFlatOptions = () => {
      return filteredList.value.map((item, index) => (
        <li
          key={(item as OptionItem).id}
          class={[
            styles.optionItem,
            (item as OptionItem).id === props.value && styles.active,
            selectedPointer.value === index && styles.highlighted,
            (item as OptionItem).disabled && styles.disabled,
          ]}
          title={String((item as OptionItem).name)}
          onMouseover={() => { selectedPointer.value = index }}
          onClick={(e: MouseEvent) => { e.stopPropagation(); selectOption(item as OptionItem) }}
        >
          {(item as OptionItem).name}
        </li>
      ))
    }

    return () => (
      <div ref={containerRef} class={styles.selectInput}>
        <div class={styles.inputWrapper}>
          <Input
            ref={inputRef}
            modelValue={displayName.value}
            disabled={props.disabled || isLoading.value}
            placeholder={isLoading.value ? t('common.loading', 'Loading...') : props.placeholder}
            autocomplete="off"
            onUpdate:modelValue={handleInputChange}
            onFocus={handleFocus}
            onKeydown={handleKeydown}
          />
          {isLoading.value ? (
            <i class={[styles.loadingIcon, 'bk-icon icon-circle-2-1']} />
          ) : (
            !props.disabled && props.value && (
              <i
                class={[styles.clearIcon, 'bk-icon icon-close-circle-shape']}
                onClick={(e: MouseEvent) => { e.stopPropagation(); clearValue() }}
              />
            )
          )}
        </div>

        {hasOption.value && optionListVisible.value && !isLoading.value && (
          <div ref={dropMenuRef} class={styles.dropdownContainer}>
            <ul class={styles.optionList}>
              {hasGroup.value ? renderGroupOptions() : renderFlatOptions()}
            </ul>
          </div>
        )}
      </div>
    )
  },
})
