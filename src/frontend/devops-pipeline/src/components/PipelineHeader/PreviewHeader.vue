<template>
    <div class="pipeline-preview-header">
        <pipeline-bread-crumb>
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
            <bk-button :disabled="executeStatus" @click="goEdit">
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
                    theme="primary"
                    @click="switchExecStep(executeSteps[0])"
                >
                    {{ $t("prev") }}
                </bk-button>
                <bk-button
                    theme="primary"
                    :disabled="executeStatus"
                    :loading="executeStatus"
                    @click="handleClick"
                >
                    {{ $t("exec") }}
                </bk-button>
            </template>
        </aside>
    </div>
</template>

<script>
    import { mapState, mapActions, mapGetters } from 'vuex'
    import { bus } from '@/utils/bus'
    import PipelineBreadCrumb from './PipelineBreadCrumb'
    export default {
        components: {
            PipelineBreadCrumb
        },
        data () {
            return {
                paramsValid: true
            }
        },
        computed: {
            ...mapState('pipelines', ['executeStep', 'executeStatus']),
            ...mapGetters({
                isEditing: 'atom/isEditing',
                canManualStartup: 'pipelines/canManualStartup'
            }),
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
            executeSteps () {
                return [
                    {
                        title: this.$t('填写参数'),
                        id: 'params',
                        icon: 1
                    },
                    {
                        title: this.$t(this.isDebugPipeline ? '选择调试插件' : '选择执行插件'),
                        id: 'optional',
                        icon: 2
                    }
                ]
            }
        },
        watch: {
            pipelineId (pipelineId) {
                this.$router.push({
                    name: 'pipelinesEdit',
                    params: {
                        projectId: this.projectId,
                        pipelineId
                    }
                })
            }
        },
        methods: {
            ...mapActions('pipelines', ['setExecuteStep']),
            handleClick () {
                bus.$emit('start-execute')
            },
            goEdit () {
                this.$router.push({
                    name: 'pipelinesHistory'
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
