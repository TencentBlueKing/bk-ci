<template>
    <div class="node-list-wrapper">
        <content-header class="env-header">
            <div slot="left">{{ $t('environment.node') }}</div>
            <div
                slot="right"
                v-if="nodeList.length > 0"
            >
                <bk-button
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
            </div>
        </content-header>
        <section
            class="sub-view-port"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }"
        >
            <template>
                <section class="filter-bar">
                    <SearchSelect
                        class="search-input"
                        v-model="searchValue"
                        :placeholder="filterPlaceHolder"
                        :data="filterData"
                        :show-condition="false"
                        clearable
                    ></SearchSelect>
                    <bk-date-picker
                        ref="dateTimeRangeRef"
                        v-model="dateTimeRange"
                        :placeholder="$t('environment.选择最近执行时间范围')"
                        :type="'datetimerange'"
                        @change="handleDateRangeChange"
                    >
                    </bk-date-picker>
                    <bk-button
                        class="export-btn"
                        @click="handleExportCSV"
                    >
                        {{ $t('environment.导出') }}
                    </bk-button>
                </section>
                <bk-table
                    v-bkloading="{ isLoading: tableLoading }"
                    :size="tableSize"
                    class="node-table-wrapper"
                    row-class-name="node-item-row"
                    :data="nodeList"
                    :pagination="pagination"
                    @page-change="handlePageChange"
                    @page-limit-change="handlePageLimitChange"
                    @sort-change="handleSortChange"
                >
                    <bk-table-column
                        :label="$t('environment.nodeInfo.displayName')"
                        sortable
                        prop="displayName"
                    >
                        <template slot-scope="props">
                            <div
                                class="bk-form-content node-item-content"
                                v-if="props.row.isEnableEdit"
                            >
                                <div class="edit-content">
                                    <input
                                        type="text"
                                        class="bk-form-input env-name-input"
                                        maxlength="30"
                                        name="nodeName"
                                        v-validate="'required'"
                                        v-model="curEditNodeDisplayName"
                                        :class="{ 'is-danger': errors.has('nodeName') }"
                                    >
                                    <div class="handler-btn">
                                        <span
                                            class="edit-base save"
                                            @click="saveEdit(props.row)"
                                        >{{ $t('environment.save') }}</span>
                                        <span
                                            class="edit-base cancel"
                                            @click="cancelEdit(props.row.nodeHashId)"
                                        >{{ $t('environment.cancel') }}</span>
                                    </div>
                                </div>
                            </div>
                            <div
                                class="table-node-item node-item-id"
                                v-else
                            >
                                <span
                                    v-perm="canShowDetail(props.row) ? {
                                        hasPermission: props.row.canView,
                                        disablePermissionApi: true,
                                        permissionData: {
                                            projectId: projectId,
                                            resourceType: NODE_RESOURCE_TYPE,
                                            resourceCode: props.row.nodeHashId,
                                            action: NODE_RESOURCE_ACTION.VIEW
                                        }
                                    } : {}"
                                    class="node-name"
                                    :class="{ 'pointer': canShowDetail(props.row), 'useless': !canShowDetail(props.row) || !props.row.canUse }"
                                    :title="props.row.displayName"
                                    @click="toNodeDetail(props.row)"
                                >
                                    {{ props.row.displayName || '-' }}
                                </span>
                                <span
                                    v-perm="{
                                        hasPermission: props.row.canEdit,
                                        disablePermissionApi: true,
                                        permissionData: {
                                            projectId: projectId,
                                            resourceType: NODE_RESOURCE_TYPE,
                                            resourceCode: props.row.nodeHashId,
                                            action: NODE_RESOURCE_ACTION.EDIT
                                        }
                                    }"
                                >
                                    <i
                                        class="devops-icon icon-edit"
                                        v-if="!isEditNodeStatus"
                                        @click="editNodeName(props.row)"
                                    ></i>
                                </span>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        label="IP"
                        sortable
                        prop="nodeIp"
                        min-width="80"
                    >
                        <template slot-scope="props">
                            {{ props.row.ip || '-' }}
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        v-if="allRenderColumnMap.os"
                        sortable
                        :label="$t('environment.nodeInfo.os')"
                        prop="osName"
                    >
                        <template slot-scope="props">
                            {{ props.row.osName || '-' }}
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        v-if="allRenderColumnMap.nodeStatus"
                        :label="`${$t('environment.status')}(${$t('environment.version')})`"
                        sortable
                        width="180"
                        prop="nodeStatus"
                    >
                        <template slot-scope="props">
                            <div
                                class="table-node-item node-item-status"
                                v-if="props.row.nodeStatus === 'BUILDING_IMAGE'"
                            >
                                <span class="node-status-icon normal-stutus-icon"></span>
                                <span class="node-status">{{ $t('environment.nodeInfo.normal') }}</span>
                            </div>
                            <div class="table-node-item node-item-status">
                                <!-- 状态icon -->
                                <span
                                    class="node-status-icon normal-stutus-icon"
                                    v-if="successStatus.includes(props.row.nodeStatus)"
                                ></span>
                                <span
                                    class="node-status-icon abnormal-stutus-icon"
                                    v-if="failStatus.includes(props.row.nodeStatus)"
                                >
                                </span>
                                <div
                                    class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary"
                                    v-if="runningStatus.includes(props.row.nodeStatus)"
                                >
                                    <div class="rotate rotate1"></div>
                                    <div class="rotate rotate2"></div>
                                    <div class="rotate rotate3"></div>
                                    <div class="rotate rotate4"></div>
                                    <div class="rotate rotate5"></div>
                                    <div class="rotate rotate6"></div>
                                    <div class="rotate rotate7"></div>
                                    <div class="rotate rotate8"></div>
                                </div>
                                <!-- 状态值 -->
                                <span
                                    class="install-agent"
                                    v-if="props.row.nodeStatus === 'RUNNING'"
                                    @click="installAgent(props.row)"
                                >
                                    {{ $t('environment.nodeStatusMap')[props.row.nodeStatus] }}
                                </span>
                                <span
                                    class="node-status"
                                    v-else
                                >
                                    {{ $t('environment.nodeStatusMap')[props.row.nodeStatus] || props.row.nodeStatus }}
                                </span>
                                <div
                                    class="install-agent"
                                    v-if="['THIRDPARTY'].includes(props.row.nodeType) && props.row.nodeStatus === 'ABNORMAL'"
                                    @click="installAgent(props.row)"
                                >
                                    {{ `（${$t('environment.install')}Agent）` }}
                                </div>
                                <span v-if="props.row.agentVersion">
                                    ({{ props.row.agentVersion }})
                                </span>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        v-if="allRenderColumnMap.usage"
                        :label="$t('environment.nodeInfo.usage')"
                        prop="usage"
                        min-width="80"
                        show-overflow-tooltip
                    >
                        <template slot-scope="props">
                            {{ usageMap[props.row.nodeType] || '-' }}
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        v-if="allRenderColumnMap.createdUser"
                        :label="$t('environment.nodeInfo.importer')"
                        sortable
                        prop="createdUser"
                        min-width="80"
                        show-overflow-tooltip
                    ></bk-table-column>
                    <bk-table-column
                        v-if="allRenderColumnMap.lastModifyBy"
                        :label="$t('environment.lastModifier')"
                        sortable
                        prop="lastModifiedUser"
                        min-width="80"
                        show-overflow-tooltip
                    ></bk-table-column>
                    <bk-table-column
                        v-if="allRenderColumnMap.lastModifyTime"
                        :label="$t('environment.nodeInfo.lastModifyTime')"
                        :width="180"
                        sortable
                        prop="lastModifiedTime"
                        min-width="80"
                        show-overflow-tooltip
                    >
                        <template slot-scope="props">
                            {{ props.row.lastModifyTime || '-' }}
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        v-if="allRenderColumnMap.latestBuildPipeline"
                        :label="$t('environment.nodeInfo.lastRunPipeline')"
                        :width="180"
                        sortable
                        prop="latestBuildPipelineId"
                        show-overflow-tooltip
                    >
                        <template slot-scope="props">
                            <span
                                class="pipeline-name"
                                @click="handleToPipelineDetail(props.row.latestBuildDetail)"
                            >
                                {{ props.row?.latestBuildDetail?.pipelineName }}
                            </span>
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        v-if="allRenderColumnMap.latestBuildTime"
                        :width="180"
                        :label="$t('environment.nodeInfo.lastRunAs')"
                        prop="latestBuildTime"
                        sortable
                        min-width="80"
                        show-overflow-tooltip
                    >
                        <template slot-scope="props">
                            {{ props.row.lastBuildTime || '--' }}
                        </template>
                    </bk-table-column>
                    <bk-table-column
                        :label="$t('environment.operation')"
                        width="160"
                    >
                        <template slot-scope="props">
                            <template v-if="props.row.canUse">
                                <div class="table-node-item node-item-handler">
                                    <span
                                        v-if="!['TSTACK'].includes(props.row.nodeType)"
                                        v-perm="{
                                            hasPermission: props.row.canDelete,
                                            disablePermissionApi: true,
                                            permissionData: {
                                                projectId: projectId,
                                                resourceType: NODE_RESOURCE_TYPE,
                                                resourceCode: props.row.nodeHashId,
                                                action: NODE_RESOURCE_ACTION.DELETE
                                            }
                                        }"
                                        class="node-handle delete-node-text"
                                        @click.stop="confirmDelete(props.row, index)"
                                    >
                                        {{ $t('environment.delete') }}
                                    </span>
                                </div>
                            </template>
                            <template v-else>
                                <bk-button
                                    v-if="!['TSTACK'].includes(props.row.nodeType)"
                                    theme="primary"
                                    outline
                                    @click="handleApplyPermission(props.row)"
                                >
                                    {{ $t('environment.applyPermission') }}
                                </bk-button>
                            </template>
                        </template>
                    </bk-table-column>
                    <bk-table-column type="setting">
                        <bk-table-setting-content
                            :fields="tableColumn"
                            :selected="selectedTableColumn"
                            :size="tableSize"
                            @setting-change="handleSettingChange"
                        />
                    </bk-table-column>
                    <template #empty>
                        <EmptyTableStatus
                            :type="(searchValue.length || !!dateTimeRange[1]) ? 'search-empty' : 'empty'"
                            @clear="clearFilter"
                        />
                    </template>
                </bk-table>
            </template>
        </section>
        <third-construct
            :construct-tool-conf="constructToolConf"
            :construct-import-form="constructImportForm"
            :connect-node-detail="connectNodeDetail"
            :gateway-list="gatewayList"
            :loading="dialogLoading"
            :requet-construct-node="requetConstructNode"
            :has-permission="hasPermission"
            :empty-tips-config="emptyTipsConfig"
            :confirm-fn="confirmFn"
            :cancel-fn="cancelFn"
            :is-agent="isAgent"
            :node-ip="nodeIp"
        ></third-construct>
    </div>
</template>

<script>
    import thirdConstruct from '@/components/devops/environment/third-construct-dialog'
    import { getQueryString } from '@/utils/util'
    import webSocketMessage from '../utils/webSocketMessage.js'
    import { NODE_RESOURCE_ACTION, NODE_RESOURCE_TYPE } from '@/utils/permission'
    import EmptyTableStatus from '@/components/empty-table-status'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'
    const NODE_TABLE_COLUMN_CACHE = 'node_list_columns'
    const ENV_NODE_TABLE_LIMIT_CACHE = 'env_node_table_limit_cache'
    export default {
        components: {
            thirdConstruct,
            SearchSelect,
            EmptyTableStatus
        },
        data () {
            return {
                NODE_RESOURCE_TYPE,
                NODE_RESOURCE_ACTION,
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
                    link: ''
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
                selectedTableColumn: [],
                tableSize: 'small',
                searchValue: [],
                pagination: {
                    current: 1,
                    count: 0,
                    limit: Number(localStorage.getItem(ENV_NODE_TABLE_LIMIT_CACHE)) || 10,
                    limitList: [10, 50, 100, 200]
                },
                requestParams: {},
                dateTimeRange: []
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            userInfo () {
                return window.userInfo
            },
            allRenderColumnMap () {
                return this.selectedTableColumn.reduce((result, item) => {
                    result[item.id] = true
                    return result
                }, {})
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
                        name: this.$t('environment.nodeInfo.usage'),
                        id: 'nodeUsage',
                        children: [
                            {
                                id: 'DEPLOY',
                                name: this.$t('environment.部署')
                            },
                            {
                                id: 'BUILD',
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
                                console.log(search)
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
            filterPlaceHolder () {
                return this.filterData.map(item => item.name).join(' / ')
            },
            usageMap () {
                return {
                    DEVCLOUD: this.$t('environment.构建'),
                    THIRDPARTY: this.$t('environment.构建'),
                    CC: this.$t('environment.部署'),
                    CMDB: this.$t('environment.部署'),
                    UNKNOWN: this.$t('environment.部署'),
                    OTHER: this.$t('environment.部署')
                }
            }
        },
        watch: {
            projectId: async function (val) {
                this.$router.push({ name: 'envList' })
            },
            // 构建机型变化
            'constructImportForm.model' (val) {
                if (val && !this.isAgent) {
                    this.constructImportForm.link = ''
                    this.constructImportForm.location = ''
                    this.requestGateway()
                }
            },
            'constructImportForm.location' (val) {
                if (val && !this.isAgent) {
                    this.requestDevCommand()
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
            this.tableColumn = [
                {
                    id: 'displayName',
                    label: this.$t('environment.nodeInfo.displayName'),
                    disabled: true
                },
                {
                    id: 'ip',
                    label: 'IP',
                    disabled: true
                },
                {
                    id: 'os',
                    label: this.$t('environment.nodeInfo.os')
                },
                {
                    id: 'nodeStatus',
                    label: this.$t('environment.status')
                },
                {
                    id: 'usage',
                    label: this.$t('environment.nodeInfo.usage')
                },
                {
                    id: 'createdUser',
                    label: this.$t('environment.nodeInfo.importer')
                },
                {
                    id: 'lastModifyBy',
                    label: this.$t('environment.nodeInfo.lastModifyBy')
                },
                {
                    id: 'lastModifyTime',
                    label: this.$t('environment.nodeInfo.lastModifyTime')
                },
                {
                    id: 'latestBuildPipeline',
                    label: this.$t('environment.nodeInfo.lastRunPipeline')
                },
                {
                    id: 'latestBuildTime',
                    label: this.$t('environment.nodeInfo.lastRunAs')
                }

            ]
            const columnsCache = JSON.parse(localStorage.getItem(NODE_TABLE_COLUMN_CACHE))
            if (columnsCache) {
                this.selectedTableColumn = Object.freeze(columnsCache.columns)
                this.tableSize = columnsCache.size
            } else {
                this.selectedTableColumn = Object.freeze([
                    { id: 'displayName' },
                    { id: 'ip' },
                    { id: 'os' },
                    { id: 'nodeStatus' },
                    { id: 'usage' },
                    { id: 'createdUser' },
                    { id: 'lastModifyBy' },
                    { id: 'lastModifyTime' },
                    { id: 'latestBuildPipeline' },
                    { id: 'latestBuildTime' }
                ])
            }

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
            async init () {
                const {
                    loading
                } = this

                loading.isLoading = true
                loading.title = this.$t('environment.loadingTitle')

                try {
                    this.requestList()
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
                            page: this.pagination.current,
                            pageSize: this.pagination.limit
                        }
                    })

                    this.nodeList.splice(0, this.nodeList.length)
                    this.pagination.count = res.count
                    res.records.forEach(item => {
                        item.isEnableEdit = item.nodeHashId === this.curEditNodeItem
                        item.isMore = item.nodeHashId === this.lastCliCKNode.nodeHashId
                        this.nodeList.push(item)
                    })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

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
            toNodeDetail (node) {
                if (this.canShowDetail(node)) {
                    this.$router.push({
                        name: 'nodeDetail',
                        params: {
                            nodeHashId: node.nodeHashId
                        }
                    })
                }
            },
            handleApplyPermission (node) {
                this.handleNoPermission({
                    projectId: this.projectId,
                    resourceType: NODE_RESOURCE_TYPE,
                    resourceCode: node.nodeHashId,
                    action: NODE_RESOURCE_ACTION.USE
                })
            },
            /**
             * 删除节点
             */
            async confirmDelete (row, index) {
                const params = []
                const id = row.nodeHashId

                params.push(id)

                this.$bkInfo({
                    theme: 'warning',
                    type: 'warning',
                    title: this.$t('environment.delete'),
                    subTitle: `${this.$t('environment.nodeInfo.deleteNodetips', [row.displayName])}`,
                    confirmFn: async () => {
                        let message, theme
                        try {
                            await this.$store.dispatch('environment/toDeleteNode', {
                                projectId: this.projectId,
                                params
                            })

                            message = this.$t('environment.successfullyDeleted')
                            theme = 'success'

                            message && this.$bkMessage({
                                message,
                                theme
                            })
                        } catch (e) {
                            this.handleError(
                                e,
                                {
                                    projectId: this.projectId,
                                    resourceType: NODE_RESOURCE_TYPE,
                                    resourceCode: row.nodeHashId,
                                    action: NODE_RESOURCE_ACTION.DELETE
                                }
                            )
                        } finally {
                            this.requestList()
                        }
                    }
                })
            },
            /**
             * 构建机信息
             */
            async requetConstructNode () {
                this.dialogLoading.isLoading = true

                try {
                    const res = await this.$store.dispatch('environment/requetConstructNode', {
                        projectId: this.projectId,
                        agentId: this.constructImportForm.agentId
                    })

                    this.connectNodeDetail = Object.assign({}, res)
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.dialogLoading.isLoading = false
                }
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
                    const res = await this.$store.dispatch('environment/requestGateway', {
                        projectId: this.projectId,
                        model: this.constructImportForm.model
                    })

                    this.gatewayList.splice(0, this.gatewayList.length)
                    res.forEach(item => {
                        this.gatewayList.push(item)
                    })

                    if (this.gatewayList.length && gateway && gateway === 'shenzhen') {
                        this.constructImportForm.location = 'shenzhen'
                    } else if (this.gatewayList.length && gateway && gateway !== 'shenzhen') {
                        const isTarget = this.gatewayList.find(item => item.showName === gateway)
                        this.constructImportForm.location = isTarget && isTarget.zoneName
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
                if (!this.constructImportForm.location && this.gatewayList.length) return

                this.dialogLoading.isLoading = true

                try {
                    const res = await this.$store.dispatch('environment/requestDevCommand', {
                        projectId: this.projectId,
                        model: this.constructImportForm.model,
                        zoneName: this.constructImportForm.location || undefined
                    })

                    this.constructImportForm.link = res.link
                    this.constructImportForm.agentId = res.agentId
                    this.requetConstructNode()
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
                        this.requetConstructNode()
                    } else if (['MACOS', 'LINUX'].includes(res.os) && res.agentScript) {
                        this.constructImportForm.link = res.agentScript
                        this.constructImportForm.agentId = res.agentId
                        this.requetConstructNode()
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
                    this.nodeIp = node.ip
                    this.isAgent = true
                    this.constructToolConf.importText = this.$t('environment.comfirm')
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
                if (!this.dialogLoading.isLoading) {
                    this.dialogLoading.isLoading = true
                    this.constructToolConf.importText = this.constructToolConf.importText === this.$t('environment.comfirm') ? `${this.$t('environment.nodeInfo.submitting')}...` : `${this.$t('environment.nodeInfo.importing')}...`

                    let message, theme

                    try {
                        await this.$store.dispatch('environment/importConstructNode', {
                            projectId: this.projectId,
                            agentId: this.constructImportForm.agentId
                        })

                        message = this.constructToolConf.importText === `${this.$t('environment.submitting')}...` ? this.$t('environment.successfullySubmited') : this.$t('environment.successfullyImported')
                        theme = 'success'
                        this.$bkMessage({
                            message,
                            theme
                        })
                        this.constructToolConf.isShow = false
                    } catch (e) {
                        this.handleError(
                            e,
                            {
                                projectId: this.projectId,
                                resourceType: NODE_RESOURCE_TYPE,
                                resourceCode: this.projectId,
                                action: NODE_RESOURCE_ACTION.CREATE
                            }
                        )
                    } finally {
                        this.dialogLoading.isLoading = false
                        this.dialogLoading.isShow = false
                        this.constructToolConf.importText = this.$t('environment.import')
                        this.requestList()
                    }
                }
            },
            cancelFn () {
                if (!this.dialogLoading.isShow) {
                    this.isAgent = false
                    this.constructToolConf.isShow = false
                    this.dialogLoading.isShow = false
                    this.constructImportForm.link = ''
                    this.constructImportForm.location = ''
                    this.constructToolConf.importText = this.$t('environment.import')
                }
            },
            editNodeName (node) {
                this.curEditNodeDisplayName = node.displayName
                this.isEditNodeStatus = true
                this.curEditNodeItem = node.nodeHashId
                this.nodeList.forEach(val => {
                    if (val.nodeHashId === node.nodeHashId) {
                        val.isEnableEdit = true
                    }
                })
            },
            async saveEdit (node) {
                const valid = await this.$validator.validate()
                const displayName = this.curEditNodeDisplayName.trim()
                if (valid) {
                    let message, theme
                    const params = {
                        displayName
                    }

                    try {
                        await this.$store.dispatch('environment/updateDisplayName', {
                            projectId: this.projectId,
                            nodeHashId: node.nodeHashId,
                            params
                        })

                        message = this.$t('environment.successfullyModified')
                        theme = 'success'
                    } catch (e) {
                        this.handleError(
                            e,
                            {
                                projectId: this.projectId,
                                resourceType: NODE_RESOURCE_TYPE,
                                resourceCode: node.nodeHashId,
                                action: NODE_RESOURCE_ACTION.EDIT
                            }
                        )
                    } finally {
                        if (theme === 'success') {
                            message && this.$bkMessage({
                                message,
                                theme
                            })
                            this.nodeList.forEach(val => {
                                if (val.nodeHashId === node.nodeHashId) {
                                    val.isEnableEdit = false
                                    val.displayName = this.curEditNodeDisplayName
                                }
                            })
                            this.isEditNodeStatus = false
                            this.curEditNodeItem = ''
                            this.curEditNodeDisplayName = ''
                            this.requestList()
                        }
                    }
                }
            },
            cancelEdit (nodeId) {
                this.isEditNodeStatus = false
                this.curEditNodeItem = ''
                this.curEditNodeDisplayName = ''
                this.nodeList.forEach(val => {
                    if (val.nodeHashId === nodeId) {
                        val.isEnableEdit = false
                    }
                })
            },
            canShowDetail (row) {
                return row.nodeType === 'THIRDPARTY'
            },
            handleSettingChange ({ fields, size }) {
                this.selectedTableColumn = Object.freeze(fields)
                this.tableSize = size
                localStorage.setItem(NODE_TABLE_COLUMN_CACHE, JSON.stringify({
                    columns: fields,
                    size
                }))
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
                    descending: 'DES'
                }
                this.pagination.current = 1
                this.requestParams.sortType = prop
                this.requestParams.collation = orderMap[order]
                this.requestList()
            },
        
            clearFilter () {
                this.$refs.dateTimeRangeRef?.handleClear()
                this.searchValue = []
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
    @import './../scss/conf';

    %flex {
        display: flex;
        align-items: center;
    }

    .node-list-wrapper {
        min-width: 1126px;
        height: 100%;
        overflow: hidden;

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

        .node-status-icon {
            display: inline-block;
            margin-left: 2px;
            width: 10px;
            height: 10px;
            border: 2px solid #30D878;
            border-radius: 50%;
            -webkit-border-radius: 50%;
        }

        .abnormal-stutus-icon {
            border-color: $failColor;
        }

        .delete-node-text {
            position: relative;
            padding-right: 9px;
        }

        .normal-status-node {
            color: #30D878;
        }

        .abnormal-status-node {
            color: $failColor;
        }

        .node-item-content {
            position: absolute;
            top: 12px;
            display: flex;
            width: 90%;
            min-width: 280px;
            margin-right: 12px;
            z-index: 2;
            .edit-content {
                display: flex;
                width: 100%;
            }
            .bk-form-input {
                height: 30px;
                font-size: 12px;
                min-width: 280px;
                padding-right: 74px;
            }
            .error-tips {
                font-size: 12px;
            }
            .handler-btn {
                display: flex;
                align-items: center;
                margin-left: 10px;
                position: absolute;
                right: 11px;
                top: 8px;
                .edit-base {
                    cursor: pointer;
                }
                .save {
                    margin-right: 8px;
                }
            }
            .is-danger {
                border-color: #ff5656;
                background-color: #fff4f4;
                
            }
        }

        .node-item-id {
            display: flex;
        }

        .node-table-wrapper {
            margin-top: 20px;
            td:first-child {
                position: relative;
                color: $primaryColor;
                .node-name {
                    line-height: 14px;
                    display: inline-block;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                }
                .pointer {
                    cursor: pointer;
                }
                .useless {
                  color: $fontLigtherColor;
                }
                .icon-edit {
                    position: relative;
                    left: 4px;
                    color: $fontColor;
                    cursor: pointer;
                    display: none;
                }
                &:hover {
                    .icon-edit {
                        display: inline-block;
                    }
                }
            }

            .th-handler,
            td:last-child {
                padding-right: 30px;
            }

            td:last-child {
                cursor: pointer;
            }

            .edit-node-item {
                width: 24%;
            }

            .node-item-row {
              &.node-row-useless {
                cursor: url('../images/cursor-lock.png'), auto;
                color: $fontLigtherColor;
                .node-count-item {
                  color: $fontLigtherColor;
                }
              }
            }

            .install-agent {
                color: $primaryColor;
                cursor: pointer;
            }
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
        .search-input {
            width: 680px;
            background: #fff;
            margin-right: 10px;
            ::placeholder {
                color: #c4c6cc;
            }
        }
        .export-btn {
            margin-left: 10px;
        }
    }
    .pipeline-name {
        cursor: pointer;
        &:hover {
            color: $primaryColor;
        }
    }
</style>
