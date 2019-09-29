<template>
    <section class="build-wrapper config-message-wrapper">
        <bk-form :label-width="130" :model="taskParam">
            <bk-form-item label="流水线：">
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
                <a :href="`/console/pipeline/${projectId}/${taskParam.bsPipelineId}/edit${taskParam.bsElementId ? '#' + taskParam.bsElementId : ''}`" target="_blank" v-if="hasPipeline" class="bk-form-link">查看流水线</a>
                <p v-if="errors.has('bsPipelineId')" class="is-danger">{{ errors.first('bsPipelineId') }}</p>
            </bk-form-item>
            <bk-form-item label="构建环境：">
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
                <bk-popover placement="right" :width="200" class="prompt-tips">
                    <i class="bk-icon icon-info-circle"></i>
                    <div slot="content" style="white-space: normal;">
                        <div class="">目前只支持Linux环境</div>
                    </div>
                </bk-popover>
                <p v-if="errors.has('bsVmSeqId')" class="is-danger">{{ errors.first('bsVmSeqId') }}</p>
            </bk-form-item>
            <bk-form-item label="编译脚本：">
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
                <bk-popover placement="right" :width="200" class="prompt-tips">
                    <i class="bk-icon icon-info-circle"></i>
                    <div slot="content" style="white-space: normal;">
                        <div class="">加速方案将为该脚本任务/代码检查原子加速，请按要求修改脚本内容</div>
                    </div>
                </bk-popover>
                <p v-if="errors.has('bsElementId')" class="is-danger">{{ errors.first('bsElementId') }}</p>
            </bk-form-item>
            <bk-form-item label="编译器：" class="form-average-two">
                <div class="bk-form-inline" style="width:226px;">
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
                <div class="bk-form-inline" style="width:226px;">
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
            <bk-form-item label="分布式编译参数：">
                <p class="bk-form-text">{{ taskParam.banDistcc === 'false' ? `加速机器${taskParam.machineNum}台（共${taskParam.cpuNum}核）` : '无' }}</p>
                <div class="build-img-container">
                    <div class="bk-form-inline build-img">
                        <img :src="distccImg" @click="viewImg" alt="分布式编译参数">
                    </div>
                </div>
            </bk-form-item>
            <bk-form-item label="其他参数：">
                <p class="bk-form-text">{{ taskParam.ccacheEnabled === 'true' ? `cache目录大小:${taskParam.cacheSize}Mb` : '无'}}</p>
            </bk-form-item>
            <bk-form-item>
                <bk-button theme="primary" :disabled="isBtnDisabled" @click="configCommit">提交配置信息</bk-button>
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
                distccImg: require('../../assets/images/distcc.png')
                // pipelineList: [],   // 流水线列表
                // containerList: [],  // 配置环境列表
                // scriptList: [],     // 脚本任务列表
            }
        },
        computed: {
            ...mapGetters('turbo', [
                'getRegister',
                'getDialogOpt'
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
            this.requestPipelineList()
            this.taskParam.bsProjectId = this.projectId
        },
        methods: {
            async configCommit () {
                const validate = await this.$validator.validateAll()

                if (!validate || this.errors.any()) {
                    return false
                }
                this.isBtnDisabled = true
                try {
                    let projectName = ''
                    this.onlineProjectList.forEach(project => {
                        if (project.project_code === this.projectId) {
                            projectName = project.project_name
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
                        operType: 'reg',
                        projectName: projectName
                    })
                    if (res) {
                        this.$store.commit('turbo/setRegister', {
                            bsPipelineId: bsPipelineId,
                            bsElementId: bsElementId,
                            gccVersion: gccVersion
                        })
                        this.$store.commit('turbo/modifyProcessHead', {
                            process: 'registSuccess',
                            current: 2
                        })
                    }
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    this.isBtnDisabled = false
                }
            },
            viewImg () {
                this.$store.commit('turbo/setDialogOpt', {
                    isShow: true,
                    hasClose: true,
                    contentType: 'picture',
                    imgSrc: this.distccImg
                })
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '../../assets/scss/conf';
    .config-message-wrapper {
        .build-img-container {
            width: 460px;
            .build-img {
                padding: 10px 40px;
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
        .form-average-two .bk-form-inline {
            width: 226px;
            .text {
                width: 100%;
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
            &+.bk-form-inline {
                margin-left: 8px;
            }
        }
    }
</style>
