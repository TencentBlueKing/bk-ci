import type { Param } from '@/api/flowModel'
import { SvgIcon } from '@/components/SvgIcon'
import type { OptionsApiConfig, ParamOption } from '@/types/variable'
import { OptionsSourceType } from '@/types/variable'
import { get } from '@/utils/http'
import { Input, Loading, Message, Popover } from 'bkui-vue'
import { computed, defineComponent, nextTick, ref, watch, type PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './OptionsEditor.module.css'

export default defineComponent({
  name: 'OptionsEditor',
  props: {
    options: {
      type: Array as PropType<ParamOption[]>,
      default: () => [],
    },
    payload: {
      type: Object as PropType<OptionsApiConfig>,
      default: () => ({ type: OptionsSourceType.LIST }),
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    // 当前流程的所有变量，用于 {variableName} 语法替换
    variables: {
      type: Array as PropType<Param[]>,
      default: () => [],
    },
    // 当前正在编辑的变量 ID，用于排除自引用
    currentVariableId: {
      type: String,
      default: '',
    },
  },
  emits: ['update:options', 'update:payload'],
  setup(props, { emit }) {
    const { t } = useI18n()

    // 本地状态
    const localOptions = ref<ParamOption[]>([...props.options])
    const localPayload = ref<OptionsApiConfig>({
      ...props.payload,
      type: props.payload?.type ?? OptionsSourceType.LIST,
      url: props.payload?.url ?? '',
      dataPath: props.payload?.dataPath ?? '',
      paramId: props.payload?.paramId ?? 'id',
      paramName: props.payload?.paramName ?? 'name',
    })

    // 批量导入输入
    const batchInput = ref('')

    // 验证错误
    const keyErrors = ref<Record<number, string>>({})
    const valueErrors = ref<Record<number, string>>({})

    // 拖拽状态
    const dragIndex = ref<number | null>(null)
    const dragOverIndex = ref<number | null>(null)

    // API 数据拉取状态
    const isLoading = ref(false)

    // 当前数据源类型
    const sourceType = computed(() => localPayload.value.type)

    // 构建变量值映射表
    const variableValueMap = computed(() => {
      const map: Record<string, string> = {}
      props.variables.forEach((v) => {
        // 排除当前正在编辑的变量
        if (v.id !== props.currentVariableId) {
          map[v.id] = String(v.defaultValue ?? '')
        }
      })
      return map
    })

    // 解析 URL 中的 {variableName} 语法，提取依赖的变量
    const urlDependencies = computed(() => {
      const url = localPayload.value.url || ''
      const regex = /\{([^}]+)\}/g
      const deps: string[] = []
      let match
      while ((match = regex.exec(url)) !== null) {
        if (match[1]) {
          deps.push(match[1])
        }
      }
      return deps
    })

    // 替换 URL 中的变量引用
    const resolvedUrl = computed(() => {
      let url = localPayload.value.url || ''
      if (!url) return ''

      url = url.replace(/\{([^}]+)\}/g, (_, varName) => {
        return variableValueMap.value[varName] ?? ''
      })
      return url
    })

    // 检查是否所有依赖变量都有值
    const allDependenciesResolved = computed(() => {
      return urlDependencies.value.every((dep) => {
        const value = variableValueMap.value[dep]
        return value !== undefined && value !== ''
      })
    })

    // 监听 props 变化
    watch(
      () => props.options,
      (newVal) => {
        localOptions.value = [...newVal]
      },
      { deep: true },
    )

    watch(
      () => props.payload,
      (newVal) => {
        localPayload.value = {
          ...newVal,
          type: newVal?.type ?? OptionsSourceType.LIST,
          url: newVal?.url ?? '',
          dataPath: newVal?.dataPath ?? '',
          paramId: newVal?.paramId ?? 'id',
          paramName: newVal?.paramName ?? 'name',
        }
      },
      { deep: true },
    )

    // 切换数据源类型
    const handleSourceTypeChange = (type: OptionsSourceType) => {
      if (props.disabled) return
      // 切换模式时，如果切换到 API 模式，清空选项列表
      if (type === OptionsSourceType.API && localPayload.value.type !== OptionsSourceType.API) {
        localOptions.value = []
        emitOptionsChange()
      }
      localPayload.value.type = type
      emitPayloadChange()
    }

    // 更新选项列表
    const emitOptionsChange = () => {
      emit('update:options', [...localOptions.value])
    }

    // 更新 API 配置
    const emitPayloadChange = () => {
      emit('update:payload', { ...localPayload.value })
    }

    // 添加选项
    const handleAddOption = (index?: number) => {
      const newOption: ParamOption = {
        key: '',
        value: '',
      }
      if (index === undefined) {
        localOptions.value.push(newOption)
      } else {
        localOptions.value.splice(index + 1, 0, newOption)
      }
      emitOptionsChange()
    }

    // 删除选项
    const handleRemoveOption = (index: number) => {
      localOptions.value.splice(index, 1)
      emitOptionsChange()
      validateAllOptions()
    }

    // 编辑选项
    const handleEditOption = (index: number, field: 'key' | 'value', val: string) => {
      const option = localOptions.value[index]
      if (option) {
        option[field] = val
        emitOptionsChange()
        validateAllOptions()
      }
    }

    // 验证所有选项
    const validateAllOptions = () => {
      nextTick(() => {
        keyErrors.value = findInvalidItems('key', t('flow.variable.optionId'))
        valueErrors.value = findInvalidItems('value', t('flow.variable.optionLabel'))
      })
    }

    // 找出校验有错误的选项
    const findInvalidItems = (key: 'key' | 'value', errPrefix: string): Record<number, string> => {
      const seen = new Map<string, number>()
      const result: Record<number, string> = {}

      for (let i = 0; i < localOptions.value.length; i++) {
        const option = localOptions.value[i]
        if (!option) continue

        const value = option[key]

        if (!value) {
          if (key === 'key') {
            result[i] = t('flow.variable.requiredTips', { 0: errPrefix })
          }
        } else {
          const existingIndex = seen.get(value)
          if (existingIndex !== undefined) {
            result[i] = t('flow.variable.noRepeatTips', { 0: errPrefix })
            if (!result[existingIndex]) {
              result[existingIndex] = t('flow.variable.noRepeatTips', { 0: errPrefix })
            }
          } else {
            seen.set(value, i)
          }
        }
      }
      return result
    }

    // 处理批量导入
    const handleBatchImport = () => {
      if (!batchInput.value.trim()) {
        return
      }

      const existingPairs = new Set(localOptions.value.map((item) => `${item.key}=${item.value}`))

      const newOptions = batchInput.value
        .split('\n')
        .map((line) => {
          const v = line.trim()
          if (!v) return null
          const equalPos = v.indexOf('=')
          const [key, value] =
            equalPos > -1 ? [v.slice(0, equalPos), v.slice(equalPos + 1)] : [v, v]
          return { key, value }
        })
        .filter((item): item is ParamOption => {
          if (!item) return false
          const identifier = `${item.key}=${item.value}`
          if (existingPairs.has(identifier)) {
            return false
          }
          existingPairs.add(identifier)
          return true
        })

      if (newOptions.length > 0) {
        localOptions.value.push(...newOptions)
        emitOptionsChange()
        validateAllOptions()
      }

      batchInput.value = ''
    }

    // 批量复制
    const handleBatchCopy = async () => {
      // 去重
      const uniqueItemsMap = new Map<string, ParamOption>()
      localOptions.value.forEach((item) => {
        const identifier = `${item.key}=${item.value}`
        if (!uniqueItemsMap.has(identifier)) {
          uniqueItemsMap.set(identifier, item)
        }
      })
      const uniqueItems = Array.from(uniqueItemsMap.values())

      const copyText = uniqueItems
        .map((item) => (item.key !== item.value ? `${item.key}=${item.value}` : item.key))
        .join('\n')

      try {
        await navigator.clipboard.writeText(copyText)
        Message({ theme: 'success', message: t('flow.variable.copySuccess') })
      } catch {
        Message({ theme: 'error', message: 'Copy failed' })
      }
    }

    // 更新 API 配置字段
    const handleApiConfigChange = (field: keyof OptionsApiConfig, value: string) => {
      ;(localPayload.value as Record<string, unknown>)[field] = value
      emitPayloadChange()
    }

    // 根据数据路径获取数据
    const getDataByPath = (data: unknown, path: string): unknown[] => {
      if (!path) {
        return Array.isArray(data) ? data : []
      }
      const keys = path.split('.')
      let result: unknown = data
      for (const key of keys) {
        if (result && typeof result === 'object' && key in result) {
          result = (result as Record<string, unknown>)[key]
        } else {
          return []
        }
      }
      return Array.isArray(result) ? result : []
    }

    // 从接口获取选项数据
    const fetchOptionsFromApi = async (showMessage = true) => {
      const url = resolvedUrl.value
      if (!url) {
        if (showMessage) {
          Message({ theme: 'warning', message: t('flow.variable.apiUrlRequired') })
        }
        return
      }

      // 检查依赖变量是否都有值
      if (!allDependenciesResolved.value) {
        if (showMessage) {
          Message({ theme: 'warning', message: t('flow.variable.dependencyNotResolved') })
        }
        return
      }

      isLoading.value = true
      try {
        const response = await get<unknown>(url)
        const dataPath = localPayload.value.dataPath || ''
        // 如果用户没有填写，使用默认值 'id' 和 'name'
        const paramId = localPayload.value.paramId?.trim() || 'id'
        const paramName = localPayload.value.paramName?.trim() || 'name'

        const list = getDataByPath(response, dataPath)
        const fetchedOptions = list.map((item: unknown) => {
          const itemObj = item as Record<string, unknown>
          return {
            key: String(itemObj[paramId] ?? ''),
            value: String(itemObj[paramName] ?? ''),
          }
        })

        // 直接更新选项列表
        localOptions.value = fetchedOptions
        emitOptionsChange()

        if (showMessage) {
          Message({
            theme: 'success',
            message: t('flow.variable.fetchSuccess', { count: fetchedOptions.length }),
          })
        }
      } catch (error) {
        console.error('Failed to fetch options from API:', error)
        if (showMessage) {
          Message({ theme: 'error', message: t('flow.variable.fetchFailed') })
        }
        localOptions.value = []
        emitOptionsChange()
      } finally {
        isLoading.value = false
      }
    }

    // URL 输入框失焦时请求数据
    const handleUrlBlur = () => {
      if (sourceType.value === OptionsSourceType.API && localPayload.value.url) {
        fetchOptionsFromApi(true)
      }
    }

    // 拖拽开始
    const handleDragStart = (index: number) => {
      dragIndex.value = index
    }

    // 拖拽经过
    const handleDragOver = (e: DragEvent, index: number) => {
      e.preventDefault()
      dragOverIndex.value = index
    }

    // 拖拽结束
    const handleDragEnd = () => {
      if (
        dragIndex.value !== null &&
        dragOverIndex.value !== null &&
        dragIndex.value !== dragOverIndex.value
      ) {
        const items = [...localOptions.value]
        const [draggedItem] = items.splice(dragIndex.value, 1)
        if (draggedItem) {
          items.splice(dragOverIndex.value, 0, draggedItem)
          localOptions.value = items
          emitOptionsChange()
        }
      }
      dragIndex.value = null
      dragOverIndex.value = null
    }

    // 渲染提示内容
    const renderTooltipContent = () => (
      <ul class={styles.tooltipsList}>
        <li>{t('flow.variable.batchImportRule1')}</li>
        <li>{t('flow.variable.batchImportRule2')}</li>
      </ul>
    )

    // 渲染批量导入区域
    const renderBatchImport = () => (
      <div class={styles.batchAddSection}>
        <div class={styles.batchAddHeader}>
          <span>{t('flow.variable.batchImportTitle')}</span>
          <Popover
            placement="top"
            popoverDelay={100}
            extCls={styles.tipsPopover}
            v-slots={{
              content: renderTooltipContent,
            }}
          >
            <span class={styles.infoIcon}>
              <SvgIcon name="info-circle" />
            </span>
          </Popover>
        </div>
        <div class={styles.batchAddField}>
          <Input
            v-model={batchInput.value}
            type="textarea"
            rows={3}
            placeholder={t('flow.variable.batchImportPlaceholder')}
            disabled={props.disabled}
          />
        </div>
        <div class={styles.batchConfirmDiv}>
          <span onClick={handleBatchImport}>{t('flow.variable.batchImportBtn')}</span>
        </div>
      </div>
    )

    // 渲染选项列表
    const renderOptionsList = () => (
      <div class={styles.keyValueNormal}>
        {/* 批量复制按钮 */}
        {localOptions.value.length > 0 && (
          <p class={styles.batchCopy} onClick={handleBatchCopy}>
            <SvgIcon name="copy" size={14} />
            {t('flow.variable.batchCopy')}
          </p>
        )}

        <div class={styles.optionFieldLabel}>
          <label>{t('flow.variable.optionSetting')}</label>
        </div>

        {localOptions.value.length > 0 ? (
          <div class={styles.optionsList}>
            {localOptions.value.map((option, index) => (
              <div
                key={`option-${index}`}
                class={[styles.paramItem, dragOverIndex.value === index && styles.dragOver]}
                draggable={!props.disabled}
                onDragstart={() => handleDragStart(index)}
                onDragover={(e: DragEvent) => handleDragOver(e, index)}
                onDragend={handleDragEnd}
              >
                <span class={styles.columnDragIcon}>
                  <SvgIcon name="drag-small" size={16} />
                </span>

                <div class={styles.optionInputWrapper}>
                  <Input
                    modelValue={option.key}
                    placeholder={t('flow.variable.optionValTips')}
                    disabled={props.disabled}
                    class={keyErrors.value[index] ? styles.hasError : ''}
                    onUpdate:modelValue={(val: string) => handleEditOption(index, 'key', val)}
                  />
                  {keyErrors.value[index] && (
                    <span class={styles.errorMsg}>{keyErrors.value[index]}</span>
                  )}
                </div>

                <div class={styles.optionInputWrapper}>
                  <Input
                    modelValue={option.value}
                    placeholder={t('flow.variable.optionNameTips')}
                    disabled={props.disabled}
                    class={valueErrors.value[index] ? styles.hasError : ''}
                    onUpdate:modelValue={(val: string) => handleEditOption(index, 'value', val)}
                  />
                  {valueErrors.value[index] && (
                    <span class={styles.errorMsg}>{valueErrors.value[index]}</span>
                  )}
                </div>

                {!props.disabled && (
                  <div class={styles.operateIconDiv}>
                    <span class={styles.operateIcon} onClick={() => handleAddOption(index)}>
                      <SvgIcon name="add-small" size={16} />
                    </span>
                    <span
                      class={[styles.operateIcon, styles.deleteIcon]}
                      onClick={() => handleRemoveOption(index)}
                    >
                      <SvgIcon name="minus-circle" size={16} />
                    </span>
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : (
          !props.disabled && (
            <a class={styles.textLink} onClick={() => handleAddOption()}>
              <SvgIcon name="add-small" size={14} />
              <span>{t('flow.variable.addItem')}</span>
            </a>
          )
        )}
      </div>
    )

    // 渲染 API 配置
    const renderApiConfig = () => (
      <Loading loading={isLoading.value}>
        <div class={styles.apiConfigForm}>
          <div class={styles.formField}>
            <label class={styles.fieldLabel}>{t('flow.variable.apiUrl')}</label>
            <div class={styles.urlInputWrapper}>
              <Input
                modelValue={localPayload.value.url ?? ''}
                placeholder={t('flow.variable.apiUrlPlaceholder')}
                disabled={props.disabled}
                onUpdate:modelValue={(val: string) => handleApiConfigChange('url', val)}
                onBlur={handleUrlBlur}
              />
              <span
                class={[
                  styles.refreshBtn,
                  (props.disabled || !localPayload.value.url) && styles.disabled,
                ]}
                onClick={() =>
                  !props.disabled && localPayload.value.url && fetchOptionsFromApi(true)
                }
                title={t('flow.variable.refreshOptions')}
              >
                <SvgIcon name="refresh-line" size={16} />
              </span>
            </div>
            <span class={styles.fieldTips}>{t('flow.variable.apiUrlTipsWithVar')}</span>
            {/* 显示解析后的 URL */}
            {urlDependencies.value.length > 0 && localPayload.value.url && (
              <div class={styles.resolvedUrlPreview}>
                <span class={styles.resolvedUrlLabel}>{t('flow.variable.resolvedUrl')}:</span>
                <span
                  class={[
                    styles.resolvedUrlValue,
                    !allDependenciesResolved.value && styles.hasUnresolved,
                  ]}
                >
                  {resolvedUrl.value || t('flow.variable.waitingForVariables')}
                </span>
              </div>
            )}
          </div>

          <div class={styles.formField}>
            <label class={styles.fieldLabel}>{t('flow.variable.dataPath')}</label>
            <Input
              modelValue={localPayload.value.dataPath ?? ''}
              placeholder={t('flow.variable.dataPathPlaceholder')}
              disabled={props.disabled}
              onUpdate:modelValue={(val: string) => handleApiConfigChange('dataPath', val)}
            />
          </div>

          <div class={styles.formField}>
            <label class={styles.fieldLabel}>{t('flow.variable.paramId')}</label>
            <Input
              modelValue={localPayload.value.paramId ?? ''}
              placeholder={t('flow.variable.paramIdPlaceholder')}
              disabled={props.disabled}
              onUpdate:modelValue={(val: string) => handleApiConfigChange('paramId', val)}
            />
          </div>

          <div class={styles.formField}>
            <label class={styles.fieldLabel}>{t('flow.variable.paramName')}</label>
            <Input
              modelValue={localPayload.value.paramName ?? ''}
              placeholder={t('flow.variable.paramNamePlaceholder')}
              disabled={props.disabled}
              onUpdate:modelValue={(val: string) => handleApiConfigChange('paramName', val)}
            />
          </div>
        </div>
      </Loading>
    )

    return () => (
      <div class={styles.selectorTypeParam}>
        {/* 数据源类型切换 Tab */}
        <div class={styles.optionType}>
          <div
            class={[
              styles.typeSelect,
              sourceType.value === OptionsSourceType.LIST && styles.isActive,
            ]}
            onClick={() => handleSourceTypeChange(OptionsSourceType.LIST)}
          >
            {t('flow.variable.optionSourceList')}
          </div>
          <div
            class={[
              styles.typeSelect,
              sourceType.value === OptionsSourceType.API && styles.isActive,
            ]}
            onClick={() => handleSourceTypeChange(OptionsSourceType.API)}
          >
            {t('flow.variable.optionSourceApi')}
          </div>
        </div>

        {/* 内容区域 */}
        <div class={styles.optionItems}>
          {sourceType.value === OptionsSourceType.LIST ? (
            <section>
              {renderBatchImport()}
              {renderOptionsList()}
            </section>
          ) : (
            <section>{renderApiConfig()}</section>
          )}
        </div>
      </div>
    )
  },
})
