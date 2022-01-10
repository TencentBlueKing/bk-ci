const buildOption = {
    data () {
        return {
            isBtnDisabled: false, // 提交按钮状态
            machineZoneDisabled: false, // 构建机地址
            pipelineDisabled: false, // 流水线禁选

            loading: {
                pipelines: false,
                build: false,
                machine: false
            },

            taskParam: { // 任务参数
                projectId: '',
                projectName: '',
                taskId: '',
                taskName: '',
                projLang: '', // 语言
                projType: '',
                ccacheEnabled: '',
                banDistcc: '',
                bsPipelineId: '',
                bsPipelineName: '',
                bsVmSeqId: '',
                bsElementId: '',
                gccVersion: '',
                machineNum: 18,
                cpuNum: 144,
                cacheSize: 1024,
                buildMachineId: '',
                city: ''
            },
            
            showLinkElement: false, // 备选

            pipelineList: [], // 流水线列表
            containerList: [], // 配置环境列表
            scriptList: [], // 脚本任务列表
            machineList: [], // 构建机列表
            
            machineParam: { // 构建机配置信息
                bsAgentId: '',
                bsProjectId: '',
                machineIp: '',
                machineHostName: '',
                machineZone: '',
                nodeHashId: '',
                gccVersion: ''
            },
            envList: [],
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
            ],
            optcompiler: [
                {
                    label: 'gcc',
                    value: '1',
                    isDisabled: false
                },
                {
                    label: 'clang',
                    value: '2',
                    isDisabled: false
                }
            ],
            optTool: [
                {
                    label: 'make/cmake',
                    value: '1',
                    isDisabled: false,
                    isChecked: 'checked'
                },
                {
                    label: 'blade',
                    value: '2',
                    isDisabled: false,
                    isChecked: false
                },
                {
                    label: 'bazel',
                    value: '3',
                    isDisabled: false,
                    isChecked: false
                }
            ]
        }
    },
    computed: {
        projectId () {
            return this.$route.params.projectId
        }
    },
    created () {
        this.getCompilerConfig()
    },
    methods: {
        pipelineSelect (value, item) {
            const { requestContainerList, taskParam } = this
            this.containerList = []
            taskParam.bsPipelineName = item.pipelineName
            requestContainerList()
            taskParam.bsVmSeqId = ''
            taskParam.bsElementId = ''
            // taskParam.gccVersion = 'gcc4.4.6'
        },
        envSelect (value, item) {
            this.gccVersion = item.gccVersion
        },
        buildSelect (value, item) {
            this.scriptList = item.elements
            const dockerBuildVersion = item.dockerBuildVersion || (item.dispatchType ? item.dispatchType.value : '')
            if (dockerBuildVersion === 'tlinux1.2') {
                this.taskParam.gccVersion = 'gcc4.4.6'
            } else if (dockerBuildVersion === 'tlinux2.2') {
                this.taskParam.gccVersion = 'gcc4.8.5'
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
            if (value) {
                this.taskParam.bsElementId = ''
            }
        },
        visiblePipeline (open) {
            open && this.requestPipelineList()
        },
        visibleMachine (open) {
            open && this.requestMachineList()
        },
        async requestPipelineList () {
            const { $store, projectId, loading, pipelineList, taskParam } = this
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

                    if (this.showLinkElement && this.testPipelineId(pipelineList)) {
                        this.requestContainerList()
                    } else if (this.showLinkElement && !this.testPipelineId(pipelineList)) {
                        taskParam.bsPipelineId = taskParam.bsVmSeqId = taskParam.bsElementId = ''
                        this.showLinkElement = false
                        this.pipelineDisabled = false
                    } else {
                        this.pipelineDisabled = false
                    }
                }
            } catch (err) {
                this.pipelineDisabled = false
                this.$bkMessage({
                    message: err.message ? err.message : err,
                    theme: 'error'
                })
            }
            loading.pipelines = false
        },
        async requestContainerList () {
            const { projectId, loading, taskParam } = this
            loading.build = true
            try {
                const res = await this.$store.dispatch('turbo/requestContainerList', {
                    projectId: projectId,
                    pipelineId: taskParam.bsPipelineId
                })
                if (res) {
                    const stageList = res.stages.splice(0, res.stages.length)
                    const { machineType } = this.taskParam
                    const reg = /stage-/
                    let orderStr = ''
                    const buildArr = []
                    let allIndex = 0
                    stageList.forEach((stage, stageIndex) => {
                        orderStr = stage.id.replace(reg, ' ')
                        stage.containers.forEach((container, index) => {
                            const isDocker = container.dockerBuildVersion || (container.dispatchType && container.dispatchType.buildType === 'DOCKER')
                            const isThirdPartyAgent = container.thirdPartyAgentId || (container.dispatchType && container.dispatchType.buildType === 'THIRD_PARTY_AGENT_ID')
                            const dockerBuildVersion = container.dockerBuildVersion || (isDocker ? container.dispatchType.value : '')
                            if ((machineType === '1' && isDocker) || (machineType === '2' && isThirdPartyAgent)) {
                                index++
                                buildArr.push({
                                    id: allIndex + '',
                                    containerId: stage.id,
                                    '@type': container['@type'],
                                    name: container.name + orderStr + '-' + index,
                                    baseOS: container.baseOS || '',
                                    elements: container.elements.concat(),
                                    dockerBuildVersion: dockerBuildVersion
                                })
                            }
                            allIndex++
                        })
                    })
                    this.containerList = buildArr.filter(item => item.baseOS === 'LINUX')
                    this.containerList.forEach(container => {
                        container.elements = container.elements.filter(item => item['@type'] === 'linuxScript'
                        || (item['@type'] === 'linuxPaasCodeCCScript' && item['languages'].includes('C_CPP') && (item['tools'].includes('COVERITY') || item['tools'].includes('KLOCWORK')))
                        )
                    })
                    if (this.showLinkElement) {
                        const container = this.testElementId(this.containerList)
                        if (!container) {
                            taskParam.bsVmSeqId = taskParam.bsElementId = ''
                        } else {
                            this.buildSelect(false, container)
                        }
                    }
                    this.showLinkElement = false
                }
            } catch (err) {
                this.$bkMessage({
                    message: err.message ? err.message : err,
                    theme: 'error'
                })
            }
            this.pipelineDisabled = false
            loading.build = false
        },

        testPipelineId (pipelineList) {
            const pipeline = pipelineList.find(item => item.pipelineId === this.taskParam.bsPipelineId)
            console.log(pipeline)
            this.taskParam.bsPipelineName = pipeline.pipelineName
            return this.taskParam.bsPipelineName
        },
        testElementId (pipelines) {
            let container
            pipelines.forEach(item => {
                item.elements.forEach(element => {
                    if (element.id === this.taskParam.bsElementId) {
                        container = item
                        this.taskParam.bsVmSeqId = item.id
                        this.scriptList = item.elements
                    }
                })
            })
            return container
        },

        // 构建机列表
        async requestMachineList () {
            const { $store, projectId, loading } = this
            loading.machine = true
            try {
                const res = await $store.dispatch('turbo/requestMachineList', {
                    projectId: projectId
                })
                if (res) {
                    const machineList = []
                    res.forEach(item => {
                        if (item.status === '正常') {
                            machineList.push(item)
                        }
                    })
                    this.machineList = machineList
                }
            } catch (err) {
                this.$bkMessage({
                    message: err.message ? err.message : err,
                    theme: 'error'
                })
            }
            loading.machine = false
        },
        // 构建机选择
        machineSelect (value, item) {
            this.machineParam.bsAgentId = item.agentId
            this.machineParam.machineIp = item.ip
            this.machineParam.machineHostName = item.hostname
            this.testMachineIP()
        },
        // 校验构建机地址
        async testMachineIP () {
            const { projectId, $store, machineParam } = this

            try {
                const res = await $store.dispatch('turbo/requestMachineIp', {
                    projectId: projectId
                })
                if (res) {
                    let geteWay = ''
                    res.forEach(item => {
                        if (item.ip === machineParam.machineIp && item.agentStatus) {
                            geteWay = item.gateway
                            this.machineParam.nodeHashId = item.nodeHashId
                        }
                    })
                    this.citys.forEach(item => {
                        if (item.showName === geteWay) {
                            this.machineParam.machineZone = item.zoneName
                            this.taskParam.city = item.zoneName
                        }
                    })
                }
            } catch (err) {
                this.$bkMessage({
                    message: err.message ? err.message : err,
                    theme: 'error'
                })
                machineParam.machineZone = ''
            }
            // finally {
            //     machineZoneDisabled = machineParam.machineZone ? false : true
            //     console.log('构建机选择之后的校验', this.machineParam)
            // }
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
        },
        // 获取编译器信息
        async getCompilerConfig () {
            const { projectId, $store } = this
            try {
                const res = await $store.dispatch('turbo/getCompilerConfig', {
                    projectId: projectId
                })
                if (res) {
                    const envList = []
                    for (let i = 0; i < res.length; i++) {
                        const envListItem = {}
                        const gccVersion = []
                        const name = res[i]['paramCode']
                        const gccVersionList = res[i]['paramExtend1'].split(';')
                        for (let j = 0; j < gccVersionList.length; j++) {
                            const gccVersionItem = {}
                            gccVersionItem.name = gccVersionList[j]
                            gccVersionItem.version = name + gccVersionList[j]
                            gccVersion.push(gccVersionItem)
                        }
                        envListItem.name = name
                        envListItem.gccVersion = gccVersion
                        envList.push(envListItem)
                    }
                    this.envList = envList
                }
            } catch (err) {
                this.$bkMessage({
                    message: err.message ? err.message : err,
                    theme: 'error'
                })
            }
        }
    }
}

export default buildOption
