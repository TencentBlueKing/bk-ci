const buildMixins = {
    data () {
        return {
            pipelineList: [], // 流水线列表
            containerList: [], // 配置环境列表
            scriptList: [], // 脚本任务列表
            machineList: [], // 构建机列表
            envList: [
                {
                    name: 'gcc',
                    gccVersion: [
                        {
                            name: '4.1.2',
                            version: 'gcc4.1.2'
                        },
                        {
                            name: '4.4.6',
                            version: 'gcc4.4.6'
                        },
                        {
                            name: '4.8.2',
                            version: 'gcc4.8.2'
                        },
                        {
                            name: '4.8.5',
                            version: 'gcc4.8.5'
                        },
                        {
                            name: '4.9.4',
                            version: 'gcc4.9.4'
                        },
                        {
                            name: '5.4.0',
                            version: 'gcc5.4.0'
                        },
                        {
                            name: '5.5.0',
                            version: 'gcc5.5.0'
                        },
                        {
                            name: '7.1.0',
                            version: 'gcc7.1.0'
                        }
                    ]
                },
                {
                    name: 'clang',
                    gccVersion: [
                        {
                            name: '2.8',
                            version: 'clang2.8'
                        },
                        {
                            name: '3.4.2',
                            version: 'clang3.4.2'
                        },
                        {
                            name: '3.5.2',
                            version: 'clang3.5.2'
                        },
                        {
                            name: '5.0.1',
                            version: 'clang5.0.1'
                        }
                    ]
                },
                {
                    name: 'tvm',
                    gccVersion: [
                        {
                            name: '4.1.2',
                            version: 'tvm4.1.2'
                        }
                    ]
                },
                {
                    name: 'gcov',
                    gccVersion: [
                        {
                            name: '4.8.5',
                            version: 'gcov4.8.5'
                        }
                    ]
                }
            ],
            gccVersion: [],
            citys: [
                {
                    zoneName: 'shenzhen',
                    showName: '深圳'
                },
                {
                    zoneName: 'shanghai',
                    showName: '上海'
                },
                {
                    zoneName: 'chengdu',
                    showName: '成都'
                },
                {
                    zoneName: 'tianjin',
                    showName: '天津'
                }
            ],
            accelerateId: '1', // 配置方案 id => default:1
            programList: [ // 配置方案
                {
                    name: 'ccache+distcc',
                    id: '1',
                    banDistcc: 'false',
                    ccacheEnabled: 'true',
                    desc: '（推荐，预计可减少80%的编译时间）',
                    isDisable: false,
                    isRecommend: true
                },
                {
                    name: 'distcc',
                    id: '2',
                    banDistcc: 'false',
                    ccacheEnabled: 'false',
                    desc: '（可选，预计可减少60%的编译时间）',
                    isDisable: false,
                    isRecommend: false
                },
                {
                    name: 'ccache',
                    id: '3',
                    banDistcc: 'true',
                    ccacheEnabled: 'true',
                    desc: '（可选，预计可减少30%的编译时间）',
                    isDisable: false,
                    isRecommend: false
                },
                {
                    name: 'Incredibuild ',
                    id: '4',
                    desc: '（不支持该项目，一般可减少80%的编译时间）',
                    key: 'incredibuild',
                    isCurrent: false,
                    isDisable: true,
                    isRecommend: false
                },
                {
                    name: 'crcache ',
                    desc: '（不支持该项目，一般可减少15%的编译时间）',
                    key: 'crcache ',
                    id: '5',
                    isDisable: true,
                    isRecommend: false
                },
                {
                    name: 'Incredibuild+ccache ',
                    id: '6',
                    desc: '（不支持该项目，一般可减少95%的编译时间）',
                    key: 'incredibuild+ccache ',
                    isDisable: true,
                    isRecommend: false
                }
            ],
            optBuild: [
                {
                    label: '使用流水线和公共构建机',
                    value: '1',
                    isDisabled: false
                },
                {
                    label: '将自己的构建机导入蓝盾，并在流水线使用其进行构建',
                    value: '2',
                    isDisabled: false
                },
                {
                    label: '下载安装包至自己的构建机，进行构建和加速',
                    value: '3',
                    isDisabled: false
                }
            ],
            optLanguage: [
                {
                    label: 'C/C++',
                    value: '1',
                    isDisabled: false
                },
                {
                    label: 'Objective C/C++',
                    value: '2',
                    isDisabled: true
                },
                {
                    label: 'C#',
                    value: '3',
                    isDisabled: true
                }
            ],
            optType: [
                {
                    label: '后台服务',
                    value: '1',
                    isDisabled: false
                },
                {
                    label: 'Unity3D',
                    value: '2',
                    isDisabled: true
                },
                {
                    label: 'UE4',
                    value: '3',
                    isDisabled: true
                }
            ]
        }
    },
    computed: {
        projectId () {
            return this.$route.params.projectId
        }
    },
    methods: {
        visiblePipeline (open) { // 流水线下拉开启
            open && this.requestPipelineList()
        },
        envSelect (value, item) { // 选择编译器
            this.gccVersion = item.gccVersion
        },
        pipelineSelect (value, item) { // 选择流水线
            const { requestContainerList, taskParam } = this
            this.containerList = []
            taskParam.bsPipelineName = item.pipelineName
            requestContainerList()
            taskParam.bsVmSeqId = ''
            taskParam.bsElementId = ''
            // taskParam.gccVersion = 'gcc4.4.6'
        },
        buildSelect (value, item) { // 选择编译环境
            this.scriptList = item.elements
            if (value) {
                this.taskParam.bsElementId = ''
            }
            let envList
            this.envList.forEach(env => {
                env.gccVersion.forEach(item => {
                    if (item.version === this.taskParam.gccVersion) {
                        envList = Object.assign({}, env)
                    }
                })
            })
            if (envList) {
                this.taskParam.envList = envList.name
                this.gccVersion = envList.gccVersion
            }
        },
        visibleMachine (open) { // 构建机下拉开启
            open && this.requestMachineList()
        },
        accelerateSelect (value, item) { // 加速方案选择
            Object.assign(this.taskParam, {
                banDistcc: item.banDistcc,
                ccacheEnabled: item.ccacheEnabled
            })
        },
        
        async requestPipelineList () { // 拉取流水线列表
            const { $store, projectId, loading, pipelineList, taskParam, requestContainerList, testPipelineId } = this
            loading.pipelines = true
            try {
                const res = await $store.dispatch('turbo/requestPipelineListPermission', {
                    projectId: projectId
                })
                if (res) {
                    pipelineList.splice(0, pipelineList.length)
                    res.records.map(item => {
                        pipelineList.push(item)
                    })
                    if (this.showLinkElement && testPipelineId(pipelineList)) {
                        requestContainerList()
                    } else if (this.showLinkElement && !testPipelineId(pipelineList)) {
                        taskParam.bsPipelineId = taskParam.bsVmSeqId = taskParam.bsElementId = ''
                        this.showLinkElement = false
                    }
                }
            } catch (err) {
                this.$bkMessage({
                    message: err.message ? err.message : err,
                    theme: 'error'
                })
            } finally {
                this.pipelineDisabled = false
            }
            loading.pipelines = false
        },

        transDateStr (dateStr) {
            if (!dateStr) {
                return ''
            }
            const date = new Date(dateStr) // 时间戳为10位需*1000，时间戳为13位的话不需乘1000
            const Y = date.getFullYear() + '年'
            const M = (date.getMonth() + 1 < 10 ? '0' + (date.getMonth() + 1) : date.getMonth() + 1) + '月'
            const D = date.getDate() + '日 '
            const h = date.getHours() + ':'
            const m = date.getMinutes() < 10 ? '0' + date.getMinutes() : date.getMinutes()
            // let s = date.getSeconds();
            return Y + M + D + h + m
        }
    }
}

export default buildMixins
