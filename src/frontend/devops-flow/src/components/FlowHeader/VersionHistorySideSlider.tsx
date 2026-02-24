import { useFlowInfoStore } from '@/stores/flowInfoStore'
import type { FlowVersion } from '@/types/flow'
import { convertTime } from '@/utils/util'
import { Input, Loading, Pagination, Sideslider, Table } from 'bkui-vue'
import type { Column } from 'bkui-vue/lib/table/props'
import { computed, defineComponent, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { SvgIcon } from '../SvgIcon'
import styles from './VersionHistorySideSlider.module.css'

export const VersionHistorySideSlider = defineComponent({
    name: 'VersionHistorySideSlider',
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

        const isLoading = ref(false)
        const versionList = ref<FlowVersion[]>([])
        const searchKeyword = ref('')
        const pagination = ref({
            current: 1,
            limit: 20,
            count: 0,
        })
        const tableBoxRef = ref<HTMLElement | null>(null)

        const columns = computed(() => [
            {
                label: t('flow.versionHistory.versionName'),
                field: 'versionName',
                width: 180,
                showOverflowTooltip: true,
                render: ({ data }: { data: FlowVersion }) => (
                    <div class={[styles.versionNameCell, data.version === props.currentVersion && styles.activeVersionName]}>
                        <span>
                            <SvgIcon name="check-circle" class={styles.checkIcon} />
                        </span>
                        {data.versionName}
                    </div>
                ),
            },
            {
                label: t('flow.versionHistory.creator'),
                field: 'creator',
                width: 120,
                showOverflowTooltip: true,
            },
            {
                label: t('flow.versionHistory.createTime'),
                field: 'createTime',
                width: 180,
                showOverflowTooltip: true,
                render: ({ data }: { data: FlowVersion }) => (
                    <span>{data.createTime ? convertTime(data.createTime) : '--'}</span>
                ),
            },
        ])

        async function fetchVersionList(page = 1) {
            try {
                isLoading.value = true
                const res = await flowInfoStore.fetchPaginatedVersionList({
                    page,
                    pageSize: pagination.value.limit,
                    versionName: searchKeyword.value || undefined,
                })
                versionList.value = res.records
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

        function handleSearch() {
            pagination.value.current = 1
            fetchVersionList(1)
        }

        function handleClearSearch() {
            searchKeyword.value = ''
            handleSearch()
        }

        function handleClose() {
            emit('update:isShow', false)
            // Reset state
            searchKeyword.value = ''
            pagination.value.current = 1
        }

        // When slider opens, fetch the first page
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
                width={960}
                title={t('flow.versionHistory.title')}
                quickClose
                onClosed={handleClose}
            >
                {{
                    default: () => (
                        <Loading loading={isLoading.value}>
                            <div class={styles.versionHistory}>
                                <header class={styles.versionHistoryHeader}>
                                    <Input
                                        v-model={searchKeyword.value}
                                        class={styles.searchInput}
                                        placeholder={t('flow.versionHistory.searchPlaceholder')}
                                        clearable
                                        type="search"
                                        onEnter={handleSearch}
                                        onClear={handleClearSearch}
                                    />
                                </header>
                                <section class={styles.versionHistoryContent} ref={tableBoxRef}>
                                    <Table
                                        data={versionList.value}
                                        columns={columns.value as Column[]}
                                        maxHeight={tableBoxRef.value?.offsetHeight ? tableBoxRef.value.offsetHeight - 50 : 500}
                                        stripe
                                    >
                                        {{
                                            empty: () => (
                                                <div style="padding: 24px; text-align: center; color: var(--color-text-placeholder);">
                                                    {t('flow.versionHistory.noData')}
                                                </div>
                                            ),
                                        }}
                                    </Table>
                                    {pagination.value.count > 0 && (
                                        <Pagination
                                            modelValue={pagination.value.current}
                                            count={pagination.value.count}
                                            limit={pagination.value.limit}
                                            onChange={handlePageChange}
                                            onLimitChange={handleLimitChange}
                                            showLimit
                                            showTotalCount
                                            style="margin-top: 16px; justify-content: flex-end;"
                                        />
                                    )}
                                </section>
                            </div>
                        </Loading>
                    ),
                }}
            </Sideslider>
        )
    },
})
