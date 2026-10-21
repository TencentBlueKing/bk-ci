<template>
    <div
        class="node-entry-main"
        v-bkloading="{ isLoading: !nodeDetailLoaded }"
    >
        <header class="node-info-header">
            <section class="node-info-left">
                <template v-if="isEditingName">
                    <bk-input
                        ref="nameInputRef"
                        v-model="editingDisplayName"
                        class="name-edit-input"
                        :class="{ 'is-error': nameError }"
                        :maxlength="30"
                        @enter="handleSaveName"
                    />
                    <template v-if="!isSavingName">
                        <i
                            class="bk-icon icon-check-line edit-action-icon save"
                            @click="handleSaveName"
                        />
                        <i
                            class="bk-icon icon-close-line-2 edit-action-icon cancel"
                            @click="handleCancelEdit"
                        />
                    </template>
                    <i
                        v-else
                        class="bk-icon icon-circle-2-1 edit-action-icon loading"
                    />
                </template>
                <template v-else>
                    <span
                        v-if="showDigitalTwinAvatar"
                        class="digital-twin-avatar"
                        :class="{ 'is-neutral': isNeutralHeader }"
                        :style="avatarStyle"
                    >
                        {{ displayNameLastChar }}
                    </span>
                    <span
                        v-bk-overflow-tips
                        class="node-name"
                        :class="{ 'is-neutral-name': isNeutralHeader }"
                    >
                        {{ currentNode?.displayName || '--' }}
                    </span>
                    <i
                        v-if="!isCreateResType"
                        class="bk-icon icon-edit-line name-edit-icon"
                        @click="handleStartEditName"
                    />
                    <bk-tag
                        v-if="isCreateResType && nodeSourceLabel"
                        class="header-meta-tag source-tag"
                    >
                        {{ nodeSourceLabel }}
                    </bk-tag>
                    <!-- 已释放 / 已销毁：与列表一致，名称后追加中性标签 -->
                    <bk-tag
                        v-if="isCreateResType && neutralStatusLabel"
                        class="header-meta-tag neutral-status-tag"
                    >
                        {{ neutralStatusLabel }}
                    </bk-tag>
                </template>
                <template v-if="showOsAndIp">
                    <span class="node-os">
                        {{ currentNode?.osName || '--' }}
                    </span>
                    <span class="node-ip">
                        {{ currentNode?.ip || '--' }}
                    </span>
                </template>
            </section>

            <section class="node-info-right">
                <template v-if="showAgentActions">
                    <span class="agent-status-wrapper">
                        <StatusIcon
                            v-if="isAgentNormal"
                            status="success"
                        />
                        <StatusIcon
                            v-else
                            status="error"
                        />
                        <span
                            class="agent-status-text"
                            :class="isAgentNormal ? 'normal' : 'abnormal'"
                        >
                            {{ isAgentNormal ? $t('environment.agentNormal') : $t('environment.agentAbnormal') }}
                        </span>
                    </span>
                    <a
                        v-if="!isAgentNormal"
                        class="reinstall-agent-btn"
                        @click="handleReinstallAgent"
                    >
                        {{ $t('environment.reinstallAgent') }}
                    </a>
                </template>
                <!-- 已释放/已销毁也保留刷新（设计稿：刷新 | 清理） -->
                <a
                    class="refresh-btn"
                    @click="handleRefresh"
                >
                    {{ $t('environment.refresh') }}
                </a>
                <a
                    v-if="showCleanAction"
                    class="clean-node-btn"
                    v-perm="{
                        hasPermission: currentNode?.canEdit,
                        disablePermissionApi: true,
                        permissionData: {
                            projectId: $route.params.projectId,
                            resourceType: creativeResourceType,
                            resourceCode: currentNode?.nodeHashId,
                            action: creativeResourceAction.EDIT
                        }
                    }"
                    @click="openCleanConfirm"
                >
                    {{ $t('environment.clean') }}
                </a>
                <a
                    v-if="showDeleteAction"
                    class="clean-node-btn"
                    @click="openDeleteConfirm"
                >
                    {{ $t('environment.delete') }}
                </a>
            </section>
        </header>
        <NodeActionConfirmDialog
            :visible="actionConfirm.visible"
            :mode="actionConfirm.mode"
            :node="currentNode"
            :relations="actionConfirm.relations"
            :relations-loading="actionConfirm.relationsLoading"
            :confirm-loading="actionConfirm.confirmLoading"
            @confirm="handleActionConfirm"
            @cancel="closeActionConfirm"
        />
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
    import { ref, watch, computed, onMounted, nextTick } from 'vue'
    import useInstance from '@/hooks/useInstance'
    import useNodeDetail from '@/hooks/useNodeDetail'
    import Overview from './components/Overview.vue'
    import Settings from './components/Settings.vue'
    import TaskList from './components/TaskList.vue'
    import AgentOfflineRecords from './components/AgentOfflineRecords.vue'
    import NodeActionConfirmDialog from './components/NodeActionConfirmDialog.vue'
    import StatusIcon from '@/components/status-icon.vue'
    import { SERVICE_RESOURCE_TYPE } from '@/store/constants'
    import useResourceType from '@/hooks/useResourceType'
    import { fetchNodeRelations, invalidateNodeRelationsCache } from '@/hooks/useNodeRelations'
    import {
        CREATIVE_STREAM_NODE_RESOURCE_ACTION,
        CREATIVE_NODE_RESOURCE_TYPE
    } from '@/utils/permission'
    
    export default {
        name: 'NodeDetail',
        components: {
            Overview,
            Settings,
            TaskList,
            AgentOfflineRecords,
            StatusIcon,
            NodeActionConfirmDialog
        },
        props: {
            installAgent: {
                type: Function,
                default: null
            },
            // 列表行：详情接口无 createWorkspaceSource，用列表数据补齐节点类型
            listNode: {
                type: Object,
                default: null
            }
        },
        setup (props) {
            const { proxy } = useInstance()
            const {
                currentNode,
                nodeHashId,
                fetchNodeDetail,
                applyListNodeMeta,
                nodeDetailLoaded,
                deleteNode
            } = useNodeDetail()
            const {
                isCreateResType: checkCreate,
                getNodeSourceLabel,
                getNeutralStatusLabelKey,
                isAgentPresent,
                getAgentStatus,
                canCleanNode,
                isDigitalTwin,
                isDestroyedNode,
                isNeutralStatusNode
            } = useResourceType()

            const AVATAR_PALETTE = ['#3A84FF', '#2DCB56', '#9B80FF', '#18C0BB', '#FF9C01', '#FF5656']

            const getDisplayNameLastChar = (name) => {
                const text = (name || '').trim()
                if (!text) return '?'
                return Array.from(text).pop()
            }

            const getAvatarStyle = (name) => {
                const text = (name || '').trim()
                let hash = 0
                for (let i = 0; i < text.length; i++) {
                    hash = (hash + text.charCodeAt(i) * (i + 1)) % AVATAR_PALETTE.length
                }
                return { backgroundColor: AVATAR_PALETTE[hash] || AVATAR_PALETTE[0] }
            }
            
            // 编辑名称相关状态
            const isEditingName = ref(false)
            const editingDisplayName = ref('')
            const isSavingName = ref(false)
            const nameError = ref(false)
            const nameInputRef = ref(null)
            const actionConfirm = ref({
                visible: false,
                mode: 'delete',
                relations: { environments: [], references: [] },
                relationsLoading: false,
                confirmLoading: false
            })
            
            // 从路由查询参数中获取初始 tab，如果没有则默认为 'overview'
            const initialTab = proxy.$route.query.tabName || 'overview'
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
                return proxy.$t('environment.nodeStatusMap')[currentNode.value?.status] || currentNode.value?.status || '--'
            })
            
            const nodeStatusClass = computed(() => {
                const successStatus = ['NORMAL', 'BUILD_IMAGE_SUCCESS']
                const failStatus = ['ABNORMAL', 'DELETED', 'LOST', 'BUILD_IMAGE_FAILED', 'UNKNOWN', 'RUNNING']
                const status = currentNode.value?.status
                
                if (successStatus.includes(status)) {
                    return 'status-normal'
                } else if (failStatus.includes(status)) {
                    return 'status-abnormal'
                }
                return ''
            })

            const isCreateResType = computed(() => checkCreate(proxy.$route))

            const nodeSourceLabel = computed(() => {
                return getNodeSourceLabel(currentNode.value, proxy.$t.bind(proxy))
            })

            const neutralStatusLabel = computed(() => {
                const key = getNeutralStatusLabelKey(currentNode.value)
                return key ? proxy.$t(key) : ''
            })

            // 数字分身不展示系统与 IP；云桌面展示（已销毁不展示）；流水线始终展示
            const showOsAndIp = computed(() => {
                if (!isCreateResType.value) return true
                if (isDestroyedNode(currentNode.value)) return false
                if (isDigitalTwin(currentNode.value)) return false
                return true
            })

            const showAgentActions = computed(() => {
                return isAgentPresent(currentNode.value)
            })

            const showCleanAction = computed(() => {
                return isCreateResType.value && canCleanNode(currentNode.value)
            })

            const showDeleteAction = computed(() => {
                return !isCreateResType.value
            })

            // 与列表一致：仅数字分身展示字头头像
            const showDigitalTwinAvatar = computed(() => {
                return isCreateResType.value && isDigitalTwin(currentNode.value)
            })

            const isNeutralHeader = computed(() => {
                return isCreateResType.value && isNeutralStatusNode(currentNode.value)
            })

            const displayNameLastChar = computed(() => {
                return getDisplayNameLastChar(currentNode.value?.displayName)
            })

            const avatarStyle = computed(() => {
                return getAvatarStyle(currentNode.value?.displayName)
            })

            const creativeResourceType = CREATIVE_NODE_RESOURCE_TYPE
            const creativeResourceAction = CREATIVE_STREAM_NODE_RESOURCE_ACTION
            
            // 判断 Agent 是否正常
            const isAgentNormal = computed(() => {
                if (isCreateResType.value) {
                    return getAgentStatus(currentNode.value) === 'normal'
                }
                const successStatus = ['NORMAL', 'BUILD_IMAGE_SUCCESS']
                return successStatus.includes(currentNode.value?.status)
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
                    label: isCreateResType.value
                        ? proxy.$t('environment.creationTask')
                        : proxy.$t('environment.nodeInfo.buildTask')
                },
                {
                    name: 'offlineRecords',
                    label: isCreateResType.value
                        ? proxy.$t('environment.agentOfflineRecords')
                        : proxy.$t('environment.offlineRecords')
                }
            ])
            
            // 获取可用的 tab 名称列表
            const availableTabs = computed(() => panels.value.map(p => p.name))

            // 监听 tabActive 变化，更新路由查询参数
            watch(() => tabActive.value, (newTab) => {
                const currentRoute = proxy.$route
                // 使用 query 而不是 params
                if (currentRoute.query.nodeHashId && currentRoute.query.tabName !== newTab) {
                    proxy.$router.replace({
                        query: {
                            ...currentRoute.query,
                            tabName: newTab
                        }
                    }).catch(err => {
                        console.error('路由导航错误:', err)
                    })
                }
            }, {
                immediate: true
            })

            // 监听路由查询参数变化，更新 tabActive
            watch(() => proxy.$route.query.tabName, (newTabName) => {
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
                        query: {
                            ...proxy.$route.query,
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
                    await fetchNodeDetail(props.listNode)
                }
                if (newNodeHashId && !proxy.$route.query.tabName) {
                    proxy.$router.replace({
                        query: {
                            ...proxy.$route.query,
                            tabName: tabActive.value
                        }
                    }).catch(err => {
                        console.error('路由导航错误:', err)
                    })
                }
            })

            // 列表异步到达或切换行时，补齐创作流节点类型字段
            watch(() => props.listNode, (listNode) => {
                if (listNode && nodeHashId.value) {
                    applyListNodeMeta(listNode)
                }
            }, { deep: true })
            
            onMounted(async () => {
                await fetchNodeDetail(props.listNode)
            })

            // 重装 Agent - 打开安装弹框
            const handleReinstallAgent = () => {
                const node = currentNode.value
                if (!node || !props.installAgent) return
                
                props.installAgent({
                    nodeType: 'THIRDPARTY',
                    nodeHashId: nodeHashId.value,
                    agentHashId: node.agentId,
                    ip: node.ip,
                    osName: node.os,
                    gateway: node.gatewayShowName
                })
            }

            // 刷新节点详情（带上列表行，避免丢失 createWorkspaceSource）
            const handleRefresh = async () => {
                await fetchNodeDetail(props.listNode)
            }

            const openActionConfirm = async (mode) => {
                actionConfirm.value = {
                    visible: true,
                    mode,
                    relations: { environments: [], references: [] },
                    relationsLoading: true,
                    confirmLoading: false
                }
                try {
                    const relations = await fetchNodeRelations({
                        projectId: proxy.$route.params.projectId,
                        nodeHashId: nodeHashId.value,
                        isCreateResType: isCreateResType.value,
                        agentId: currentNode.value?.agentId || currentNode.value?.agentHashId,
                        envIds: Array.isArray(currentNode.value?.envIds)
                            ? currentNode.value.envIds
                            : undefined,
                        store: proxy.$store,
                        resType: proxy.$route.params.resType
                    })
                    actionConfirm.value.relations = relations
                } catch (e) {
                    console.error(e)
                } finally {
                    actionConfirm.value.relationsLoading = false
                }
            }

            const openCleanConfirm = () => openActionConfirm('clean')
            const openDeleteConfirm = () => openActionConfirm('delete')

            const closeActionConfirm = () => {
                actionConfirm.value.visible = false
                actionConfirm.value.confirmLoading = false
            }

            const handleActionConfirm = async () => {
                actionConfirm.value.confirmLoading = true
                try {
                    await deleteNode()
                    proxy.$bkMessage({
                        theme: 'success',
                        message: proxy.$t('environment.successfullyDeleted')
                    })
                    invalidateNodeRelationsCache(nodeHashId.value)
                    closeActionConfirm()
                    // 清理/删除后回到列表
                    proxy.$router.replace({
                        name: 'nodeList',
                        params: { ...proxy.$route.params },
                        query: {}
                    })
                } catch (e) {
                    proxy.$bkMessage({
                        theme: 'error',
                        message: e.message || proxy.$t('environment.updateFailed')
                    })
                } finally {
                    actionConfirm.value.confirmLoading = false
                }
            }
            
            const handleStartEditName = () => {
                editingDisplayName.value = currentNode.value?.displayName || ''
                isEditingName.value = true
                nameError.value = false
                nextTick(() => {
                    nameInputRef.value?.focus?.()
                })
            }
            
            const handleCancelEdit = () => {
                isEditingName.value = false
                editingDisplayName.value = ''
                nameError.value = false
            }
            
            const handleSaveName = async () => {
                if (isSavingName.value) return
                const displayName = editingDisplayName.value.trim()
                if (!displayName) {
                    nameError.value = true
                    proxy.$bkMessage({
                        theme: 'error',
                        message: proxy.$t('environment.nodeInfo.enterDisplayName')
                    })
                    return
                }
                
                if (displayName === currentNode.value?.displayName) {
                    handleCancelEdit()
                    return
                }
                
                isSavingName.value = true
                try {
                    await proxy.$store.dispatch('environment/updateDisplayName', {
                        projectId: proxy.$route.params.projectId,
                        nodeHashId: proxy.$route.query?.nodeHashId,
                        params: { displayName }
                    })
                    
                    proxy.$bkMessage({
                        theme: 'success',
                        message: proxy.$t('environment.successfullyModified')
                    })
                    
                    // 更新本地数据
                    currentNode.value.displayName = displayName
                    handleCancelEdit()
                } catch (e) {
                    console.error('更新节点名称失败:', e)
                    proxy.$bkMessage({
                        theme: 'error',
                        message: e.message || proxy.$t('environment.updateFailed')
                    })
                } finally {
                    isSavingName.value = false
                }
            }
            
            return {
                renderComponent,
                currentNode,
                tabActive,
                panels,
                nodeDetailLoaded,
                nodeTypeDisplayName,
                nodeStatusDisplayName,
                nodeStatusClass,
                isAgentNormal,
                handleReinstallAgent,
                handleRefresh,
                // 编辑名称相关
                isEditingName,
                editingDisplayName,
                isSavingName,
                nameError,
                nameInputRef,
                isCreateResType,
                nodeSourceLabel,
                neutralStatusLabel,
                showOsAndIp,
                showAgentActions,
                showCleanAction,
                showDeleteAction,
                showDigitalTwinAvatar,
                isNeutralHeader,
                displayNameLastChar,
                avatarStyle,
                creativeResourceType,
                creativeResourceAction,
                actionConfirm,
                openCleanConfirm,
                openDeleteConfirm,
                closeActionConfirm,
                handleActionConfirm,
                handleStartEditName,
                handleCancelEdit,
                handleSaveName
            }
        }
    }
</script>

<style lang="scss" scoped>
.node-entry-main {
    display: flex;
    flex-direction: column;
    height: 100%;
    background: #f5f7fa;
}
.node-info-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 52px;
    background: #FAFBFD;
    padding: 0 24px;
    .node-info-left {
        display: flex;
        align-items: center;
        min-width: 0;
        flex: 1;
    }
    .node-info-right {
        display: flex;
        align-items: center;
        flex-shrink: 0;
        margin-left: 16px;
    }
    .digital-twin-avatar {
        flex-shrink: 0;
        width: 24px;
        height: 24px;
        margin-right: 8px;
        border-radius: 50%;
        color: #fff;
        font-size: 12px;
        line-height: 24px;
        text-align: center;
        &.is-neutral {
            opacity: 0.45;
        }
    }
    .agent-status-wrapper {
        display: flex;
        align-items: center;
        margin-right: 16px;
    }
    .agent-status-text {
        font-size: 12px;
        color: #63656E;
        &.normal {
            color: #2DCB56;
        }
        &.abnormal {
            color: #EA3636;
        }
    }
    .node-name {
        font-weight: 700;
        font-size: 14px;
        line-height: 22px;
        max-width: 300px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        color: #313238;
        margin-right: 8px;
        &.is-neutral-name {
            color: #C4C6CC;
        }
    }
    .header-meta-tag {
        flex-shrink: 0;
        height: 22px;
        line-height: 22px;
        padding: 0 8px;
        background: #F0F1F5 !important;
        color: #63656E !important;
        border: none !important;
        border-radius: 2px;
    }
    .clean-node-btn {
        font-size: 12px;
        margin-left: 10px;
        color: #3A84FF;
        cursor: pointer;
    }
    .name-edit-icon {
        font-size: 14px;
        color: #979BA5;
        cursor: pointer;
        &:hover {
            color: #3A84FF;
        }
    }
    .name-edit-input {
        width: 200px;
        &.is-error {
            .bk-form-input {
                border-color: #EA3636;
            }
        }
        .bk-form-input {
            height: 26px;
            font-size: 14px;
        }
    }
    .edit-action-icon {
        font-size: 16px;
        cursor: pointer;
        margin-left: 8px;
        &.save {
            color: #2DCB56;
            &:hover {
                color: #45E35F;
            }
        }
        &.cancel {
            color: #979BA5;
            &:hover {
                color: #63656E;
            }
        }
        &.loading {
            color: #3A84FF;
            animation: spin 1s linear infinite;
        }
    }
    .node-os {
        margin-left: 24px;
    }
    .node-os,
    .node-ip {
        font-size: 12px;
        color: #979BA5;
    }
    .node-ip {
        &::before {
            content: '|';
            margin: 0 6px;
        }
    }
    .reinstall-agent-btn {
        font-size: 12px;
        margin-left: auto;
        color: #3A84FF;
        cursor: pointer;
    }
    .refresh-btn {
        font-size: 12px;
        margin-left: 10px;
        color: #3A84FF;
        cursor: pointer;
    }
}

@keyframes spin {
    from {
        transform: rotate(0deg);
    }
    to {
        transform: rotate(360deg);
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
