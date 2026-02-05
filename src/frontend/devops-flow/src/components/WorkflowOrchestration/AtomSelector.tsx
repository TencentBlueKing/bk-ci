import { JobCategory, JobType, type AtomClassify, type AtomItem } from '@/api/atom'
import type { Container, Element } from '@/api/flowModel'
import { SvgIcon } from '@/components/SvgIcon'
import { useAtomManager } from '@/hooks/useAtomManager'
import { useAtomVersion } from '@/hooks/useAtomVersion'
import { Exception, Input, Loading, Message, Tab } from 'bkui-vue'
import {
  Transition,
  computed,
  defineComponent,
  ref,
  watch,
  type PropType
} from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import AtomCard from './AtomCard'
import styles from './AtomSelector.module.css'

const { TabPanel } = Tab

export default defineComponent({
  name: 'AtomSelector',
  props: {
    visible: {
      type: Boolean,
      default: false,
    },
    container: {
      type: Object as PropType<Container>,
      default: () => ({}),
    },
    stageIndex: {
      type: Number,
      default: 0,
    },
    containerIndex: {
      type: Number,
      default: 0,
    },
    atom: {
      type: Object as PropType<Element>,
    },
  },
  emits: ['update:visible', 'select', 'close'],
  setup(props, { emit }) {
    // ========== Hooks ==========
    const { t } = useI18n()
    const route = useRoute()
    const projectCode = computed(() => route.params.projectId as string)    
    const atomManager = useAtomManager({
      category: JobCategory.TASK,
    })
    const atomVersion = useAtomVersion({
      projectCode: projectCode.value
    })

    // ========== Refs ==========
    const searchKey = ref('')
    const classifyCode = ref('all')
    const activeAtomCode = ref('')
    const currentPage = ref(1)
    const tabSectionRef = ref<HTMLElement | null>(null)
    const searchResultRef = ref<HTMLElement | null>(null)
    const isThrottled = ref(false)
    const isSelectingAtom = ref(false)
    const atomList = ref<AtomItem[]>([])
    const hasMore = ref(true)

    // ========== Computed ==========
    
    const currentAtomCode = computed(() => {
      if (props.atom) {
        // 如果是第三方插件，使用 atomCode，否则使用 @type
        const isThird = props.atom.atomCode && props.atom['@type'] !== props.atom.atomCode
        return (isThird ? props.atom.atomCode : props.atom['@type']) || ''
      }
      return ''
    })

    const curTabList = computed(() => {
      let list = atomList.value

      // 按搜索关键词过滤
      if (searchKey.value) {
        const keyword = searchKey.value.toLowerCase()
        list = list.filter(
          (atom) =>
            atom.name.toLowerCase().includes(keyword) ||
            atom.summary?.toLowerCase().includes(keyword),
        )
      }

      return list
    })

    const installArr = computed(() =>
      curTabList.value.filter((atom) => atom.installed || atom.defaultFlag),
    )

    const uninstallArr = computed(() =>
      curTabList.value.filter((atom) => !atom.installed && !atom.defaultFlag),
    )

    const classifyList = computed(() => {
      return atomManager.classifyOptions.value.map((item) => item.classifyCode)
    })

    const classifyMap = computed(() => {
      return atomManager.classifyOptions.value.reduce(
        (map, item) => {
          map[item.classifyCode] = item
          return map
        },
        {} as Record<string, AtomClassify>,
      )
    })

    const classifyId = computed(() => {
      if (classifyCode.value === 'all') return ''
      return classifyMap.value[classifyCode.value]?.id || ''
    })

    const isLoadingAtoms = computed(() =>
      atomManager.isLoadingAtoms({
        classifyId: classifyId.value,
        keyword: searchKey.value,
      }),
    )

    // ========== Lifecycle Hooks ==========
    watch(
      () => props.visible,
      async (visible) => {
        if (visible) {
          // 获取分类列表
            await Promise.all([
                atomManager.fetchClassifyList(),
                loadAtomList(true)
            ])

            const currentAtom = atomList.value.find((atom) => atom.atomCode === currentAtomCode.value)
            
            if (currentAtom) {
                classifyCode.value = currentAtom.classifyCode || classifyList.value[0] || 'all'
            } else {
                classifyCode.value = classifyList.value[0] || 'all'
            }

            activeAtomCode.value = currentAtomCode.value
            searchKey.value = ''
        }
      },
    )

    watch(classifyCode, () => {
      loadAtomList(true)
    })

    watch(searchKey, () => {
      if (searchKey.value) {
        loadAtomList(true)
      }
    })

    // ========== Functions ==========
    function handleClose() {
      emit('update:visible', false)
      searchKey.value = ''
      activeAtomCode.value = ''
    }

    function handleSearch(value: string) {
      searchKey.value = value.trim()
      loadAtomList(true)
    }

    function handleClear(str: string) {
      if (str === '') {
        searchKey.value = ''
        loadAtomList(true)
      }
    }

    async function handleSelectAtom(atom: AtomItem) {
      try {
        isSelectingAtom.value = true
        const { atomCode, defaultVersion: atomDefaultVersion } = atom

        // 1. 优先使用插件自带的 defaultVersion，如果没有则从版本列表获取
        let version = atomDefaultVersion
        if (!version) {
          const versionList = await atomVersion.loadVersionList(atomCode)
          version = atomVersion.getDefaultVersion(versionList)
        }

        // 2. 获取插件配置
        const atomModal = await atomVersion.loadAtomModal(atomCode, version)

        // 3. 发送选择事件，包含版本和配置信息
        emit('select', {
          atomCode,
          version,
          atomModal,
        })

        handleClose()
      } catch (error) {
        console.error('Failed to select atom:', error)
        Message({
          theme: 'error',
          message: t('flow.orchestration.selectAtomFailed'),
        })
      } finally {
        isSelectingAtom.value = false
      }
    }

    function handleSetActiveAtom(code: string) {
      activeAtomCode.value = code
    }

    function handleInstallSuccess(atom: AtomItem) {
      // 安装成功后，更新插件状态
      const index = atomList.value.findIndex((item) => item.atomCode === atom.atomCode)
      if (index !== -1 && atomList.value[index]) {
        atomList.value[index].installed = true
      }
    }

    function handleScrollLoadMore(event: Event) {
      const target = event.target as HTMLElement
      if (!target || isThrottled.value || !hasMore.value) return

      const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
      if (bottomDis <= 600) {
        isThrottled.value = true
        setTimeout(() => {
          isThrottled.value = false
          loadAtomList(false)
        }, 100)
      }
    }

    async function loadAtomList(reset = false) {
      if (reset) {
        currentPage.value = 1
        atomList.value = []
        hasMore.value = true
      }

      if (!hasMore.value) return

      try {
        const result = await atomManager.fetchAtomList({
          classifyId: classifyId.value,
          keyword: searchKey.value,
          jobType: props.container?.['@type'] === 'normal' ? JobType.AGENT_LESS : JobType.AGENT,
          page: currentPage.value,
          pageSize: 20,
        })
        if (reset) {
          atomList.value = result.records
        } else {
          atomList.value = [...atomList.value, ...result.records]
        }

        hasMore.value = result.hasMore
        currentPage.value = result.page + 1
      } catch (error) {
        console.error('Failed to load atom list:', error)
      }
    }

    async function refreshAtomList() {
      if (isLoadingAtoms.value) return

      await atomManager.refreshData({
        classifyId: classifyId.value,
        keyword: searchKey.value,
      })

      loadAtomList(true)
    }

    function handleClearWrapper() {
      handleClear('')
    }

    return () => (
      <Transition name="selector-slide">
        {props.visible && (
          <div v-clickoutside={handleClose} class={styles.atomSelectorPopup}>
            <header class={styles.atomSelectorHeader}>
              <h3>
                {t('flow.orchestration.choosePlugin')}
                <span
                  class={[styles.atomFresh, isLoadingAtoms.value && styles.spinIcon]}
                  onClick={refreshAtomList}
                >
                  <SvgIcon name="refresh-line" />
                </span>
              </h3>
              <Input
                v-model={searchKey.value}
                placeholder={t('flow.orchestration.searchPluginPlaceholder')}
                clearable
                onInput={handleClear}
                onEnter={handleSearch}
                onClear={handleClearWrapper}
                class={styles.atomSearchInput}
              />
            </header>

            {!searchKey.value ? (
              <Tab v-model:active={classifyCode.value} type="unborder-card" class={styles.atomTab}>
                {classifyList.value.map((classify) => (
                  <TabPanel
                    key={classify}
                    name={classify}
                    label={classifyMap.value[classify]?.classifyName}
                  >
                    <Loading loading={isLoadingAtoms.value}>
                      {curTabList.value.length > 0 ? (
                        <div
                          ref={tabSectionRef}
                          class={styles.tabSection}
                          onScroll={handleScrollLoadMore}
                        >
                          {curTabList.value.map((atom) => (
                            <AtomCard
                              key={atom.atomCode}
                              atom={atom}
                              activeAtomCode={activeAtomCode.value}
                              currentAtomCode={currentAtomCode.value}
                              projectCode={projectCode.value}
                              onSelect={handleSelectAtom}
                              onInstall-success={handleInstallSuccess}
                              onClick={handleSetActiveAtom}
                            />
                          ))}
                        </div>
                      ) : (
                        <div class={styles.emptyAtomList}>
                          <Exception type="search-empty" />
                        </div>
                      )}
                    </Loading>
                  </TabPanel>
                ))}
              </Tab>
            ) : (
              <Loading loading={isLoadingAtoms.value}>
                <section
                  ref={searchResultRef}
                  class={styles.searchResult}
                  onScroll={handleScrollLoadMore}
                >
                  {installArr.value.length > 0 && (
                    <>
                      <h3 class={styles.searchTitle}>
                        {t('flow.orchestration.installed')} ({installArr.value.length})
                      </h3>
                      {installArr.value.map((atom) => (
                        <AtomCard
                          key={atom.atomCode}
                          atom={atom}
                          activeAtomCode={activeAtomCode.value}
                          currentAtomCode={currentAtomCode.value}
                          projectCode={projectCode.value}
                          onSelect={handleSelectAtom}
                          onInstall-success={handleInstallSuccess}
                          onClick={handleSetActiveAtom}
                        />
                      ))}
                    </>
                  )}

                  {uninstallArr.value.length > 0 && (
                    <>
                      <h3
                        class={[
                          styles.searchTitle,
                          installArr.value.length > 0 && styles.gapBorder,
                        ]}
                      >
                        {t('flow.orchestration.notInstalled')} ({uninstallArr.value.length})
                      </h3>
                      {uninstallArr.value.map((atom) => (
                        <AtomCard
                          key={atom.atomCode}
                          atom={atom}
                          activeAtomCode={activeAtomCode.value}
                          currentAtomCode={currentAtomCode.value}
                          projectCode={projectCode.value}
                          onSelect={handleSelectAtom}
                          onInstall-success={handleInstallSuccess}
                          onClick={handleSetActiveAtom}
                        />
                      ))}
                    </>
                  )}

                  {curTabList.value.length === 0 && (
                    <div class={styles.emptyAtomList}>
                      <Exception type="search-empty" />
                    </div>
                  )}
                </section>
              </Loading>
            )}
          </div>
        )}
      </Transition>
    )
  },
})
