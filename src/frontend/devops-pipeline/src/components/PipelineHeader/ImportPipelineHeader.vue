<template>
    <div class="pipeline-import-edit-header">
        <template-bread-crumb
            v-if="isTemplate"
            :template-name="pipeline?.name"
            :is-loading="!pipeline"
        />
        <pipeline-bread-crumb v-else />
        <mode-switch draft />
        <aside class="pipeline-edit-right-aside">
            <bk-button
                :disabled="saveStatus"
                :loading="saveStatus"
                outline
                theme="primary"
                @click="saveDraft"
            >
                {{ $t("saveDraft") }}
            </bk-button>
        </aside>
    </div>
</template>

<script>
    import ModeSwitch from '@/components/ModeSwitch'
    import TemplateBreadCrumb from '@/components/template/TemplateBreadCrumb.vue'
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    import { TEMPLATE_TYPE } from '@/utils/pipelineConst'
    import {
        showPipelineCheckMsg
    } from '@/utils/util'
    import { mapActions, mapGetters, mapState } from 'vuex'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'

    export default {
        components: {
            PipelineBreadCrumb,
            TemplateBreadCrumb,
            ModeSwitch
        },
        data () {
            return {
                RESOURCE_ACTION
            }
        },
        computed: {
            ...mapState([
                'pipelineMode'
            ]),
            ...mapState('atom', [
                'pipeline',
                'saveStatus',
                'pipelineWithoutTrigger',
                'pipelineSetting',
                'pipelineYaml'
            ]),
            ...mapGetters({
                isEditing: 'atom/isEditing',
                checkPipelineInvalid: 'atom/checkPipelineInvalid'
            }),
            projectId () {
                return this.$route.params.projectId
            },
            isTemplate () {
                return !!TEMPLATE_TYPE[this.pipelineSetting?.type]
            }
        },
        methods: {
            ...mapActions('atom', [
                'setPipelineEditing',
                'saveDraftPipeline',
                'setSaveStatus',
                'updateContainer',
                'requestPipelineSummary',
                'requestTemplateSummary',
                'saveDraftTemplate'
            ]),
            formatParams (pipeline) {
                const params = pipeline.stages[0].containers[0].params
                const paramList
                    = params
                        && params.map((param) => {
                            const { paramIdKey, ...temp } = param
                            return temp
                        })
                this.updateContainer({
                    container: this.pipeline.stages[0].containers[0],
                    newParam: {
                        params: paramList
                    }
                })
            },

            async handleSaveTemplatePipelineDraft (params) {
                const { data: { templateId, version } } = await this.saveDraftTemplate(params)

                this.$showTips({
                    message: this.$t('editPage.saveDraftSuccess', [this.pipelineSetting.pipelineName]),
                    theme: 'success'
                })
                this.setPipelineEditing(false)

                await this.requestTemplateSummary({
                    projectId: this.$route.params.projectId,
                    templateId
                })

                this.$router.push({
                    name: 'templateEdit',
                    params: {
                        ...this.$route.params,
                        type: 'pipeline',
                        version,
                        templateId
                    }
                })
            },
            async handleSavePipelineDraft (params) {
                const { data: { pipelineId } } = await this.saveDraftPipeline(params)

                this.setPipelineEditing(false)

                this.$bkMessage({
                    theme: 'success',
                    message: this.$t('editPage.saveDraftSuccess', [this.pipelineSetting.pipelineName]),
                    limit: 1
                })

                await this.requestPipelineSummary({
                    projectId: this.$route.params.projectId,
                    pipelineId
                })

                this.$router.replace({
                    name: 'pipelinesEdit',
                    params: {
                        projectId: this.$route.params.projectId,
                        pipelineId
                    }
                })
            },

            async saveDraft () {
                try {
                    this.setSaveStatus(true)
                    const pipeline = Object.assign({}, this.pipeline, {
                        stages: [
                            this.pipeline.stages[0],
                            ...this.pipelineWithoutTrigger.stages
                        ],
                        ...(this.isTemplate && { name: this.pipelineSetting.pipelineName })
                    })
                    const { pipelineSetting, checkPipelineInvalid, pipelineYaml } = this
                    const { inValid, message } = checkPipelineInvalid(pipeline.stages, pipelineSetting)
                    const { projectId } = this.$route.params
                    const model = {
                        ...pipeline,
                        name: pipelineSetting.pipelineName,
                        desc: pipelineSetting.desc
                    }
                    if (inValid) {
                        throw new Error(message)
                    }
                    // 清除流水线参数渲染过程中添加的key
                    this.formatParams(pipeline)
                    const params = this.isTemplate
                        ? {
                            projectId,
                            storageType: this.pipelineMode,
                            model,
                            templateSetting: pipelineSetting,
                            yaml: pipelineYaml,
                            type: pipelineSetting?.type
                        }
                        : {
                            projectId,
                            storageType: this.pipelineMode,
                            modelAndSetting: {
                                model,
                                setting: pipelineSetting
                            },
                            yaml: pipelineYaml
                        }

                    // 请求执行构建
                    if (this.isTemplate) {
                        await this.handleSaveTemplatePipelineDraft(params)
                    } else {
                        await this.handleSavePipelineDraft(params)
                    }
                } catch (e) {
                    if (e.code === 2101244) {
                        showPipelineCheckMsg(this.$bkMessage, e.message, this.$createElement)
                    } else {
                        this.handleError(e)
                    }
                } finally {
                    this.setSaveStatus(false)
                }
            }
        }
    }
</script>

<style lang="scss">
@import '@/scss/conf';
.pipeline-import-edit-header {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  padding: 0 14px;
  align-self: stretch;
  height: 48px;
  flex-shrink: 0;
  box-shadow: 0 2px 5px 0 rgba(51, 60, 72, 0.03);
  border-bottom: 1px solid #DCDEE5;

  .pipeline-edit-right-aside {
    display: grid;
    grid-gap: 10px;
    grid-auto-flow: column;
    height: 100%;
    align-items: center;
  }
}

  </style>
