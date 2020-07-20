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
                        <template slot="extension">
                            <div class="bk-selector-create-item">
                                <a class="bk-selector-link" target="_blank" :href="`/console/pipeline/${projectId}/list/allPipeline`">
                                    <i class="bk-icon icon-plus-circle"></i>
                                    <span class="text">添加流水线</span>
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
                        :search-key="'name'"
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
            <bk-form-item label="分布式编译加速：" class="form-item-text">
                <p class="bk-form-text">{{ taskParam.banDistcc === 'false' && typeof taskParam.machineNum !== 'undefined' ? `${ taskParam.machineNum }台加速机器(共${ taskParam.cpuNum }核)` : '无'}}<a href="wxwork://message/?username=DevOps" class="bk-form-link">如需升级资源请联系DevOps小助手</a></p>
            </bk-form-item>
            <bk-form-item label="其他参数：" class="form-item-text">
                <p class="bk-form-text">{{ taskParam.ccacheEnabled === 'true' && typeof taskParam.cacheSize !== 'undefined' ? `ccache目录大小${ taskParam.cacheSize }MB` : '无'}}</p>
            </bk-form-item>
            <bk-form-item>
                <bk-button theme="primary" :disabled="isDisabled" @click="configCommit">保存</bk-button>
                <bk-button theme="default" :disabled="isDisabled" @click="configCancel">取消</bk-button>
            </bk-form-item>
        </bk-form>
    </section>
</template>

<script>
    import vvSelector from '@/components/common/vv-selector'
    import { mapGetters } from 'vuex'
    import buildMixins from '@/components/buildMixins'

    export default {
        components: {
            vvSelector
        },
        mixins: [buildMixins],
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
                loading: {
                    pipelines: false,
                    build: false
                },
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
                taskParam: {
                    banDistcc: '',
                    bsVmSeqId: '',
                    bsElementId: '',
                    bsPipelineId: '',
                    bsPipelineName: '',
                    ccacheEnabled: '',
                    createdDate: '',
                    latestStatus: '',
                    machineType: '',
                    projLang: '',
                    projType: '',
                    taskId: '',
                    taskName: ''
                }
            }
        },
        computed: {
            ...mapGetters([
                'enableProjectList'
            ])
        },
        created () {
            const { taskParam, taskInfo, programList, requestPipelineList } = this
            Object.assign(taskParam, taskInfo)
            !taskParam.cacheSize && (taskParam.cacheSize = 1024)
            !taskParam.machineNum && (taskParam.machineNum = 18) && (taskParam.cpuNum = 144)
            !taskParam.gccVersion && (taskParam.gccVersion = 'gcc4.4.6')
            programList.forEach(item => {
                (item.banDistcc === taskParam.banDistcc && item.ccacheEnabled === taskParam.ccacheEnabled) && (this.accelerateId = item.id)
            })
            if (taskParam.bsElementId) {
                this.showLinkElement = true
                this.pipelineDisabled = true
                requestPipelineList()
            }
        },
        methods: {
            async configCommit () {
                const validate = await this.$validator.validateAll()
                if (!validate || this.errors.any()) {
                    return false
                }
                this.isLoading = true
                try {
                    if (!this.taskParam.bsPipelineName) {
                        this.pipelineList.forEach(item => {
                            if (this.taskParam.bsPipelineId === item.pipelineId) {
                                this.taskParam.bsPipelineName = item.pipelineName
                            }
                        })
                    }
                    let projectName = ''
                    this.enableProjectList.forEach(project => {
                        if (project.projectCode === this.projectId) {
                            projectName = project.projectName
                        }
                    })
                    const { taskId, taskName, ccacheEnabled, banDistcc, bsPipelineId, bsPipelineName, bsVmSeqId, bsElementId, gccVersion, machineNum, cpuNum, cacheSize } = this.taskParam
                    const res = await this.$store.dispatch('turbo/commitBuild', {
                        params: Object.assign({
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
                            cacheSize: cacheSize
                        }),
                        operType: 'conf',
                        projectName: projectName
                    })
                    // const res = await this.$store.dispatch('turbo/commitBuild', {
                    //     params: Object.assign({}, this.taskParam),
                    //     operType: 'conf',
                    //     projectName: projectName
                    // })
                    if (res) {
                        this.$emit('configCancel')
                        this.$bkMessage({
                            message: '保存成功',
                            theme: 'success'
                        })
                        this.$emit('buildSetting', this.taskParam)
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
            
            // 验证已选配置
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
            }
        }
    }
</script>
