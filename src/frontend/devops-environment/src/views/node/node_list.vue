<template>
    <div class="node-list-wrapper">
        <section
            class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }"
        >
            <template>
                <section class="filter-bar">
                    <div class="btn-part">
                        <template v-if="isExtendTx">
                            <bk-dropdown-menu
                                trigger="click"
                                class="mr10"
                                :font-size="'medium'"
                                @show="dropdown"
                                @hide="dropdown"
                            >
                                <bk-button
                                    theme="primary"
                                    key="thirdPartyBuildMachine"
                                    slot="dropdown-trigger"
                                >
                                    {{ $t('environment.nodeInfo.importNode') }}
                                    <i :class="['bk-icon icon-angle-down',{ 'icon-flip': isDropdownShow }]"></i>
                                </bk-button>
                                <ul
                                    class="bk-dropdown-list"
                                    slot="dropdown-content"
                                >
                                    <li>
                                        <a
                                            href="javascript:;"
                                            v-perm="{
                                                permissionData: {
                                                    projectId: projectId,
                                                    resourceType: NODE_RESOURCE_TYPE,
                                                    resourceCode: projectId,
                                                    action: NODE_RESOURCE_ACTION.CREATE
                                                }
                                            }"
                                            @click="toImportNode('construct')"
                                            key="thirdPartyBuildMachine"
                                        >
                                            {{ $t('environment.thirdPartyBuildMachine') }}
                                        </a>
                                    </li>
                                    <li>
                                        <a
                                            href="javascript:;"
                                            v-perm="{
                                                permissionData: {
                                                    projectId: projectId,
                                                    resourceType: NODE_RESOURCE_TYPE,
                                                    resourceCode: projectId,
                                                    action: NODE_RESOURCE_ACTION.CREATE
                                                }
                                            }"
                                            theme="primary"
                                            @click="toImportNode('cmdb')"
                                            key="idcTestMachine"
                                        >
                                            {{ $t('environment.nodeInfo.idcTestMachine') }}
                                        </a>
                                    </li>
                                </ul>
                            </bk-dropdown-menu>
                        </template>
                        <bk-button
                            v-else
                            :key="projectId"
                            v-perm="{
                                permissionData: {
                                    projectId: projectId,
                                    resourceType: NODE_RESOURCE_TYPE,
                                    resourceCode: projectId,
                                    action: NODE_RESOURCE_ACTION.CREATE
                                }
                            }"
                            theme="primary"
                            @click="toImportNode('construct')"
                        >
                            {{ $t('environment.nodeInfo.importNode') }}
                        </bk-button>
                        <bk-dropdown-menu
                            trigger="click"
                            ext-cls="batch-menu"
                            :font-size="'medium'"
                            @show="batchDropdown"
                            @hide="batchDropdown"
                        >
                            <bk-button
                                key="batchOperation"
                                slot="dropdown-trigger"
                            >
                                {{ $t('environment.batchOperation') }}
                                <i :class="['bk-icon icon-angle-down',{ 'icon-flip': isBatchDropdownShow }]"></i>
                            </bk-button>
                            <ul
                                class="bk-dropdown-list"
                                slot="dropdown-content"
                            >
                                <li>
                                    <a
                                        href="javascript:;"
                                        v-perm="{
                                            permissionData: {
                                                projectId: projectId,
                                                resourceType: NODE_RESOURCE_TYPE,
                                                resourceCode: projectId,
                                                action: NODE_RESOURCE_ACTION.CREATE
                                            }
                                        }"
                                        @click="batchSetTag"
                                        key="thirdPartyBuildMachine"
                                    >
                                        {{ $t('environment.batchSetTag') }}
                                    </a>
                                </li>
                                <li>
                                    <a
                                        href="javascript:;"
                                        v-perm="{
                                            permissionData: {
                                                projectId: projectId,
                                                resourceType: NODE_RESOURCE_TYPE,
                                                resourceCode: projectId,
                                                action: NODE_RESOURCE_ACTION.CREATE
                                            }
                                        }"
                                        theme="primary"
                                        @click="batchDeleteNode"
                                        key="idcTestMachine"
                                    >
                                        {{ $t('environment.batchDeleteNode') }}
                                    </a>
                                </li>
                            </ul>
                        </bk-dropdown-menu>
                        <bk-button
                            
                            @click="handleExportCSV"
                        >
                            {{ $t('environment.export') }}
                        </bk-button>
                    </div>
                    <div class="search-part">
                        <SearchSelect
                            class="search-input ml15"
                            v-model="searchValue"
                            :placeholder="filterPlaceHolder"
                            :data="filterData"
                            :show-condition="false"
                            clearable
                            key="search"
                        ></SearchSelect>
                        <bk-search-select
                            class="tag-search"
                            v-model="tagSearchValue"
                            :placeholder="$t('environment.pleaseEnterTag')"
                            :data="tagFilterData"
                            :show-condition="false"
                            clearable
                            @change="handleTagChange"
                            @clear="handleClearTagSearch"
                        ></bk-search-select>
                        <bk-date-picker
                            ref="dateTimeRangeRef"
                            v-model="dateTimeRange"
                            :placeholder="$t('environment.selectRecentExecutionTimeRange')"
                            :type="'datetimerange'"
                            @change="handleDateRangeChange"
                        >
                        </bk-date-picker>
                    </div>
                </section>

                <list-table
                    ref="listTable"
                    :node-list="nodeList"
                    :table-loading="tableLoading"
                    :pagination="pagination"
                    :search-value="searchValue"
                    :tag-search-value="tagSearchValue"
                    :date-time-range="dateTimeRange"
                    :node-tag-list="nodeTagList"
                    @page-change="handlePageChange"
                    @page-limit-change="handlePageLimitChange"
                    @sort-change="handleSortChange"
                    @changePageCurrent="changePageCurrent"
                    @refresh="requestList"
                    @updataCurEditNodeItem="updataCurEditNodeItem"
                    @install-agent="installAgent"
                    @clear-filter="clearFilter"
                    @node="handleNode"
                    @showLogDetail="handleShowLogDetail"
                    @reImport="handleReImport"
                    @selected-change="handleSelectedChange"
                />
            </template>
        </section>

        <!-- 导入CMDB -->
        <config-manage-node
            :node-select-conf="cmdbNodeSelectConf"
            @confirm-fn="confirmCmdbFn"
            @cancel-fn="cancelCmdbFn"
        ></config-manage-node>

        <!-- 导入第三方构建机 -->
        <third-construct
            :construct-tool-conf="constructToolConf"
            :construct-import-form="constructImportForm"
            :connect-node-detail="connectNodeDetail"
            :gateway-list="gatewayList"
            :loading="dialogLoading"
            :has-permission="hasPermission"
            :empty-tips-config="emptyTipsConfig"
            :confirm-fn="confirmFn"
            :is-agent="isAgent"
            :node-ip="nodeIp"
            :request-dev-command="requestDevCommand"
        ></third-construct>

        <make-mirror-dialog
            :current-node="createImageNode"
            :make-mirror-conf="makeMirrorConf"
            @cancelMakeMirror="makeMirrorConf.isShow = false"
            @submitMakeMirror="requestList"
        ></make-mirror-dialog>

        <!-- 导入成功、失败提示弹框 -->
        <import-tips-dialog
            ref="importTipsDialog"
            :status="importStatus"
            :message="importMessage"
            :agent-abnormal-nodes-count="agentAbnormalNodesCount"
            :agent-not-install-nodes-count="agentNotInstallNodesCount"
        />

        <!-- 重装/安装Agent -->
        <installAgent
            ref="installAgent"
            v-bind="curNode"
            :task-id.sync="taskId"
            @install="handleInstallEnd"
        />
    </div>
</template>

<script>
    import configManageNode from '@/components/devops/environment/config-manage-node'
    import importTipsDialog from '@/components/devops/environment/import-tips-dialog'
    import installAgent from '@/components/devops/environment/install-agent'
    import makeMirrorDialog from '@/components/devops/environment/make-mirror-dialog'
    import thirdConstruct from '@/components/devops/environment/third-construct-dialog'
    import { NODE_RESOURCE_ACTION, NODE_RESOURCE_TYPE } from '@/utils/permission'
    import { getQueryString } from '@/utils/util'
    import webSocketMessage from '@/utils/webSocketMessage.js'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'
    import { mapActions, mapState } from 'vuex'
    import ListTable from './list_table.vue'
    const ENV_NODE_TABLE_LIMIT_CACHE = 'env_node_table_limit_cache'
    import { ENV_ACTIVE_NODE_TYPE, ALLNODE } from '@/store/constants'
    export default {
        components: {
            thirdConstruct,
            configManageNode,
            makeMirrorDialog,
            importTipsDialog,
            installAgent,
            ListTable,
            SearchSelect
        },
        data () {
            return {
                NODE_RESOURCE_TYPE,
                NODE_RESOURCE_ACTION,
                ENV_ACTIVE_NODE_TYPE,
                ALLNODE,
                curEditNodeItem: '',
                createImageNode: '',
                nodeIp: '',
                isAgent: false,
                isMultipleBtn: false,
                hasPermission: true, // 构建机权限
                showTooltip: false,
                curNodeDialog: '', // 当前弹窗节点
                lastCliCKNode: {},
                nodeList: [], // 节点列表
                allNodeList: [],
                gatewayList: [], // 网关列表
                tableLoading: false,
                // 页面loading
                loading: {
                    isLoading: false,
                    title: this.$t('environment.loadingTitle')
                },
                // 弹窗loading
                dialogLoading: {
                    isLoading: false,
                    title: ''
                },
                emptyInfo: {
                    title: this.$t('environment.nodeInfo.emptyNode'),
                    desc: this.$t('environment.nodeInfo.emptyNodeTips')
                },
                // 构建机弹窗配置
                constructToolConf: {
                    isShow: false,
                    hasHeader: false,
                    quickClose: false,
                    importText: this.$t('environment.import')
                },
                // 构建机内容
                constructImportForm: {
                    model: 'Linux',
                    location: '',
                    link: '',
                    loginName: '',
                    loginPassword: '',
                    installType: 'SERVICE',
                    autoSwitchAccount: true
                },
                // 构建机信息
                connectNodeDetail: {
                    isConnectNode: false,
                    hostname: '',
                    status: 'UN_IMPORT',
                    os: 'macOS 10.13.4'
                },
                // CMDB弹窗配置
                cmdbNodeSelectConf: {
                    isShow: false,
                    quickClose: false,
                    hasHeader: false,
                    unselected: true,
                    importText: '导入'
                },
                makeMirrorConf: {
                    isShow: false
                },
                // 权限配置
                emptyTipsConfig: {
                    title: this.$t('environment.noPermission'),
                    desc: this.$t('environment.nodeInfo.noCreateNodePermissionTips'),
                    btns: [
                        {
                            type: 'primary',
                            size: 'normal',
                            handler: this.changeProject,
                            text: this.$t('environment.switchProject')
                        },
                        {
                            type: 'success',
                            size: 'normal',
                            handler: this.goToApplyPerm,
                            text: this.$t('environment.applyPermission')
                        }
                    ]
                },
                searchValue: [],
                importStatus: 'success',
                importMessage: '',
                agentAbnormalNodesCount: 0,
                agentNotInstallNodesCount: 0,
                pagination: {
                    current: 1,
                    count: 0,
                    limit: Number(localStorage.getItem(ENV_NODE_TABLE_LIMIT_CACHE)) || 10,
                    limitList: [10, 50, 100, 200]
                },
                requestParams: {},
                dateTimeRange: [],
                buildNodes: ['DEVCLOUD', 'THIRDPARTY'], // Build 构建用途的节点 - 第三方构建机类型
                deploymentNodes: ['CC', 'CMDB', 'UNKNOWN', 'OTHER'], // deployment 部署用途的节点
                curNode: {},
                installAgentIp: '',
                installHostId: 0,
                installOsType: '',
                
                taskId: 0, // 查询安装日志 -> 安装Agent任务Id
                currentNodeType: '',
                currentTags: [],
                tagSearchValue: [],
                isDropdownShow: false,
                isBatchDropdownShow: false,
                selectedNodes: []
            }
        },
        computed: {
            ...mapState('environment', ['nodeTagList']),
            projectId () {
                return this.$route.params.projectId
            },
            filterData () {
                const data = [
                    {
                        name: this.$t('environment.关键字'),
                        id: 'keywords',
                        default: true
                    },
                    {
                        name: 'IP',
                        id: 'nodeIp',
                        default: true
                    },
                    {
                        name: this.$t('environment.alias'),
                        id: 'displayName'
                    },
                    {
                        name: this.$t('environment.nodeInfo.os'),
                        id: 'osName'
                    },
                    {
                        name: this.$t('environment.nodeInfo.usage'),
                        id: 'nodeType',
                        children: [
                            {
                                id: 'CMDB',
                                name: this.$t('environment.部署')
                            },
                            {
                                id: 'THIRDPARTY',
                                name: this.$t('environment.构建')
                            }
                        ]
                    },
                    {
                        name: this.$t('environment.nodeInfo.importer'),
                        id: 'createdUser'
                    },
                    {
                        name: this.$t('environment.status'),
                        id: 'nodeStatus',
                        children: [
                            {
                                id: 'NORMAL',
                                name: this.$t('environment.nodeStatusMap.NORMAL')
                            },
                            {
                                id: 'ABNORMAL',
                                name: this.$t('environment.nodeStatusMap.ABNORMAL')
                            },
                            {
                                id: 'NOT_INSTALLED',
                                name: this.$t('environment.nodeStatusMap.NOT_INSTALLED')
                            }
                        ]
                    },
                    {
                        name: this.$t('environment.nodeInfo.agentVersion'),
                        id: 'agentVersion'
                    },
                    {
                        name: this.$t('environment.lastModifier'),
                        id: 'lastModifiedUser'
                    },
                    {
                        name: this.$t('environment.nodeInfo.lastRunPipeline'),
                        id: 'latestBuildPipelineId',
                        remoteMethod:
                            async (search) => {
                                const res = await this.$store.dispatch('environment/getLatestBuildPipelineList', {
                                    projectId: this.projectId
                                })
                                return res.records.map(item => ({
                                    name: item.pipelineName,
                                    id: item.pipelineId
                                }))
                            }
                    }
                ]
                return data.filter(data => {
                    return !this.searchValue.find(val => val.id === data.id)
                })
            },
            tagFilterData () {
                const data = this.nodeTagList.map(item => ({
                    name: item.tagKeyName,
                    id: item.tagKeyId,
                    multiable: true,
                    children: item.tagValues.map(i => ({
                        name: i.tagValueName,
                        id: i.tagValueId
                    }))
                }))
                return data.filter(data => {
                    return !this.tagSearchValue.find(val => val.id === data.id)
                })
            },
            filterPlaceHolder () {
                return this.filterData.map(item => item.name).join(' / ')
            },
            installModeAsService () {
                return this.constructImportForm.installType === 'SERVICE'
            }
        },
        watch: {
            projectId: function () {
                this.$router.push({ name: 'envList' })
            },
            nodeTagList: {
                immediate: true,
                handler () {
                    this.syncCurrentTags()
                }
            },
            '$route.params.nodeType' (newVal) {
                if (newVal) {
                    this.handleNodeTypeChange()
                }
            },
            // 构建机型变化
            'constructImportForm.model' (val) {
                if (val && !this.isAgent) {
                    this.constructImportForm.link = ''
                    this.constructImportForm.location = ''
                    this.requestGateway()
                }
            },
            'constructImportForm.installType' (val) {
                if (!this.isAgent) {
                    this.constructImportForm.link = ''
                    this.requestDevCommand()
                }
            },
            'constructImportForm.autoSwitchAccount' (val) {
                if (!this.isAgent) {
                    this.constructImportForm.link = ''
                    this.requestDevCommand()
                }
            },
            'constructImportForm.location' (val) {
                if (val && !this.isAgent) {
                    this.requestDevCommand()
                }
            },
            'constructImportForm.loginPassword' (val) {
                if (!val && !this.isAgent) {
                    this.constructImportForm.link = ''
                }
            },
            'constructImportForm.loginName' (val) {
                if (!val && !this.isAgent) {
                    this.constructImportForm.link = ''
                }
            },
            searchValue (val) {
                if (val.length) {
                    val.forEach(i => {
                        if (i.values) {
                            this.requestParams[i.id] = i.values[0].id.trim()
                        } else {
                            this.requestParams[i.id] = i.id
                        }
                    })
                    this.pagination.current = 1
                } else {
                    this.requestParams = {}
                }
                this.requestList(this.requestParams)
            }
        },
        created () {
            const urlParams = getQueryString('type')
            if (urlParams) {
                this.constructImportForm.model = urlParams
                this.toImportNode('construct')
            }

            webSocketMessage.installWsMessage(this.requestList)
            this.$once('hook:beforeDestroy', webSocketMessage.unInstallWsMessage)
        },
        async mounted () {
            await this.init()
        },
        methods: {
            ...mapActions('environment', ['requestGetCounts']),
            dropdown () {
                this.isDropdownShow = !this.isDropdownShow
            },
            batchDropdown () {
                this.isBatchDropdownShow = !this.isBatchDropdownShow
            },
            batchSetTag () {
                if (!this.selectedNodes.length) {
                    this.$bkMessage({
                        message: this.$t('environment.placeSelectNode'),
                        theme: 'error'
                    })
                } else {
                    const currentNodeType = this.$route.params.nodeType || ALLNODE
                    localStorage.setItem(ENV_ACTIVE_NODE_TYPE, currentNodeType)
                    this.$store.commit('environment/setSelectionTagList', this.selectedNodes)
                    this.$router.push({
                        name: 'setNodeTag',
                        params: {
                            projectId: this.projectId
                        }
                    })
                }
            },
            async batchDeleteNode () {
                if (!this.selectedNodes.length) {
                    this.$bkMessage({
                        message: this.$t('environment.placeSelectNode'),
                        theme: 'error'
                    })
                    return
                }
                this.$bkInfo({
                    title: `${this.$t('environment.deleteNodetips', [this.selectedNodes.length])}`,
                    extCls: 'info-content',
                    confirmFn: async () => {
                        try {
                            const params = this.selectedNodes.map(i=>i.nodeHashId)
                            await this.$store.dispatch('environment/toDeleteNode', {
                                projectId: this.projectId,
                                params
                            })

                            this.$bkMessage({
                                message: this.$t('environment.successfullyDeleted'),
                                theme: 'success'
                            })
                        } catch (err) {
                            console.log(err)
                        } finally {
                            this.requestList()
                            await this.requestGetCounts(this.projectId)
                        }
                    }
                })
            },
            findTagByValueId (tagValueId) {
                if (!this.nodeTagList?.length) return []
                
                for (const tagGroup of this.nodeTagList) {
                    const foundTag = tagGroup.tagValues?.find(tag => String(tag.tagValueId) === String(tagValueId))
                    if (foundTag) {
                        this.tagSearchValue = [{
                            id: tagGroup.tagKeyId,
                            name: tagGroup.tagKeyName,
                            values: [{
                                id: foundTag.tagValueId,
                                name: foundTag.tagValueName
                            }]
                        }]
                        return [{
                            tagKeyId: tagGroup.tagKeyId,
                            tagValues: [foundTag.tagValueId]
                        }]
                    }
                }
                return []
            },
            syncCurrentTags () {
                if (!this.$route.params.nodeType) return
            
                const nodeType = this.$route.params.nodeType
                if (['allNode', 'THIRDPARTY', 'CMDB'].includes(nodeType)) {
                    this.currentNodeType = nodeType !== ALLNODE ? nodeType : ''
                    this.currentTags = []
                } else {
                    this.currentTags = this.findTagByValueId(nodeType)
                    this.currentNodeType = ''
                }
            },
            handleTagChange (val) {
                if (val.length) {
                    const tags = val.map(item => {
                        return {
                            tagKeyId: item.id,
                            tagValues: item.values.map(value => value.id)
                        }
                    })
                    this.currentTags = tags
                    this.pagination.current = 1
                } else {
                    this.syncCurrentTags()
                }
                this.requestList(this.requestParams)
            },
            async handleNodeTypeChange () {
                this.tagSearchValue = []
                await this.syncCurrentTags()
                await this.requestList()
            },

            handleClearTagSearch () {
                this.tagSearchValue = []
                this.currentTags = []
                if (!this.currentNodeType) {
                    this.$router.push({ name: 'nodeList', params: { nodeType: ALLNODE } })
                } else {
                    this.requestList()
                }
            },

            async init () {
                const {
                    loading
                } = this

                loading.isLoading = true
                loading.title = this.$t('environment.loadingTitle')

                try {
                    await this.syncCurrentTags()
                    setTimeout(() => {
                        this.requestList()
                    }, 500)
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                    }, 1000)
                }
            },
            /**
             * 节点列表
             */
            async requestList (params = this.requestParams) {
                try {
                    this.tableLoading = true
                    const res = await this.$store.dispatch('environment/requestNodeList', {
                        projectId: this.projectId,
                        params: {
                            ...params,
                            ...(this.currentNodeType ? { nodeType: this.currentNodeType } : {}),
                            page: this.pagination.current,
                            pageSize: this.pagination.limit
                        },
                        ...(this.currentTags.length ? { tags: this.currentTags } : {})
                    })
                    this.pagination.count = res.count
                    this.nodeList = res.records.map(i => {
                        return {
                            isEnableEdit: i.nodeHashId === this.curEditNodeItem,
                            ...i
                        }
                    })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'
                    this.nodeList = []
                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.tableLoading = false
                }
            },
            changeProject () {
                this.$toggleProjectMenu(true)
            },
            goToApplyPerm () {
                this.handleNoPermission({
                    projectId: this.projectId,
                    resourceType: NODE_RESOURCE_TYPE,
                    resourceCode: this.projectId,
                    action: NODE_RESOURCE_ACTION.CREATE
                })
            },
            changePageCurrent () {
                // 最后一页最后一条删除后，往前翻一页
                if (
                    this.pagination.limit * (this.pagination.current - 1) + 1 === this.pagination.count
                ) {
                    this.pagination.current -= 1
                }
            },
            updataCurEditNodeItem (item) {
                this.curEditNodeItem = item
            },
            
            /**
             * 是否启动了构建机
             */
            async switchConstruct (node) {
                let message, theme
                this.dialogLoading.isLoading = true

                try {
                    const res = await this.$store.dispatch('environment/hasConstructPermission', {
                        projectId: this.projectId
                    })

                    if (res) {
                        this.constructToolConf.isShow = true
                        if (node) {
                            const gateway = node.nodeType === 'DEVCLOUD' ? 'shenzhen' : node.gateway
                            this.constructImportForm.model = node.nodeType === 'DEVCLOUD' ? 'LINUX' : node.osName.toUpperCase()
                            this.requestGateway(gateway, node)
                        } else {
                            this.constructImportForm.model = 'LINUX'
                            this.requestGateway()
                        }
                    } else {
                        message = this.$t('environment.nodeInfo.grayscalePublicBeta')
                        theme = 'warning'

                        this.$bkMessage({
                            message,
                            theme
                        })
                    }

                    this.dialogLoading.isLoading = false
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            /**
             * 获取网关列表
             */
            async requestGateway (gateway, node) {
                try {
                    this.gatewayList = await this.$store.dispatch('environment/requestGateway', {
                        projectId: this.projectId,
                        model: this.constructImportForm.model
                    })
                    this.constructImportForm.location = this.gatewayList[0]?.zoneName

                    if (this.gatewayList.length && gateway && gateway === 'shenzhen') {
                        this.constructImportForm.location = 'shenzhen'
                    } else if (this.gatewayList.length && gateway && gateway !== 'shenzhen') {
                        const isTarget = this.gatewayList.find(item => item.showName === gateway)
                        this.constructImportForm.location = isTarget && isTarget.zoneName
                    }

                    if (node && this.buildNodes.includes(node.nodeType)) { // 如果是第三方构建机类型则获取构建机详情以获得安装命令或下载链接
                        this.getVmBuildDetail(node.nodeHashId)
                    } else {
                        this.requestDevCommand()
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    if (err.httpStatus === 403) {
                        this.hasPermission = false
                    } else {
                        this.$bkMessage({
                            message,
                            theme
                        })
                    }
                }
            },
            /**
             * 生成链接
             */
            async requestDevCommand () {
                const { location, model, loginName, loginPassword, autoSwitchAccount, installType } = this.constructImportForm
                if (!location && this.gatewayList.length) return
                // 当OS为Windows时，生成安装命令的条件
                // 1. 如果 installType 为 SERVICE, autoSwitchAccount 为 true 时，需填写 loginName, loginPassword 才可获取生成安装命令
                // 2. 如果 installType 为 SERVICE, autoSwitchAccount 为 false 时, 直接获取生成安装命令
                // 3. 如果 installType 为 TASK 时, 直接获取生成安装命令
                if (model === 'WINDOWS') {
                    if (this.installModeAsService && autoSwitchAccount && (!loginName || !loginPassword)) return
                }
                this.dialogLoading.isLoading = true

                try {
                    const res = await this.$store.dispatch('environment/requestDevCommand', {
                        projectId: this.projectId,
                        model: model,
                        params: {
                            zoneName: location,
                            ...(
                                model === 'WINDOWS' ? {
                                    installType,
                                } : {}
                            ),
                            ...(
                                model === 'WINDOWS' && autoSwitchAccount && this.installModeAsService
                                    ? {
                                        loginName,
                                        loginPassword
                                    }
                                    : {}
                            )
                        }
                    })

                    this.constructImportForm.link = res
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    if (err.httpStatus === 403) {
                        this.hasPermission = false
                    } else {
                        this.$bkMessage({
                            message,
                            theme
                        })
                    }
                } finally {
                    this.dialogLoading.isLoading = false
                }
            },
            async getVmBuildDetail (nodeHashId) {
                try {
                    const res = await this.$store.dispatch('environment/requestNodeDetail', {
                        projectId: this.projectId,
                        nodeHashId
                    })
                    if (res.os === 'WINDOWS' && res.agentUrl) {
                        this.constructImportForm.link = res.agentUrl
                        this.constructImportForm.agentId = res.agentId
                    } else if (['MACOS', 'LINUX'].includes(res.os) && res.agentScript) {
                        this.constructImportForm.link = res.agentScript
                        this.constructImportForm.agentId = res.agentId
                    } else {
                        this.requestDevCommand()
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            installAgent (node) {
                if (this.buildNodes.includes(node.nodeType)) {
                    this.nodeIp = node.ip
                    this.isAgent = true
                    this.constructToolConf.importText = this.$t('environment.confirm')
                    this.switchConstruct(node)
                } else if (this.deploymentNodes.includes(node.nodeType)) {
                    this.curNode = node
                    this.$refs.installAgent.isShow = true
                }
            },

            handleShowLogDetail (node) {
                this.curNode = node
                this.taskId = node.taskId
                this.$refs.installAgent.isShow = true
            },

            async toImportNode (type) {
                if (type === 'cmdb') {
                    this.cmdbNodeSelectConf.isShow = true
                } else {
                    this.switchConstruct()
                }
            },
            /**
             * 构建机导入节点
             */
            async confirmFn () {
                this.dialogLoading.isLoading = false
                this.dialogLoading.isShow = false
                this.constructToolConf.isShow = false
                this.constructImportForm.link = ''
                this.constructImportForm.loginName = ''
                this.constructImportForm.loginPassword = ''
                this.constructImportForm.autoSwitchAccount = true
                this.constructImportForm.installType = 'SERVICE'
                this.constructToolConf.importText = this.$t('environment.import')
                this.requestList()
                await this.requestGetCounts(this.projectId)
            },
            async destoryNode (node) {
                const h = this.$createElement
                const content = h('p', {
                    style: {
                        textAlign: 'center'
                    }
                }, `${this.$t('environment.nodeInfo.destoryNode')}？`)

                this.$bkInfo({
                    theme: 'warning',
                    type: 'warning',
                    subHeader: content,
                    confirmFn: async () => {
                        let message, theme
                        try {
                            await this.$store.dispatch('environment/toDestoryNode', {
                                projectId: this.projectId,
                                nodeHashId: node.nodeHashId
                            })

                            message = this.$t('environment.successfullySubmited')
                            theme = 'success'
                        } catch (err) {
                            message = err.message ? err.message : err
                            theme = 'error'
                        } finally {
                            this.$bkMessage({
                                message,
                                theme
                            })
                            this.requestList()
                        }
                    }
                })
            },
            makeImage (node) {
                this.createImageNode = node
                // this.showCreateImage = true
                this.makeMirrorConf.isShow = true
            },
            handleNode (name, canUse, node) {
                if (canUse) {
                    switch (name) {
                        case 'destory':
                            this.destoryNode(node)
                            break
                        case 'makeImage':
                            this.makeImage(node)
                            break
                        default:
                            break
                    }
                }
            },
            async confirmCmdbFn ({ theme, message, agentAbnormalNodesCount, agentNotInstallNodesCount }) {
                this.importStatus = theme
                this.importMessage = message
                this.agentAbnormalNodesCount = agentAbnormalNodesCount
                this.agentNotInstallNodesCount = agentNotInstallNodesCount
                this.$refs.importTipsDialog.isShow = true
                this.cmdbNodeSelectConf.isShow = false
                if (this.$route.params.nodeType === 'CMDB') {
                    this.requestList()
                } else {
                    this.$router.push({
                        name: 'nodeList',
                        params: {
                            nodeType: 'CMDB'
                        }
                    })
                }
                await this.requestGetCounts(this.projectId)
            },
            cancelCmdbFn () {
                this.cmdbNodeSelectConf.isShow = false
            },
            handleSelectedChange (selection) {
                this.selectedNodes = selection
            },
            handlePageChange (page) {
                this.pagination.current = page
                this.requestList(this.requestParams)
            },
            
            handlePageLimitChange (limit) {
                localStorage.setItem(ENV_NODE_TABLE_LIMIT_CACHE, limit)
                this.pagination.current = 1
                this.pagination.limit = limit
                this.requestList(this.requestParams)
            },
            handleSortChange ({ column, prop, order }) {
                const orderMap = {
                    ascending: 'ASC',
                    descending: 'DESC'
                }
                this.pagination.current = 1
                this.requestParams.sortType = prop
                this.requestParams.collation = orderMap[order]
                this.requestList()
            },
            formatTime (date) {
                try {
                    return +new Date(date)
                } catch (e) {
                    return ''
                }
            },
            handleDateRangeChange (value) {
                const startTime = this.formatTime(value[0])
                const endTime = this.formatTime(value[1])
                if (startTime && endTime) {
                    this.requestParams.latestBuildTimeStart = startTime
                    this.requestParams.latestBuildTimeEnd = endTime
                } else {
                    delete this.requestParams.latestBuildTimeStart
                    delete this.requestParams.latestBuildTimeEnd
                }
                this.pagination.current = 1
                this.requestList()
            },
            async handleExportCSV () {
                try {
                    const res = await this.$store.dispatch('environment/exportNodeListCSV', {
                        projectId: this.projectId,
                        params: this.requestParams
                    })
                    this.downloadCsv(res)
                } catch (e) {
                    console.error(e)
                }
            },
            downloadCsv (response) {
                if (!response) return
                const csvContent = response.split(',')
                const bom = new Uint8Array([0xEF, 0xBB, 0xBF])
                const blob = new Blob([bom, csvContent], { type: 'text/csv;charset=utf-8;' })
                const link = document.createElement('a')
                link.href = URL.createObjectURL(blob)
                link.download = 'data.csv'
                document.body.appendChild(link)
                link.click()
                document.body.removeChild(link)
            },
            handleReImport (row) {
                const params = []
                params.push({
                    nodeIp: row.ip,
                    nodeId: row.nodeId
                })
                const confirmFn = async () => {
                    let theme, message, agentAbnormalNodesCount, agentNotInstallNodesCount
                    try {
                        const res = await this.$store.dispatch('environment/reImportCmdbNode', {
                            projectId: this.projectId,
                            params
                        })
                        agentAbnormalNodesCount = res.agentAbnormalNodesCount
                        agentNotInstallNodesCount = res.agentNotInstallNodesCount
                        theme = 'success'
                        await this.confirmCmdbFn({ theme, message, agentAbnormalNodesCount, agentNotInstallNodesCount })
                    } catch (e) {
                        theme = 'error'
                        message = e.message || e
                    }
                }
                this.$bkInfo({
                    title: this.$t('environment.确认重新导入节点吗？', [row.ip]),
                    okText: this.$t('environment.confirm'),
                    cancelText: this.$t('environment.cancel'),
                    confirmFn
                })
            },
            clearFilter () {
                this.$refs.dateTimeRangeRef?.handleClear()
                this.searchValue = []
                this.tagSearchValue = []
                this.currentTags = []
                this.$router.push({ name: 'nodeList', params: { nodeType: ALLNODE } })
            },
            handleInstallEnd () {
                this.requestList(this.requestParams)
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf';

    %flex {
        display: flex;
        align-items: center;
    }

    .node-list-wrapper {
        height: 100%;
        overflow: hidden;

        .sub-view-port {
            margin: 24px;
        }

        .prompt-operator,
        .edit-operator {
            padding-right: 10px;
            color: #ffbf00;

            .devops-icon {
                margin-right: 4px;
            }
        }

        .dashboard-entry {
            color: #3c96ff;
            line-height: 32px;
            cursor: pointer;
            a {
                font-size: 14px;
                color: #3c96ff;
            }
        }

        .edit-operator {
            cursor: pointer;
        }

        // .over-handler {
        //     max-width: 100px;
        //     min-width: 90px;
        // }

        .node-reset,
        .node-handle,
        .install-btn {
            color: $primaryColor;
            cursor: pointer;

            .icon-angle-down {
                display: inline-block;
                margin-left: 4px;
                transition: all ease 0.2s;

                &.icon-flip {
                    transform: rotate(180deg);
                }
            }
        }

        .more-handle {
            position: relative;
        }

        .install-btn {
            margin-left: 4px;
        }

        .node-reset {
            margin-right: 10px;
        }

        .normal-status-node {
            color: #30D878;
        }

        .abnormal-status-node {
            color: $failColor;
        }
    }

    .node-reset-info {
        .bk-dialog-body {
            padding: 15px 55px;
            color: $fontWeightColor;
        }
    }

    .filter-bar {
        display: flex;
        align-items: center;
        justify-content: space-between;

        .btn-part {
            display: flex;
        }

        .search-part {
            display: flex;
            flex: 1;
            justify-content: end;

            .bk-date-picker.long {
                max-width: 170px;
            }
        }
        
        .devops-icon.icon-plus {
            vertical-align: middle;
            font-size: 11px;
            margin-right: 8px;
        }
        .import-btn {
            display: flex;
            align-items: center;
        }
        .search-input {
            width: 50%;
            background: #fff;
            margin-right: 10px;
            ::placeholder {
                color: #c4c6cc;
            }
        }
        .tag-search {
            width: 140px;
            margin-right: 10px;
        }
    }

    .batch-menu {
        margin-right: 8px;
    }

    .info-content {
        .bk-dialog-header-inner {
            white-space: normal !important;
        }
    }
</style>
