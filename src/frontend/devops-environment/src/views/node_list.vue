<template>
    <div class="node-list-wrapper">
        <content-header class="env-header">
            <div slot="left">{{ $t('environment.node') }}</div>
            <div slot="right" v-if="nodeList.length > 0">
                <bk-button
                    theme="primary"
                    class="import-vmbuild-btn"
                    @click="toImportNode('construct')">
                    {{ $t('environment.nodeInfo.importNode') }}
                </bk-button>
            </div>
        </content-header>
        <section class="sub-view-port" v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
            <bk-table v-if="showContent && nodeList.length"
                size="medium"
                class="node-table-wrapper"
                :row-class-name="getRowCls"
                :data="nodeList">
                <bk-table-column :label="$t('environment.nodeInfo.displayName')" prop="displayName">
                    <template slot-scope="props">
                        <div class="bk-form-content node-item-content" v-if="props.row.isEnableEdit">
                            <div class="edit-content">
                                <input type="text" class="bk-form-input env-name-input"
                                    maxlength="30"
                                    name="nodeName"
                                    v-validate="'required'"
                                    v-model="curEditNodeDisplayName"
                                    :class="{ 'is-danger': errors.has('nodeName') }">
                                <div class="handler-btn">
                                    <span class="edit-base save" @click="saveEdit(props.row)">{{ $t('environment.save') }}</span>
                                    <span class="edit-base cancel" @click="cancelEdit(props.row.nodeHashId)">{{ $t('environment.cancel') }}</span>
                                </div>
                            </div>
                        </div>
                        <div class="table-node-item node-item-id" v-else>
                            <span class="node-name"
                                :class="{ 'pointer': canShowDetail(props.row), 'useless': !canShowDetail(props.row) || !props.row.canUse }"
                                :title="props.row.displayName"
                                @click="toNodeDetail(props.row)"
                            >{{ props.row.displayName || '-' }}</span>
                            <i class="devops-icon icon-edit" v-if="!isEditNodeStatus && props.row.canEdit" @click="editNodeName(props.row)"></i>
                        </div>
                    </template>
                </bk-table-column>
                <bk-table-column :label="`${$t('environment.nodeInfo.intranet')}IP`" prop="ip" min-width="80">
                    <template slot-scope="props">
                        {{ props.row.ip || '-' }}
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('environment.nodeInfo.os')" prop="osName">
                    <template slot-scope="props">
                        {{ props.row.osName || '-' }}
                    </template>
                </bk-table-column>
                <bk-table-column :label="`${$t('environment.nodeInfo.source')}/${$t('environment.nodeInfo.importer')}`" prop="createdUser" min-width="120">
                    <template slot-scope="props">
                        <div>
                            <span class="node-name">{{ $t('environment.nodeTypeMap')[props.row.nodeType] || '-' }}</span>
                            <span>({{ props.row.createdUser }})</span>
                        </div>
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('environment.status')" prop="nodeStatus">
                    <template slot-scope="props">
                        <div class="table-node-item node-item-status"
                            v-if="props.row.nodeStatus === 'BUILDING_IMAGE'">
                            <span class="node-status-icon normal-stutus-icon"></span>
                            <span class="node-status">{{ $t('environment.nodeInfo.normal') }}</span>
                        </div>
                        <div class="table-node-item node-item-status">
                            <!-- 状态icon -->
                            <span class="node-status-icon normal-stutus-icon" v-if="successStatus.includes(props.row.nodeStatus)"></span>
                            <span class="node-status-icon abnormal-stutus-icon"
                                v-if="failStatus.includes(props.row.nodeStatus)">
                            </span>
                            <div class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary"
                                v-if="runningStatus.includes(props.row.nodeStatus)">
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
                            <span class="install-agent"
                                v-if="props.row.nodeStatus === 'RUNNING'"
                                @click="installAgent(props.row)">
                                {{ $t('environment.nodeStatusMap')[props.row.nodeStatus] }}
                            </span>
                            <span class="node-status" v-else>{{ $t('environment.nodeStatusMap')[props.row.nodeStatus] }}</span>
                            <div class="install-agent"
                                v-if="['THIRDPARTY'].includes(props.row.nodeType) && props.row.nodeStatus === 'ABNORMAL'"
                                @click="installAgent(props.row)"
                            >{{ `（${$t('environment.install')}Agent）` }}</div>
                        </div>
                    </template>
                </bk-table-column>
                <bk-table-column :label="`${$t('environment.create')}/${$t('environment.nodeInfo.importTime')}`" prop="createTime" min-width="80">
                    <template slot-scope="props">
                        {{ props.row.createTime || '-' }}
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('environment.nodeInfo.lastModifyTime')" prop="lastModifyTime" min-width="80">
                    <template slot-scope="props">
                        {{ props.row.lastModifyTime || '-' }}
                    </template>
                </bk-table-column>
                <bk-table-column :label="$t('environment.operation')" width="160">
                    <template slot-scope="props">
                        <div class="table-node-item node-item-handler"
                            :class="{ 'over-handler': isMultipleBtn }">
                            <span class="node-handle delete-node-text" :class="{ 'no-node-delete-permission': !props.row.canDelete }"
                                v-if="props.row.canDelete && !['TSTACK'].includes(props.row.nodeType)"
                                @click.stop="confirmDelete(props.row, index)"
                            >{{ $t('environment.delete') }}</span>
                        </div>
                    </template>
                </bk-table-column>
            </bk-table>

            <empty-node v-if="showContent && !nodeList.length"
                :to-import-node="toImportNode"
                :empty-info="emptyInfo"
            ></empty-node>
        </section>
        <third-construct :construct-tool-conf="constructToolConf"
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
    import emptyNode from './empty_node'
    import thirdConstruct from '@/components/devops/environment/third-construct-dialog'
    import { getQueryString } from '@/utils/util'
    import webSocketMessage from '../utils/webSocketMessage.js'

    export default {
        components: {
            emptyNode,
            thirdConstruct
        },
        data () {
            return {
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
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            userInfo () {
                return window.userInfo
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
            getRowCls ({ row }) {
                return `node-item-row ${row.canUse ? '' : 'node-row-useless'}`
            },
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
            async requestList () {
                try {
                    const res = await this.$store.dispatch('environment/requestNodeList', {
                        projectId: this.projectId
                    })

                    this.nodeList.splice(0, this.nodeList.length)

                    res.forEach(item => {
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
                    this.showContent = true
                }
            },
            changeProject () {
                this.$toggleProjectMenu(true)
            },
            goToApplyPerm () {
                this.applyPermission(this.$permissionActionMap.view, this.$permissionResourceMap.envNode, [{
                    id: this.projectId,
                    type: this.$permissionResourceTypeMap.PROJECT
                }])
            },
            toNodeApplyPerm (row) {
                this.applyPermission(this.$permissionActionMap.use, this.$permissionResourceMap.envNode, [{
                    id: this.projectId,
                    type: this.$permissionResourceTypeMap.PROJECT
                }, {
                    id: row.nodeHashId,
                    type: this.$permissionResourceTypeMap.ENVIRONMENT_ENV_NODE
                }])
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
                    if (node.canUse) {
                        this.$router.push({ name: 'nodeDetail', params: { nodeHashId: node.nodeHashId } })
                    } else {
                        this.$showAskPermissionDialog({
                            noPermissionList: [{
                                actionId: this.$permissionActionMap.use,
                                resourceId: this.$permissionResourceMap.envNode,
                                instanceId: [{
                                    id: node.nodeHashId,
                                    name: node.displayName
                                }],
                                projectId: this.projectId
                            }]
                        })
                    }
                }
            },
            /**
             * 删除节点
             */
            async confirmDelete (row, index) {
                const params = []
                const id = row.nodeHashId

                params.push(id)
                if (!row.canDelete) {
                    this.$showAskPermissionDialog({
                        noPermissionList: [{
                            actionId: this.$permissionActionMap.delete,
                            resourceId: this.$permissionResourceMap.envNode,
                            instanceId: [{
                                id,
                                name: row.nodeId
                            }],
                            projectId: this.projectId
                        }]
                    })
                } else {
                    this.$bkInfo({
                        theme: 'warning',
                        type: 'warning',
                        title: this.$t('environment.delete'),
                        subTitle: `${this.$t('environment.nodeInfo.deleteNodetips', [row.nodeId])}`,
                        confirmFn: async () => {
                            let message, theme
                            try {
                                await this.$store.dispatch('environment/toDeleteNode', {
                                    projectId: this.projectId,
                                    params: params
                                })

                                message = this.$t('environment.successfullyDeleted')
                                theme = 'success'
                            } catch (err) {
                                if (err.code === 403) {
                                    this.$showAskPermissionDialog({
                                        noPermissionList: [{
                                            actionId: this.$permissionActionMap.delete,
                                            resourceId: this.$permissionResourceMap.envNode,
                                            instanceId: [{
                                                id,
                                                name: row.nodeId
                                            }],
                                            projectId: this.projectId
                                        }]
                                    })
                                } else {
                                    message = err.data ? err.data.message : err
                                    theme = 'error'
                                }
                            } finally {
                                message && this.$bkMessage({
                                    message,
                                    theme
                                })
                                this.requestList()
                            }
                        }
                    })
                }
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
                        this.constructToolConf.isShow = false
                    } catch (err) {
                        message = err.message ? err.message : err
                        theme = 'error'
                    } finally {
                        this.$bkMessage({
                            message,
                            theme
                        })

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
                    } catch (err) {
                        if (err.code === 403) {
                            this.$showAskPermissionDialog({
                                noPermissionList: [{
                                    actionId: this.$permissionActionMap.edit,
                                    resourceId: this.$permissionResourceMap.envNode,
                                    instanceId: [{
                                        id: node.nodeHashId,
                                        name: displayName
                                    }],
                                    projectId: this.projectId
                                }]
                            })
                        } else {
                            message = err.message ? err.message : err
                            theme = 'error'
                        }
                    } finally {
                        message && this.$bkMessage({
                            message,
                            theme
                        })
                        if (theme === 'success') {
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
              .no-node-delete-permission {
                cursor: url('../images/cursor-lock.png'), auto;
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
</style>
