<template>
    <div class="pipelines-triggers">
        <div class="pipeline-trigger-wrapper" @click="toggleStatus">
            <slot name="exec-bar" :isDisable="disabled">
                <div class="pipeline-trigger-item pipeline-disable" v-if="!canManualStartup">
                    <i class="bk-icon icon-displayable trigger-icon" :title="$t('newlist.cannotManual')"></i>
                </div>
                <div class="pipeline-trigger-item pipeline-ready" v-if="canManualStartup && status !== 'running'">
                    <i class="bk-icon icon-right-shape trigger-icon"></i>
                </div>
                <div class="pipeline-trigger-item pipeline-running" v-if="canManualStartup && status === 'running'">
                    <i alt="running" class="bk-icon icon-circle-2-1 spin-icon"></i>
                </div>
            </slot>
        </div>
    </div>
</template>

<script>
    import { bus } from '@/utils/bus'
    import { mapActions } from 'vuex'
    export default {
        props: {
            beforeExec: {
                type: Function,
                default: () => () => {}
            },
            canManualStartup: {
                type: Boolean,
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
            projectId () {
                return this.$route.params.projectId
            },
            replay () {
                return this.$store.state.pipelines.replay
            }
        },
        watch: {
            async replay (val, oldVal) {
                if (val) {
                    this.$store.state.pipelines.replay = false
                    await this.toggleStatus()
                }
            }
        },
        mounted () {
            bus.$on('trigger-excute', this.toggleStatus)
        },
        beforeDestroy () {
            bus.$off('trigger-excute', this.toggleStatus)
        },
        methods: {
            ...mapActions('atom', [
                'setExecuteStatus'
            ]),
            /**
             * 清除select错误
             */
            selectedHandler (name) {
                this.$validator.errors.remove(name)
            },

            handleParamChange (name, value) {
                this.paramValues[name] = value
            },
            /**
             * 切换状态
             */
            async toggleStatus () {
                if (this.disabled || this.status === 'running' || !this.canManualStartup) return
                this.setExecuteStatus(true)
                this.disabled = true
                this.setExecuteStatus(true)
                // debugger
                if (this.beforeExec && typeof this.beforeExec === 'function') {
                    if (!await this.beforeExec(true)) {
                        this.disabled = false
                        this.setExecuteStatus(false)
                        return
                    }
                }

                this.$emit('click-event')
                try {
                    if (this.pipelineId && this.projectId) {
                        const res = await this.$store.dispatch('pipelines/requestStartupInfo', {
                            projectId: this.projectId,
                            pipelineId: this.pipelineId
                        })
                        if (res.canManualStartup) {
                            this.paramList = res.properties.filter(p => p.required)

                            if (res.canElementSkip || this.paramList.length || (res.buildNo && res.buildNo.required)) {
                                this.$store.commit('pipelines/updateCurAtomPrams', res)
                                this.$router.push({
                                    name: 'pipelinesPreview',
                                    params: {
                                        projectId: this.projectId,
                                        pipelineId: this.pipelineId
                                    }
                                })
                            } else {
                                await this.execPipeline()
                            }
                        } else {
                            throw new Error(this.$t('newlist.withoutManualAtom'))
                        }
                    } else {
                        throw new Error(this.$t('newlist.paramsErr'))
                    }
                } catch (err) {
                    if (err.code === 403) { // 没有权限执行
                        this.$showAskPermissionDialog({
                            noPermissionList: [{
                                resource: this.$t('pipeline'),
                                option: this.$t('exec')
                            }],
                            applyPermissionUrl: `${PERM_URL_PIRFIX}/backend/api/perm/apply/subsystem/?client_id=pipeline&project_code=${this.projectId}&service_code=pipeline&role_executor=pipeline:${this.pipelineId}`
                        })
                    } else {
                        this.$showTips({
                            message: err.message || err,
                            theme: 'error'
                        })
                    }
                } finally {
                    this.setExecuteStatus(false)
                    this.disabled = false
                }
            },
            /**
             * 执行流水线
             */
            async execPipeline (params = {}) {
                await this.$emit('exec', {
                    ...params,
                    pipelineId: this.pipelineId
                })
                this.disabled = false
            },
            /**
             *  点击确定的回调函数
             */
            async confirmHandler () {
                let valid = true
                if (this.$refs.paramsForm) {
                    valid = await this.$refs.paramsForm.$validator.validateAll()
                    this.$refs.paramsForm.submitForm()
                }
                if (valid) {
                    const { paramValues, execPipeline } = this
                    await execPipeline(paramValues)
                }
            },
            /**
             * 点击取消的回调函数
             */
            cancelHandler () {
                this.disabled = false
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
