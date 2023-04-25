<template>
    <div class="node-content-wrapper">
        <div class="node-content-header">
            <bk-button theme="primary" @click="importNewNode">{{ $t('environment.import') }}</bk-button>
        </div>

        <div class="node-table" v-if="showContent && nodeList.length">
            <bk-table
                ref="shareDiaglogTable"
                :data="nodeList"
            >
                <bk-table-column :label="$t('environment.envInfo.name')" prop="displayName"></bk-table-column>
                <bk-table-column :width="150" label="IP" prop="ip"></bk-table-column>
                <bk-table-column :label="`${$t('environment.nodeInfo.source')}/${$t('environment.nodeInfo.importer')}`">
                    <template slot-scope="props">
                        <span class="node-name">{{ props.row.nodeType }}</span>
                        <span>({{ props.row.createdUser }})</span>
                    </template>
                </bk-table-column>
                <bk-table-column :width="80" :label="$t('environment.nodeInfo.os')" prop="osName"></bk-table-column>
                <bk-table-column :width="80" :label="$t('environment.nodeInfo.gateway')" prop="gateway"></bk-table-column>
                <bk-table-column :label="$t('environment.nodeInfo.cpuStatus')">
                    <template slot-scope="props">
                        <div
                            v-if="props.row.nodeStatusIcon === 'creating'"
                            class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary"
                        >
                            <div
                                v-for="i in [1,2,3,4,5,6,7,8]"
                                :key="i"
                                :class="`rotate rotate${i}`"
                            ></div>
                        </div>
                        <span
                            v-if="props.row.nodeStatusIcon === 'normal'"
                            class="node-status-icon normal-stutus-icon"
                        >
                        </span>
                        <span
                            v-if="props.row.nodeStatusIcon === 'unnormal'"
                            class="node-status-icon abnormal-stutus-icon"
                        >
                        </span>

                        <span class="node-status">{{ props.row.nodeStatus }}</span>
                    </template>
                </bk-table-column>
                <bk-table-column :width="80" :label="$t('environment.operation')">
                    <template slot-scope="props">
                        <span class="node-delete delete-node-text" @click.stop="confirmDelete(props.row)">{{ $t('environment.remove') }}</span>
                    </template>
                </bk-table-column>
            </bk-table>
        </div>
        <bk-exception
            v-if="showContent && !nodeList.length"
            class="exception-wrap-item exception-part" type="empty" scene="part"
        />
        <node-select :node-select-conf="nodeSelectConf"
            :search-info="searchInfo"
            :cur-user-info="curUserInfo"
            :row-list="importNodeList"
            :change-created-user="changeCreatedUser"
            :select-handlerc-conf="selectHandlercConf"
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
    export default {
        name: 'node-tab',
        components: {
            nodeSelect
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
                selectHandlercConf: {
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
                showContent: false
            }
        },
        computed: {
            curUserInfo () {
                return window.userInfo
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

                    this.selectHandlercConf.selectedNodeCount = curCount
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
                this.nodeSelectConf.importText = `${this.$t('environment.nodeType.importing')}...`

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
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })

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

            getNodeStatusIcon (nodeStatus) {
                console.log(nodeStatus)
                const i18nPrefix = 'environment.nodeInfo'
                const statusArray = [
                    'abnormal',
                    'unknown',
                    'deleted',
                    'loss'
                ]
            
                switch (true) {
                    case nodeStatus === this.$t(`${i18nPrefix}.creating`):
                        return 'ceating'
                    case nodeStatus === this.$t(`${i18nPrefix}.normal`):
                        return 'normal'
                    case statusArray.some(status => nodeStatus === this.$t(`${i18nPrefix}.${status}`)):
                    default:
                        return 'unnormal'
                }
            },
            /**
             * 获取环境节点列表
             */
            async requestList () {
                try {
                    const res = await this.$store.dispatch('environment/requestEnvNodeList', {
                        projectId: this.projectId,
                        envHashId: this.envHashId
                    })

                    this.nodeList.splice(0, this.nodeList.length)
                    res.forEach(item => {
                        this.nodeList.push({
                            ...item,
                            nodeStatusIcon: this.getNodeStatusIcon(item.nodeStatus)
                        })
                    })

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
                this.showContent = true
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
                    if (target.nodeType === this.$t('environment.BCSVirtualMachine') && (target.nodeStatus === this.$t('environment.nodeInfo.abnormal')
                        || target.nodeStatus === this.$t('environment.nodeInfo.creating')
                        || target.nodeStatus === this.$t('environment.nodeInfo.unknown')
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
                        envHashId: this.envHashId
                    })

                    this.importNodeList.splice(0, this.importNodeList.length)

                    res.forEach(item => {
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

                    this.selectHandlercConf.curTotalCount = curCount

                    const result = this.importNodeList.some(element => {
                        return element.isDisplay
                    })

                    if (result) {
                        this.selectHandlercConf.searchEmpty = false
                    } else {
                        this.selectHandlercConf.searchEmpty = true
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
                }, `${this.$t('environment.nodeInfo.removeNodetips', [row.nodeId])}？`)

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

                this.selectHandlercConf.curDisplayCount = curCount

                if (curCount === curCheckCount) {
                    this.selectHandlercConf.allNodeSelected = true
                } else {
                    this.selectHandlercConf.allNodeSelected = false
                }
            },
            /**
             * 节点全选
             */
            toggleAllSelect () {
                if (this.selectHandlercConf.allNodeSelected) {
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
                        this.selectHandlercConf.searchEmpty = false
                    } else {
                        this.selectHandlercConf.searchEmpty = true
                    }
                } else {
                    this.selectHandlercConf.searchEmpty = false

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
                    this.selectHandlercConf.searchEmpty = false
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
</style>
