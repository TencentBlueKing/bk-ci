<template>
    <div class="operate-log-container">
        <div class="operate-log-header">
            <bk-select
                v-model="selectedOperator"
                :placeholder="$t('environment.selectOperator')"
                :loading="memberLoading"
                :filterable="true"
                :clearable="true"
                :scroll-loading="memberHasMore"
                enable-scroll-load
                ext-cls="operator-select"
                @change="handleOperatorChange"
                @clear="handleOperatorClear"
                @scroll-end="handleMemberScrollEnd"
            >
                <bk-option
                    v-for="member in projectMemberList"
                    :key="member.name"
                    :id="member.id"
                    :name="member.id"
                />
            </bk-select>
        </div>
        <div
            v-if="operateLogList.length"
            class="operate-log-table-wrapper"
        >
            <bk-table
                v-bkloading="{ isLoading: logLoading }"
                :data="operateLogList"
                :pagination="pagination"
                @page-change="handlePageChange"
                @page-limit-change="handlePageLimitChange"
            >
                <bk-table-column
                    :label="$t('environment.operateUser')"
                    prop="operator"
                    width="200"
                />
                <bk-table-column
                    :label="$t('environment.operateTime')"
                    prop="createTime"
                    width="250"
                >
                    <template slot-scope="{ row }">
                        {{ formatTime(row.createTime) }}
                    </template>
                </bk-table-column>
                <bk-table-column
                    :label="$t('environment.operateContent')"
                >
                    <template slot-scope="{ row }">
                        {{ $t(`environment.operateNameMap.${row.operateName}`, '') }}
                        <template v-if="row.operateContent && row.operateContent.content">
                            {{ nodeStatusField.includes(row.operateName) ? $t('environment.operateReasonPrefix') : $t('environment.operateContentPrefix') }}{{ row.operateContent.content }}
                        </template>
                    </template>
                </bk-table-column>
            </bk-table>
        </div>
        <div
            v-else
            class="operate-log-empty"
        >
            <bk-exception
                type="empty"
                scene="part"
            >
                <template #default>
                    <div class="empty-content">
                        <p class="empty-title">{{ $t('environment.operateLogEmptyTips') }}</p>
                    </div>
                </template>
            </bk-exception>
        </div>
    </div>
</template>

<script>
    import { onMounted, ref } from 'vue'
    import useEnvDetail from '@/hooks/useEnvDetail'
    import useInstance from '@/hooks/useInstance'
    import usePagination from '@/hooks/usePagination'

    export default {
        name: 'OperateLog',
        setup () {
            const { proxy } = useInstance()
            const {
                operateLogList,
                fetchOperateLogList,
                projectMemberList,
                memberHasMore,
                memberLoading,
                fetchProjectMembers
            } = useEnvDetail()

            const { pagination, pageChange, pageLimitChange, updatePagination } = usePagination({
                limit: 10,
                current: 1,
                limitList: [10, 20, 50, 100]
            })

            const logLoading = ref(false)
            const selectedOperator = ref('')
            const nodeStatusField  = ref(['ENABLE_NODE', 'DISABLE_NODE'])
            /**
             * 加载操作日志数据
             */
            const loadData = async () => {
                logLoading.value = true
                try {
                    const res = await fetchOperateLogList({
                        page: pagination.value.current,
                        pageSize: pagination.value.limit,
                        ...(selectedOperator.value ? { operator: selectedOperator.value } : {})
                    })
                    if (res) {
                        updatePagination({
                            count: res.count || 0,
                            page: res.page || 1,
                            pageSize: res.pageSize || pagination.value.limit
                        })
                    }
                } catch (e) {
                    console.error('Failed to load operate log', e)
                } finally {
                    logLoading.value = false
                }
            }

            /**
             * 加载项目成员列表（首次加载）
             */
            const loadMembers = async () => {
                try {
                    await fetchProjectMembers()
                } catch (e) {
                    console.error('Failed to load project members', e)
                }
            }

            /**
             * 处理成员列表滚动到底部，加载更多
             */
            const handleMemberScrollEnd = async () => {
                console.log(1)
                try {
                    await fetchProjectMembers(true)
                } catch (e) {
                    console.error('Failed to load more project members', e)
                }
            }

            /**
             * 格式化时间显示
             * @param {String} time - ISO 8601 格式的时间字符串
             */
            const formatTime = (time) => {
                if (!time) return '--'
                return time.replace('T', ' ').replace(/\.\d+Z$/, '').replace(/Z$/, '')
            }

            /**
             * 处理操作人筛选变化
             */
            const handleOperatorChange = (value) => {
                selectedOperator.value = value
                pagination.value.current = 1
                loadData()
            }

            /**
             * 处理清空操作人筛选
             */
            const handleOperatorClear = () => {
                selectedOperator.value = ''
                pagination.value.current = 1
                loadData()
            }

            /**
             * 处理页码变化
             */
            const handlePageChange = (page) => {
                pageChange(page)
                loadData()
            }

            /**
             * 处理每页条数变化
             */
            const handlePageLimitChange = (limit) => {
                pageLimitChange(limit)
                loadData()
            }

            onMounted(() => {
                loadData()
                loadMembers()
            })

            return {
                operateLogList,
                projectMemberList,
                memberHasMore,
                pagination,
                logLoading,
                memberLoading,
                selectedOperator,
                nodeStatusField,
                formatTime,
                handleOperatorChange,
                handleOperatorClear,
                handleMemberScrollEnd,
                handlePageChange,
                handlePageLimitChange
            }
        }
    }
</script>

<style lang="scss" scoped>
.operate-log-container {
    padding: 16px 0;

    .operate-log-header {
        display: flex;
        align-items: center;
        margin-bottom: 16px;

        .operator-select {
            width: 260px;
        }
    }

    .operate-log-empty {
        padding: 80px 0;
        text-align: center;

        .empty-content {
            .empty-title {
                font-size: 14px;
                color: #63656E;
                margin-bottom: 8px;
            }
            .empty-desc {
                font-size: 12px;
                color: #979BA5;
            }
        }
    }
}
</style>
