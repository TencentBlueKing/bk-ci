<template>
    <div class="agent-offline-records-container">
        <bk-table
            v-bkloading="{ isLoading: agentOfflineData.loading }"
            :data="renderList"
            :pagination="pagination"
            @page-change="handlePageChange"
            @page-limit-change="handlePageLimitChange"
        >
            <bk-table-column
                :label="$t('environment.offlineTime')"
                width="400"
            >
                <template slot-scope="{ row }">
                    {{ row.offlineTime }} ~ {{ row.onlineTime }}
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('environment.offlineDuration')"
                width="200"
            >
                <template slot-scope="{ row }">
                    {{ row.duration }}
                </template>
            </bk-table-column>
        </bk-table>
    </div>
</template>

<script>
    import { onMounted, watch, computed } from 'vue'
    import usePagination from '@/hooks/usePagination'
    import useNodeDetail from '@/hooks/useNodeDetail'

    export default {
        name: 'AgentOfflineRecords',
        setup () {
            const { agentOfflineData, fetchAgentOfflinePeriod, nodeHashId } = useNodeDetail()
            const { pagination, pageChange, pageLimitChange, updatePagination } = usePagination({
                limit: 10,
                current: 1,
                limitList: [10, 20, 50, 100]
            })

            /**
             * 加载离线记录数据
             */
            const loadData = async () => {
                const res = await fetchAgentOfflinePeriod(pagination.value.current, pagination.value.limit)
                if (res) {
                    updatePagination({
                        count: res.count || 0,
                        page: res.page || 1,
                        pageSize: res.pageSize || pagination.value.limit
                    })
                }
            }
            const renderList = computed(() => {
                return agentOfflineData.list.map(i => {
                    return {
                        ...i,
                        duration: formatDuration(i.duration),
                        offlineTime: formatTime(i.offlineTime),
                        onlineTime: formatTime(i.onlineTime)
                    }
                })
            })
            
            /**
             * 格式化时间显示，去掉 ISO 8601 格式中的 T
             * @param {String} time - 时间字符串，如 "2025-12-24T18:18:55"
             */
            const formatTime = (time) => {
                if (!time) return ''
                return time.replace('T', ' ')
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

            /**
             * 格式化时长显示
             * @param {Number} duration - 时长（秒）
             */
            const formatDuration = (duration) => {
                if (!duration) return '0 秒'
                
                const hours = Math.floor(duration / 3600)
                const minutes = Math.floor((duration % 3600) / 60)
                const seconds = duration % 60

                const parts = []
                if (hours > 0) {
                    parts.push(`${hours} 时`)
                }
                if (minutes > 0) {
                    parts.push(`${minutes} 分`)
                }
                
                return parts.join(' ')
            }

            // 监听 nodeHashId 变化，重新加载数据
            watch(nodeHashId, (newVal) => {
                if (newVal) {
                    loadData()
                }
            })

            // 组件挂载时加载数据
            onMounted(() => {
                if (nodeHashId.value) {
                    loadData()
                }
            })

            return {
                agentOfflineData,
                pagination,
                handlePageChange,
                handlePageLimitChange,
                formatDuration,
                renderList
            }
        }
    }
</script>

<style lang="scss" scoped>
.agent-offline-records-container {
    padding-bottom: 20px;
    
    ::v-deep .bk-table {
        width: 620px;
        
        .bk-table-body-wrapper {
            overflow-y: auto;
        }
    }
    
    .empty-container {
        padding: 60px 0;
        text-align: center;
        color: #979BA5;
        font-size: 14px;
    }
}
</style>

