import { ROUTE_NAMES } from '@/constants/routes'
import { useFlowInfoStore } from '@/stores/flowInfoStore'
import type { FlowVersion } from '@/types/flow'
import { VERSION_STATUS_ENUM } from '@/utils/flowConst'
import { convertTime } from '@/utils/util'
import SearchSelect from '@blueking/search-select-v3'
import { Button, InfoBox, Loading, Message, Popover, Sideslider, Table } from 'bkui-vue'
import { computed, defineComponent, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { SvgIcon } from '../SvgIcon'
import styles from './VersionHistorySideSlider.module.css'

interface VersionRow extends FlowVersion {
  isDraft: boolean
  canRollback: boolean
  isBranchVersion: boolean
  displayName: string
}

export const VersionHistorySideSlider = defineComponent({
  name: 'VersionHistorySideSlider',
  components: { SearchSelect },
  props: {
    isShow: {
      type: Boolean,
      default: false,
    },
    currentVersion: {
      type: Number,
      default: 0,
    },
  },
  emits: ['update:isShow', 'select'],
  setup(props, { emit }) {
    const { t } = useI18n()
    const flowInfoStore = useFlowInfoStore()
    const route = useRoute()
    const router = useRouter()

    const isLoading = ref(false)
    const operatingVersion = ref<number | null>(null)
    const operatingType = ref<'delete' | 'rollback' | null>(null)
    const versionList = ref<VersionRow[]>([])
    const filterKeys = ref<Array<{ id: string; name: string; values: Array<{ id: string; name: string }> }>>([])
    const pagination = ref({
      current: 1,
      limit: 20,
      count: 0,
    })
    const tableBoxRef = ref<HTMLElement | null>(null)

    const releaseVersion = computed(() => flowInfoStore.flowInfo?.releaseVersion)
    const canEdit = computed(() => flowInfoStore.flowInfo?.permissions?.canEdit)

    const filterData = computed(() => [
      {
        id: 'versionName',
        name: t('flow.versionHistory.versionName'),
      },
      {
        id: 'description',
        name: t('flow.versionHistory.description'),
      },
      {
        id: 'updater',
        name: t('flow.versionHistory.updater'),
      },
    ])

    const filterTips = computed(() =>
      filterData.value.map((item) => item.name).join(' / '),
    )

    const filterQuery = computed(() =>
      filterKeys.value.reduce(
        (query, item) => {
          query[item.id] = item.values.map((v) => v.id).join(',')
          return query
        },
        {} as Record<string, string>,
      ),
    )

    const emptyType = computed(() =>
      filterKeys.value.length > 0 ? 'search-empty' : 'empty',
    )

    const columns = computed(() => [
      {
        label: t('flow.versionHistory.versionName'),
        field: 'displayName',
        width: 120,
        showOverflowTooltip: true,
        render: ({ data }: any) => {
          const row = data as VersionRow
          return (
            <div
              class={[
                styles.versionNameCell,
                row.version === releaseVersion.value && styles.activeVersionName,
                row.version !== props.currentVersion && styles.clickableVersionName,
              ]}
              onClick={() => handleSelectVersion(row)}
            >
              <span class={styles.versionIcon}>
                {row.isDraft ? (
                  <SvgIcon name="edit" size={14} />
                ) : row.isBranchVersion ? (
                  <SvgIcon name="branch" size={14} />
                ) : (
                  <SvgIcon name="check-circle" size={14} />
                )}
              </span>
              {row.displayName}
            </div>
          )
        },
      },
      {
        label: t('flow.versionHistory.description'),
        field: 'description',
        width: 120,
        showOverflowTooltip: true,
        render: ({ data }: any) => (
          <span>{(data as VersionRow)?.description || '--'}</span>
        ),
      },
      {
        label: t('flow.versionHistory.createTime'),
        field: 'createTime',
        width: 156,
        showOverflowTooltip: true,
        render: ({ data }: any) => {
          const row = data as VersionRow
          return <span>{row?.createTime ? convertTime(row.createTime) : '--'}</span>
        },
      },
      {
        label: t('flow.versionHistory.creator'),
        field: 'creator',
        width: 120,
        showOverflowTooltip: true,
      },
      {
        label: t('flow.versionHistory.updateTime'),
        field: 'updateTime',
        width: 156,
        showOverflowTooltip: true,
        render: ({ data }: any) => {
          const row = data as VersionRow
          return <span>{row?.updateTime ? convertTime(row.updateTime) : '--'}</span>
        },
      },
      {
        label: t('flow.versionHistory.updater'),
        field: 'updater',
        width: 120,
        showOverflowTooltip: true,
        render: ({ data }: any) => (
          <span>{(data as VersionRow)?.updater || '--'}</span>
        ),
      },
      {
        label: t('flow.versionHistory.operate'),
        field: 'operate',
        width: 200,
        fixed: 'right',
        render: ({ data }: any) => {
          const row = data as VersionRow
          const isOperating = operatingVersion.value === row.version
          const anyOperating = operatingVersion.value !== null
          return (
            <div class={styles.operateCell}>
              {row.canRollback && canEdit.value && (
                <Button
                  text
                  theme="primary"
                  loading={isOperating && operatingType.value === 'rollback'}
                  disabled={anyOperating && !isOperating}
                  onClick={() => handleRollback(row)}
                >
                  {t('flow.versionHistory.rollback')}
                </Button>
              )}
              {!row.canRollback && !row.isDraft && canEdit.value && (
                <Button
                  text
                  theme="primary"
                  disabled={anyOperating}
                  onClick={() => handleEdit(row)}
                >
                  {t('flow.versionHistory.edit')}
                </Button>
              )}
              <Button
                text
                theme="primary"
                loading={isOperating && operatingType.value === 'delete'}
                disabled={releaseVersion.value === row.version || (anyOperating && !isOperating)}
                onClick={() => handleDeleteVersion(row)}
              >
                {t('flow.versionHistory.delete')}
              </Button>
            </div>
          )
        },
      },
    ])

    function mapVersionRow(item: FlowVersion): VersionRow {
      const isDraft =
        (item.status || item.versionStatus) === VERSION_STATUS_ENUM.COMMITTING
      const isBranchVersion =
        (item.status || item.versionStatus) === VERSION_STATUS_ENUM.BRANCH
      return {
        ...item,
        isDraft,
        canRollback: !isDraft && item.version !== releaseVersion.value,
        isBranchVersion,
        displayName:
          item.versionName ||
          (isDraft
            ? t('flow.versionHistory.draftVersion', [item.baseVersionName || ''])
            : `V${item.version}`),
      }
    }

    async function fetchVersionList(page = 1) {
      try {
        isLoading.value = true
        const res = await flowInfoStore.fetchPaginatedVersionList({
          page,
          pageSize: pagination.value.limit,
          ...filterQuery.value,
        })
        versionList.value = (res.records || []).map(mapVersionRow)
        pagination.value.current = res.page ?? page
        pagination.value.count = res.total ?? 0
      } catch (error) {
        console.error('Failed to fetch version list:', error)
      } finally {
        isLoading.value = false
      }
    }

    function handlePageChange(page: number) {
      pagination.value.current = page
      fetchVersionList(page)
    }

    function handleLimitChange(limit: number) {
      pagination.value.limit = limit
      pagination.value.current = 1
      fetchVersionList(1)
    }

    function handleSearchChange() {
      pagination.value.current = 1
      fetchVersionList(1)
    }

    function clearFilter() {
      filterKeys.value = []
      handleSearchChange()
    }

    async function handleDeleteVersion(row: VersionRow) {
      if (releaseVersion.value === row.version) return

      const confirmed = await new Promise<boolean>((resolve) => {
        InfoBox({
          title: t('flow.versionHistory.deleteVersionConfirm', [row.displayName]),
          confirmText: t('flow.versionHistory.delete'),
          cancelText: t('flow.common.cancel'),
          theme: 'danger',
          onConfirm: () => {
            resolve(true)
          },
          onClose: () => {
            resolve(false)
            return true
          },
        })
      })

      if (!confirmed) return

      try {
        operatingVersion.value = row.version
        operatingType.value = 'delete'
        await flowInfoStore.deleteFlowVersion(row.version)
        Message({ theme: 'success', message: t('flow.versionHistory.deleteSuccess') })
        fetchVersionList(1)
        flowInfoStore.getFlowInfo()
      } catch (error: any) {
        Message({ theme: 'error', message: error?.message || t('flow.common.failed') })
      } finally {
        operatingVersion.value = null
        operatingType.value = null
      }
    }

    async function handleRollback(row: VersionRow) {
      try {
        operatingVersion.value = row.version
        operatingType.value = 'rollback'
        const res = await flowInfoStore.rollbackFlowVersion(row.version)
        Message({ theme: 'success', message: t('flow.versionHistory.rollbackSuccess') })
        await flowInfoStore.getFlowInfo()
        if (res?.version) {
          router.push({
            name: ROUTE_NAMES.FLOW_EDIT_WORKFLOW_ORCHESTRATION,
            params: {
              projectId: route.params.projectId,
              flowId: route.params.flowId,
              version: String(res.version),
            },
          })
          handleClose()
        }
      } catch (error: any) {
        Message({ theme: 'error', message: error?.message || t('flow.common.failed') })
      } finally {
        operatingVersion.value = null
        operatingType.value = null
      }
    }

    function handleEdit(row: VersionRow) {
      router.push({
        name: ROUTE_NAMES.FLOW_EDIT_WORKFLOW_ORCHESTRATION,
        params: {
          projectId: route.params.projectId,
          flowId: route.params.flowId,
          version: String(row.version),
        },
      })
      handleClose()
    }

    function handleSelectVersion(row: VersionRow) {
      if (row.version === props.currentVersion) return
      emit('select', row.version)
      handleClose()
    }

    function handleClose() {
      emit('update:isShow', false)
      filterKeys.value = []
      pagination.value.current = 1
    }

    watch(
      () => props.isShow,
      (show) => {
        if (show) {
          fetchVersionList(1)
        }
      },
    )

    return () => (
      <Sideslider
        isShow={props.isShow}
        width={1080}
        quickClose
        transfer
        zIndex={2500}
        onClosed={handleClose}
        class={styles.versionSideslider}
      >
        {{
          header: () => (
            <div class={styles.sliderHeader}>
              <span>{t('flow.versionHistory.title')}</span>
              <Popover
                theme="light"
                trigger="hover"
                placement="bottom"
              >
                {{
                  default: () => (
                    <span class={styles.versionRuleTips}>
                      <SvgIcon name="question-circle" size={14} />
                      {t('flow.versionHistory.versionRule')}
                    </span>
                  ),
                  content: () => (
                    <div class={styles.versionRuleContent}>
                      <p>{t('flow.versionHistory.versionRule')}</p>
                      <p>{t('flow.versionHistory.versionRuleP')}</p>
                      <p>{t('flow.versionHistory.versionRuleT')}</p>
                      <p>{t('flow.versionHistory.versionRuleA')}</p>
                    </div>
                  ),
                }}
              </Popover>
            </div>
          ),
          default: () => (
            <Loading loading={isLoading.value}>
              <div class={styles.versionHistory}>
                <header class={styles.versionHistoryHeader}>
                  <SearchSelect
                    modelValue={filterKeys.value}
                    data={filterData.value}
                    placeholder={filterTips.value}
                    class={styles.searchSelect}
                    onUpdate:modelValue={(val: typeof filterKeys.value) => {
                      filterKeys.value = val
                      handleSearchChange()
                    }}
                  />
                </header>
                <section class={styles.versionHistoryContent} ref={tableBoxRef}>
                  <Table
                    data={versionList.value}
                    columns={columns.value as any}
                    maxHeight={
                      tableBoxRef.value?.offsetHeight
                        ? tableBoxRef.value.offsetHeight
                        : 500
                    }
                    pagination={pagination.value}
                    onPageValueChange={handlePageChange}
                    onPageLimitChange={handleLimitChange}
                    remotePagination
                    size="small"
                  >
                    {{
                      empty: () => (
                        <div class={styles.emptyState}>
                          {emptyType.value === 'search-empty' ? (
                            <div>
                              <p>{t('flow.versionHistory.noData')}</p>
                              <Button text theme="primary" onClick={clearFilter}>
                                {t('flow.clearFilterCriteria')}
                              </Button>
                            </div>
                          ) : (
                            <p>{t('flow.versionHistory.noData')}</p>
                          )}
                        </div>
                      ),
                    }}
                  </Table>
                </section>
              </div>
            </Loading>
          ),
        }}
      </Sideslider>
    )
  },
})
