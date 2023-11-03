<template>
    <div class="pipeline-import-edit-header">
        <pipeline-bread-crumb />
        <mode-switch
            :is-yaml-support="isYamlSupport"
            :yaml-invalid-msg="yamlInvalidMsg"
        />
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
    import { mapState, mapGetters, mapActions } from 'vuex'
    import PipelineBreadCrumb from './PipelineBreadCrumb.vue'
    import ModeSwitch from '@/components/ModeSwitch'
    import { UPDATE_PIPELINE_INFO } from '@/store/modules/atom/constants'

    export default {
        components: {
            PipelineBreadCrumb,
            ModeSwitch
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
                'pipelineYaml',
                'pipelineInfo'
            ]),
            ...mapGetters({
                isEditing: 'atom/isEditing',
                checkPipelineInvalid: 'atom/checkPipelineInvalid',
                isYamlSupport: 'atom/isYamlSupport',
                yamlInvalidMsg: 'atom/yamlInvalidMsg'
            })
        },
        methods: {
            ...mapActions('atom', [
                'setPipelineEditing',
                'saveDraftPipeline',
                'setSaveStatus',
                'updateContainer'
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

            async saveDraft () {
                try {
                    this.setSaveStatus(true)
                    const pipeline = Object.assign({}, this.pipeline, {
                        stages: [
                            this.pipeline.stages[0],
                            ...this.pipelineWithoutTrigger.stages
                        ]
                    })
                    const { pipelineSetting, checkPipelineInvalid, pipelineYaml } = this
                    const { inValid, message } = checkPipelineInvalid(pipeline.stages, pipelineSetting)
                    const { projectId } = this.$route.params
                    if (inValid) {
                        throw new Error(message)
                    }
                    // 清除流水线参数渲染过程中添加的key
                    this.formatParams(pipeline)

                    // 请求执行构建
                    const { data: { version, versionName, pipelineId } } = await this.saveDraftPipeline({
                        projectId,
                        storageType: this.pipelineMode,
                        modelAndSetting: {
                            model: {
                                ...pipeline,
                                name: pipelineSetting.pipelineName,
                                desc: pipelineSetting.desc
                            },
                            setting: pipelineSetting
                        },
                        yaml: pipelineYaml
                    })
                    this.setPipelineEditing(false)
                    this.$store.commit(`atom/${UPDATE_PIPELINE_INFO}`, {
                        canDebug: true,
                        canRelease: false,
                        version,
                        versionName
                    })

                    this.$bkMessage({
                        theme: 'success',
                        message: this.$t('editPage.saveDraftSuccess', [pipelineSetting.pipelineName])
                    })
                    this.$router.replace({
                        name: 'pipelinesEdit',
                        params: {
                            projectId,
                            pipelineId
                        }
                    })
                } catch (e) {
                    this.handleError(e, [
                        {
                            actionId: this.$permissionActionMap.edit,
                            resourceId: this.$permissionResourceMap.pipeline,
                            instanceId: [
                                {
                                    id: this.pipeline.pipelineId,
                                    name: this.pipeline.name
                                }
                            ],
                            projectId: this.$route.params.projectId
                        }
                    ])
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
  padding: 0 14px 0 0;
  align-self: stretch;
  height: 48px;
  flex-shrink: 0;
  box-shadow: 0 2px 5px 0 rgba(51, 60, 72, 0.03);

  .pipeline-edit-right-aside {
    display: grid;
    grid-gap: 10px;
    grid-auto-flow: column;
    height: 100%;
    align-items: center;
  }
}

  </style>
