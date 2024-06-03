import { setting } from '@/http'

const nodeSelectMixin = {
    data () {
        return {
            nodeList: [], // pool已有节点
            importNodeList: [], // 导入的节点
            nodeDialogLoading: {
                isLoading: false,
                title: ''
            },
            // 节点选择弹窗
            nodeSelectConf: {
                isShow: false,
                quickClose: false,
                hasHeader: false,
                unselected: true
            },
            // 选择节点
            selectHandlercConf: {
                curTotalCount: 0,
                curDisplayCount: 0,
                selectedNodeCount: 0,
                allNodeSelected: false,
                searchEmpty: false
            }
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

                this.importNodeList.filter(item => {
                    if (item.isChecked && !item.isEixtEnvNode) curCount++
                })

                this.selectHandlercConf.selectedNodeCount = curCount
                this.decideToggle()
            }
        }
    },
    methods: {
        importNewNode () {
            this.nodeSelectConf.isShow = true
            this.requestNodeList()
        },

        /**
         * 获取弹窗节点列表
         */
        requestNodeList () {
            this.nodeDialogLoading.isLoading = true

            setting.requestNodeList(this.projectId).then((res) => {
                this.importNodeList.splice(0, this.importNodeList.length)

                res.map(item => {
                    item.isChecked = false
                    item.isDisplay = true
                    this.importNodeList.push(item)
                })
                this.importNodeList = this.importNodeList.filter(item => (item.nodeType === 'THIRDPARTY'))
                this.importNodeList.filter(kk => {
                    this.nodeList.filter(vv => {
                        if (vv.nodeHashId === kk.nodeHashId) {
                            kk.isChecked = true
                            kk.isEixtEnvNode = true
                        }
                    })
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
            }).catch((err) => {
                const message = err.message ? err.message : err
                const theme = 'error'

                this.$bkMessage({
                    message,
                    theme
                })
            }).finally(() => {
                this.nodeDialogLoading.isLoading = false
            })
        },

        /**
         * 导入节点
         */
        async importEnvNode (nodeArr) {
            let message, theme
            const params = []

            this.nodeDialogLoading.isLoading = true

            nodeArr.map(item => {
                params.push(item)
            })

            setting.importEnvNode(this.projectId, this.envHashId, params).then(() => {
                message = 'Import successfully'
                theme = 'success'
            }).catch((err) => {
                message = err.message ? err.message : err
                theme = 'error'
            }).finally(() => {
                this.$bkMessage({
                    message,
                    theme
                })

                this.nodeSelectConf.isShow = false
                this.nodeDialogLoading.isLoading = false
                this.$emit('refresh')
                setting.getNodeList(this.projectId, this.envHashId).then((res) => {
                    this.nodeList = res
                    this.agentList = res
                })
            })
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
                    for (let i = 0; i < target.length; i++) {
                        // if (target[i] && str === target[i] && item.canUse) {
                        if (target[i] && str === target[i]) {
                            item.isDisplay = true
                            break
                        } else {
                            item.isDisplay = false
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
                this.importNodeList.forEach(item => {
                    item.isDisplay = true
                })
            }

            this.decideToggle()
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
        }
    }
}

export default nodeSelectMixin
