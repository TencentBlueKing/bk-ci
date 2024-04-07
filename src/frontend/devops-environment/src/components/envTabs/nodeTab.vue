<template>
    <div class="node-content-wrapper">
        <div class="node-content-header">
            <bk-button
                v-perm="{
                    hasPermission: curEnvDetail.canEdit,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId: projectId,
                        resourceType: ENV_RESOURCE_TYPE,
                        resourceCode: envHashId,
                        action: ENV_RESOURCE_ACTION.EDIT
                    }
                }"
                theme="primary" @click="importNewNode"
            >
                {{ $t('environment.import') }}
            </bk-button>
        </div>

        <div class="node-table">
            <bk-table
                v-bkloading="{ isLoading: tableLoading }"
                ref="shareDiaglogTable"
                :data="curNodeList"
                :pagination="pagination"
                @page-change="handlePageChange"
                @page-limit-change="handlePageLimitChange"
            >
                <bk-table-column :label="$t('environment.envInfo.name')" width="150" prop="displayName" show-overflow-tooltip></bk-table-column>
                <bk-table-column :width="150" label="IP" prop="ip" show-overflow-tooltip></bk-table-column>
                <bk-table-column :label="`${$t('environment.nodeInfo.source')}/${$t('environment.nodeInfo.importer')}`" show-overflow-tooltip>
                    <template slot-scope="props">
                        <span class="node-name">{{ props.row.nodeType }}</span>
                        <span>({{ props.row.createdUser }})</span>
                    </template>
                </bk-table-column>
                <bk-table-column :width="150" :label="$t('environment.nodeInfo.os')" prop="osName"></bk-table-column>
                <bk-table-column :label="$t('environment.nodeInfo.cpuStatus')">
                    <template slot-scope="props">
                        <div class="status-cell">
                            <StatusIcon v-if="successStatus.includes(props.row.nodeStatus)" status="success" />
                            <StatusIcon v-else-if="failStatus.includes(props.row.nodeStatus)" status="error" />
                            <StatusIcon v-else-if="['NOT_INSTALLED'].includes(props.row.nodeStatus)" status="normal" />
                                
                            <div v-else-if="runningStatus.includes(props.row.nodeStatus)"
                                class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary loading-icon"
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
                            <span class="node-status">{{ $t('environment.nodeStatusMap')[props.row.nodeStatus] || props.row.nodeStatus }}</span>
                        </div>
                    </template>
                </bk-table-column>
                <bk-table-column :width="180" :label="$t('environment.operation')">
                    <template slot-scope="props">
                        <span
                            v-perm="{
                                hasPermission: curEnvDetail.canEdit,
                                disablePermissionApi: true,
                                permissionData: {
                                    projectId: projectId,
                                    resourceType: ENV_RESOURCE_TYPE,
                                    resourceCode: envHashId,
                                    action: ENV_RESOURCE_ACTION.EDIT
                                }
                            }"
                            class="node-delete delete-node-text"
                            @click.stop="confirmDelete(props.row)"
                        >{{ $t('environment.remove') }}</span>
                    </template>
                </bk-table-column>
            </bk-table>
        </div>
        <node-select :node-select-conf="nodeSelectConf"
            :search-info="searchInfo"
            :cur-user-info="curUserInfo"
            :row-list="importNodeList"
            :change-created-user="changeCreatedUser"
            :select-handler-conf="selectHandlerConf"
            :confirm-fn="confirmFn"
            :toggle-all-select="toggleAllSelect"
            :loading="nodeDialogLoading"
            :cancel-fn="cancelFn"
            :query="query">
        </node-select>
        
    </div>
</template>

<script>
    import nodeSelect from '@/components/devops/environment/node-select-dialog'
    import StatusIcon from '@/components/status-icon.vue'
    import { ENV_RESOURCE_ACTION, ENV_RESOURCE_TYPE } from '@/utils/permission'
    export default {
        name: 'node-tab',
        components: {
            nodeSelect,
            StatusIcon
        },
        props: {
            projectId: {
                type: String,
                required: true
            },
            envHashId: {
                type: String,
                required: true
            },
            curEnvDetail: {
                type: Object,
                default: () => ({})
            },
            requestEnvDetail: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                ENV_RESOURCE_TYPE,
                ENV_RESOURCE_ACTION,
                timer: null,
                loading: {
                    isLoading: false,
                    title: ''
                },
                importNodeList: [], // 导入的节点
                nodeList: [], // 环境的节点
                nodeDialogLoading: {
                    isLoading: false,
                    title: ''
                },
                // 搜索节点
                searchInfo: {
                    search: ''
                },
                // 选择节点
                selectHandlerConf: {
                    curTotalCount: 0,
                    curDisplayCount: 0,
                    selectedNodeCount: 0,
                    allNodeSelected: false,
                    searchEmpty: false
                },
                // 节点选择弹窗
                nodeSelectConf: {
                    isShow: false,
                    quickClose: false,
                    hasHeader: false,
                    unselected: true,
                    importText: this.$t('environment.import')
                },
                pagination: {
                    current: 1,
                    count: 0,
                    limit: 10
                },
                tableLoading: false,
                runningStatus: ['CREATING', 'RUNNING', 'STARTING', 'STOPPING', 'RESTARTING', 'DELETING', 'BUILDING_IMAGE'],
                successStatus: ['NORMAL', 'BUILD_IMAGE_SUCCESS'],
                failStatus: ['ABNORMAL', 'DELETED', 'LOST', 'BUILD_IMAGE_FAILED', 'UNKNOWN', 'NOT_IN_CC', 'NOT_IN_CMDB']
            }
        },
        computed: {
            curUserInfo () {
                return window.userInfo
            },
            curNodeList () {
                const { limit, current } = this.pagination
                return this.nodeList.slice(limit * (current - 1), limit * current)
            }
        },
        watch: {
            importNodeList: {
                deep: true,
                handler: function (val) {
                    let curCount = 0
                    const isSelected = this.importNodeList.some(item => {
                        return item.isChecked === true && !item.isEixtEnvNode
                    })

                    if (isSelected) {
                        this.nodeSelectConf.unselected = false
                    } else {
                        this.nodeSelectConf.unselected = true
                    }

                    this.importNodeList.forEach(item => {
                        if (item.isChecked && !item.isEixtEnvNode) curCount++
                    })

                    this.selectHandlerConf.selectedNodeCount = curCount
                    this.decideToggle()
                }
            }
        },
        mounted () {
            this.init()
        },
        beforeDestroy () {
            clearTimeout(this.timer)
        },
        methods: {
            /**
             * 导入节点
             */
            async importEnvNode (nodeArr) {
                let message, theme
                const params = []

                this.nodeDialogLoading.isLoading = true
                this.nodeSelectConf.importText = `${this.$t('environment.nodeInfo.importing')}...`

                nodeArr.forEach(item => {
                    params.push(item)
                })

                try {
                    await this.$store.dispatch('environment/importEnvNode', {
                        projectId: this.projectId,
                        envHashId: this.envHashId,
                        params: params
                    })

                    message = this.$t('environment.successfullyImported')
                    theme = 'success'
                    this.$bkMessage({
                        message,
                        theme
                    })
                } catch (e) {
                    this.handleError(
                        e,
                        {
                            projectId: this.projectId,
                            resourceType: ENV_RESOURCE_TYPE,
                            resourceCode: this.envHashId,
                            action: ENV_RESOURCE_ACTION.EDIT
                        }
                    )
                } finally {
                    this.nodeSelectConf.isShow = false
                    this.nodeDialogLoading.isLoading = false
                    this.nodeSelectConf.importText = this.$t('environment.import')
                    this.requestList()
                    this.requestEnvDetail()
                }
            },

            importNewNode () {
                this.searchInfo = {
                    search: ''
                }
                this.nodeSelectConf.isShow = true
                this.requestNodeList()
            },
            /**
             * 获取环境节点列表
             */
            async requestList () {
                try {
                    this.tableLoading = true
                    const res = await this.$store.dispatch('environment/requestEnvNodeList', {
                        projectId: this.projectId,
                        envHashId: this.envHashId,
                        params: {
                            page: -1
                        }
                    })

                    this.tableLoading = false
                    this.nodeList = res.records
                    this.pagination.count = res.count

                    if (this.importNodeList.length) {
                        this.nodeList.forEach(vv => {
                            this.importNodeList.forEach(kk => {
                                if (vv.nodeHashId === kk.nodeHashId) {
                                    kk.isChecked = true
                                    kk.isEixtEnvNode = true
                                }
                            })
                        })
                    }

                    if (this.nodeList.length) {
                        this.loopCheck()
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
            async init () {
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
             *  轮询环境的节点列表状态
             */
            async loopCheck () {
                const {
                    timer
                } = this
                let res

                clearTimeout(timer)

                for (let i = 0; i < this.nodeList.length; i++) {
                    const target = this.nodeList[i]
                    if (target.nodeType === this.$t('environment.BCSVirtualMachine') && (target.nodeStatus === 'ABNORMAL'
                        || this.runningStatus.includes(target.nodeStatus)
                        || target.nodeStatus === 'UNKNOWN'
                        || !target.agentStatus)) {
                        res = true
                        break
                    } else {
                        res = false
                    }
                }

                if (res) {
                    this.timer = setTimeout(async () => {
                        await this.requestList()
                    }, 10000)
                }
            },
            /**
             * 获取弹窗节点列表
             */
            async requestNodeList () {
                this.nodeDialogLoading.isLoading = true

                try {
                    const res = await this.$store.dispatch('environment/requestNodeList', {
                        projectId: this.projectId,
                        envHashId: this.envHashId,
                        params: {
                            page: -1
                        }
                    })

                    this.importNodeList.splice(0, this.importNodeList.length)

                    res.records.forEach(item => {
                        item.isChecked = false
                        item.isDisplay = true
                        this.importNodeList.push(item)
                    })

                    this.importNodeList.forEach(kk => {
                        this.nodeList.forEach(vv => {
                            if (vv.nodeHashId === kk.nodeHashId) {
                                kk.isChecked = true
                                kk.isEixtEnvNode = true
                            }
                        })

                        if (this.curEnvDetail.envType === 'BUILD') {
                            if (kk.nodeType !== 'THIRDPARTY' || !kk.canUse) {
                                kk.isDisplay = false
                            }
                        } else {
                            if (kk.nodeType === 'THIRDPARTY' || !kk.canUse) {
                                kk.isDisplay = false
                            }
                        }
                    })

                    let curCount = 0

                    this.importNodeList.forEach(item => {
                        if (item.isDisplay) curCount++
                    })

                    this.selectHandlerConf.curTotalCount = curCount

                    const result = this.importNodeList.some(element => {
                        return element.isDisplay
                    })

                    if (result) {
                        this.selectHandlerConf.searchEmpty = false
                    } else {
                        this.selectHandlerConf.searchEmpty = true
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.nodeDialogLoading.isLoading = false
                }
            },
            
            /**
             * 删除节点
             */
            async confirmDelete (row) {
                const params = []
                const id = row.nodeHashId

                params.push(id)

                const h = this.$createElement
                const content = h('p', {
                    style: {
                        textAlign: 'center'
                    }
                }, `${this.$t('environment.nodeInfo.removeNodetips', [row.displayName])}？`)

                this.$bkInfo({
                    title: this.$t('environment.remove'),
                    subHeader: content,
                    confirmFn: async () => {
                        let message, theme
                        try {
                            await this.$store.dispatch('environment/toDeleteEnvNode', {
                                projectId: this.projectId,
                                envHashId: this.envHashId,
                                params: params
                            })

                            message = this.$t('environment.successfullyDeleted')
                            theme = 'success'
                            this.$bkMessage({
                                message,
                                theme
                            })
                        } catch (e) {
                            this.handleError(
                                e,
                                {
                                    projectId: this.projectId,
                                    resourceType: ENV_RESOURCE_TYPE,
                                    resourceCode: this.envHashId,
                                    action: ENV_RESOURCE_ACTION.EDIT
                                }
                            )
                        } finally {
                            this.requestList()
                        }
                    }
                })
            },
            /**
             * 弹窗全选联动
             */
            decideToggle () {
                let curCount = 0
                let curCheckCount = 0

                this.importNodeList.forEach(item => {
                    if (item.isDisplay) {
                        curCount++
                        if (item.isChecked) curCheckCount++
                    }
                })

                this.selectHandlerConf.curDisplayCount = curCount

                if (curCount === curCheckCount) {
                    this.selectHandlerConf.allNodeSelected = true
                } else {
                    this.selectHandlerConf.allNodeSelected = false
                }
            },
            /**
             * 节点全选
             */
            toggleAllSelect (value) {
                this.selectHandlerConf.allNodeSelected = value
                if (this.selectHandlerConf.allNodeSelected) {
                    this.importNodeList.forEach(item => {
                        if (item.isDisplay && !item.isEixtEnvNode) {
                            item.isChecked = true
                        }
                    })
                } else {
                    this.importNodeList.forEach(item => {
                        if (item.isDisplay && !item.isEixtEnvNode) {
                            item.isChecked = false
                        }
                    })
                }
            },
            /**
             * 搜索节点
             */
            query (target) {
                if (target.length) {
                    target.filter(item => {
                        return item && item.length
                    })
                    this.importNodeList.forEach(item => {
                        const str = item.ip

                        if (this.curEnvDetail.envType === 'BUILD') {
                            for (let i = 0; i < target.length; i++) {
                                if (target[i] && str === target[i] && item.nodeType === 'THIRDPARTY' && item.canUse) {
                                    item.isDisplay = true
                                    break
                                } else {
                                    item.isDisplay = false
                                }
                            }
                        } else {
                            for (let i = 0; i < target.length; i++) {
                                if (target[i] && str === target[i] && item.nodeType !== 'THIRDPARTY' && item.canUse) {
                                    item.isDisplay = true
                                    break
                                } else {
                                    item.isDisplay = false
                                }
                            }
                        }
                    })

                    const result = this.importNodeList.some(element => {
                        return element.isDisplay
                    })

                    if (result) {
                        this.selectHandlerConf.searchEmpty = false
                    } else {
                        this.selectHandlerConf.searchEmpty = true
                    }
                } else {
                    this.selectHandlerConf.searchEmpty = false

                    if (this.curEnvDetail.envType === 'BUILD') {
                        this.importNodeList.forEach(item => {
                            if (item.nodeType === 'THIRDPARTY' && item.canUse) {
                                item.isDisplay = true
                            }
                        })
                    } else {
                        this.importNodeList.forEach(item => {
                            if (item.nodeType !== 'THIRDPARTY' && item.canUse) {
                                item.isDisplay = true
                            }
                        })
                    }
                }

                this.decideToggle()
            },
            
            confirmFn () {
                if (!this.nodeDialogLoading.isLoading) {
                    const nodeArr = []

                    this.importNodeList.forEach(item => {
                        if (item.isChecked && !item.isEixtEnvNode) {
                            nodeArr.push(item.nodeHashId)
                        }
                    })

                    this.importEnvNode(nodeArr)
                }
            },
            cancelFn () {
                if (!this.nodeDialogLoading.isLoading) {
                    this.nodeSelectConf.isShow = false
                    this.selectHandlerConf.searchEmpty = false
                    this.nodeSelectConf.importText = this.$t('environment.import')
                }
            },
            async changeCreatedUser (id) {
                this.$bkInfo({
                    title: this.$t('environment.nodeInfo.modifyImporter'),
                    subTitle: this.$t('environment.nodeInfo.modifyOperatorTips'),
                    confirmFn: async () => {
                        let message, theme
                        try {
                            await this.$store.dispatch('environment/changeCreatedUser', {
                                projectId: this.projectId,
                                nodeHashId: id
                            })

                            message = this.$t('environment.successfullyModified')
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
            handlePageChange (page) {
                this.pagination.current = page
            },
            handlePageLimitChange (limit) {
                this.pagination.current = 1
                this.pagination.limit = limit
            }
        }
    }
</script>

<style lang="scss">
    .node-table {
        margin-top: 24px;
        height: calc(95% - 32px);
        overflow: auto;
    }
    .loading-icon {
        display: inline-flex;
        margin-right: 5px;
    }
</style>
