import { JobCategory, JobType, type AtomClassify, type AtomItem } from '@/api/atom'
import type { Container, Element } from '@/api/flowModel'
import { SvgIcon } from '@/components/SvgIcon'
import { useAtomManager, RD_STORE_CODE } from '@/hooks/useAtomManager'
import { useAtomVersion } from '@/hooks/useAtomVersion'
import { useUIStore } from '@/stores/ui'
import { Exception, Input, Loading, Message, Tab } from 'bkui-vue'
import { storeToRefs } from 'pinia'
import { Transition, computed, defineComponent, ref, watch, type PropType } from 'vue'
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
      projectCode: projectCode.value,
    })
    const uiStore = useUIStore()
    const { authoringBaseOS } = storeToRefs(uiStore)

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
    
    // 搜索模式状态
    const searchInstalledList = ref<AtomItem[]>([])
    const searchUninstalledList = ref<AtomItem[]>([])
    const searchInstalledHasMore = ref(true)
    const searchUninstalledHasMore = ref(true)
    const searchInstalledPage = ref(1)
    const searchUninstalledPage = ref(1)
    const isSearchMode = ref(false)

    // ========== Computed ==========

    const isCloudJob = computed(() => props.container?.['@type'] === 'normal')

    const jobType = computed(() => (isCloudJob.value ? JobType.CLOUD_TASK : JobType.CREATIVE_STREAM))

    const atomListOs = computed(() => (isCloudJob.value ? undefined : authoringBaseOS.value))

    const currentAtomCode = computed(() => {
      if (props.atom) {
        // 如果是第三方插件，使用 atomCode，否则使用 @type
        const isThird = props.atom.atomCode && props.atom['@type'] !== props.atom.atomCode
        return (isThird ? props.atom.atomCode : props.atom['@type']) || ''
      }
      return ''
    })

    const curTabList = computed(() => {
      // 搜索模式下使用搜索列表
      if (isSearchMode.value) {
        return [...searchInstalledList.value, ...searchUninstalledList.value]
      }

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

    const installArr = computed(() => {
      if (isSearchMode.value) {
        return searchInstalledList.value
      }
      return curTabList.value.filter((atom) => atom.installed || atom.defaultFlag)
    })

    const uninstallArr = computed(() => {
      if (isSearchMode.value) {
        return searchUninstalledList.value
      }
      return curTabList.value.filter((atom) => !atom.installed && !atom.defaultFlag)
    })

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
      if (classifyCode.value === 'all' || classifyCode.value === RD_STORE_CODE) return ''
      return classifyMap.value[classifyCode.value]?.id || ''
    })

    const queryProjectAtomFlag = computed(() => classifyCode.value !== RD_STORE_CODE)

    const isLoadingAtoms = computed(() =>
      atomManager.isLoadingAtoms({
        classifyId: classifyId.value,
        keyword: searchKey.value,
        jobType: jobType.value,
        os: atomListOs.value,
        queryProjectAtomFlag: queryProjectAtomFlag.value,
      }),
    )

    // ========== Lifecycle Hooks ==========
    watch(
      () => props.visible,
      async (visible) => {
        if (visible) {
          // 获取分类列表
          await Promise.all([atomManager.fetchClassifyList(), loadAtomList(true)])

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

    watch(atomListOs, () => {
      if (props.visible) {
        loadAtomList(true)
      }
    })

    // 搜索防抖
    const loadSearchAtomListWithDebounce = (() => {
      let timer: ReturnType<typeof setTimeout> | null = null
      return (reset: boolean) => {
        if (timer) clearTimeout(timer)
        timer = setTimeout(() => {
          loadSearchAtomList(reset)
        }, 300)
      }
    })()

    watch(searchKey, () => {
      if (searchKey.value) {
        isSearchMode.value = true
        // 重置搜索状态（包括页码）
        searchInstalledList.value = []
        searchUninstalledList.value = []
        searchInstalledPage.value = 1
        searchUninstalledPage.value = 1
        searchInstalledHasMore.value = true
        searchUninstalledHasMore.value = true
        loadSearchAtomListWithDebounce(true)
      }
    })

    // ========== Functions ==========
    function handleClose() {
      emit('update:visible', false)
      searchKey.value = ''
      activeAtomCode.value = ''
      isSearchMode.value = false
      // 重置搜索状态
      searchInstalledList.value = []
      searchUninstalledList.value = []
      searchInstalledPage.value = 1
      searchUninstalledPage.value = 1
      searchInstalledHasMore.value = true
      searchUninstalledHasMore.value = true
    }

    function handleSearch(value: string) {
      const trimmedValue = value.trim()
      searchKey.value = trimmedValue
      
      // 如果没有搜索关键词，退出搜索模式
      if (!trimmedValue) {
        isSearchMode.value = false
        loadAtomList(true)
        return
      }
      
      // 按回车时立即执行搜索（不需要防抖，因为是用户主动触发）
      isSearchMode.value = true
      // 重置搜索状态
      searchInstalledList.value = []
      searchUninstalledList.value = []
      searchInstalledPage.value = 1
      searchUninstalledPage.value = 1
      searchInstalledHasMore.value = true
      searchUninstalledHasMore.value = true
      // 开始加载搜索结果
      loadSearchAtomList(true)
    }

    function handleClear(str: string) {
      if (str === '') {
        searchKey.value = ''
        isSearchMode.value = false
        // 重置搜索状态
        searchInstalledList.value = []
        searchUninstalledList.value = []
        searchInstalledPage.value = 1
        searchUninstalledPage.value = 1
        searchInstalledHasMore.value = true
        searchUninstalledHasMore.value = true
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
      if (!target || isThrottled.value) return

      const bottomDis = target.scrollHeight - target.clientHeight - target.scrollTop
      if (bottomDis <= 600) {
        isThrottled.value = true
        setTimeout(() => {
          isThrottled.value = false
          // 根据当前模式调用不同的加载函数
          if (isSearchMode.value) {
            loadSearchAtomList(false)
          } else {
            loadAtomList(false)
          }
        }, 100)
      }
    }

    async function loadAtomList(reset = false, forceRefresh = false) {
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
          jobType: jobType.value,
          os: atomListOs.value,
          queryProjectAtomFlag: queryProjectAtomFlag.value,
          page: currentPage.value,
          pageSize: 20,
          forceRefresh,
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

    // 搜索模式：加载搜索结果（已安装/未安装）
    async function loadSearchAtomList(reset = false) {
      // 先加载已安装的插件
      if (reset || searchInstalledHasMore.value) {
        try {
          const result = await atomManager.fetchSearchAtomList({
            searchKey: searchKey.value,
            installed: true,
            os: atomListOs.value,
            page: searchInstalledPage.value,
            pageSize: 100,
          })
          if (result.records && result.records.length > 0) {
            if (reset) {
              searchInstalledList.value = result.records
            } else {
              searchInstalledList.value = [...searchInstalledList.value, ...result.records]
            }
            searchInstalledHasMore.value = result.hasMore
            searchInstalledPage.value = result.page + 1
          }
        } catch (error) {
          console.error('Failed to load installed search atoms:', error)
        }
      }

      // 然后加载未安装的插件
      if (reset || searchUninstalledHasMore.value) {
        try {
          const result = await atomManager.fetchSearchAtomList({
            searchKey: searchKey.value,
            installed: false,
            os: atomListOs.value,
            page: searchUninstalledPage.value,
            pageSize: 100,
          })
          if (result.records && result.records.length > 0) {
            if (reset) {
              searchUninstalledList.value = result.records
            } else {
              searchUninstalledList.value = [...searchUninstalledList.value, ...result.records]
            }
            searchUninstalledHasMore.value = result.hasMore
            searchUninstalledPage.value = result.page + 1
          }
        } catch (error) {
          console.error('Failed to load uninstalled search atoms:', error)
        }
      }
    }

    async function refreshAtomList() {
      if (isLoadingAtoms.value) return
      if (isSearchMode.value) {
        await loadSearchAtomList(true)
      } else {
        await loadAtomList(true, true)
      }
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
              <div class={styles.searchResultWrapper}>
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
              </div>
            )}
          </div>
        )}
      </Transition>
    )
  },
})
