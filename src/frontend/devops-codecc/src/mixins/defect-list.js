import { format } from 'date-fns'
import { mapState } from 'vuex'

export default {
    data () {
        return {
            active: 'defect',
            pathPanels: [
                { name: 'choose', label: this.$t('选择路径') },
                { name: 'input', label: this.$t('手动输入') }
            ],
            tabSelect: 'choose',
            inputFileList: ['']
        }
    },
    computed: {
        ...mapState([
            'toolMeta'
        ]),
        ...mapState('tool', {
            toolMap: 'mapList'
        }),
        ...mapState('defect', {
            defaultCheckset: 'defaultCheckset'
        }),
        toolList () {
            const toolType = this.toolMeta['TOOL_TYPE'].filter(item => {
                return item.key !== 'CCN' && item.key !== 'DUPC' && item.key !== 'CLOC'
            })
            const enTooList = this.taskDetail.enableToolList.filter(item => {
                return item.toolName !== 'CCN' && item.toolName !== 'DUPC' && item.toolName !== 'CLOC'
            })
            if (this.toolMap) {
                toolType.forEach(item => {
                    if (item.name === '代码安全') item.name = '安全漏洞'
                    item['toolList'] = []
                    enTooList.forEach(i => {
                        if (this.toolMap[i.toolName] && this.toolMap[i.toolName].type === item.key) {
                            item['toolList'].push(i)
                        }
                    })
                })
            }
            return toolType
        },
        treeList () {
            return this.searchFormData.filePathTree.name ? [this.searchFormData.filePathTree] : []
        }
    },
    methods: {
        formatTime (date, token, options = {}) {
            return date ? format(Number(date), token, options) : ''
        },
        getTreeData () {
            const dirstrs = []
            this.$refs.filePathTree.checked.map(item => {
                const checkeditem = this.$refs.filePathTree.getNodeById(item)
                if (checkeditem.parent) {
                    if (checkeditem.parent.parent && checkeditem.parent.indeterminate) {
                        const fullPath = this.getFullPath(checkeditem).join('/')
                        if (checkeditem.children.length) {
                            dirstrs.push(`.*/${fullPath}/.*`)
                        } else {
                            dirstrs.push(`.*/${fullPath}`)
                        }
                    } else if (!checkeditem.parent.parent) { // 当为第二层时候，此时是虚拟层，需要传第三层内容给后台
                        checkeditem.children.map(child => {
                            const fullPath = this.getFullPath(child).join('/')
                            if (child.children.length) {
                                dirstrs.push(`.*/${fullPath}/.*`)
                            } else {
                                dirstrs.push(`.*/${fullPath}`)
                            }
                        })
                    }
                }
            })
            this.searchParams.fileList = dirstrs
            return dirstrs
        },
        getFullPath (item) {
            let fullPath = [item.name]
            const getPath = function (node, path = []) {
                if (node.parent) {
                    path.unshift(node.parent.name)
                    fullPath.unshift(node.parent.name)
                    getPath(node.parent, path)
                }
            }
            getPath(item)
            fullPath = fullPath.slice(2)
            return fullPath
        },
        handleFilePathClearClick () {
            const filePathDropdown = this.$refs.filePathDropdown
            this.$refs.filePathTree.removeChecked()
            this.inputFileList = ['']
            this.searchFormData.filePathShow = ''
            this.searchParams.fileList = []
            filePathDropdown.hide()
        },
        // 文件路径相关交互
        handleFilePathSearch (val) {
            this.$refs.filePathTree.filter(val)
        },
        // 路径过滤函数
        filterMethod (keyword, node) {
            return node.name.toLowerCase().indexOf(keyword.toLowerCase()) > -1
        },
        handleSelectTool (toolName) {
            const tool = this.taskDetail.enableToolList.find(item => item.toolName === toolName)
            const toolPattern = tool.toolPattern.toLocaleLowerCase()
            this.$router.push({
                name: `defect-${toolPattern}-list`,
                params: { ...this.$route.params, toolId: toolName }
            })
        },
        handleTableChange (value) {
            const toolName = this.toolId
            const toolPattern = this.toolMap[toolName].pattern.toLocaleLowerCase()
            const params = { ...this.$route.params, toolId: toolName }
            if (value === 'defect') {
                this.$router.push({
                    name: `defect-${toolPattern}-list`,
                    params
                })
            } else {
                this.$router.push({
                    name: `defect-${toolPattern}-charts`,
                    params
                })
            }
        },
        addTool (query) {
            if (this.taskDetail.createFrom.indexOf('pipeline') !== -1) {
                const that = this
                this.$bkInfo({
                    title: this.$t('配置规则集'),
                    subTitle: this.$t('此代码检查任务为流水线创建，规则集需前往相应流水线配置。'),
                    maskClose: true,
                    confirmFn (name) {
                        window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${that.taskDetail.projectId}/${that.taskDetail.pipelineId}/edit#${that.taskDetail.atomCode}`, '_blank')
                    }
                })
            } else {
                this.$router.push({ name: 'task-settings-checkerset', query })
                // const from = query.from
                // if (from === 'cov' || from === 'lint') {
                //     this.$router.push({ name: 'task-settings-checkerset', query })
                //     return
                // }
                // const codeLang = this.taskDetail.codeLang
                // const codeLangKeys = this.toolMeta.LANG.map(lang => {
                //     if (lang.key & codeLang) {
                //         return lang.key
                //     }
                // }).filter(name => name)
                // const installList = codeLangKeys.map(key => {
                //     const checkerSetId = this.defaultCheckset[key] && this.defaultCheckset[key][from]
                //     if (checkerSetId) {
                //         const params = {
                //             type: 'TASK',
                //             projectId: this.projectId,
                //             taskId: this.taskId,
                //             checkerSetId
                //         }
                //         return this.$store.dispatch('checkerset/install', params)
                //     }
                // }).filter(item => item)
                // if (installList.length) {
                //     Promise.all(installList).then(() => {
                //         window.location.reload()
                //     })
                // } else {
                //     this.$bkMessage({ theme: 'warning', message: this.$t('该任务语言暂无合适规则集') })
                // }
            }
        },
        handleDateChange (date, type) {
            this.searchParams.daterange = date
            this.dateType = type
        },
        changeTab (name) {
            this.tabSelect = name
        },
        // 添加input框
        addPath (index) {
            this.inputFileList.push('')
        },
        cutPath (index) {
            if (this.inputFileList.length > 1) {
                this.inputFileList.splice(index, 1)
            }
        },
        handleFilePathConfirmClick () {
            if (this.tabSelect === 'choose') {
                const filePath = this.getTreeData()
                this.searchFormData.filePathShow = filePath.join(';')
            } else if (this.tabSelect === 'input') {
                const inputFileList = this.inputFileList.filter(item => item).slice()
                this.searchParams.fileList = inputFileList
                this.searchFormData.filePathShow = inputFileList.join(';')
            }

            this.$refs.filePathDropdown.hide()
        }
    }
}
