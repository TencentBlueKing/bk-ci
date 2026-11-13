<template>
    <div class="node-container">
        <div class="node-list-main-header">
            <div class="header-left">
                <span
                    v-bk-tooltips="{
                        content: $t('environment.builtInEnvRelateNodeTips'),
                        disabled: !isBuiltInEnv
                    }"
                >
                    <bk-button
                        theme="primary"
                        :disabled="isBuiltInEnv"
                        v-perm="{
                            permissionData: {
                                projectId: projectId,
                                resourceType: ENV_RESOURCE_TYPE,
                                resourceCode: envHashId,
                                action: ENV_RESOURCE_ACTION.EDIT
                            }
                        }"
                        key="relatedNodes"
                        @click="handleShowAddNodesDialog"
                    >
                        {{ $t('environment.relatedNodes') }}
                    </bk-button>
                </span>
                <bk-dropdown-menu
                    trigger="click"
                    :font-size="'medium'"
                    @show="batchMenuOpen = true"
                    @hide="batchMenuOpen = false"
                >
                    <bk-button slot="dropdown-trigger">
                        {{ $t('environment.batchOperation') }}
                        <i :class="['bk-icon icon-angle-down', { 'icon-flip': batchMenuOpen }]"></i>
                    </bk-button>
                    <ul
                        slot="dropdown-content"
                        class="bk-dropdown-list env-batch-list"
                    >
                        <template v-for="(group, index) in batchGroups">
                            <li
                                :key="`${group.key}-label`"
                                class="batch-group-label"
                                :class="{ 'is-follow': index > 0 }"
                            >
                                {{ group.label }}
                            </li>
                            <li
                                v-for="item in group.items"
                                :key="item.key"
                            >
                                <a
                                    href="javascript:;"
                                    :class="{ disabled: item.disabled }"
                                    v-bk-tooltips="{
                                        content: item.tooltips,
                                        disabled: !item.disabled || !item.tooltips
                                    }"
                                    v-perm="item.perm"
                                    @click="onBatchItem(item)"
                                >
                                    {{ item.text }}
                                </a>
                            </li>
                        </template>
                    </ul>
                </bk-dropdown-menu>
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
        >
            <bk-table-column
                fixed="left"
                width="72"
                :render-header="renderSelectionHeader"
            >
                <template slot-scope="{ row }">
                    <bk-checkbox
                        :value="isRowSelected(row)"
                        @change="(checked) => toggleRowSelected(row, checked)"
                    />
                </template>
            </bk-table-column>
            <bk-table-column
                :label="$t('environment.nodeInfo.displayName')"
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
                width="200"
            />
            <bk-table-column
                :label="$t('environment.operateTime')"
                prop="operateTime"
                width="200"
            />
            <bk-table-column
                v-if="!isCreateResType && isBuildEnv"
                :label="$t('environment.enableTitle')"
                prop="operate"
                fixed="right"
                min-width="80"
            >
                <template slot-scope="{ row }">
                    <div class="switcher-wrapper">
                        <bk-switcher
                            v-model="row.envEnableNode"
                            size="small"
                            theme="primary"
                        />
                        <div
                            class="switcher-container"
                            @click="handleSwitcherClick(row)"
                        />
                    </div>
                </template>
            </bk-table-column>
            <bk-table-column
                v-if="!isDynamicEnv"
                :label="$t('environment.operation')"
                prop="operate"
                fixed="right"
                min-width="100"
            >
                <template slot-scope="{ row }">
                    <span
                        v-bk-tooltips="{
                            content: $t('environment.builtInEnvRemoveNodeTips'),
                            disabled: !isBuiltInEnv || !!selectedNodesList.length
                        }"
                    >
                        <bk-button
                            theme="primary"
                            text
                            :disabled="isBuiltInEnv"
                            @click="handleRemoveNode(row)"
                        >
                            {{ $t('environment.remove') }}
                        </bk-button>
                    </span>
                </template>
            </bk-table-column>
        </bk-table>
        <related-nodes-dialog
            @save-success="handleRelateSuccess"
        />
    </div>
</template>

<script>
    import { ref, computed, watch, onMounted, nextTick } from 'vue'
    import {
        ENV_RESOURCE_ACTION,
        ENV_RESOURCE_TYPE
    } from '@/utils/permission'
    import useInstance from '@/hooks/useInstance'
    import usePagination from '@/hooks/usePagination'
    import useEnvDetail from '@/hooks/useEnvDetail'
    import useEnvAside from '@/hooks/useEnvAside'
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
                fetchEnvList,
                isCreateResType
            } = useEnvAside()
            const {
                projectId,
                envHashId,
                envNodeList,
                currentEnv,
                fetchEnvDetail,
                fetchEnvNodeList,
                requestRemoveNode,
                toggleEnableNode,
                isBuiltInEnv,
                isBuildEnv
            } = useEnvDetail()
            const isLoading = ref(false)
            const tableSize = ref('small')
            const tableMaxHeight = ref(565)
            const ROW_HEIGHT = 42
            const TABLE_HEADER_HEIGHT = 42
            const PAGINATION_HEIGHT = 64
            const TABLE_MARGIN_TOP = 20
            const selectedNodesList = ref([])
            const batchMenuOpen = ref(false)
            const searchValue = ref([])
            const runningStatus = ref(['CREATING', 'STARTING', 'STOPPING', 'RESTARTING', 'DELETING', 'BUILDING_IMAGE'])
            const successStatus = ref(['NORMAL', 'BUILD_IMAGE_SUCCESS'])
            const failStatus = ref(['ABNORMAL', 'DELETED', 'LOST', 'BUILD_IMAGE_FAILED', 'UNKNOWN', 'RUNNING'])

            const resType = computed(() => proxy.$route.params.resType)
            // 是否为动态关联环境（按标签规则关联，envNodeType 为 TAG）
            const isDynamicEnv = computed(() => currentEnv.value?.envNodeType === 'TAG')
            const searchList = computed(() => ([
                {
                    name: proxy.$t('environment.nodeInfo.displayName'),
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
                selectedNodesList.value = []
                resetPage()
                fetchData()
            })

            // 监听 envHashId 变化
            watch(() => envHashId.value, () => {
                resetPagination() // 重置分页配置
                isEnvIdChanging.value = true
                searchValue.value = []
                selectedNodesList.value = []
                fetchData()
                nextTick(() => {
                    isEnvIdChanging.value = false
                })
            })

            // 动态计算表格高度及每页可展示条数（按 42px 行高）
            const calculateTableLayout = () => {
                const container = document.querySelector('.node-container')
                if (!container) return false
                const containerHeight = container.clientHeight
                const headerEl = container.querySelector('.node-list-main-header')
                const headerHeight = headerEl?.offsetHeight || 32
                const calculatedHeight = containerHeight - headerHeight - TABLE_MARGIN_TOP
                if (calculatedHeight <= 200) return false

                tableMaxHeight.value = calculatedHeight

                const tableBodyHeight = calculatedHeight - TABLE_HEADER_HEIGHT - PAGINATION_HEIGHT
                const rowCount = Math.max(1, Math.floor(tableBodyHeight / ROW_HEIGHT))
                const prevLimit = pagination.value.limit
                if (prevLimit !== rowCount) {
                    pageLimitChange(rowCount)
                    pagination.value.limitList = [rowCount, ...pagination.value.limitList.filter(item => item !== rowCount)]
                        .sort((a, b) => a - b)
                }

                return prevLimit !== rowCount
            }

            const handleShowAddNodesDialog = () => {
                handleShowRelatedNodes()
            }
            
            // 关联节点保存成功后的回调
            const handleRelateSuccess = () => {
                resetPage()
                fetchData()
                fetchEnvDetail()
                fetchEnvList()
                proxy.$bkMessage({
                    theme: 'success',
                    message: proxy.$t('environment.successfullySaved')
                })
            }
            
            const isRowSelected = (row) => selectedNodesList.value.some(item => item.nodeHashId === row.nodeHashId)
            const isPageAllSelected = computed(() => {
                return envNodeList.value.length > 0 && envNodeList.value.every(row => isRowSelected(row))
            })
            const isPageIndeterminate = computed(() => {
                if (!envNodeList.value.length || isPageAllSelected.value) return false
                return envNodeList.value.some(row => isRowSelected(row))
            })
            const toggleRowSelected = (row, checked) => {
                const rest = selectedNodesList.value.filter(item => item.nodeHashId !== row.nodeHashId)
                selectedNodesList.value = checked ? [...rest, row] : rest
            }
            const selectCurrentPage = () => {
                const pageIds = new Set(envNodeList.value.map(row => row.nodeHashId))
                const kept = selectedNodesList.value.filter(row => !pageIds.has(row.nodeHashId))
                selectedNodesList.value = [...kept, ...envNodeList.value]
            }
            const toggleCurrentPage = (checked) => {
                if (checked) {
                    selectCurrentPage()
                    return
                }
                const pageIds = new Set(envNodeList.value.map(row => row.nodeHashId))
                selectedNodesList.value = selectedNodesList.value.filter(row => !pageIds.has(row.nodeHashId))
            }

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
                    selectedNodesList.value = []
                    resetPage()
                    fetchData()
                    fetchEnvList()
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

            const nodeConfirmLabel = (row) => {
                const name = String(row?.displayName || '').trim()
                const ip = String(row?.ip || '').trim()
                if (name && ip) return `${name}（${ip}）`
                return name || ip || '-'
            }
            const nodeFailLabel = (row) => String(row?.displayName || '').trim() || String(row?.ip || '').trim() || '-'

            const askToggle = (rows, enable) => {
                const ordered = rows || []
                const targets = ordered.filter(row => !!row.envEnableNode !== enable)
                const skipped = ordered.filter(row => !!row.envEnableNode === enable)
                if (!targets.length) {
                    proxy.$bkMessage({
                        theme: 'primary',
                        message: enable
                            ? proxy.$t('environment.selectedAlreadyEnabled')
                            : proxy.$t('environment.selectedAlreadyDisabled')
                    })
                    return
                }
                const willNameList = targets.map(nodeConfirmLabel)
                const skipNameList = skipped.map(nodeConfirmLabel)
                const willTitle = proxy.$t(
                    enable ? 'environment.willEnableCount' : 'environment.willDisableCount',
                    [willNameList.length]
                )
                const skipTitle = proxy.$t(
                    enable ? 'environment.alreadyEnabledKeep' : 'environment.alreadyDisabledKeep',
                    [skipNameList.length]
                )
                const defaultReason = enable
                    ? proxy.$t('environment.enableNodeDefaultReason')
                    : proxy.$t('environment.disableNodeDefaultReason')
                const reasonRequiredText = proxy.$t('environment.reasonRequired')
                const reasonLabel = proxy.$t('environment.reasonLabel')
                const reasonPlaceholder = proxy.$t('environment.reasonPlaceholder')
                let reasonPanel = null
                const ReasonPanel = {
                    name: 'EnvToggleReason',
                    data () {
                        return { text: '', error: '' }
                    },
                    mounted () {
                        reasonPanel = this
                    },
                    beforeDestroy () {
                        if (reasonPanel === this) reasonPanel = null
                    },
                    methods: {
                        onFocus () {
                            if (!this.text) this.text = defaultReason
                        },
                        onInput (event) {
                            this.text = event.target.value
                            if (this.error) this.error = ''
                        },
                        takeReason () {
                            const value = this.text.trim()
                            if (!value) {
                                this.error = reasonRequiredText
                                return ''
                            }
                            this.error = ''
                            return value
                        }
                    },
                    render (h) {
                        const nameList = (names, extraClass) => h('ul', {
                            class: `toggle-name-list ${extraClass}`
                        }, names.map((name, index) => h('li', { key: index }, name)))
                        return h('div', { class: 'env-toggle-reason' }, [
                            h('p', { class: 'toggle-will-title' }, willTitle),
                            nameList(willNameList, 'is-will'),
                            skipNameList.length ? h('p', { class: 'toggle-skip-title' }, skipTitle) : null,
                            skipNameList.length ? nameList(skipNameList, 'is-skip') : null,
                            h('div', { class: 'toggle-enable-label' }, [
                                `${reasonLabel} `,
                                h('span', { class: 'required' }, '*')
                            ]),
                            h('textarea', {
                                class: 'toggle-reason-input',
                                attrs: {
                                    maxlength: 200,
                                    placeholder: reasonPlaceholder
                                },
                                domProps: { value: this.text },
                                on: {
                                    focus: this.onFocus,
                                    input: this.onInput
                                }
                            }),
                            this.error ? h('p', { class: 'toggle-reason-error' }, this.error) : null
                        ])
                    }
                }
                proxy.$bkInfo({
                    type: 'warning',
                    title: enable
                        ? proxy.$t('environment.confirmEnableNode')
                        : proxy.$t('environment.confirmDisableNode'),
                    width: 480,
                    okText: enable ? proxy.$t('environment.enableBtn') : proxy.$t('environment.disableBtn'),
                    cancelText: proxy.$t('environment.cancel'),
                    theme: 'primary',
                    maskClose: false,
                    confirmLoading: true,
                    extCls: 'env-toggle-node-dialog',
                    subHeader: proxy.$createElement(ReasonPanel),
                    confirmFn: async () => {
                        const reason = reasonPanel?.takeReason()
                        if (!reason) return false
                        if (targets.length > 1) {
                            try {
                                const res = await toggleEnableNode(
                                    targets[0].nodeHashId,
                                    enable,
                                    reason,
                                    targets.map(row => row.nodeHashId)
                                )
                                const allOk = res === true || res?.data === true
                                proxy.$bkMessage({
                                    theme: allOk ? 'success' : 'warning',
                                    message: res?.message || (enable
                                        ? proxy.$t('environment.enableSuccess')
                                        : proxy.$t('environment.disableSuccess'))
                                })
                                fetchData()
                            } catch (err) {
                                if (err?.message) {
                                    proxy.$bkMessage({
                                        theme: 'error',
                                        message: err.message
                                    })
                                }
                                return false
                            }
                            return
                        }
                        const failed = []
                        for (const row of targets) {
                            try {
                                await toggleEnableNode(row.nodeHashId, enable, reason)
                                row.envEnableNode = enable
                            } catch (err) {
                                failed.push(row)
                            }
                        }
                        const successCount = targets.length - failed.length
                        const failedText = failed.map(nodeFailLabel).join('、')
                        if (!failed.length) {
                            proxy.$bkMessage({
                                theme: 'success',
                                message: enable ? proxy.$t('environment.enableSuccess') : proxy.$t('environment.disableSuccess')
                            })
                        } else if (successCount > 0) {
                            proxy.$bkMessage({
                                theme: 'warning',
                                message: proxy.$t(enable ? 'environment.enablePartial' : 'environment.disablePartial', [successCount, failedText])
                            })
                        } else {
                            proxy.$bkMessage({
                                theme: 'error',
                                message: proxy.$t(enable ? 'environment.enableNone' : 'environment.disableNone', [failedText])
                            })
                        }
                        if (successCount > 0) fetchData()
                    }
                })
            }

            const handleSwitcherClick = (row) => {
                askToggle([row], !row.envEnableNode)
            }

            const copySelectedIps = async () => {
                const ips = selectedNodesList.value
                    .map(row => String(row.ip || '').trim())
                    .filter(Boolean)
                if (!ips.length) {
                    proxy.$bkMessage({
                        theme: 'warning',
                        message: proxy.$t('environment.noIpToCopy')
                    })
                    return
                }
                const text = ips.join('\n')
                let copied = false
                try {
                    if (navigator.clipboard?.writeText) {
                        await navigator.clipboard.writeText(text)
                        copied = true
                    }
                } catch (err) {
                    copied = false
                }
                if (!copied) {
                    const el = document.createElement('textarea')
                    el.value = text
                    el.setAttribute('readonly', '')
                    el.style.position = 'fixed'
                    el.style.left = '-9999px'
                    document.body.appendChild(el)
                    el.select()
                    try {
                        copied = document.execCommand('copy')
                    } catch (err) {
                        copied = false
                    }
                    document.body.removeChild(el)
                }
                proxy.$bkMessage({
                    theme: copied ? 'success' : 'warning',
                    message: copied
                        ? proxy.$t('environment.copiedIpCount', [ips.length])
                        : proxy.$t('environment.copyIpFailed')
                })
            }

            const editPerm = computed(() => ({
                permissionData: {
                    projectId: projectId.value,
                    resourceType: ENV_RESOURCE_TYPE,
                    resourceCode: envHashId.value,
                    action: ENV_RESOURCE_ACTION.EDIT
                }
            }))
            const allowPerm = { hasPermission: true, disablePermissionApi: true }
            const batchGroups = computed(() => {
                const none = !selectedNodesList.value.length
                const checkTip = none ? proxy.$t('environment.pleaseCheckNode') : ''
                const item = (key, text, handler, extra = {}) => ({
                    key,
                    text,
                    disabled: none || !!extra.disabled,
                    tooltips: none ? checkTip : (extra.tooltips || ''),
                    perm: extra.perm || allowPerm,
                    handler
                })
                const groups = []
                if (isBuildEnv.value && !isCreateResType.value) {
                    groups.push({
                        key: 'toggle',
                        label: proxy.$t('environment.batchGroupToggle'),
                        items: [
                            item('enable', proxy.$t('environment.batchEnable'), () => askToggle(selectedNodesList.value, true), { perm: editPerm.value }),
                            item('disable', proxy.$t('environment.batchDisable'), () => askToggle(selectedNodesList.value, false), { perm: editPerm.value })
                        ]
                    })
                }
                groups.push({
                    key: 'copy',
                    label: proxy.$t('environment.batchGroupCopy'),
                    items: [
                        item('copy', proxy.$t('environment.copyIp'), copySelectedIps)
                    ]
                })
                if (!isDynamicEnv.value) {
                    groups.push({
                        key: 'remove',
                        label: proxy.$t('environment.batchGroupRemove'),
                        items: [
                            item('remove', proxy.$t('environment.bulkRemove'), handleBatchRemove, {
                                disabled: isBuiltInEnv.value,
                                tooltips: isBuiltInEnv.value ? proxy.$t('environment.builtInEnvRemoveNodeTips') : '',
                                perm: editPerm.value
                            })
                        ]
                    })
                }
                return groups
            })
            const onBatchItem = (item) => {
                if (item.disabled) return
                item.handler()
            }

            const renderSelectionHeader = (h) => {
                return h('div', {
                    class: 'selection-header',
                    on: {
                        click: (event) => event.stopPropagation()
                    }
                }, [
                    h('bk-checkbox', {
                        props: {
                            value: isPageAllSelected.value,
                            indeterminate: isPageIndeterminate.value
                        },
                        on: {
                            change: toggleCurrentPage
                        }
                    })
                ])
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
                    calculateTableLayout()
                    // 获取节点列表
                    fetchData()
                })
            })
            return {
                // data
                isLoading,
                envNodeList,
                tableSize,
                tableMaxHeight,
                selectedNodesList,
                batchMenuOpen,
                batchGroups,
                isPageAllSelected,
                isPageIndeterminate,
                pagination,
                searchValue,
                searchList,
                filterTips,
                runningStatus,
                successStatus,
                failStatus,
                projectId,
                envHashId,
                isCreateResType,
                isBuildEnv,
                isBuiltInEnv,
                isDynamicEnv,
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
                handleBatchRemove,
                handleRemoveNode,
                isRowSelected,
                toggleRowSelected,
                toggleCurrentPage,
                renderSelectionHeader,
                onBatchItem,
                handleSwitcherClick,
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
        ::v-deep th .cell {
            overflow: visible;
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

    .selection-header {
        display: inline-flex;
        align-items: center;
    }
    ::v-deep .bk-dropdown-menu.medium-font .env-batch-list > li > a {
        height: 32px;
        line-height: 33px;
        font-size: 12px;
    }
    .batch-group-label {
        padding: 6px 16px 0;
        font-size: 12px;
        line-height: 20px;
        color: #979ba5;
        pointer-events: none;
    }
    .batch-group-label.is-follow {
        margin-top: 4px;
        padding-top: 6px;
        border-top: 1px solid #f0f1f5;
    }
    .env-batch-list a.disabled {
        color: #c4c6cc;
        cursor: not-allowed;
    }
    .switcher-wrapper {
        position: relative;
        display: inline-block;

        .switcher-container {
            position: absolute;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            cursor: pointer;
            z-index: 1;
        }
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
.bk-dialog-wrapper.env-toggle-node-dialog {
    .bk-info-box {
        .bk-dialog-type-body {
            padding: 0 24px;
        }
        .bk-dialog-type-header.has-sub-header {
            padding: 24px 24px 0;
        }
        .bk-dialog-type-header .header {
            font-size: 20px;
            line-height: 32px;
            color: #313238;
        }
        .bk-dialog-type-sub-header {
            padding: 16px 24px 0;
            text-align: left;
            line-height: 20px;
        }
        .bk-dialog-type-sub-header .header {
            text-align: left;
            font-size: 12px;
            line-height: 20px;
            color: #63656e;
        }
        .bk-dialog-footer {
            background: #fff;
            border-top: none;
            padding: 24px;
            text-align: center;
        }
        .bk-dialog-footer .footer-wrapper {
            display: inline-block;
        }
        .bk-dialog-footer .footer-wrapper button {
            min-width: 88px;
        }
    }
    .env-toggle-reason {
        font-size: 12px;
        line-height: 20px;
        text-align: left;
    }
    .toggle-will-title,
    .toggle-skip-title {
        margin: 0;
        font-size: 12px;
        line-height: 20px;
    }
    .toggle-will-title {
        color: #313238;
    }
    .toggle-skip-title {
        margin-top: 12px;
        color: #979ba5;
    }
    .toggle-name-list {
        margin: 4px 0 0;
        padding: 0;
        list-style: none;
        overflow: auto;
        li {
            line-height: 22px;
            word-break: break-all;
        }
        &.is-will {
            max-height: 176px;
            color: #313238;
        }
        &.is-skip {
            max-height: 88px;
            color: #979ba5;
        }
    }
    .toggle-enable-label {
        margin: 12px 0 8px;
        color: #63656e;
        .required {
            color: #ea3636;
        }
    }
    .toggle-reason-input {
        display: block;
        width: 100%;
        min-height: 80px;
        box-sizing: border-box;
        padding: 8px 10px;
        border: 1px solid #c4c6cc;
        border-radius: 2px;
        font: inherit;
        color: #63656e;
        resize: vertical;
        background: #fff;
        outline: none;
        &:focus {
            border-color: #3a84ff;
        }
    }
    .toggle-reason-error {
        margin: 8px 0 0;
        color: #ea3636;
        font-size: 12px;
    }
}
</style>
