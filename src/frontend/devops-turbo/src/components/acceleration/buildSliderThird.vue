<template>
    <section class="build-wrapper" v-bkloading="{ isLoading: isLoading }">
        <bk-form :label-width="130" :model="taskParam">
            <bk-form-item label="任务 ID：" class="form-item-text">
                <p class="bk-form-text">{{ taskParam.taskId }}<span class="desc-ash">（系统自动生成）</span></p>
            </bk-form-item>
            <bk-form-item label="创建时间：" class="form-item-text">
                <p class="bk-form-text">{{ transDateStr(taskParam.createdDate) }}</p>
            </bk-form-item>
            <bk-form-item label="创建人：" class="form-item-text">
                <p class="bk-form-text">{{ taskParam.createdBy }}</p>
            </bk-form-item>
            <devops-form-item label="任务名称：" :required="true" :property="'taskName'"
                :is-error="errors.has('taskName')"
                :error-msg="errors.first('taskName')">
                <bk-input
                    type="text"
                    name="taskName"
                    placeholder="请输入任务名称"
                    v-validate="'max:20|required'"
                    v-model.trim="taskParam.taskName">
                </bk-input>
            </devops-form-item>
            <bk-form-item label="构建类型：" class="form-item-text">
                <p class="bk-form-text">{{ taskParam.machineType === '1' ? '公共构建机' : taskParam.machineType === '2' ? '第三方构建机' : '下载安装包至自己的构建机，进行构建和加速' }}</p>
            </bk-form-item>
            <bk-form-item label="构建机系统：" class="form-item-text">
                <p class="bk-form-text">{{ paramsMap['osType'][taskParam.osType] }}</p>
            </bk-form-item>
            <bk-form-item label="项目语言：" class="form-item-text">
                <p class="bk-form-text">{{ setLanguage(taskParam.projLang) }}</p>
            </bk-form-item>
            <bk-form-item label="项目类型：" class="form-item-text">
                <p class="bk-form-text">{{ taskParam.projType === '1' ? '后台服务' : (taskParam.projType === '2' ? 'Unity3D' : 'UE4') }}</p>
            </bk-form-item>
            <bk-form-item label="构建工具：" class="form-item-text">
                <p class="bk-form-text">{{ paramsMap['toolType'][taskParam.toolType] }}</p>
            </bk-form-item>
            <bk-form-item label="加速方案：" class="form-item-text label-item-input">
                <div class="bk-form-inline">
                    <vv-selector
                        name="accelerateId"
                        v-validate="'required'"
                        :list="programList"
                        :placeholder="'请选择加速方案'"
                        :display-key="'name'"
                        :setting-key="'id'"
                        :multi-select="false"
                        :disabled="pipelineDisabled"
                        v-model="accelerateId"
                        @item-selected="accelerateSelect">
                    </vv-selector>
                </div>
                <p v-if="errors.has('accelerateId')" class="is-danger">{{ errors.first('accelerateId') }}</p>
            </bk-form-item>
            <bk-form-item label="第三方构建机：" class="form-item-text label-item-input">
                <div class="bk-form-inline">
                    <vv-selector
                        name="bsMachineId"
                        v-validate="'required'"
                        :list="machineList"
                        :is-loading="loading.machine"
                        :placeholder="'请选择'"
                        :display-key="'ip'"
                        :setting-key="'ip'"
                        :multi-select="false"
                        :searchable="true"
                        :disabled="false"
                        v-model="taskParam.machineIp"
                        @visibleToggle="visibleMachine"
                        @item-selected="machineSelect">
                    </vv-selector>
                </div>
                <p v-if="errors.has('bsMachineId')" class="is-danger">{{ errors.first('bsMachineId') }}</p>
            </bk-form-item>
            <bk-form-item label="构建机地址：" class="form-item-text label-item-input">
                <div class="bk-form-inline">
                    <vv-selector
                        name="machineZone"
                        v-validate="'required'"
                        :list="citys"
                        :placeholder="'请选择'"
                        :display-key="'showName'"
                        :setting-key="'zoneName'"
                        :multi-select="false"
                        :disabled="machineZoneDisabled"
                        v-model="machineInfo.machineZone">
                    </vv-selector>
                </div>
                <p v-if="errors.has('machineZone')" class="is-danger">{{ errors.first('machineZone') }}</p>
            </bk-form-item>
            <bk-form-item label="编译器：" class="form-item-text label-item-input form-average-two-small">
                <div class="bk-form-inline" style="width:184px;">
                    <vv-selector
                        :list="envList"
                        :display-key="'name'"
                        :setting-key="'name'"
                        :selected.sync="taskParam.envList"
                        :multi-select="false"
                        :disabled="false"
                        v-model="taskParam.envList"
                        @item-selected="envSelect">
                    </vv-selector>
                </div>
                <div class="bk-form-inline" style="width:184px;">
                    <vv-selector
                        name="gccVersion"
                        v-validate="'required'"
                        :list="gccVersion"
                        :display-key="'name'"
                        :setting-key="'version'"
                        :selected.sync="taskParam.gccVersion"
                        :multi-select="false"
                        v-model="taskParam.gccVersion"
                        :disabled="false">
                    </vv-selector>
                    <p v-if="errors.has('gccVersion')" class="is-danger">{{ errors.first('gccVersion') }}</p>
                </div>
            </bk-form-item>
            <bk-form-item label="软件安装包：" class="form-item-text">
                <p class="bk-form-text">LD-Turbo
                    <span v-if="installStatus === 'installing' && softwareInstalled === 'false'" class="mg-left-10 primary">正在安装中...</span>
                    <template v-else-if="installStatus === 'success' || softwareInstalled === 'true'">
                        <span class="mg-left-10 success">安装成功</span>
                        <a href="javascript: void(0);" class="mg-left-10 text-link" @click.stop="installSoftware">重新安装</a>
                    </template>
                    <template v-else-if="installStatus === 'error' || (!installStatus && softwareInstalled === 'false')">
                        <span class="mg-left-10 danger">安装异常</span>
                        <a href="javascript: void(0);" class="mg-left-10 text-link" @click.stop="installSoftware">重新安装</a>
                    </template>
                </p>
            </bk-form-item>
            <bk-form-item label="流水线：" class="form-item-text label-item-input">
                <div class="bk-form-inline">
                    <vv-selector
                        name="bsPipelineId"
                        v-validate="'required'"
                        :list="pipelineList"
                        :is-loading="loading.pipelines"
                        :placeholder="'请选择流水线'"
                        :display-key="'pipelineName'"
                        :setting-key="'pipelineId'"
                        :multi-select="false"
                        :searchable="true"
                        :disabled="pipelineDisabled"
                        v-model="taskParam.bsPipelineId"
                        @visibleToggle="visiblePipeline"
                        @item-selected="pipelineSelect">
                        <template>
                            <div class="bk-selector-create-item">
                                <a class="bk-selector-link" target="_blank" :href="`/console/pipeline/${projectId}/list/allPipeline`">
                                    <i class="bk-icon icon-plus-circle"></i>
                                    <i class="text">添加流水线</i>
                                </a>
                            </div>
                        </template>
                    </vv-selector>
                </div>
                <p v-if="errors.has('bsPipelineId')" class="is-danger">{{ errors.first('bsPipelineId') }}</p>
            </bk-form-item>
            <bk-form-item label="构建环境：" class="form-item-text label-item-input">
                <div class="bk-form-inline">
                    <vv-selector
                        name="bsVmSeqId"
                        v-validate="'required'"
                        :list="containerList"
                        :is-loading="loading.build"
                        :placeholder="'请选择构建环境'"
                        :display-key="'name'"
                        :setting-key="'id'"
                        :multi-select="false"
                        :searchable="true"
                        :disabled="pipelineDisabled"
                        v-model="taskParam.bsVmSeqId"
                        @item-selected="buildSelect">
                    </vv-selector>
                </div>
                <p v-if="errors.has('bsVmSeqId')" class="is-danger">{{ errors.first('bsVmSeqId') }}</p>
            </bk-form-item>
            <bk-form-item label="编译脚本：" class="form-item-text label-item-input">
                <div class="bk-form-inline">
                    <vv-selector
                        name="bsElementId"
                        v-validate="'required'"
                        :list="scriptList"
                        :placeholder="'请选择脚本任务/代码检查原子'"
                        :display-key="'name'"
                        :setting-key="'id'"
                        :multi-select="false"
                        :searchable="true"
                        :disabled="pipelineDisabled"
                        v-model="taskParam.bsElementId">
                    </vv-selector>
                </div>
                <p v-if="errors.has('bsElementId')" class="is-danger">{{ errors.first('bsElementId') }}</p>
            </bk-form-item>
            <bk-form-item label="分布式编译加速：" class="form-item-text">
                <p class="bk-form-text">{{ taskParam.banDistcc === 'false' && typeof taskParam.machineNum !== 'undefined' ? `${ taskParam.machineNum }台加速机器(共${ taskParam.cpuNum }核)` : '无'}}<a href="wxwork://message/?username=DevOps" class="bk-form-link">如需升级资源请联系DevOps小助手</a></p>
            </bk-form-item>
            <bk-form-item label="其他参数：" class="form-item-text">
                <p class="bk-form-text">{{ taskParam.ccacheEnabled === 'true' && typeof taskParam.cacheSize !== 'undefined' ? `ccache目录大小${ taskParam.cacheSize }MB` : '无'}}</p>
            </bk-form-item>
            <bk-form-item>
                <bk-button theme="primary" :disabled="isDisabled" @click="configCommit()">保存</bk-button>
                <bk-button theme="default" :disabled="isDisabled" @click="configCancel">取消</bk-button>
            </bk-form-item>
        </bk-form>
    </section>
</template>

<script>
    import vvSelector from '@/components/common/vv-selector'
    import { mapGetters, mapMutations } from 'vuex'
    
    export default {
        components: {
            vvSelector
        },
        props: {
            taskInfo: {
                type: Object
            },
            envList: {
                type: Array
            },
            isLoading: {
                type: Boolean,
                default: false
            }
        },
        data () {
            return {
                isDisabled: false, // 按钮状态
                showLinkElement: false, // 备选
                pipelineDisabled: false,
                accelerateId: '',
                seqId: '',
                installStatus: '',
                softwareInstalled: '',
                hasMachine: false,
                paramsMap: {
                    'osType': {
                        '1': 'tlinux'
                    },
                    'compilerType': {
                        '1': 'gcc',
                        '2': 'clang'
                    },
                    'toolType': {
                        '1': 'make/cmake',
                        '2': 'blade',
                        '3': 'bazel'
                    }
                },
                gccVersion: [],
                programList: [ // 配置方案
                    {
                        name: 'ccache+distcc',
                        id: '1',
                        banDistcc: 'false',
                        ccacheEnabled: 'true'
                    },
                    {
                        name: 'ccache',
                        id: '2',
                        banDistcc: 'true',
                        ccacheEnabled: 'true'
                    },
                    {
                        name: 'distcc',
                        id: '3',
                        banDistcc: 'false',
                        ccacheEnabled: 'false'
                    }
                ],
                pipelineList: [], // 流水线列表
                containerList: [], // 配置环境列表
                scriptList: [], // 脚本任务列表
                loading: {
                    pipelines: false,
                    build: false,
                    machine: false
                },
                taskParam: {
                    banDistcc: '',
                    bsVmSeqId: '',
                    bsElementId: '',
                    bsPipelineId: '',
                    bsPipelineName: '',
                    ccacheEnabled: '',
                    createdDate: '',
                    gccVersion: '',
                    latestStatus: '',
                    machineType: '',
                    projLang: '',
                    projType: '',
                    taskId: '',
                    taskName: '',
                    projectName: '',
                    city: '',
                    buildMachineId: ''
                },

                // 构建机相关
                machineZoneDisabled: false,
                machineList: [],
                machineInfo: {
                    bsAgentId: '',
                    machineIp: '',
                    machineHostName: '',
                    machineZone: '',
                    nodeHashId: ''
                },
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
                ]
            }
        },
        computed: {
            ...mapGetters([
                'onlineProjectList'
            ]),
            ...mapGetters('turbo', [
                'getSoftwareInstallList'
            ]),
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            'taskParam.buildMachineId' (newVal, oldVal) {
                if (newVal) {
                    const install = this.getSoftwareInstallList.find(item => item.buildMachineId === newVal)
                    this.installStatus = install ? install.status : ''
                }
                console.log(newVal, oldVal, this.installStatus)
            }
        },
        created () {
            this.init()
        },
        methods: {
            ...mapMutations('turbo', [
                'setInstallTimer',
                'clearInstallTimer'
            ]),
            visiblePipeline (open) {
                open && this.requestPipelineList()
            },
            visibleMachine (open) {
                open && this.requestMachineList()
            },
            pipelineSelect (value, item) {
                const { requestContainerList, taskParam } = this
                this.containerList = []
                taskParam.bsPipelineName = item.pipelineName
                requestContainerList()
                taskParam.bsVmSeqId = ''
                taskParam.bsElementId = ''
                // taskParam.gccVersion = 'gcc4.4.6'
            },
            envSelect (value, item) { // 选择编译器
                this.gccVersion = item.gccVersion
            },
            buildSelect (value, item) {
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
            accelerateSelect (value, item) {
                Object.assign(this.taskParam, {
                    banDistcc: item.banDistcc,
                    ccacheEnabled: item.ccacheEnabled
                })
            },
            
            // 提交配置
            async configCommit (isReloadSoft) { //  machineId
                const validate = await this.$validator.validateAll()
                if (!validate || this.errors.any()) {
                    return false
                }
                this.isLoading = true
                try {
                    let projectName = ''
                    this.onlineProjectList.forEach(project => {
                        if (project.project_code === this.projectId) {
                            projectName = project.project_name
                        }
                    })
                    if (!this.taskParam.bsPipelineName) {
                        this.pipelineList.forEach(item => {
                            if (this.taskParam.bsPipelineId === item.pipelineId) {
                                this.taskParam.bsPipelineName = item.pipelineName
                            }
                        })
                    }
                    const { taskId, taskName, ccacheEnabled, banDistcc, bsPipelineId, bsPipelineName, bsVmSeqId, bsElementId, gccVersion, machineNum, cpuNum, cacheSize, buildMachineId } = this.taskParam
                    const res = await this.$store.dispatch('turbo/commitBuild', {
                        params: {
                            taskId: taskId,
                            taskName: taskName,
                            ccacheEnabled: ccacheEnabled,
                            banDistcc: banDistcc,
                            bsProjectId: this.projectId,
                            bsPipelineId: bsPipelineId,
                            bsPipelineName: bsPipelineName,
                            bsVmSeqId: bsVmSeqId,
                            bsElementId: bsElementId,
                            gccVersion: gccVersion,
                            machineNum: machineNum,
                            cpuNum: cpuNum,
                            cacheSize: cacheSize,
                            city: this.machineInfo.machineZone,
                            buildMachineId: buildMachineId
                        },
                        operType: 'conf',
                        projectName: projectName
                    })
                    if (res && !isReloadSoft) {
                        this.machineCommit()
                        this.$emit('configCancel')
                        this.$bkMessage({
                            message: '保存成功',
                            theme: 'success'
                        })
                        this.$emit('buildSetting', this.taskParam)
                    } else if (res && isReloadSoft) {
                        await this.machineCommit()
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    this.isLoading = false
                }
            },
            configCancel () {
                this.$emit('configCancel')
            },
            // 流水线
            async requestPipelineList () {
                const { $store, projectId, loading, pipelineList, taskParam, showLinkElement, testPipelineId } = this
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
                        if (showLinkElement && testPipelineId(pipelineList)) {
                            this.requestContainerList()
                        } else if (showLinkElement && !testPipelineId(pipelineList)) {
                            taskParam.bsPipelineId = taskParam.bsVmSeqId = taskParam.bsElementId = ''
                            this.showLinkElement = false
                        }
                        pipelineList.sort(this.sortByKey('createTime'))
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
            // 流水线构建
            async requestContainerList () {
                const { $store, projectId, loading, taskParam } = this
                loading.build = true
                try {
                    const res = await $store.dispatch('turbo/requestContainerList', {
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
                            const containerId = this.testElementId(this.containerList)
                            if (!containerId) {
                                taskParam.bsVmSeqId = taskParam.bsElementId = ''
                            } else {
                                this.buildSelect(false, containerId)
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
                return pipelineList.find(pipeline => pipeline.pipelineId === this.taskParam.bsPipelineId)
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
                        const machine = this.testMachine()
                        if (this.hasMachine && machine) {
                            this.machineSelect(false, machine)
                        }
                    }

                    // const res = await $store.dispatch('turbo/requestMachineByProject', {
                    //     projectId: projectId
                    // })
                    // if (res) {
                    //     this.machineList = res
                    //     let machine = this.testMachine()
                    //     if (this.hasMachine && machine) {
                    //         this.machineSelect(false, machine)
                    //     }
                    // }
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
                console.log(item)
                this.machineInfo.bsAgentId = item.agentId
                this.machineInfo.machineIp = item.ip
                this.machineInfo.machineHostName = item.hostname
                if (value) {
                    this.testSoftInstall(item.ip)
                }
                this.testMachineIP(value ? false : this.taskParam.city)
            },
            // 校验构建机
            testMachine () {
                let machine
                this.machineList.forEach(item => {
                    if (item.machineId === this.taskParam.buildMachineId) {
                        machine = item
                    }
                })
                return machine
            },
            // 校验构建机地址
            async testMachineIP (city) {
                const { projectId, $store, machineInfo } = this
                try {
                    const res = await $store.dispatch('turbo/requestMachineIp', {
                        projectId: projectId
                    })
                    if (res) {
                        let geteWay = ''
                        res.forEach(item => {
                            // console.log(item.ip, machineInfo.machineIp)
                            if (item.ip === machineInfo.machineIp && item.agentStatus && item.nodeType === 'THIRDPARTY') {
                                geteWay = item.gateway
                                machineInfo.nodeHashId = item.nodeHashId
                            }
                        })
                        this.citys.forEach(item => {
                            if (item.showName === geteWay) {
                                if (city) {
                                    city === item.showName ? (this.machineInfo.machineZone = item.zoneName) : (this.machineInfo.machineZone = city)
                                } else {
                                    this.machineInfo.machineZone = item.zoneName
                                }
                            }
                        })
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                    machineInfo.machineZone = ''
                } finally {
                    console.log('构建机选择之后的校验', this.machineInfo)
                }
            },
            // 校验构建机软件安装状态
            async testSoftInstall (ip) {
                const { projectId, $store } = this
                try {
                    const res = await $store.dispatch('turbo/requestSoftInstall', {
                        projectId: projectId,
                        ip: ip
                    })
                    if (res) {
                        console.log(res)
                        this.softwareInstalled = res.softwareInstalled
                        if (!res.softwareInstalled) {
                            this.installStatus = 'error'
                            this.softwareInstalled = 'false'
                        }
                        console.log(this.installStatus)
                        this.taskParam.buildMachineId = res.machineId
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            },
            // 安装
            async machineCommit () {
                const validate = await this.$validator.validateAll()
                if (!validate || this.errors.any()) {
                    return false
                }
                this.isDisabled = true
                try {
                    const res = await this.$store.dispatch('turbo/commitMachine', {
                        params: Object.assign({}, this.machineInfo, { gccVersion: this.taskParam.gccVersion }, { bsProjectId: this.projectId }, { machineId: this.taskParam.buildMachineId }),
                        taskId: this.taskParam.taskId,
                        operType: 'conf'
                    })
                    if (res) {
                        this.taskParam.buildMachineId = res.machineId
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    this.isDisabled = false
                }
            },
            async installSoftware () {
                // 重新安装需要保存一下，不然新的构建机没有一些必要信息，安装不成功
                await this.configCommit(true)
                const { $store, projectId, installPolling } = this
                const { taskId, buildMachineId } = this.taskParam
                console.log(taskId, buildMachineId)
                try {
                    const res = await $store.dispatch('turbo/downloadDistcc', {
                        projectId: projectId,
                        taskId: taskId,
                        machineId: buildMachineId,
                        operType: 'conf'
                    })
                    if (res) {
                        this.seqId = res.seqId
                        let requestNum = 0
                        this.installStatus = 'installing'
                        this.softwareInstalled = 'false'
                        const firstInstall = await installPolling({ timer: null })
                        if (firstInstall === 'PENDING') {
                            const timer = setInterval(async () => {
                                requestNum++
                                await installPolling({
                                    timer: timer,
                                    requestNum: requestNum
                                })
                            }, 4000)
                        }
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            },
            async installPolling ({ timer, requestNum }) { // 轮询下载状态
                const { $store, seqId, projectId, setStatu } = this
                const { taskId, buildMachineId } = this.taskParam
                const { nodeHashId } = this.machineInfo
                let statu = ''
                let installIndex = this.getInstallIndex(this.getSoftwareInstallList, buildMachineId) // 查询是否有安装轮询
                if (installIndex || installIndex === 0) {
                    const installTimer = this.getSoftwareInstallList[installIndex].timer
                    !installTimer && ($store.commit('turbo/setInstallTimer', {
                        installIndex: installIndex,
                        timer: timer
                    }))
                } else {
                    $store.commit('turbo/setInstall', {
                        buildMachineId: buildMachineId,
                        timer: timer,
                        status: 'installing'
                    })
                    installIndex = this.getSoftwareInstallList.length - 1
                }
                try {
                    const resulte = await $store.dispatch('turbo/requestDownloadStatus', {
                        params: {
                            projectId: projectId,
                            nodeHashId: nodeHashId,
                            machineId: buildMachineId,
                            taskId: taskId,
                            seqId: seqId
                        }
                    })
                    if (resulte) {
                        statu = resulte.status
                        if (statu === 'TIMEOUT' || statu === 'FAILURE') {
                            setStatu('error', installIndex)
                        } else if (statu === 'SUCCESS') {
                            setStatu('success', installIndex)
                        }
                        return statu
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                    setStatu('error', installIndex)
                } finally {
                    if (statu !== 'SUCCESS' && requestNum >= 15) {
                        setStatu('error', installIndex)
                    }
                }
            },
            setStatu (status, installIndex) {
                this.installStatus = status
                this.$store.dispatch('turbo/setInstallOnly', { installIndex: installIndex,
                                                               param: {
                                                                   status: status
                                                               } })
            },

            setLanguage (lang) {
                const langArr = lang.split(',')
                let langStr = ''
                langArr.forEach(item => {
                    this.optLanguage.forEach(itemLang => {
                        if (item === itemLang.value) {
                            langStr += langStr ? '、' + itemLang.label : itemLang.label
                        }
                    })
                })
                return langStr
            },

            // 排序
            sortByKey (sortKey) {
                return (obj1, obj2) => {
                    const val1 = obj1[sortKey]
                    const val2 = obj2[sortKey]
                    if (val1 < val2) { // 正序
                        return 1
                    } else if (val1 > val2) {
                        return -1
                    } else {
                        return 0
                    }
                }
            },

            init () {
                const { taskParam, taskInfo, programList, machineInfo } = this
                // 参数初始化
                Object.assign(taskParam, taskInfo)
                !taskParam.cacheSize && (taskParam.cacheSize = 1024)
                !taskParam.machineNum && (taskParam.machineNum = 18) && (taskParam.cpuNum = 144)
                !taskParam.gccVersion && (taskParam.gccVersion = 'gcc4.4.6')
                machineInfo.machineZone = taskParam.city
                machineInfo.bsAgentId = taskParam.bsAgentId
                machineInfo.machineIp = taskParam.machineIp
                machineInfo.nodeHashId = taskParam.nodeHashId
                machineInfo.machineHostName = taskParam.machineHostName
                programList.forEach(item => {
                    (item.banDistcc === taskParam.banDistcc && item.ccacheEnabled === taskParam.ccacheEnabled) && (this.accelerateId = item.id)
                })
                if (taskParam.bsElementId) {
                    this.showLinkElement = true
                    this.pipelineDisabled = true
                    this.requestPipelineList()
                }
                if (taskParam.buildMachineId && taskParam.machineIp) {
                    this.hasMachine = true
                    this.requestMachineList()
                    this.testSoftInstall(taskParam.machineIp)
                }
            },
            getInstallIndex (arrays, buildMachineId) {
                let length = arrays.length
                while (length--) {
                    if (arrays[length].buildMachineId === buildMachineId) {
                        return length
                    }
                }
                return false
            }
        }
    }
</script>
