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
                        <bk-button
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
                            <span class="import-btn">
                                <i class="devops-icon icon-plus"></i>
                                {{ $t('environment.nodeInfo.importNode') }}
                            </span>
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
                                <li
                                    v-for="item in batchMenuItems"
                                    :key="item.key"
                                >
                                    <a
                                        href="javascript:;"
                                        :class="item.disabled ? 'disabled' : ''"
                                        v-bk-tooltips="{
                                            content: item.tooltips,
                                            disabled: !item.disabled
                                        }"
                                        v-perm="{
                                            permissionData: {
                                                projectId: projectId,
                                                resourceType: NODE_RESOURCE_TYPE,
                                                resourceCode: projectId,
                                                action: NODE_RESOURCE_ACTION.CREATE
                                            }
                                        }"
                                        @click="item.handler"
                                        key="thirdPartyBuildMachine"
                                    >
                                        {{ $t(item.textKey) }}
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
                    @refresh="requestList"
                    @updataCurEditNodeItem="updataCurEditNodeItem"
                    @install-agent="installAgent"
                    @clear-filter="clearFilter"
                    @selected-change="handleSelectedChange"
                />
            </template>
        </section>
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

        <bk-dialog
            v-model="isShowEditMaxConcurrency"
            :width="480"
            :mask-close="false"
            :loading="dialogLoading.isLoading"
            header-position="left"
            ext-cls="max-concurrency"
            :ok-text="$t('environment.save')"
            :title="$t('environment.bulkEditMaxConcurrency')"
            @confirm="handleSetMaxConcurrency"
        >
            <div>
                <p class="tag">
                    <i18n
                        tag="span"
                        path="environment.已选X个节点"
                    >
                        <span class="third">
                            {{ selectNodeCounts.total }}
                        </span>
                    </i18n>
                    <i18n
                        tag="span"
                        v-if="selectNodeCounts.cmdb"
                        path="environment.其中X个非构建节点已忽略"
                    >
                        <span class="ignored blod">
                            {{ selectNodeCounts.cmdb }}
                        </span>
                        <span class="ignored">
                            {{ $t('environment.ignored') }}
                        </span>
                    </i18n>
                </p>
                <div class="content">
                    <div>
                        <p>{{ $t('environment.最大构建并发数') }}</p>
                        <bk-input
                            type="number"
                            size="small"
                            :min="0"
                            :precision="0"
                            :placeholder="$t('environment.保持不变')"
                            v-model="parallelTaskCount"
                        ></bk-input>
                    </div>
                    <div>
                        <p>docker {{ $t('environment.最大构建并发数') }}</p>
                        <bk-input
                            type="number"
                            size="small"
                            :min="0"
                            :precision="0"
                            :placeholder="$t('environment.保持不变')"
                            v-model="dockerParallelTaskCount"
                        ></bk-input>
                    </div>
                </div>
            </div>
        </bk-dialog>
        <bk-dialog
            v-model="isShowResetImportUser"
            :width="480"
            :mask-close="false"
            :loading="dialogLoading.isLoading"
            footer-position="center"
            ext-cls="reset-import-user"
            :title="$t('environment.confirmBulkResetImportUser')"
        >
            <div class="content">
                <div class="top">
                    <i18n
                        v-if="selectNodeCounts.thirdParty || selectNodeCounts.noPermission"
                        tag="p"
                        path="environment.已选X个节点，其中"
                    >
                        <span class="third">
                            {{ selectNodeCounts.total }}
                        </span>
                    </i18n>
                    <i18n
                        v-else
                        tag="p"
                        path="environment.已选X个节点"
                    >
                        <span class="third">
                            {{ selectNodeCounts.total }}
                        </span>
                    </i18n>
                    <i18n
                        v-if="selectNodeCounts.thirdParty"
                        tag="p"
                        path="environment.X个非部署节点已忽略"
                    >
                        <span class="ignored blod">
                            {{ selectNodeCounts.thirdParty }}
                        </span>
                        <span class="ignored">
                            {{ $t('environment.ignored') }}
                        </span>
                    </i18n>
                    <i18n
                        v-if="selectNodeCounts.noPermission"
                        tag="p"
                        path="environment.X个节点你无权限（主备负责人不是你）已忽略"
                    >
                        <span class="ignored blod">
                            {{ selectNodeCounts.noPermission }}
                        </span>
                        <span class="ignored">
                            {{ $t('environment.ignored') }}
                        </span>
                    </i18n>
                </div>
                <div class="bottom">{{ $t('environment.重置后，「作业平台-脚本执行」和「 作业平台-构件分发 」插件运行时将以你的授权执行，请谨慎操作，避免流水线越权。') }}</div>
            </div>
            <div slot="footer">
                <p
                    v-bk-tooltips="{
                        content: $t('environment.没有能导入的节点'),
                        disabled: hasPermissionCMDBCount
                    }"
                    style="display: inline-block;"
                >
                    <bk-button
                        theme="primary"
                        :disabled="!hasPermissionCMDBCount"
                        @click="handleChangeImportUser"
                    >
                        {{ $t('environment.reset') }}
                    </bk-button>
                </p>
                <bk-button
                    @click="cancelFn"
                >
                    {{ $t('environment.cancel') }}
                </bk-button>
            </div>
        </bk-dialog>
    </div>
</template>

<script>
    import thirdConstruct from '@/components/devops/environment/third-construct-dialog'
    import { getQueryString } from '@/utils/util'
    import webSocketMessage from '@/utils/webSocketMessage.js'
    import { NODE_RESOURCE_ACTION, NODE_RESOURCE_TYPE } from '@/utils/permission'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'
    import ListTable from './list_table.vue'
    import { mapState, mapActions } from 'vuex'
    const ENV_NODE_TABLE_LIMIT_CACHE = 'env_node_table_limit_cache'
    import { ENV_ACTIVE_NODE_TYPE, ALLNODE } from '@/store/constants'
    export default {
        components: {
            ListTable,
            thirdConstruct,
            SearchSelect
        },
        data () {
            return {
                NODE_RESOURCE_TYPE,
                NODE_RESOURCE_ACTION,
                ENV_ACTIVE_NODE_TYPE,
                ALLNODE,
                curEditNodeItem: '',
                curEditNodeDisplayName: '',
                nodeIp: '',
                isAgent: false,
                isMultipleBtn: false,
                isEditNodeStatus: false,
                isDropdownShow: false, // 导入菜单
                showContent: false, // 内容显示
                hasPermission: true, // 构建机权限
                showTooltip: false,
                curNodeDialog: '', // 当前弹窗节点
                lastCliCKNode: {},
                nodeList: [], // 节点列表
                gatewayList: [], // 网关列表
                runningStatus: ['CREATING', 'STARTING', 'STOPPING', 'RESTARTING', 'DELETING', 'BUILDING_IMAGE'],
                successStatus: ['NORMAL', 'BUILD_IMAGE_SUCCESS'],
                failStatus: ['ABNORMAL', 'DELETED', 'LOST', 'BUILD_IMAGE_FAILED', 'UNKNOWN', 'RUNNING'],
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
                    autoSwitchAccount: false
                },
                // 构建机信息
                connectNodeDetail: {
                    isConnectNode: false,
                    hostname: '',
                    status: 'UN_IMPORT',
                    os: 'macOS 10.13.4'
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
                pagination: {
                    current: 1,
                    count: 0,
                    limit: Number(localStorage.getItem(ENV_NODE_TABLE_LIMIT_CACHE)) || 10,
                    limitList: [10, 50, 100, 200]
                },
                requestParams: {},
                dateTimeRange: [],
                currentNodeType: '',
                currentTags: [],
                tagSearchValue: [],
                isBatchDropdownShow: false,
                selectedNodes: [],
                reInstallId: '',
                isShowEditMaxConcurrency: false,
                isShowResetImportUser: false,
                parallelTaskCount: 0,
                dockerParallelTaskCount: 0
            }
        },
        computed: {
            ...mapState('environment', ['nodeTagList']),
            projectId () {
                return this.$route.params.projectId
            },
            userInfo () {
                return window.userInfo
            },
            filterData () {
                const data = [
                    {
                        name: this.$t('environment.keywords'),
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
                                name: this.$t('environment.deploy')
                            },
                            {
                                id: 'THIRDPARTY',
                                name: this.$t('environment.build')
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
            },
            batchMenuItems () {
                return [
                    {
                        key: 'thirdPartyBuildMachine',
                        textKey: 'environment.batchSetTag',
                        handler: () => this.batchSetTag()
                    },
                    {
                        key: 'bulkEditMaxConcurrency',
                        textKey: 'environment.bulkEditMaxConcurrency',
                        tooltips: this.$t('environment.未选择构建节点，不支持修改'),
                        disabled: this.selectedNodes.length && this.selectedNodes.every(i => i.nodeType !== 'THIRDPARTY'),
                        handler: () => this.batchSetMaxConcurrency()
                    },
                    {
                        key: 'bulkResetImportUser',
                        textKey: 'environment.bulkResetImportUser',
                        tooltips: this.$t('environment.未选择部署节点，不支持重置'),
                        disabled: this.selectedNodes.length && this.selectedNodes.every(i => i.nodeType !== 'CMDB'),
                        handler: () => this.batchResetImportUser()
                    },
                    {
                        key: 'idcTestMachine',
                        textKey: 'environment.batchDeleteNode',
                        handler: () => this.batchDeleteNode()
                    }
                ]
            },
            username (){
                return window.top.userInfo.username
            },
            selectNodeCounts () {
                const count = this.selectedNodes.reduce(
                    (acc, node) => {
                        acc.total++

                        if (node.nodeType === 'THIRDPARTY') acc.thirdParty++

                        if (node.nodeType === 'CMDB') acc.cmdb++

                        if (node.nodeType === 'CMDB' && !(node.bakOperator?.split(';').includes(this.username) || node.operator === this.username)) {
                            acc.noPermission++
                        }
      
                        return acc
                    },
                    { total: 0, cmdb: 0, thirdParty: 0, noPermission: 0 }
                )
                return count
            },
            hasPermissionCMDBCount () {
                const { total = 0, thirdParty = 0, noPermission = 0 } = this.selectNodeCounts
                return total - thirdParty - noPermission
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
                this.constructImportForm.link = ''
                this.requestDevCommand()
            },
            'constructImportForm.autoSwitchAccount' (val) {
                this.constructImportForm.link = ''
                this.requestDevCommand()
            },
            'constructImportForm.location' (val) {
                if (val) {
                    this.requestDevCommand()
                }
            },
            'constructImportForm.loginPassword' (val) {
                if (!val) {
                    this.constructImportForm.link = ''
                }
            },
            'constructImportForm.loginName' (val) {
                if (!val) {
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
            batchDropdown () {
                this.isBatchDropdownShow = !this.isBatchDropdownShow
            },
            hasSelectedNode (){
                if (!this.selectedNodes.length) {
                    this.$bkMessage({
                        message: this.$t('environment.placeSelectNode'),
                        theme: 'error'
                    })
                    return false
                }
                return true
            },
            batchSetTag () {
                if (!this.hasSelectedNode()) return
                const currentNodeType = this.$route.params.nodeType || ALLNODE
                localStorage.setItem(ENV_ACTIVE_NODE_TYPE, currentNodeType)
                this.$store.commit('environment/setSelectionTagList', this.selectedNodes)
                this.$router.push({
                    name: 'setNodeTag',
                    params: {
                        projectId: this.projectId
                    }
                })
            },
            batchSetMaxConcurrency (){
                if (!this.hasSelectedNode() || this.selectedNodes.every(i => i.nodeType !== 'THIRDPARTY')) return
                this.isShowEditMaxConcurrency = true
            },
            async handleSetMaxConcurrency () {
                try {
                    this.dialogLoading.isLoading = true
                    const {parallelTaskCount, dockerParallelTaskCount} = this
                    const nodeHashIds = this.selectedNodes.filter(node => node.nodeType === 'THIRDPARTY').map(node => node.nodeHashId)
                    const params = {
                        nodeHashIds,
                        parallelTaskCount: Number(parallelTaskCount),
                        dockerParallelTaskCount: Number(dockerParallelTaskCount)
                    }
                    const res = await this.$store.dispatch('environment/batchUpdateParallelTaskCount', {
                        projectId: this.projectId,
                        params
                    })
                    if (res) {
                        this.$bkMessage({
                            message: this.$t('environment.successfullySaved'),
                            theme: 'success'
                        })
                        this.requestList()
                        await this.requestGetCounts(this.projectId)
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'
                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.dialogLoading.isLoading = false
                    this.isShowEditMaxConcurrency = false
                    this.parallelTaskCount = 0
                    this.dockerParallelTaskCount = 0
                }
            },
            batchResetImportUser (){
                if (!this.hasSelectedNode() || this.selectedNodes.every(i => i.nodeType !== 'CMDB')) return
                this.isShowResetImportUser = true
            },
            async handleChangeImportUser () {
                try {
                    this.dialogLoading.isLoading = true
                    const nodeHashIds = this.selectedNodes
                        .filter(node => node.nodeType === 'CMDB' && (node.bakOperator?.split(';').includes(this.username) || node.operator === this.username))
                        .map(node => node.nodeHashId)
                    const res = await this.$store.dispatch('environment/batchChangeImportUser', {
                        projectId: this.projectId,
                        nodeHashIds
                    })
                    if (res) {
                        this.$bkMessage({
                            message: this.$t('environment.successfullyModified'),
                            theme: 'success'
                        })
                        this.requestList()
                        await this.requestGetCounts(this.projectId)
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'
                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.dialogLoading.isLoading = false
                    this.isShowResetImportUser = false
                }
            },
            cancelFn () {
                this.isShowResetImportUser = false
            },
            async batchDeleteNode () {
                if (!this.hasSelectedNode()) return
                this.$bkInfo({
                    title: `${this.$t('environment.deleteNodetips', [this.selectedNodes.length])}`,
                    extCls: 'info-content',
                    theme: 'danger',
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
                if (!this.currentNodeType && this.$route.params.nodeType !== ALLNODE) {
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
                            isMore: i.nodeHashId === this.lastCliCKNode.nodeHashId,
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
            dropdownIsShow (isShow) {
                if (isShow === 'show') {
                    this.isDropdownShow = true
                } else {
                    this.isDropdownShow = false
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
                            const gateway = node.gateway
                            this.constructImportForm.model = node.osName.toUpperCase()
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
                        if (isTarget) {
                            this.constructImportForm.location = isTarget.zoneName
                        }
                    }
                    
                    if (node && ['THIRDPARTY'].includes(node.nodeType)) { // 如果是第三方构建机类型则获取构建机详情以获得安装命令或下载链接
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
                            ),
                            ...(
                                this.isAgent ? {
                                    reInstallId: this.reInstallId
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
                if (['THIRDPARTY'].includes(node.nodeType)) {
                    this.reInstallId = node.agentHashId
                    this.nodeIp = node.ip
                    this.isAgent = true
                    this.constructToolConf.importText = this.$t('environment.confirm')
                    this.switchConstruct(node)
                }
            },
            async toImportNode (type) {
                this.switchConstruct()
            },
            /**
             * 构建机导入节点
             */
            async confirmFn () {
                this.isAgent = false
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
        
            clearFilter () {
                this.$refs.dateTimeRangeRef?.handleClear()
                this.searchValue = []
                this.tagSearchValue = []
                this.currentTags = []
                this.$router.push({ name: 'nodeList', params: { nodeType: ALLNODE } })
            },

            handleToPipelineDetail (param) {
                if (!param.projectId) return
                window.open(`${window.location.origin}/console/pipeline/${param.projectId}/${param.pipelineId}/detail/${param.buildId}/executeDetail`, '_blank')
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

        .create-node-btn {
            margin-right: 6px;
        }

        .prompt-operator,
        .edit-operator {
            padding-right: 10px;
            color: #ffbf00;

            .devops-icon {
                margin-right: 6px;
            }
        }

        .edit-operator {
            cursor: pointer;
        }

        .over-handler {
            max-width: 100px;
            min-width: 90px;
        }

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
        margin: 0 8px;

        .disabled {
            color: #C4C6CC !important;
            cursor: not-allowed;
        }
    }
    
    .info-content {
        .bk-dialog-header-inner {
            white-space: normal !important;
        }
    }
    .max-concurrency {

        .tag {
            display: inline-block;
            padding: 6px 16px;
            background: #F0F1F5;
            font-size: 12px;
            text-align: left;
            border-radius: 16px;

            .third {
                color: #3A84FF;
                font-weight: 700;
            }

            .ignored {
                color: #E38B02;
            }
            
            .blod {
                font-weight: 700;
            }
        }
        .content {
            display: flex;
            gap: 24px;
            justify-content: space-between;
            margin-top: 24px;
            div {
                flex:1;
                p {
                    text-align: left;
                    margin-bottom: 6px;
                    font-size: 12px;
                }
            }
        }
    }
    .reset-import-user {
        .content {
            padding: 12px 16px;
            background-color: #F5F7FA;
            font-size: 14px;
            color: #4D4F56;
            text-align: left;
            .top {
                padding-bottom: 12px;
                .third {
                    color: #3A84FF;
                    font-weight: 700;
                }

                .ignored {
                    color: #E38B02;
                }
                
                .blod {
                    font-weight: 700;
                }

                p {
                    text-align: left;

                }
            }
            .bottom {
                padding-top: 12px;
                border-top: 1px solid #DCDEE5;
            }
        }
    }
</style>
