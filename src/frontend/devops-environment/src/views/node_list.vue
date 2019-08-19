<template>
    <div class="node-list-wrapper">
        <div class="node-header">
            <div class="title">节点</div>
            <div class="header-handler-row" v-if="nodeList.length > 0">
                <bk-button theme="primary" class="import-vmbuild-btn" @click="toImportNode('construct')">导入节点</bk-button>
            </div>
        </div>
        <div class="node-container" v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
            <section class="sub-view-port">
                <bk-table v-if="showContent && nodeList.length"
                    size="medium"
                    class="node-table-wrapper"
                    :data="nodeList">
                    <bk-table-column label="别名" prop="displayName">
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
                                        <span class="edit-base save" @click="saveEdit(props.row)">保存</span>
                                        <span class="edit-base cancel" @click="cancelEdit(props.row.nodeHashId)">取消</span>
                                    </div>
                                </div>
                            </div>
                            <div class="table-node-item node-item-id" v-else>
                                <span class="node-name"
                                    :class="{ 'pointer': canShowDetail(props.row) }"
                                    :title="props.row.displayName"
                                    @click="toNodeDetail(props.row)"
                                >{{ props.row.displayName || '-' }}</span>
                                <i class="bk-icon icon-edit" v-if="!isEditNodeStatus && props.row.canEdit" @click="editNodeName(props.row)"></i>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="内网IP" prop="ip" min-width="80">
                        <template slot-scope="props">
                            {{ props.row.ip || '-' }}
                        </template>
                    </bk-table-column>
                    <bk-table-column label="操作系统" prop="osName">
                        <template slot-scope="props">
                            {{ props.row.osName || '-' }}
                        </template>
                    </bk-table-column>
                    <bk-table-column label="来源/导入人" prop="createdUser" min-width="120">
                        <template slot-scope="props">
                            <div>
                                <span class="node-name">{{ getNodeTypeMap[props.row.nodeType] || '-' }}</span>
                                <span>({{ props.row.createdUser }})</span>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="状态" prop="nodeStatus">
                        <template slot-scope="props">
                            <div class="table-node-item node-item-status"
                                v-if="props.row.nodeStatus === 'BUILDING_IMAGE' && props.row.nodeType === 'DEVCLOUD'">
                                <span class="node-status-icon normal-stutus-icon"></span>
                                <span class="node-status">正常</span>
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
                                    v-if="props.row.nodeType === 'DEVCLOUD' && props.row.nodeStatus === 'RUNNING'"
                                    @click="installAgent(props.row)">
                                    {{ getNodeStatusMap[props.row.nodeStatus] }}
                                </span>
                                <span class="node-status" v-else>{{ getNodeStatusMap[props.row.nodeStatus] }}</span>
                            </div>
                        </template>
                    </bk-table-column>
                    <bk-table-column label="创建/导入时间" prop="createTime" min-width="80">
                        <template slot-scope="props">
                            {{ props.row.createTime || '-' }}
                        </template>
                    </bk-table-column>
                    <bk-table-column label="最后修改时间" prop="lastModifyTime" min-width="80">
                        <template slot-scope="props">
                            {{ props.row.lastModifyTime || '-' }}
                        </template>
                    </bk-table-column>
                    <bk-table-column label="操作" width="160">
                        <template slot-scope="props">
                            <div class="table-node-item node-item-handler"
                                :class="{ 'over-handler': isMultipleBtn }">
                                <span class="node-handle delete-node-text"
                                    v-if="props.row.canDelete && !['TSTACK', 'DEVCLOUD'].includes(props.row.nodeType)"
                                    @click.stop="confirmDelete(props.row, index)"
                                >删除</span>
                                <span class="node-handle delete-node-text"
                                    v-if="!props.row.canUse && props.row.nodeStatus !== 'CREATING'"
                                    @click.stop="toNodeApplyPerm(props.row)"
                                >申请权限</span>
                            </div>
                        </template>
                    </bk-table-column>
                </bk-table>

                <empty-node v-if="showContent && !nodeList.length"
                    :to-import-node="toImportNode"
                    :empty-info="emptyInfo"
                ></empty-node>
            </section>
        </div>
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
    import { mapGetters } from 'vuex'
    import emptyNode from './empty_node'
    import thirdConstruct from '@/components/devops/environment/third-construct-dialog'
    import { getQueryString } from '@/utils/util'

    export default {
        components: {
            emptyNode,
            thirdConstruct
        },
        data () {
            return {
                timer: -1,
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
                    title: '数据加载中，请稍候'
                },
                // 弹窗loading
                dialogLoading: {
                    isLoading: false,
                    title: ''
                },
                emptyInfo: {
                    title: '导入你的第一个节点',
                    desc: '节点可以是你的开发机，也可以是团队公用的编译机'
                },
                // 构建机弹窗配置
                constructToolConf: {
                    isShow: false,
                    hasHeader: false,
                    quickClose: false,
                    importText: '导入'
                },
                // 构建机内容
                constructImportForm: {
                    model: 'MACOS',
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
                    title: '没有权限',
                    desc: `你在该项目【节点管理】下没有【创建】权限，请切换项目访问或申请`,
                    btns: [
                        {
                            type: 'primary',
                            size: 'normal',
                            handler: this.changeProject,
                            text: '切换项目'
                        },
                        {
                            type: 'success',
                            size: 'normal',
                            handler: this.goToApplyPerm,
                            text: '去申请权限'
                        }
                    ]
                }
            }
        },
        computed: {
            ...mapGetters('environment', [
                'getNodeTypeMap',
                'getNodeStatusMap'
            ]),
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
        },
        async mounted () {
            await this.init()
        },
        beforeDestroy () {
            clearTimeout(this.timer)
            this.timer = null
        },
        methods: {
            async init () {
                const {
                    loading
                } = this

                loading.isLoading = true
                loading.title = '数据加载中，请稍候'

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
                clearTimeout(this.timer)

                try {
                    const res = await this.$store.dispatch('environment/requestNodeList', {
                        projectId: this.projectId
                    })

                    this.nodeList.splice(0, this.nodeList.length)

                    res.map(item => {
                        item.isEnableEdit = item.nodeHashId === this.curEditNodeItem
                        item.isMore = item.nodeHashId === this.lastCliCKNode.nodeHashId
                        this.nodeList.push(item)
                    })

                    // for (let i = 0; i < this.nodeList.length; i++) {
                    //     if (this.nodeList[i].canDelete && !this.nodeList[i].canUse) {
                    //         this.isMultipleBtn = true
                    //         break
                    //     }
                    // }

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
                } finally {
                    this.showContent = true
                }
            },
            /**
             *  轮询整个列表状态
             */
            async loopCheck () {
                // let {
                //     timer
                // } = this
                // let res

                // clearTimeout(timer)

                // for (let i = 0; i < this.nodeList.length; i++) {
                //     let target = this.nodeList[i]
                //     if (target.nodeType === 'BCS虚拟机' && (target.nodeStatus === '异常' ||
                //         target.nodeStatus === '正在创建中' ||
                //         target.nodeStatus === '未知' ||
                //         !target.agentStatus)) {
                //         res = true
                //         break
                //     } else {
                //         res = false
                //     }
                // }

                // if (res) {
                //     this.timer = setTimeout(async () => {
                //         await this.requestList()
                //     }, 10000)
                // }

                clearTimeout(this.timer)

                if (this.nodeList.length) {
                    this.timer = setTimeout(async () => {
                        await this.requestList()
                    }, 8000)
                }
            },
            changeProject () {
                this.$toggleProjectMenu(true)
            },
            goToApplyPerm () {
                const url = `/backend/api/perm/apply/subsystem/?client_id=node&project_code=${this.projectId}&service_code=environment&role_creator=env_node`
                window.open(url, '_blank')
            },
            toNodeApplyPerm (row) {
                const url = `/backend/api/perm/apply/subsystem/?client_id=node&project_code=${this.projectId}&service_code=environment&role_manager=env_node:${row.nodeHashId}`
                window.open(url, '_blank')
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
                    this.$router.push({ name: 'nodeDetail', params: { nodeHashId: node.nodeHashId } })
                }
            },
            toInstall () {
                const url = `${DOCS_URL_PREFIX}/%E6%89%80%E6%9C%89%E6%9C%8D%E5%8A%A1/%E7%8E%AF%E5%A2%83%E7%AE%A1%E7%90%86/installGseAgentGuide.html`
                window.open(url, '_blank')
            },
            /**
             * 删除节点
             */
            async confirmDelete (row, index) {
                const params = []
                const id = row.nodeHashId

                params.push(id)

                const h = this.$createElement
                const content = h('p', {
                    style: {
                        textAlign: 'center'
                    }
                }, `确定删除节点(${row.nodeId})？`)

                this.$bkInfo({
                    title: `删除`,
                    subHeader: content,
                    confirmFn: async () => {
                        let message, theme
                        try {
                            await this.$store.dispatch('environment/toDeleteNode', {
                                projectId: this.projectId,
                                params: params
                            })

                            message = '删除成功'
                            theme = 'success'
                        } catch (err) {
                            message = err.data ? err.data.message : err
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
            /**
             * 构建机信息
             */
            async changeCreatedUser (id) {
                const h = this.$createElement
                const content = h('p', {
                    style: {
                        textAlign: 'center'
                    }
                }, `是否修改主机责任人为当前用户？`)

                this.$bkInfo({
                    title: `修改导入人`,
                    subHeader: content,
                    confirmFn: async () => {
                        let message, theme
                        const params = {}
                        try {
                            await this.$store.dispatch('environment/changeCreatedUser', {
                                projectId: this.projectId,
                                nodeHashId: id,
                                params
                            })

                            message = '修改成功'
                            theme = 'success'
                        } catch (err) {
                            const message = err.message ? err.message : err
                            const theme = 'error'

                            this.$bkMessage({
                                message,
                                theme
                            })
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
                            const gateway = node.nodeType === 'DEVCLOUD' ? 'shenzhen' : node.gateway
                            this.constructImportForm.model = node.nodeType === 'DEVCLOUD' ? 'LINUX' : node.osName.toUpperCase()
                            this.requestGateway(gateway, node)
                        } else {
                            this.constructImportForm.model = 'MACOS'
                            this.requestGateway()
                        }
                    } else {
                        message = '第三方构建机接入灰度公测中'
                        theme = 'warning'

                        this.$bkMessage({
                            message,
                            theme
                        })
                    }
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
                    this.constructImportForm.location = ''
                    res.map(item => {
                        this.gatewayList.push(item)
                    })

                    if (this.gatewayList.length && gateway && gateway === 'shenzhen') {
                        this.constructImportForm.location = 'shenzhen'
                    } else if (this.gatewayList.length && gateway && gateway !== 'shenzhen') {
                        const isTarget = this.gatewayList.find(item => item.showName === gateway)
                        this.constructImportForm.location = isTarget && isTarget.zoneName
                    } else if (this.gatewayList.length && !gateway) {
                        this.constructImportForm.location = this.gatewayList[0].zoneName
                    }

                    if (node && node.nodeType === 'THIRDPARTY') { // 如果是第三方构建机类型则获取构建机详情以获得安装命令或下载链接
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
                if (['DEVCLOUD', 'THIRDPARTY'].includes(node.nodeType)) {
                    this.nodeIp = node.ip
                    this.isAgent = true
                    this.constructToolConf.importText = '确定'
                    this.switchConstruct(node)
                } else if (['CC', 'CMDB'].includes(node.nodeType)) {
                    const url = `${DOCS_URL_PREFIX}/所有服务/环境管理/installGseAgentGuide.html`
                    window.open(url, '_blank')
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
                    this.constructToolConf.importText = this.constructToolConf.importText === '确定' ? '提交中...' : '导入中...'

                    let message, theme

                    try {
                        await this.$store.dispatch('environment/importConstructNode', {
                            projectId: this.projectId,
                            agentId: this.constructImportForm.agentId
                        })

                        message = this.constructToolConf.importText === '提交中...' ? '提交成功' : '导入成功'
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
                        this.constructToolConf.importText = '导入'
                        this.requestList()
                    }
                }
            },
            cancelFn () {
                if (!this.dialogLoading.isShow) {
                    this.isAgent = false
                    this.constructToolConf.isShow = false
                    this.dialogLoading.isShow = false
                    this.constructToolConf.importText = '导入'
                }
            },
            editNodeName (node) {
                this.curEditNodeDisplayName = node.displayName
                this.isEditNodeStatus = true
                this.curEditNodeItem = node.nodeHashId
                this.nodeList.map(val => {
                    if (val.nodeHashId === node.nodeHashId) {
                        val.isEnableEdit = true
                    }
                })
            },
            async saveEdit (node) {
                const valid = await this.$validator.validate()
                if (valid) {
                    let message, theme
                    const params = {
                        displayName: this.curEditNodeDisplayName.trim()
                    }

                    try {
                        await this.$store.dispatch('environment/updateDisplayName', {
                            projectId: this.projectId,
                            nodeHashId: node.nodeHashId,
                            params
                        })

                        message = '修改成功'
                        theme = 'success'
                    } catch (err) {
                        message = err.message ? err.message : err
                        theme = 'error'
                    } finally {
                        this.$bkMessage({
                            message,
                            theme
                        })
                        if (theme === 'success') {
                            this.nodeList.map(val => {
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
                this.nodeList.map(val => {
                    if (val.nodeHashId === nodeId) {
                        val.isEnableEdit = false
                    }
                })
            },
            async destoryNode (node) {
                const h = this.$createElement
                const content = h('p', {
                    style: {
                        textAlign: 'center'
                    }
                }, `确定销毁该节点？`)

                this.$bkInfo({
                    title: `确定`,
                    subHeader: content,
                    confirmFn: async () => {
                        clearTimeout(this.timer)

                        let message, theme
                        try {
                            await this.$store.dispatch('environment/toDestoryNode', {
                                projectId: this.projectId,
                                nodeHashId: node.nodeHashId
                            })

                            message = '提交成功'
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
            canShowDetail (row) {
                return row.nodeType === 'THIRDPARTY' || (row.nodeType === 'DEVCLOUD' && row.nodeStatus === 'NORMAL')
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
        .node-header {
            display: flex;
            justify-content: space-between;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid $borderWeightColor;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
        }

        .header-handler-row {
            position: relative;
            top: -4px;

            .create-node-btn,
            .import-node-btn,
            .import-vmbuild-btn {
                width: 76px;
                height: 32px;
                line-height: 30px;
            }

            .bk-icon {
                margin-left: 0;
                position: relative;
                top: 0;
                font-size: 12px;
            }

            .import-vmbuild-btn {
                width: 150px;
            }
        }

        .create-node-btn {
            margin-right: 6px;
        }

        .node-container {
            overflow: auto;
            height: calc(100% - 60px);
        }

        .sub-view-port {
            padding: 20px;
            height: auto;
        }

        .prompt-operator,
        .edit-operator {
            padding-right: 10px;
            color: #ffbf00;

            .bk-icon {
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
