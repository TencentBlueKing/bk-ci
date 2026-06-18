import { useDataSource, type SelectDataConf } from '@/hooks/useDataSource'
import { Input } from 'bkui-vue'
import { computed, defineComponent, nextTick, onBeforeUnmount, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './DevopsSelect.module.css'

type SelectValue = string | number | Array<string | number>

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
  name: 'devops-select',
  props: {
    value: {
      type: [String, Number, Array] as PropType<SelectValue>,
      default: '',
    },
    name: {
      type: String,
      required: true,
    },
    label: {
      type: String,
      default: '',
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
      type: Object as PropType<SelectDataConf & { hasGroup?: boolean; multiple?: boolean }>,
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
    isLoading: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['change', 'update:value', 'focus', 'blur'],
  setup(props, { emit }) {
    const { t } = useI18n()

    const inputRef = ref<HTMLElement | null>(null)
    const dropMenuRef = ref<HTMLElement | null>(null)
    const containerRef = ref<HTMLElement | null>(null)

    const displayName = ref('')
    const optionListVisible = ref(false)
    const selectedPointer = ref(-1)
    const selectedGroupPointer = ref(-1)
    const isFocused = ref(false)
    // For multi-select: { [id]: name }
    const selectedMap = ref<Record<string, string>>({})

    const hasGroup = computed(() => !!props.optionsConf?.hasGroup)
    const isMultiple = computed(() => !!props.optionsConf?.multiple)

    // For grouped data, we skip useDataSource normalization and use props.options
    // directly to preserve the nested children structure.
    const staticOptionsForDataSource = props.optionsConf?.hasGroup
      ? []
      : props.list?.length
        ? props.list
        : props.options?.length
          ? (props.options as Array<{ id: string | number; name: string; disabled?: boolean }>)
          : props.optionsConf?.options

    const {
      list: dataSourceList,
      isLoading: dataLoading,
      refreshList,
      isApiMode,
    } = useDataSource({
      ...props.optionsConf,
      options: staticOptionsForDataSource,
      atomValue: props.atomValue,
    })

    const loading = computed(() => props.isLoading || dataLoading.value)

    const optionList = computed<Array<OptionItem | GroupOption>>(() => {
      // Group source (preserve nested structure)
      if (hasGroup.value && Array.isArray(props.options) && props.options.length) {
        return props.options as GroupOption[]
      }
      if (dataSourceList.value.length) {
        return dataSourceList.value.map((item) => ({
          ...item,
          id: item.value,
          name: String(item.label),
        })) as OptionItem[]
      }
      if (Array.isArray(props.options) && props.options.length) {
        return props.options as OptionItem[]
      }
      return []
    })

    const filteredList = computed(() => {
      const searchStr = String(displayName.value || '').toLowerCase()

      if (hasGroup.value) {
        const groups = optionList.value as GroupOption[]
        // In multi-select, do not filter (show all)
        if (!searchStr || isMultiple.value) return groups
        return groups
          .map((group) => ({
            ...group,
            children: (group.children || []).filter((child) =>
              String(child.name).toLowerCase().includes(searchStr),
            ),
          }))
          .filter((group) => group.children.length > 0)
      }

      const flat = optionList.value as OptionItem[]
      if (!searchStr || isMultiple.value) return flat
      return flat.filter((item) => String(item.name).toLowerCase().includes(searchStr))
    })

    const hasOption = computed(() => filteredList.value.length > 0)

    const showClearIcon = computed(() => {
      if (props.disabled || loading.value) return false
      if (isMultiple.value) return Object.keys(selectedMap.value).length > 0
      return !!props.value && props.value !== ''
    })

    const isActive = (id: string | number): boolean => {
      if (isMultiple.value) {
        if (Array.isArray(props.value)) return props.value.includes(id)
        if (typeof props.value === 'string' && props.value) {
          return props.value.split(',').includes(String(id))
        }
        return false
      }
      return id === props.value
    }

    const flattenOptions = (): OptionItem[] => {
      if (hasGroup.value) {
        return (optionList.value as GroupOption[]).reduce<OptionItem[]>(
          (acc, group) => acc.concat(group.children || []),
          [],
        )
      }
      return optionList.value as OptionItem[]
    }

    const getDisplayName = (val: string | number): string => {
      if (val === '' || val === undefined || val === null) return ''
      const list = flattenOptions()
      const matched = list.find((item) => item.id === val)
      if (matched) return String(matched.name)
      // Value exists but no matching option (data not loaded yet or invalid)
      if (!loading.value && list.length > 0) {
        showInvalidValueTips(String(val))
      }
      return ''
    }

    const showInvalidValueTips = (val: string) => {
      // Show a console warning instead of bk-message to avoid intrusive popups in flow
      console.warn(`[devops-select] Invalid value for "${props.label || props.name}": ${val}`)
    }

    const syncMultipleDisplay = (val: SelectValue) => {
      const valArr = Array.isArray(val)
        ? val.map((v) => String(v))
        : typeof val === 'string' && val
          ? val.split(',')
          : []
      const valSet = new Set(valArr)
      const list = flattenOptions()
      const typeMap = list.reduce<Record<string, OptionItem>>((acc, opt) => {
        acc[String(opt.id)] = opt
        return acc
      }, {})
      const resultMap: Record<string, string> = {}
      const invalid: string[] = []
      valSet.forEach((v) => {
        if (Object.prototype.hasOwnProperty.call(typeMap, v)) {
          const opt = typeMap[v]!
          resultMap[String(opt.id)] = String(opt.name)
        } else if (v) {
          invalid.push(v)
        }
      })
      selectedMap.value = resultMap
      displayName.value = Object.values(resultMap).join(',')
      if (!loading.value && invalid.length && list.length > 0) {
        showInvalidValueTips(invalid.join(','))
      }
    }

    const syncSingleDisplay = (val: SelectValue) => {
      if (Array.isArray(val)) {
        displayName.value = val.join(',')
      } else {
        displayName.value = getDisplayName(val)
      }
    }

    const syncDisplay = (val: SelectValue) => {
      if (isMultiple.value) syncMultipleDisplay(val)
      else syncSingleDisplay(val)
    }

    watch(
      () => props.value,
      (newVal) => {
        syncDisplay(newVal)
      },
      { immediate: true },
    )

    watch(optionList, () => {
      if (
        props.value !== '' &&
        props.value !== undefined &&
        props.value !== null &&
        !(Array.isArray(props.value) && props.value.length === 0)
      ) {
        syncDisplay(props.value)
      }
    })

    const emitChange = (val: SelectValue) => {
      emit('update:value', val)
      emit('change', val)
      props.handleChange(props.name, val)
    }

    const resetSelectPointer = () => {
      selectedPointer.value = -1
      selectedGroupPointer.value = -1
    }

    const setSelectPointer = (index: number) => {
      selectedPointer.value = index
    }

    const setSelectGroupPointer = (groupIndex: number, childIndex: number) => {
      selectedGroupPointer.value = groupIndex
      selectedPointer.value = childIndex
    }

    const handleInputChange = (val: string) => {
      displayName.value = val
      optionListVisible.value = true
    }

    const selectOption = (item: OptionItem) => {
      if (item.disabled) return
      if (!isMultiple.value) {
        emitChange(item.id)
        nextTick(() => handleBlur())
      } else {
        const id = String(item.id)
        const next = { ...selectedMap.value }
        if (id in next) {
          delete next[id]
        } else {
          next[id] = String(item.name)
        }
        selectedMap.value = next
        const ids = Object.keys(next)
        // Maintain output type as comma-separated string (matches devops-pipeline behavior)
        emitChange(ids.join(','))
        displayName.value = Object.values(next).join(',')
      }
    }

    const clearValue = () => {
      if (isMultiple.value) {
        selectedMap.value = {}
        emitChange('')
      } else {
        emitChange('')
      }
      displayName.value = ''
      nextTick(() => {
        const inputEl = containerRef.value?.querySelector('input') as HTMLInputElement | null
        inputEl?.focus()
      })
    }

    const handleFocus = (e?: Event) => {
      if (props.disabled) return
      isFocused.value = true
      if (!optionListVisible.value) {
        optionListVisible.value = true
        emit('focus', e)
        if (isApiMode()) {
          refreshList()
        }
      }
    }

    const handleBlur = () => {
      if (!optionListVisible.value) return
      optionListVisible.value = false
      resetSelectPointer()
      isFocused.value = false
      const inputEl = containerRef.value?.querySelector('input') as HTMLInputElement | null
      inputEl?.blur()
      emit('blur', null)

      // Sync display back to source of truth so users don't see stale search text
      syncDisplay(props.value)
    }

    const handleEnter = () => {
      let option: OptionItem | undefined
      if (
        hasGroup.value &&
        selectedGroupPointer.value >= 0 &&
        selectedPointer.value >= 0
      ) {
        const group = (filteredList.value as GroupOption[])[selectedGroupPointer.value]
        option = group?.children?.[selectedPointer.value]
      } else if (!hasGroup.value && selectedPointer.value >= 0) {
        option = (filteredList.value as OptionItem[])[selectedPointer.value]
      }

      if (option) {
        selectOption(option)
      } else {
        handleBlur()
      }
    }

    const handleKeydown = (e: KeyboardEvent) => {
      const flatItems = hasGroup.value
        ? (filteredList.value as GroupOption[]).flatMap((g) => g.children || [])
        : (filteredList.value as OptionItem[])

      switch (e.key) {
        case 'ArrowDown': {
          e.preventDefault()
          if (!optionListVisible.value) optionListVisible.value = true
          if (selectedPointer.value < flatItems.length - 1) {
            selectedPointer.value++
            scrollToSelected()
          }
          break
        }
        case 'ArrowUp': {
          e.preventDefault()
          if (selectedPointer.value > 0) {
            selectedPointer.value--
            scrollToSelected()
          }
          break
        }
        case 'Enter': {
          e.preventDefault()
          handleEnter()
          break
        }
        case 'Tab':
        case 'Escape':
          handleBlur()
          break
        case ',':
          resetSelectPointer()
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

    if (typeof document !== 'undefined') {
      document.addEventListener('mousedown', handleClickOutside)
    }

    onBeforeUnmount(() => {
      document.removeEventListener('mousedown', handleClickOutside)
    })

    const renderGroupOptions = () => {
      let flatIndex = 0
      return (filteredList.value as GroupOption[]).map((group, groupIndex) => (
        <li key={String(group.id) + groupIndex} class={styles.groupWrap}>
          <div class={styles.groupName} title={String(group.name)}>{group.name}</div>
          {group.children?.map((child, childIndex) => {
            const idx = flatIndex++
            return (
              <div
                key={child.id}
                class={[
                  styles.groupItem,
                  isActive(child.id) && styles.active,
                  selectedGroupPointer.value === groupIndex &&
                    selectedPointer.value === childIndex &&
                    styles.highlighted,
                  child.disabled && styles.disabled,
                ]}
                title={String(child.name)}
                onMouseover={() => setSelectGroupPointer(groupIndex, childIndex)}
                onClick={(e: MouseEvent) => {
                  e.stopPropagation()
                  selectOption(child)
                }}
                {...{ 'data-flat-index': idx }}
              >
                <span class={styles.optionLabel}>{child.name}</span>
                {isMultiple.value && isActive(child.id) && (
                  <i class={['devops-icon', 'icon-check-1', styles.checkIcon]} />
                )}
              </div>
            )
          })}
        </li>
      ))
    }

    const renderFlatOptions = () => {
      return (filteredList.value as OptionItem[]).map((item, index) => (
        <li
          key={String(item.id) + index}
          class={[
            styles.optionItem,
            isActive(item.id) && styles.active,
            selectedPointer.value === index && styles.highlighted,
            item.disabled && styles.disabled,
          ]}
          title={String(item.name)}
          onMouseover={() => setSelectPointer(index)}
          onClick={(e: MouseEvent) => {
            e.stopPropagation()
            selectOption(item)
          }}
        >
          <span class={styles.optionLabel}>{item.name}</span>
          {isMultiple.value && isActive(item.id) && (
            <i class={['devops-icon', 'icon-check-1', styles.checkIcon]} />
          )}
        </li>
      ))
    }

    return () => (
      <div ref={containerRef} class={styles.devopsSelect}>
        <div class={styles.inputWrapper}>
          <Input
            ref={inputRef}
            modelValue={displayName.value}
            disabled={props.disabled || loading.value}
            placeholder={loading.value ? t('common.loading', 'Loading...') : props.placeholder}
            autocomplete="off"
            title={displayName.value}
            onUpdate:modelValue={handleInputChange}
            onFocus={handleFocus}
            onKeydown={handleKeydown}
          />
          {loading.value ? (
            <i class={[styles.loadingIcon, 'bk-icon icon-circle-2-1']} />
          ) : (
            showClearIcon.value && (
              <i
                class={[styles.clearIcon, 'bk-icon icon-close-circle-shape']}
                onClick={(e: MouseEvent) => {
                  e.stopPropagation()
                  clearValue()
                }}
              />
            )
          )}
        </div>

        {hasOption.value && optionListVisible.value && !loading.value && (
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
