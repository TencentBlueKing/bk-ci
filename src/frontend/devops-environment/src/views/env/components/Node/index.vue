<template>
    <div class="node-container">
        <div class="node-list-main-header">
            <div class="header-left">
                <bk-button
                    theme="primary"
                    v-perm="{
                        permissionData: {
                            projectId: projectId,
                            resourceType: ENV_RESOURCE_TYPE,
                            resourceCode: envHashId,
                            action: ENV_RESOURCE_ACTION.EDIT
                        }
                    }"
                    @click="handleShowAddNodesDialog"
                >
                    {{ $t('environment.relatedNodes') }}
                </bk-button>
                <bk-button
                    :disabled="!selectedNodesList.length"
                    v-perm="{
                        permissionData: {
                            projectId: projectId,
                            resourceType: ENV_RESOURCE_TYPE,
                            resourceCode: envHashId,
                            action: ENV_RESOURCE_ACTION.EDIT
                        }
                    }"
                    @click="handleBatchRemove"
                >
                    {{ $t('environment.bulkRemove') }}
                </bk-button>
            </div>
            <search-select
                ref="searchSelect"
                class="search-input"
                v-model="searchValue"
                :data="searchList"
                clearable
                :show-condition="false"
                :placeholder="filterTips"
            />
        </div>
        <bk-table
            class="node-list-table"
            v-bkloading="{ isLoading }"
            :size="tableSize"
            row-class-name="node-item-row"
            :data="envNodeList"
            :max-height="tableMaxHeight"
            :pagination="pagination"
            @page-change="handlePageChange"
            @page-limit-change="handlePageLimitChange"
            @selection-change="handleSelectionChange"
        >
            <bk-table-column
                type="selection"
                fixed="left"
                width="40"
            />
            <bk-table-column
                :label="$t('environment.nodeInfo.hostName')"
                fixed="left"
                prop="displayName"
                width="280"
                show-overflow-tooltip
            >
                <template slot-scope="{ row }">
                    <span
                        :class="{
                            'node-name': row.nodeType !== 'CMDB'
                        }"
                        :title="row.displayName"
                        @click="handleToNodeDetail(row)"
                    >
                        {{ row.displayName || '-' }}
                    </span>
                </template>
            </bk-table-column>
            <bk-table-column
                label="IP"
                prop="ip"
                width="180"
            />
            <bk-table-column
                :label="$t('environment.nodeInfo.os')"
                prop="osName"
                width="180"
            />
            <bk-table-column
                :label="`${$t('environment.status')}(${$t('environment.version')})`"
                width="240"
                prop="nodeStatus"
            >
                <template slot-scope="{ row }">
                    <div
                        class="table-node-item node-item-status"
                        v-if="row.nodeStatus === 'BUILDING_IMAGE'"
                    >
                        <span class="node-status-icon normal-stutus-icon"></span>
                        <span class="node-status">{{ $t('environment.nodeInfo.normal') }}</span>
                    </div>
                    <div class="table-node-item node-item-status">
                        <!-- 状态icon -->
                        <span
                            class="node-status-icon normal-stutus-icon"
                            v-if="successStatus?.includes(row.nodeStatus)"
                        />
                        <span
                            class="node-status-icon abnormal-stutus-icon"
                            v-if="failStatus?.includes(row.nodeStatus)"
                        />
                        <span
                            v-if="runningStatus?.includes(row.nodeStatus)"
                            class="loading-icon"
                        >
                            <bk-loading
                                theme="primary"
                                mode="spin"
                                size="mini"
                                is-loading
                            />
                        </span>
                        <!-- 状态值 -->
                        <span
                            class="install-agent"
                            v-if="row.nodeStatus === 'RUNNING'"
                            @click="installAgent(row)"
                        >
                            {{ $t(`environment.nodeStatusMap.${row.nodeStatus}`) }}
                        </span>
                        <span
                            class="node-status"
                            v-else
                        >
                            {{ $t(`environment.nodeStatusMap.${row.nodeStatus}`) }}
                        </span>
                        <span v-if="row.agentVersion">
                            ({{ row.agentVersion }})
                        </span>
                    </div>
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('environment.operateUser')"
                prop="createdUser"
                width="180"
            />
            <bk-table-column
                :label="$t('environment.operateTime')"
                prop="operateTime"
                width="180"
            />

            <bk-table-column
                :label="$t('environment.operation')"
                prop="operate"
                fixed="right"
                min-width="150"
            >
                <template slot-scope="{ row }">
                    <bk-button
                        theme="primary"
                        text
                        @click="handleToggleEnableNode(row)"
                    >
                        {{ row.envEnableNode ? $t('environment.Disable') : $t('environment.Enable') }}
                    </bk-button>
                    <bk-button
                        theme="primary"
                        text
                        @click="handleRemoveNode(row)"
                    >
                        {{ $t('environment.remove') }}
                    </bk-button>
                </template>
            </bk-table-column>
        </bk-table>
        <related-nodes-dialog
            :current-node-list="envNodeList"
            @save-success="handleRelateSuccess"
        />
    </div>
</template>

<script>
    import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
    import {
        ENV_RESOURCE_ACTION,
        ENV_RESOURCE_TYPE
    } from '@/utils/permission'
    import useInstance from '@/hooks/useInstance'
    import usePagination from '@/hooks/usePagination'
    import useEnvDetail from '@/hooks/useEnvDetail'
    import useRelatedNodes from '@/hooks/useRelatedNodes'
    import RelatedNodesDialog from '@/components/RelatedNodesDialog.vue'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'
    export default {
        name: 'Node',
        components: {
            SearchSelect,
            RelatedNodesDialog
        },
        setup () {
            const { proxy } = useInstance()
            const {
                pagination,
                resetPage,
                resetPagination,
                pageChange,
                pageLimitChange,
                updateCount
            } = usePagination()
       
            const  {
                handleShowRelatedNodes
            } = useRelatedNodes()
            const {
                projectId,
                envHashId,
                envNodeList,
                fetchEnvDetail,
                fetchEnvNodeList,
                requestRemoveNode,
                toggleEnableNode
            } = useEnvDetail()
            const isLoading = ref(false)
            const tableSize = ref('small')
            const tableMaxHeight = ref(565)
            const selectedNodesList = ref([])
            const searchValue = ref([])
            const runningStatus = ref(['CREATING', 'STARTING', 'STOPPING', 'RESTARTING', 'DELETING', 'BUILDING_IMAGE'])
            const successStatus = ref(['NORMAL', 'BUILD_IMAGE_SUCCESS'])
            const failStatus = ref(['ABNORMAL', 'DELETED', 'LOST', 'BUILD_IMAGE_FAILED', 'UNKNOWN', 'RUNNING'])

            const resType = computed(() => proxy.$route.params.resType)
            const searchList = computed(() => ([
                {
                    name: proxy.$t('environment.nodeInfo.hostName'),
                    id: 'displayName',
                    default: true
                },
                {
                    name: 'IP',
                    id: 'nodeIp'
                },
                {
                    name: proxy.$t('environment.status'),
                    id: 'nodeStatus',
                    children: [
                        {
                            id: 'NORMAL',
                            name: proxy.$t('environment.nodeStatusMap.NORMAL')
                        },
                        {
                            id: 'ABNORMAL',
                            name: proxy.$t('environment.nodeStatusMap.ABNORMAL')
                        }
                        // {
                        //     id: 'NOT_INSTALLED',
                        //     name: proxy.$t('environment.nodeStatusMap.NOT_INSTALLED')
                        // }
                    ]
                },
                {
                    name: proxy.$t('environment.operateUser'),
                    id: 'createdUser'
                }
            ]))
            const filterTips = computed(() => {
                return searchList.value.map(item => item.name).join(' / ')
            })
            // 获取节点列表参数（分页，过滤选项）
            const searchQuery = computed(() => {
                return searchValue.value.reduce((acc, item) => {
                    acc[item.id] = item.values.map(value => value.id).join(',')
                    return acc
                }, {
                    page: pagination.value.current,
                    pageSize: pagination.value.limit
                })
            })

            // 用于标记是否是 envHashId 变化导致的搜索值清空
            const isEnvIdChanging = ref(false)
            // 监听搜索值变化
            watch(() => searchValue.value, () => {
                // 如果是 envHashId 变化导致的清空，不触发搜索
                if (isEnvIdChanging.value) return
                resetPage()
                fetchData()
            })

            // 监听 envHashId 变化
            watch(() => envHashId.value, () => {
                resetPagination() // 重置分页配置
                isEnvIdChanging.value = true
                searchValue.value = []
                fetchData()
                nextTick(() => {
                    isEnvIdChanging.value = false
                })
            })

            // 动态计算表格最大高度
            const calculateTableHeight = () => {
                // 获取容器高度
                const container = document.querySelector('.node-container')
                if (container) {
                    const containerHeight = container.clientHeight
                    // 减去头部高度（按钮和搜索框区域）和表格上边距
                    // 头部高度约 32px（按钮高度）+ 20px（margin-top）= 52px
                    const headerHeight = 52
                    const calculatedHeight = containerHeight - headerHeight
                    // 确保计算出的高度大于最小值
                    if (calculatedHeight > 200) {
                        tableMaxHeight.value = calculatedHeight
                    }
                }
            }

            // 监听窗口大小变化
            const handleResize = () => {
                calculateTableHeight()
            }

            const handleShowAddNodesDialog = () => {
                handleShowRelatedNodes()
            }
            
            // 关联节点保存成功后的回调
            const handleRelateSuccess = () => {
                resetPage()
                fetchData()
                fetchEnvDetail()
                proxy.$bkMessage({
                    theme: 'success',
                    message: proxy.$t('environment.successfullySaved')
                })
            }
            
            const handleSelectionChange = ((list) => {
                selectedNodesList.value = list
            })

            // 获取节点列表
            const fetchData = async () => {
                try {
                    isLoading.value = true
                    const res = await fetchEnvNodeList(searchQuery.value)
                    if (res) {
                        updateCount(res.count)
                    }
                } catch (err) {
                    console.error('获取节点列表失败:', err)
                } finally {
                    isLoading.value = false
                }
            }

            const removeNodeFn = async (params) => {
                try {
                    const res = await requestRemoveNode(params)
                    proxy.$bkMessage({
                        message: proxy.$t('environment.removeSuccess'),
                        theme: 'success'
                    })
                    resetPage()
                    fetchData()
                } catch (err) {
                    console.error('删除节点失败:', err)
                }

            }
            // 移除节点
            const handleRemoveNode = (row) => {
                proxy.$bkInfo({
                    title: proxy.$t('environment.confirmRemoveNode', [row.displayName]),
                    extCls: 'env-remove-node-dialog',
                    confirmFn: () => removeNodeFn([row.nodeHashId])
                })
            }
            
            // 批量移除节点
            const handleBatchRemove = () => {
                if (!selectedNodesList.value.length) return
                const title = selectedNodesList.value.length === 1
                    ? proxy.$t('environment.confirmRemoveNode', [selectedNodesList.value[0]?.displayName])
                    : proxy.$t('environment.confirmBatchRemoveNode', [selectedNodesList.value.length])
                const params = selectedNodesList.value.map(i => i.nodeHashId)
                proxy.$bkInfo({
                    title,
                    extCls: 'env-remove-node-dialog',
                    confirmFn: () => removeNodeFn(params)
                })
            }

            // 启用/停用节点
            const handleToggleEnableNode = async (row) => {
                try {
                    const enableNode = !row.envEnableNode
                    await toggleEnableNode(row.nodeHashId, enableNode)
                    proxy.$bkMessage({
                        message: enableNode ? proxy.$t('environment.enableSuccess') : proxy.$t('environment.disableSuccess'),
                        theme: 'success'
                    })
                    fetchData()
                } catch (err) {
                    console.error('启用/停用节点失败:', err)
                }
            }

            const handlePageChange = (page) => {
                pageChange(page)
                fetchData()
            }
            const handlePageLimitChange = (limit) => {
                pageChange(1)
                pageLimitChange(limit)
                fetchData()
            }

            const handleToNodeDetail = (row) => {
                if (row.nodeType === 'CMDB') return
                window.open(`${location.origin}/console/environment/${projectId.value}/${resType.value}/node/allNode?keywords=${row.displayName}&nodeHashId=${row.nodeHashId}`, '_blank')
            }

            onMounted(() => {
                nextTick(() => {
                    calculateTableHeight()
                })
                window.addEventListener('resize', handleResize)

                // 获取节点列表
                fetchData()
            })

            onUnmounted(() => {
                window.removeEventListener('resize', handleResize)
            })
            return {
                // data
                isLoading,
                envNodeList,
                tableSize,
                tableMaxHeight,
                selectedNodesList,
                pagination,
                searchValue,
                searchList,
                filterTips,
                runningStatus,
                successStatus,
                failStatus,
                projectId,
                envHashId,
                ENV_RESOURCE_ACTION,
                ENV_RESOURCE_TYPE,

                // function
                fetchData,
                resetPage,
                resetPagination,
                handlePageChange,
                handlePageLimitChange,
                handleShowAddNodesDialog,
                handleRelateSuccess,
                handleSelectionChange,
                handleBatchRemove,
                handleRemoveNode,
                handleToggleEnableNode,
                handleToNodeDetail
            }
        }
    }
</script>

<style lang="scss" scoped>
@import '@/scss/conf';
.node-container {
    height: calc(100% - 90px);
    .node-list-main-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
    }
    .search-input {
        width: 480px;
        background: white;
        ::placeholder {
            color: #c4c6cc;
        }
    }
    .node-list-table {
        margin-top: 20px;
        .node-name {
            color: #3a84ff;
            cursor: pointer;
        }
    }
    .node-status-icon {
        display: inline-block;
        margin-left: 2px;
        width: 10px;
        height: 10px;
        border: 2px solid #30D878;
        border-radius: 50%;
        -webkit-border-radius: 50%;
    }

    .loading-icon {
        display: inline-block;
        position: relative;
        width: 12px;
        top: -12px;
        margin-right: 5px;
    }

    .abnormal-stutus-icon {
        border-color: $failColor;
    }

    .normal-status-node {
        color: #30D878;
    }

    .abnormal-status-node {
        color: $failColor;
    }
}
</style>
<style lang="scss">
.env-remove-node-dialog {
    .bk-dialog-header {
        .bk-dialog-header-inner {
            white-space: pre-wrap !important;
        }
    }
}
</style>
