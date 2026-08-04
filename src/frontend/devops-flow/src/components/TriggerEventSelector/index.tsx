import type { TriggerBaseItem } from '@/api/trigger'
import { SvgIcon } from '@/components/SvgIcon'
import { useTriggerManager } from '@/hooks/useTriggerManager'
import { isManualTriggerAtomCode } from '@/utils/flowConst'
import { Input, Loading, Message } from 'bkui-vue'
import { computed, defineComponent, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import TriggerEventCard from './TriggerEventCard'
import styles from './TriggerEventSelector.module.css'

export default defineComponent({
  name: 'TriggerEventSelector',
  props: {
    projectCode: {
      type: String,
      default: '',
    },
    existingTriggerAtomCodes: {
      type: Array as () => string[],
      default: () => [],
    },
  },
  emits: ['update:visible', 'select', 'close'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const triggerManager = useTriggerManager()

    const searchKey = ref('')
    const searchInputWrapRef = ref<HTMLElement | null>(null)
    const selectedClassify = ref<string>('')
    const allEventList = ref<TriggerBaseItem[]>([])
    /** 全量事件列表，仅用于搜索时重算左侧分类数量 */
    const masterEventList = ref<TriggerBaseItem[]>([])
    const loading = ref(false)
    const selectingAtomCode = ref<string | null>(null)
    let nativeSearchInput: HTMLInputElement | null = null
    let isComposing = false
    let removeNativeSearchListeners: (() => void) | null = null

    const updateSearchKey = (val: string | number = '') => {
      searchKey.value = String(val ?? '')
    }

    /**
     * 英文输入走 input → v-model，本身正常。
     * 中文拼音会先 compositionstart，bkui Input 在此期间吞掉 input/change，
     * 依赖 compositionend 再同步；现网该同步偶发失败，出现「框里有字、searchKey 仍空」。
     * 因此在原生 input 上补一层 compositionend 同步。
     */
    const bindNativeSearchSync = () => {
      removeNativeSearchListeners?.()
      removeNativeSearchListeners = null

      nativeSearchInput = searchInputWrapRef.value?.querySelector('input') ?? null
      if (!nativeSearchInput) return

      const syncFromNative = () => {
        if (nativeSearchInput && nativeSearchInput.value !== searchKey.value) {
          searchKey.value = nativeSearchInput.value
        }
      }

      const onCompositionStart = () => {
        isComposing = true
      }
      const onCompositionEnd = () => {
        isComposing = false
        syncFromNative()
      }
      // 部分输入法上屏后 compositionend 丢失时，keyup 仍能拿到最终值（bkui 不拦截 keyup）
      const onNativeKeyup = () => {
        if (isComposing) return
        syncFromNative()
      }

      nativeSearchInput.addEventListener('compositionstart', onCompositionStart)
      nativeSearchInput.addEventListener('compositionend', onCompositionEnd)
      nativeSearchInput.addEventListener('keyup', onNativeKeyup)

      removeNativeSearchListeners = () => {
        nativeSearchInput?.removeEventListener('compositionstart', onCompositionStart)
        nativeSearchInput?.removeEventListener('compositionend', onCompositionEnd)
        nativeSearchInput?.removeEventListener('keyup', onNativeKeyup)
        nativeSearchInput = null
      }
    }

    const isAllClassify = (ownerStoreCode: string) => {
      const firstCode = triggerManager.typeList.value[0]?.ownerStoreCode ?? ''
      return !ownerStoreCode || ownerStoreCode === firstCode
    }

    // 按搜索关键词过滤当前分类下的事件列表
    const filteredEventList = computed(() => {
      let list = allEventList.value
      const keyword = searchKey.value.trim().toLowerCase()
      if (keyword) {
        list = list.filter(
          (item) =>
            item.name.toLowerCase().includes(keyword) ||
            item.summary?.toLowerCase().includes(keyword),
        )
      }
      return list
    })

    // 左侧分类：有搜索关键词时按全量匹配结果重算数量
    const displayTypeList = computed(() => {
      const types = triggerManager.typeList.value
      const keyword = searchKey.value.trim().toLowerCase()
      if (!keyword) return types

      const matched = masterEventList.value.filter(
        (item) =>
          item.name.toLowerCase().includes(keyword) ||
          item.summary?.toLowerCase().includes(keyword),
      )

      return types.map((type) => ({
        ...type,
        count: isAllClassify(type.ownerStoreCode)
          ? matched.length
          : matched.filter((item) => item.ownerStoreCode === type.ownerStoreCode).length,
      }))
    })

    const loadTypeList = async () => {
      try {
        const response = await triggerManager.fetchTypeList()
        selectedClassify.value = response[0]?.ownerStoreCode || ''
      } catch (error) {
        console.error('Failed to load trigger types:', error)
      }
    }

    // 加载当前分类的事件列表
    const loadEventList = async (ownerStoreCode?: string) => {
      try {
        loading.value = true
        const response = await triggerManager.fetchList({
          ownerStoreCode,
          page: 1,
          pageSize: 100,
        })
        allEventList.value = response.records || []
      } catch (error) {
        console.error('Failed to load trigger events:', error)
        allEventList.value = []
      } finally {
        loading.value = false
      }
    }

    // 按分类拉取全量事件并标记归属，供搜索时统计左侧数量
    // 全量接口不带 ownerStoreCode，且与右侧列表数据源不一致，必须按分类拉取
    const loadMasterEventList = async () => {
      try {
        const types = triggerManager.typeList.value.filter(
          (type) => !isAllClassify(type.ownerStoreCode),
        )
        const lists = await Promise.all(
          types.map(async (type) => {
            const response = await triggerManager.fetchList({
              ownerStoreCode: type.ownerStoreCode,
              page: 1,
              pageSize: 100,
            })
            return (response.records || []).map((item) => ({
              ...item,
              ownerStoreCode: item.ownerStoreCode || type.ownerStoreCode,
            }))
          }),
        )
        masterEventList.value = lists.flat()
      } catch (error) {
        console.error('Failed to load master trigger events:', error)
        masterEventList.value = []
      }
    }

    // 初始化：加载分类列表和事件列表
    onMounted(async () => {
      await nextTick()
      bindNativeSearchSync()
      await loadTypeList()
      await Promise.all([
        loadEventList(selectedClassify.value || undefined),
        loadMasterEventList(),
      ])
    })

    onUnmounted(() => {
      removeNativeSearchListeners?.()
      searchKey.value = ''
      selectedClassify.value = ''
    })

    // 处理分类切换
    const handleClassifyChange = async (classifyCode: string) => {
      selectedClassify.value = classifyCode
      await loadEventList(classifyCode || undefined)
    }

    // 处理选择事件
    const handleSelectEvent = async (trigger: TriggerBaseItem) => {
      if (isEventDisabled(trigger.atomCode)) return

      try {
        selectingAtomCode.value = trigger.atomCode

        // 发送选择事件，包含触发器基础信息和配置详情
        emit('select', trigger)

        emit('update:visible', false)
      } catch (error) {
        console.error('Failed to get trigger modal:', error)
        Message({
          theme: 'error',
          message: t('flow.content.getTriggerConfigFailed'),
        })
      } finally {
        selectingAtomCode.value = null
      }
    }

    // 跳转到发布指南
    const handleGoToPublishGuide = () => {
      window.open('https://iwiki.example.com/publish-guide', '_blank')
    }

    // 检查是否正在选中某个触发器
    const isSelectingTrigger = (atomCode: string) => {
      return selectingAtomCode.value === atomCode
    }

    // 检查手动触发器是否已存在（同一创作流仅需一个手动触发器）
    const hasExistingManualTrigger = computed(() => {
      return props.existingTriggerAtomCodes.some(isManualTriggerAtomCode)
    })

    // 判断某个事件是否应被禁用
    const isEventDisabled = (atomCode: string) => {
      if (isManualTriggerAtomCode(atomCode) && hasExistingManualTrigger.value) {
        return true
      }
      return false
    }

    return () => (
      <div class={styles.triggerEventSelector}>
        <div ref={searchInputWrapRef} class={styles.searchBox}>
          <Input
            behavior="simplicity"
            modelValue={searchKey.value}
            placeholder={t('flow.content.enterKeywords')}
            clearable
            onUpdate:modelValue={updateSearchKey}
            onChange={updateSearchKey}
            onClear={() => updateSearchKey('')}
          >
            {{
              suffix: () => <SvgIcon name="search" size={16} class={styles.searchIcon} />,
            }}
          </Input>
        </div>

        <div class={styles.body}>
          {/* 左侧分类导航 */}
          <div class={styles.nav}>
            {displayTypeList.value.map((type) => (
              <div
                key={type.ownerStoreCode}
                class={[
                  styles.navItem,
                  selectedClassify.value === type.ownerStoreCode && styles.navItemActive,
                ]}
                onClick={() => handleClassifyChange(type.ownerStoreCode)}
              >
                <span class={styles.navName}>{type.name}</span>
                <span class={styles.navCount}>{type.count}</span>
              </div>
            ))}
          </div>

          {/* 右侧事件列表 */}
          <div class={styles.listContainer}>
            <Loading
              loading={loading.value || triggerManager.isLoadingTypes.value}
              class={styles.list}
            >
              {filteredEventList.value.length ? (
                filteredEventList.value.map((eventAtom) => {
                  const disabled = isEventDisabled(eventAtom.atomCode)
                  const tooltipContent = disabled
                    ? t('flow.content.manualTriggerAlreadyExist')
                    : ''
                  return (
                    <div
                      key={eventAtom.atomCode}
                      v-bk-tooltips={{ content: tooltipContent, disabled: !tooltipContent }}
                    >
                      <TriggerEventCard
                        eventAtom={eventAtom}
                        keyword={searchKey.value}
                        loading={isSelectingTrigger(eventAtom.atomCode)}
                        disabled={disabled}
                        onClick={() => handleSelectEvent(eventAtom)}
                      />
                    </div>
                  )
                })
              ) : (
                <div class={styles.emptyState}>{t('flow.content.noEventsFound')}</div>
              )}
            </Loading>
            {/* <div class={styles.footer}>
              <a class={styles.publishGuideLink} onClick={handleGoToPublishGuide}>
                {t('flow.content.noEventsMeetRequirements')}
              </a>
            </div> */}
          </div>
        </div>
      </div>
    )
  },
})
