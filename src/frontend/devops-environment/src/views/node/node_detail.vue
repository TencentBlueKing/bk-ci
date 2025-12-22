<template>
    <div
        class="node-entry-main"
        v-bkloading="{ isLoading: !nodeDetailLoaded }"
    >
        <header class="node-info-header">
            <span
                v-bk-overflow-tips
                class="node-name"
            >
                {{ currentNode?.displayName || '--' }}
            </span>
            <bk-tag>{{ nodeTypeDisplayName }}</bk-tag>
            <span
                class="node-status-tag"
                :class="nodeStatusClass"
            >
                {{ nodeStatusDisplayName }}
            </span>
        </header>
        <div
            class="node-content-main"
        >
            <template v-if="nodeDetailLoaded">
                <bk-tab
                    :active.sync="tabActive"
                    type="unborder-card"
                    ext-cls="node-details-tab"
                >
                    <bk-tab-panel
                        v-for="(panel, index) in panels"
                        v-bind="panel"
                        :key="index"
                    />
                </bk-tab>
                <component
                    :is="renderComponent"
                    :key="currentNode.nodeHashId"
                />
            </template>
        </div>
    </div>
</template>

<script>
    import { ref, watch, computed, onMounted } from 'vue'
    import useInstance from '@/hooks/useInstance'
    import useNodeDetail from '@/hooks/useNodeDetail'
    import Overview from './components/Overview.vue'
    import Settings from './components/Settings.vue'
    import TaskList from './components/TaskList.vue'
    import AgentOfflineRecords from './components/AgentOfflineRecords.vue'
    
    export default {
        name: 'NodeDetail',
        components: {
            Overview,
            Settings,
            TaskList,
            AgentOfflineRecords
        },
        setup () {
            const { proxy } = useInstance()
            const {
                currentNode,
                nodeHashId,
                fetchNodeDetail
            } = useNodeDetail()
            
            // 节点详情是否加载完成
            const nodeDetailLoaded = ref(false)
            
            // 从路由参数中获取初始 tab，如果没有则默认为 'overview'
            const initialTab = proxy.$route.params.tabName || 'overview'
            const tabActive = ref(initialTab)
            
            const renderComponent = computed(() => {
                const comMap = {
                    overview: Overview,
                    settings: Settings,
                    taskList: TaskList,
                    offlineRecords: AgentOfflineRecords
                }
                return comMap[tabActive.value]
            })

            const nodeTypeDisplayName = computed(() => {
                const nodeTypeMap = {
                    'THIRDPARTY': proxy.$t('environment.build'),
                    'DEVCLOUD': proxy.$t('environment.build'),
                    'CC': proxy.$t('environment.deploy'),
                    'CMDB': proxy.$t('environment.deploy'),
                    'TSTACK': proxy.$t('environment.deploy')
                }
                return nodeTypeMap[currentNode.value?.nodeType] || '--'
            })
            
            const nodeStatusDisplayName = computed(() => {
                return proxy.$t('environment.nodeStatusMap')[currentNode.value?.nodeStatus] || currentNode.value?.nodeStatus || '--'
            })
            
            const nodeStatusClass = computed(() => {
                const successStatus = ['NORMAL', 'BUILD_IMAGE_SUCCESS']
                const failStatus = ['ABNORMAL', 'DELETED', 'LOST', 'BUILD_IMAGE_FAILED', 'UNKNOWN', 'RUNNING']
                const status = currentNode.value?.nodeStatus
                
                if (successStatus.includes(status)) {
                    return 'status-normal'
                } else if (failStatus.includes(status)) {
                    return 'status-abnormal'
                }
                return ''
            })
            
            const panels = computed(() => [
                {
                    name: 'overview',
                    label: proxy.$t('environment.overview')
                },
                {
                    name: 'settings',
                    label: proxy.$t('environment.settings')
                },
                {
                    name: 'taskList',
                    label: proxy.$t('environment.taskList')
                },
                {
                    name: 'offlineRecords',
                    label: proxy.$t('environment.offlineRecords')
                }
            ])
            
            // 获取可用的 tab 名称列表
            const availableTabs = computed(() => panels.value.map(p => p.name))

            // 监听 tabActive 变化，更新路由参数
            watch(() => tabActive.value, (newTab) => {
                const currentRoute = proxy.$route
                // 只有在有 nodeHashId 且 tabName 不同时才进行导航
                if (currentRoute.params.nodeHashId && currentRoute.params.tabName !== newTab) {
                    proxy.$router.replace({
                        name: 'nodeDetail',
                        params: {
                            ...currentRoute.params,
                            tabName: newTab
                        }
                    }).catch(err => {
                        console.error('路由导航错误:', err)
                    })
                }
            }, {
                immediate: true
            })

            // 监听路由参数变化，更新 tabActive
            watch(() => proxy.$route.params.tabName, (newTabName) => {
                if (newTabName && newTabName !== tabActive.value) {
                    tabActive.value = newTabName
                }
            })
            
            // 监听 currentNode 和 availableTabs 变化，检查当前 tab 是否可用
            watch([() => currentNode.value?.nodeHashId, availableTabs], ([nodeHashId, tabs]) => {
                const currentTabName = tabActive.value
                // 当 currentNode 加载完成后，检查当前 tab 是否在可用列表中
                if (nodeHashId && currentTabName && !tabs.includes(currentTabName)) {
                    tabActive.value = 'overview'
                    proxy.$router.replace({
                        name: 'nodeDetail',
                        params: {
                            ...proxy.$route.params,
                            tabName: 'overview'
                        }
                    }).catch(err => {
                        console.error('路由导航错误:', err)
                    })
                }
            }, {
                immediate: true
            })

            // 监听 nodeHashId 变化，当 nodeHashId 存在但没有 tabName 时，添加默认的 tabName
            watch(() => nodeHashId.value, async (newNodeHashId) => {
                if (newNodeHashId) {
                    nodeDetailLoaded.value = false
                    await fetchNodeDetail()
                    nodeDetailLoaded.value = true
                }
                if (newNodeHashId && !proxy.$route.params.tabName) {
                    proxy.$router.replace({
                        name: 'nodeDetail',
                        params: {
                            ...proxy.$route.params,
                            tabName: tabActive.value
                        }
                    }).catch(err => {
                        console.error('路由导航错误:', err)
                    })
                }
            })
            
            onMounted(async () => {
                nodeDetailLoaded.value = false
                await fetchNodeDetail()
                nodeDetailLoaded.value = true
            })
            
            return {
                renderComponent,
                currentNode,
                tabActive,
                panels,
                nodeDetailLoaded,
                nodeTypeDisplayName,
                nodeStatusDisplayName,
                nodeStatusClass
            }
        }
    }
</script>

<style lang="scss" scoped>
.node-entry-main {
    display: flex;
    flex-direction: column;
    padding: 24px;
    height: 100%;
    background: #f5f7fa;
}
.node-info-header {
    display: flex;
    align-items: center;
    height: 54px;
    line-height: 54px;
    background: #FAFBFD;
    padding: 0 24px;
    .node-name {
        font-weight: 700;
        font-size: 14px;
        max-width: 300px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        color: #63656E;
        margin-right: 16px;
    }
    .node-status-tag {
        min-width: 68px;
        height: 22px;
        line-height: 22px;
        font-size: 12px;
        text-align: center;
        color: #63656E;
        margin-left: 8px;
        padding: 0 8px;
        border: 1px solid #979ba54d;
        border-radius: 2px;
        
        &.status-normal {
            color: #30D878;
            border-color: #30D878;
            background-color: #E5F6EB;
        }
        
        &.status-abnormal {
            color: #EA3636;
            border-color: #EA3636;
            background-color: #FEEBEA;
        }
    }
}
.node-content-main {
    flex: 1;
    padding: 0 24px;
    background-color: #fff;
    overflow: hidden;
    box-shadow: 0 2px 2px 0 #00000026;
    display: flex;
    flex-direction: column;
}
</style>
<style lang="scss">
.node-details-tab {
    .bk-tab-header {
        margin-bottom: 18px;
    }
    .bk-tab-section {
        display: none !important;
    }
}
</style>
