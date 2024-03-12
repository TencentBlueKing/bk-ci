<template>
    <div v-if="pipelineName" class="pipeline-preview-header">
        <pipeline-bread-crumb :pipeline-name="pipelineName">
            <span class="build-num-switcher-wrapper">
                {{ title }}
            </span>
        </pipeline-bread-crumb>
        <bk-steps
            class="pipeline-execute-step"
            :cur-step="executeStep"
            @update:value="setExecuteStep"
            :steps="executeSteps"
        />
        <aside class="pipeline-preview-right-aside">
            <bk-button
                :disabled="executeStatus"
                v-perm="{
                    hasPermission: canEdit,
                    disablePermissionApi: true,
                    permissionData: {
                        projectId,
                        resourceType: 'pipeline',
                        resourceCode: pipelineId,
                        action: RESOURCE_ACTION.EDIT
                    }
                }"
                @click="goEdit"
            >
                {{ $t("cancel") }}
            </bk-button>
            <bk-button
                theme="primary"
                v-if="executeStep === 1"
                @click="switchExecStep(executeSteps[1])"
            >
                {{ $t("next") }}
            </bk-button>
            <template v-if="executeStep === 2">
                <bk-button
                    :theme="isDebugPipeline ? '' : 'primary'"
                    @click="switchExecStep(executeSteps[0])"
                >
                    {{ $t("prev") }}
                </bk-button>
                <bk-button
                    theme="primary"
                    :disabled="executeStatus"
                    :loading="executeStatus"
                    v-perm="{
                        hasPermission: canExecute,
                        disablePermissionApi: true,
                        permissionData: {
                            projectId: projectId,
                            resourceType: 'pipeline',
                            resourceCode: pipelineId,
                            action: RESOURCE_ACTION.EXECUTE
                        }
                    }"
                    @click="handleClick"
                >
                    {{ $t(isDebugPipeline ? "debug" : "exec") }}
                </bk-button>
            </template>
        </aside>
    </div>
    <i v-else class="devops-icon icon-circle-2-1 spin-icon" style="margin-left: 20px;" />
</template>

<script>
    import { mapState, mapActions, mapGetters } from 'vuex'
    import { bus, UPDATE_PREVIEW_PIPELINE_NAME } from '@/utils/bus'
    import {
        RESOURCE_ACTION
    } from '@/utils/permission'
    import PipelineBreadCrumb from './PipelineBreadCrumb'
    export default {
        components: {
            PipelineBreadCrumb
        },
        data () {
            return {
                paramsValid: true,
                pipelineName: '--'
            }
        },
        computed: {
            ...mapState('pipelines', ['executeStep', 'executeStatus']),
            ...mapGetters({
                isEditing: 'atom/isEditing',
                canManualStartup: 'pipelines/canManualStartup'
            }),
            ...mapState('atom', [
                'pipelineInfo'
            ]),
            RESOURCE_ACTION () {
                return RESOURCE_ACTION
            },
            title () {
                return this.$t(`details.${this.isDebugPipeline ? 'debug' : 'exec'}Preview`)
            },
            isDebugPipeline () {
                return Object.prototype.hasOwnProperty.call(this.$route.query, 'debug')
            },
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            canEdit () {
                return this.pipelineInfo?.permissions.canEdit ?? true
            },
            canExecute () {
                return this.pipelineInfo?.permissions.canExecute ?? true
            },
            executeSteps () {
                return [
                    {
                        title: this.$t('paramFill'),
                        id: 'params',
                        icon: 1
                    },
                    {
                        title: this.$t('stepPreview'),
                        id: 'optional',
                        icon: 2
                    }
                ]
            }
        },
        watch: {
            pipelineId (pipelineId) {
                this.$router.push({
                    name: 'pipelinesHistory',
                    params: {
                        projectId: this.projectId,
                        pipelineId,
                        type: 'history'
                    }
                })
            }
        },
        mounted () {
            bus.$on(UPDATE_PREVIEW_PIPELINE_NAME, this.updatePipelineName)
        },
        beforeDestroy () {
            bus.$off(UPDATE_PREVIEW_PIPELINE_NAME, this.updatePipelineName)
        },
        methods: {
            ...mapActions('pipelines', ['setExecuteStep']),
            updatePipelineName (name) {
                this.pipelineName = name
            },
            handleClick () {
                bus.$emit('start-execute')
            },
            goEdit () {
                this.$router.push({
                    name: 'pipelinesEdit'
                })
            },
            beforeSwitchStep () {
                return new Promise((resolve, reject) => {
                    if (this.executeStep === 1) {
                        bus.$emit('validate-execute-param-form', resolve)
                    } else {
                        resolve(true)
                    }
                })
            },
            async switchExecStep (step) {
                const result = await this.beforeSwitchStep(step)
                this.paramsValid = result

                if (!result) return
                this.setExecuteStep(step.icon)
            }
        }
    }
</script>

<style lang="scss">
.pipeline-preview-header {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px 0 14px;
  .build-num-switcher-wrapper {
    display: grid;
    grid-auto-flow: column;
    grid-gap: 6px;
  }
  .pipeline-execute-step {
    width: 300px;
    flex-shrink: 0;
  }
  .pipeline-preview-right-aside {
    display: grid;
    grid-gap: 10px;
    grid-auto-flow: column;
    flex-shrink: 0;
  }
}
</style>
