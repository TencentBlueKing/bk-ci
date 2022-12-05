<template>
    <div class="pipelines-triggers">
        <div class="pipeline-trigger-wrapper" @click="toggleStatus">
            <slot name="exec-bar" :isDisable="disabled">
                <div class="pipeline-trigger-item pipeline-disable" v-if="!canManualStartup">
                    <i class="devops-icon icon-displayable trigger-icon" :title="$t('newlist.cannotManual')"></i>
                </div>
                <div class="pipeline-trigger-item pipeline-ready" v-if="canManualStartup && status !== 'running'">
                    <i class="devops-icon icon-right-shape trigger-icon"></i>
                </div>
                <div class="pipeline-trigger-item pipeline-running" v-if="canManualStartup && status === 'running'">
                    <i alt="running" class="devops-icon icon-circle-2-1 spin-icon"></i>
                </div>
            </slot>
        </div>
    </div>
</template>

<script>
    import { bus } from '@/utils/bus'
    import { mapState } from 'vuex'
    export default {
        props: {
            beforeExec: {
                type: Function
            },
            canManualStartup: {
                type: [Boolean, Number],
                default: true
            },
            status: {
                type: String,
                default: 'ready'
            },
            pipelineId: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                paramList: [],
                paramValues: {},
                disabled: false
            }
        },
        computed: {
            ...mapState('atom', [
                'executeStatus'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            replay () {
                return this.$store.state.pipelines.replay
            }
        },
        watch: {
            replay (val, oldVal) {
                if (val) {
                    this.$store.state.pipelines.replay = false
                    this.toggleStatus()
                }
            },
            executeStatus (executing) {
                this.disabled = executing
            }
        },
        mounted () {
            bus.$on('trigger-excute', this.toggleStatus)
        },
        beforeDestroy () {
            bus.$off('trigger-excute', this.toggleStatus)
        },
        methods: {
            handleParamChange (name, value) {
                this.paramValues[name] = value
            },
            /**
             * 切换状态
             */
            async toggleStatus () {
                if (this.disabled || this.status === 'running' || !this.canManualStartup) return
                this.disabled = true

                if (this.beforeExec && typeof this.beforeExec === 'function') {
                    const result = await this.beforeExec(true)
                    if (result.code !== 0) {
                        this.disabled = false
                        return
                    }
                }

                try {
                    if (this.pipelineId && this.projectId) {
                        const res = await this.$store.dispatch('pipelines/requestStartupInfo', {
                            projectId: this.projectId,
                            pipelineId: this.pipelineId
                        })
                        if (res.canManualStartup) {
                            this.paramList = res.properties.filter(p => p.required)
                            const buildList = res.properties.filter(p => p.propertyType === 'BUILD')

                            if (res.canElementSkip || this.paramList.length || (res.buildNo && res.buildNo.required) || buildList.length) {
                                this.$store.commit('pipelines/updateCurAtomPrams', res)
                                this.$router.push({
                                    name: 'pipelinesPreview',
                                    params: {
                                        projectId: this.projectId,
                                        pipelineId: this.pipelineId
                                    }
                                })
                                this.disabled = false
                            } else {
                                this.execPipeline()
                            }
                        } else {
                            throw new Error(this.$t('newlist.withoutManualAtom'))
                        }
                    } else {
                        throw new Error(this.$t('newlist.paramsErr'))
                    }
                } catch (err) {
                    this.disabled = false
                    this.handleError(err, [{
                        actionId: this.$permissionActionMap.execute,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId: [{
                            id: this.pipelineId,
                            name: this.pipelineId
                        }],
                        projectId: this.projectId
                    }], this.getPermUrlByRole(this.projectId, this.pipeline, this.roleMap.executor))
                }
            },
            /**
             * 执行流水线
             */
            execPipeline (params = {}) {
                this.$emit('exec', {
                    ...params,
                    pipelineId: this.pipelineId
                })
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .pipelines-triggers {
        position: relative;
        width: 32px;
        height: 32px;
        font-size: 0;
        .pipeline-trigger-wrapper {
            display: block;
            height: 100%;
        }
        .pipeline-ready {
            width: 32px;
            height: 32px;
            border: 2px solid $borderWeightColor;
            border-radius: 50%;
            cursor: pointer;
            &:hover {
                background-color: $primaryColor;
                border-color: $primaryColor;
                .trigger-icon {
                    color: #fff;
                    box-shadow: 0 2px 4px rgba(60, 150, 255, 0.3);
                }
            }
        }
        .pipeline-disable {
            cursor: default;
            .trigger-icon {
                font-size: 32px;
                color: $borderWeightColor;
            }
        }
        .pipeline-trigger-item {
            height: 100%;
            padding: 4px;
            &.pipeline-running {
                top: 0;
                padding: 0 4px;
            }
            .spin-icon {
                position: absolute;
                font-size: 24px;
                line-height: 32px;
                color: $primaryColor;
            }
        }
        .trigger-icon {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            font-size: 16px;
            color: #c3cdd7;
        }
        .bk-dialog-style {
            .bk-dialog-header {
                padding-top: 20px;
            }
        }
        .bk-dialog {
            overflow: auto;
            .bk-form-item+.bk-form-item {
                margin-top: 8px;
            }
            .bk-label, .bk-form-help {
                word-wrap: break-word;
                word-break: break-all;
                display: block;
            }
            .bk-label {
                float: none;
                width: 100%;
                text-align: left;
                white-space: nowrap;
                text-overflow: ellipsis;
                overflow: hidden;
            }
            .bk-form-content {
                margin-left: 0;
            }
        }
        .dialog-params-form {
            padding: 15px 30px;
        }
    }
</style>
