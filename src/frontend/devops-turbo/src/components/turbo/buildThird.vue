<template>
    <section class="build-wrapper build-wrapper-third">
        <div class="build-tooltips">
            <i class="bk-icon icon-info-circle-shape"></i>
            <span>选择你导入的构建机并填写相关信息，系统将为你安装必要的软件。</span>
        </div>
        <bk-form :label-width="130" :model="machineParam">
            <bk-form-item label="第三方构建机：">
                <div class="bk-form-inline">
                    <vv-selector
                        name="buildMachineId"
                        v-validate="'required'"
                        :list="machineList"
                        :is-loading="loading.machine"
                        :placeholder="'请选择'"
                        :display-key="'ip'"
                        :setting-key="'ip'"
                        :multi-select="false"
                        :searchable="true"
                        :disabled="false"
                        v-model="machineParam.machineIp"
                        @visibleToggle="visibleMachine"
                        @item-selected="machineSelect">
                        <template>
                            <div class="bk-selector-create-item">
                                <a class="bk-selector-link" target="_blank" :href="`/console/environment/${projectId}/nodeList?type=LINUX`">
                                    <i class="bk-icon icon-plus-circle"></i>
                                    <i class="text">导入新构建机</i>
                                </a>
                            </div>
                        </template>
                    </vv-selector>
                </div>
                <bk-popover placement="right" :width="200" class="prompt-tips">
                    <i class="bk-icon icon-info-circle"></i>
                    <div slot="content" style="white-space: normal;">
                        <div>请将自己的构建机导入到蓝盾平台</div>
                    </div>
                </bk-popover>
                <p v-if="errors.has('buildMachineId')" class="is-danger">{{ errors.first('buildMachineId') }}</p>
            </bk-form-item>
            <bk-form-item label="构建机地址：">
                <div class="bk-form-inline">
                    <vv-selector
                        name="machineZone"
                        v-validate="'required'"
                        :list="citys"
                        :placeholder="'请选择'"
                        :display-key="'showName'"
                        :setting-key="'zoneName'"
                        :multi-select="false"
                        :disabled="false"
                        v-model="machineParam.machineZone">
                    </vv-selector>
                </div>
                <p v-if="errors.has('machineZone')" class="is-danger">{{ errors.first('machineZone') }}</p>
            </bk-form-item>
            <bk-form-item label="编译器：" class="form-average-two">
                <div class="bk-form-inline" style="width:244px;">
                    <vv-selector
                        :list="envList"
                        :display-key="'name'"
                        :setting-key="'name'"
                        :selected.sync="taskParam.envList"
                        :multi-select="false"
                        v-model="taskParam.envList"
                        :disabled="false"
                        @item-selected="envSelect">
                    </vv-selector>
                </div>
                <div class="bk-form-inline" style="width:244px;">
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
            <bk-form-item label="软件安装包：">
                <p class="bk-form-text">LD-Turbo</p>
            </bk-form-item>
            <bk-form-item label="编译脚本设置：" class="form-average-three">
                <div class="bk-form-inline" style="width:160px;">
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
                    <p v-if="errors.has('bsPipelineId')" class="is-danger">{{ errors.first('bsPipelineId') }}</p>
                </div>
                <div class="bk-form-inline" style="width:160px;">
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
                    <p v-if="errors.has('bsVmSeqId')" class="is-danger">{{ errors.first('bsVmSeqId') }}</p>
                </div>
                <div class="bk-form-inline" style="width:160px;">
                    <vv-selector
                        name="bsElementId"
                        v-validate="'required'"
                        :list="scriptList"
                        :placeholder="'请选择编译脚本'"
                        :display-key="'name'"
                        :setting-key="'id'"
                        :multi-select="false"
                        :searchable="true"
                        :disabled="pipelineDisabled"
                        v-model="taskParam.bsElementId">
                    </vv-selector>
                    <p v-if="errors.has('bsElementId')" class="is-danger">{{ errors.first('bsElementId') }}</p>
                </div>
                <a :href="`/console/pipeline/${projectId}/${taskParam.bsPipelineId}/edit${taskParam.bsElementId ? '#' + taskParam.bsElementId : ''}`" target="_blank" v-if="hasPipeline" class="bk-form-link">查看流水线</a>
                <p class="bk-form-text bk-form-tips">选择或新建一条流水线，将其构建环境设置为导入的构建机，并新建一个脚本任务（看下图）。</p>
                <div class="build-img-container">
                    <div class="build-img">
                        <img :src="ccacheImgThumb" @click="viewImg('ccacheImg')" alt="分布式编译参数">
                    </div>
                </div>
            </bk-form-item>
            <bk-form-item label="分布式编译参数：">
                <p class="bk-form-text">{{ taskParam.banDistcc === 'false' ? `加速机器${taskParam.machineNum}台（共${taskParam.cpuNum}核）` : '无' }}</p>
                <div class="build-img-container">
                    <div class="build-img" style="padding: 20px 23px;">
                        <img :src="distccImg" @click="viewImg('distccImg')" alt="分布式编译参数">
                    </div>
                </div>
            </bk-form-item>
            <bk-form-item label="其他参数：">
                <p class="bk-form-text">{{ taskParam.ccacheEnabled === 'true' ? `cache目录大小:${taskParam.cacheSize}Mb` : '无'}}</p>
            </bk-form-item>
            <bk-form-item>
                <bk-button theme="primary" :disabled="isBtnDisabled" @click="configCommit">安装软件并完成配置</bk-button>
                <bk-button theme="default" :disabled="isBtnDisabled" @click="$parent.registCancel">取消</bk-button>
            </bk-form-item>
        </bk-form>
    </section>
</template>

<script>
    import vvSelector from '@/components/common/vv-selector'
    import { mapGetters } from 'vuex'
    import buildOption from '@/components/buildOption'

    export default {
        components: {
            vvSelector
        },
        mixins: [buildOption],
        data () {
            return {
                hasPipeline: false,
                showLinkElement: false,
                pipelineDisabled: false,
                distccImg: require('../../assets/images/distcc.png'),
                ccacheImg: require('../../assets/images/build.png'),
                ccacheImgThumb: require('../../assets/images/build_thumb.png'),
                // machineParam: {
                //     machineIp: '10.7.96'
                // }
                seqId: ''
            }
        },
        computed: {
            ...mapGetters('turbo', [
                'getRegister'
            ]),
            ...mapGetters([
                'onlineProjectList'
            ])
        },
        watch: {
            'taskParam.bsPipelineId' () {
                this.hasPipeline = !!this.taskParam.bsPipelineId
            }
        },
        created () {
            Object.assign(this.taskParam, this.getRegister)
            const { hash } = this.$route
            if (hash) {
                const arrLink = hash.slice(1).split('&')
                this.showLinkElement = true
                this.taskParam.bsPipelineId = arrLink[0]
                this.taskParam.bsElementId = arrLink[1]
                this.pipelineDisabled = true
            }
            this.requestMachineList()
            this.requestPipelineList()
            this.taskParam.bsProjectId = this.projectId
        },
        
        methods: {
            viewImg (imgType) {
                const dialogOpt = {
                    isShow: true,
                    hasClose: true,
                    contentType: 'picture',
                    imgSrc: imgType === 'distccImg' ? this.distccImg : this.ccacheImg,
                    imgType: imgType
                    // , width: typeof width === 'number' ? width : 500
                }
                this.$store.commit('turbo/setDialogOpt', dialogOpt)
            },

            // 安装
            async machineCommit () {
                this.isDisabled = true
                try {
                    console.log('构建机安装参数:', Object.assign({}, this.machineParam, { gccVersion: this.taskParam.gccVersion }, { bsProjectId: this.projectId }))
                    const res = await this.$store.dispatch('turbo/commitMachine', {
                        params: Object.assign({}, this.machineParam, { gccVersion: this.taskParam.gccVersion }, { bsProjectId: this.projectId }),
                        taskId: this.taskParam.taskId,
                        operType: 'reg'
                    })
                    if (res) {
                        this.taskParam.buildMachineId = res.machineId
                        this.installSoftware()
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
            async installSoftware () { // 开始下载安装
                const { $store, projectId, installPolling } = this
                const { taskId, buildMachineId } = this.taskParam
                try {
                    const res = await $store.dispatch('turbo/downloadDistcc', {
                        projectId: projectId,
                        taskId: taskId,
                        machineId: buildMachineId,
                        operType: 'reg'
                    })
                    if (res) {
                        let timeout = 0
                        let second = 5
                        let progress = 0
                        let hasClose = false
                        let currentState = 'primary'
                        const isShow = true
                        this.$store.commit('turbo/setDialogOpt', {
                            isShow: true,
                            hasClose: false,
                            contentType: 'progress',
                            progressWidth: progress + '%',
                            restDate: '00:00:0' + second,
                            currentState: 'primary'
                        })
                        this.seqId = res.seqId
                        // let status = await installPolling() // 轮询开始
                        // if (status !== 'PENDING' || status !== 'PENDING') {
                        const timer = setInterval(async () => {
                            // second === 0 ? '' : (second--)
                            if (second !== 0) {
                                second--
                            }
                            progress >= 80 ? (progress = 99) : (progress += 20)
                            const statu = await installPolling({ second: second, progress: progress })
                            if (statu === 'TIMEOUT' || statu === 'FAILURE') {
                                currentState = 'error'
                                hasClose = true
                                clearInterval(timer)
                            } else if (statu === 'SUCCESS') {
                                currentState = 'success'
                                progress = 100
                                second = 0
                                clearInterval(timer)
                                setTimeout(() => {
                                    this.$store.commit('turbo/setDialogOpt', {
                                        isShow: false
                                    })
                                    this.$store.commit('turbo/modifyProcessHead', {
                                        process: 'registSuccess',
                                        current: 2
                                    })
                                }, 1500)
                            }
                            timeout++
                            if (timeout > 60) {
                                currentState = 'error'
                                hasClose = true
                                clearInterval(timer)
                            }
                            this.$store.commit('turbo/setDialogOpt', {
                                isShow: isShow,
                                hasClose: hasClose,
                                contentType: 'progress',
                                progressWidth: progress + '%',
                                restDate: '00:00:0' + second,
                                currentState: currentState
                            })
                        }, 1000)
                        // }
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            },
            async installPolling () { // 轮询下载状态
                const { seqId, projectId } = this
                const { taskId, buildMachineId } = this.taskParam
                const { nodeHashId } = this.machineParam
                let status = ''
                try {
                    const resulte = await this.$store.dispatch('turbo/requestDownloadStatus', {
                        params: {
                            projectId: projectId,
                            nodeHashId: nodeHashId,
                            machineId: buildMachineId,
                            taskId: taskId,
                            seqId: seqId
                        }
                    })
                    if (resulte) {
                        status = resulte.status
                        return status
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                    status = 'FAILURE'
                    return status
                }
            },
            
            // 提交配置
            async configCommit () { //  machineId
                const validate = await this.$validator.validateAll()
                if (!validate || this.errors.any()) {
                    return false
                }
                this.isDisabled = true
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
                    const { machineZone } = this.machineParam
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
                            city: machineZone,
                            gccVersion: gccVersion,
                            machineNum: machineNum,
                            cpuNum: cpuNum,
                            cacheSize: cacheSize,
                            buildMachineId: buildMachineId
                        }),
                        operType: 'reg',
                        projectName: projectName
                    })
                    if (res) {
                        this.machineCommit()
                        this.$store.commit('turbo/setRegister', {
                            bsPipelineId: bsPipelineId,
                            bsElementId: bsElementId,
                            gccVersion: gccVersion
                        })
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    this.isDisabled = false
                }
            }
        }
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';

    .build-wrapper-third {
        .bk-form {
            .bk-form-content {
                .bk-form-input {
                    width: 496px;
                }
                .bk-form-inline {
                    width: 496px;
                }
                .bk-form-text {
                    width: 496px;
                }
                .bk-form-tips {
                    padding-top: 20px;
                }
            }
        }
        .form-average-two .bk-form-inline {
            width: 244px;
            .text {
                width: 100%;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
            &+.bk-form-inline {
                margin-left: 4px;
            }
        }
        .form-average-three .bk-form-inline {
            width: 160px;
            .text {
                width: 100%;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
            &+.bk-form-inline {
                margin-left: 4px;
            }
        }
        .bk-form-content.form-average .bk-form-text {
            padding: 20px 0 9px 0;
        }
        .build-img-container {
            width: 496px;
            .build-img {
                display: inline-block;
                width: 496px;
                min-height: 150px;
                border: 1px solid $borderWeightColor;
                border-radius: 2px;
                font-size: 14px;
                overflow: hidden;
                > img {
                    width: 100%;
                    cursor: pointer;
                }
            }
        }
    }
    .bk-selector-link {
        display: block;
        &:hover .text {
            color:#3c96ff;
        }
    }
</style>
