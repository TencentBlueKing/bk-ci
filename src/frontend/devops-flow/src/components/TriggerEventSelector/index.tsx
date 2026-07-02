import type { TriggerBaseItem } from '@/api/trigger'
import { SvgIcon } from '@/components/SvgIcon'
import { useTriggerManager } from '@/hooks/useTriggerManager'
import { TRIGGER_TYPE } from '@/utils/flowConst'
import { Input, Loading, Message } from 'bkui-vue'
import { computed, defineComponent, onMounted, onUnmounted, ref } from 'vue'
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
    const selectedClassify = ref<string>('')
    const allEventList = ref<TriggerBaseItem[]>([])
    const loading = ref(false)
    const selectingAtomCode = ref<string | null>(null)

    // 根据分类和搜索关键词过滤事件列表
    const filteredEventList = computed(() => {
      let list = allEventList.value

      // 按搜索关键词过滤
      if (searchKey.value) {
        const keyword = searchKey.value.toLowerCase()
        list = list.filter(
          (item) =>
            item.name.toLowerCase().includes(keyword) ||
            item.summary?.toLowerCase().includes(keyword),
        )
      }

      return list
    })

    const loadTypeList = async () => {
      try {
        const response = await triggerManager.fetchTypeList()
        selectedClassify.value = response[0]?.ownerStoreCode || ''
      } catch (error) {
        console.error('Failed to load trigger types:', error)
      }
    }

    // 加载事件列表
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

    // 初始化：加载分类列表和事件列表
    onMounted(() => {
      loadTypeList()
      loadEventList()
    })

    onUnmounted(() => {
      searchKey.value = ''
      selectedClassify.value = ''
    })

    // 处理分类切换
    const handleClassifyChange = async (classifyCode: string) => {
      selectedClassify.value = classifyCode
      // 重新加载对应分类的列表
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
      return props.existingTriggerAtomCodes.includes(TRIGGER_TYPE.MANUAL)
    })

    // 判断某个事件是否应被禁用
    const isEventDisabled = (atomCode: string) => {
      if (atomCode === TRIGGER_TYPE.MANUAL && hasExistingManualTrigger.value) {
        return true
      }
      return false
    }

    return () => (
      <div class={styles.triggerEventSelector}>
        <Input
          behavior="simplicity"
          v-model={searchKey.value}
          placeholder={t('flow.content.enterKeywords')}
          clearable
        >
          {{
            suffix: () => <SvgIcon name="search" size={16} class={styles.searchIcon} />,
          }}
        </Input>

        <div class={styles.body}>
          {/* 左侧分类导航 */}
          <div class={styles.nav}>
            {triggerManager.typeList.value.map((type) => (
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
