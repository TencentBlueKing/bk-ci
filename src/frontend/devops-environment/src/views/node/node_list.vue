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
                        <bk-button
                            class="mr10"
                            @click="handleExportCSV"
                        >
                            {{ $t('environment.导出') }}
                        </bk-button>
                    </div>
                    <div class="search-part">
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
                    </div>
                </section>

                <list-table
                    ref="listTable"
                    :node-list="nodeList"
                    :table-loading="tableLoading"
                    :pagination="pagination"
                    :search-value="searchValue"
                    :date-time-range="dateTimeRange"
                    @page-change="handlePageChange"
                    @page-limit-change="handlePageLimitChange"
                    @sort-change="handleSortChange"
                    @refresh="requestList"
                    @updataCurEditNodeItem="updataCurEditNodeItem"
                    @install-agent="installAgent"
                    @clear-filter="clearFilter"
                />
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
    import webSocketMessage from '@/utils/webSocketMessage.js'
    import { NODE_RESOURCE_ACTION, NODE_RESOURCE_TYPE } from '@/utils/permission'
    import SearchSelect from '@blueking/search-select'
    import '@blueking/search-select/dist/styles/index.css'
    import ListTable from './list_table.vue'
    const ENV_NODE_TABLE_LIMIT_CACHE = 'env_node_table_limit_cache'

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
                        name: this.$t('environment.标签'),
                        id: 'label',
                        children: [
                            {
                                id: 'os',
                                name: 'window'
                            }
                        ]
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
                            // nodeType: ,
                            page: this.pagination.current,
                            pageSize: this.pagination.limit
                        }
                        // tags: [
                        //     {
                        //         tagKeyId: 0,
                        //         tagValues: [
                        //             0
                        //         ]
                        //     }
                        // ]
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

            .bk-date-picker.long {
                max-width: 180px;
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
            min-width: 500px;
            max-width: 650px;
            background: #fff;
            margin-right: 10px;
            ::placeholder {
                color: #c4c6cc;
            }
        }
    }
</style>
