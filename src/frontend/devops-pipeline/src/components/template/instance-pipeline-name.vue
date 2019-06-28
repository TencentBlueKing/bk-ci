<template>
    <bk-dialog
        width="400"
        ext-cls="instance-pipeline-name-dialog"
        v-model="showInstanceCreate"
        :show-footer="instanceDialogConfig.hasFooter"
        :close-icon="instanceDialogConfig.closeIcon"
    >
        <template>
            <section class="create-pipeline-content"
                v-bkloading="{
                    isLoading: instanceDialogConfig.loading
                }">
                <div class="info-title">实例化流水线名称</div>
                <div class="bk-form create-form">
                    <div class="item-label">流水线名称</div>
                    <input type="text" class="bk-form-input pipeline-name-input"
                        placeholder="请输入流水线名称"
                        name="pipelineName"
                        v-model="pipelineName"
                        v-focus="isFocus()"
                        v-validate="{
                            required: true,
                            max: 40
                        }"
                        :class="{ 'is-danger': errors.has('pipelineName') }">
                    <div :class="errors.has('pipelineName') ? 'error-tips' : 'normal-tips'">{{ errors.first("pipelineName") }}</div>
                </div>
            </section>
            <div class="form-footer">
                <bk-button theme="primary" @click="confirm()">确定</bk-button>
                <bk-button @click="cancel()">取消</bk-button>
            </div>
        </template>
    </bk-dialog>
</template>

<script>
    export default {
        directives: {
            focus: {
                inserted: function (el) {
                    el.focus()
                }
            }
        },
        props: {
            showInstanceCreate: Boolean
        },
        data () {
            return {
                pipelineName: '',
                instanceDialogConfig: {
                    loading: false,
                    hasHeader: false,
                    hasFooter: false,
                    closeIcon: false,
                    quickClose: false
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            showInstanceCreate (val) {
                if (!val) {
                    this.pipelineName = ''
                }
            }
        },
        methods: {
            async confirm () {
                const valid = await this.$validator.validate()
                if (valid) {
                    let message, theme
                    this.instanceDialogConfig.isLoading = true

                    try {
                        const res = await this.$store.dispatch('pipelines/checkPipelineName', {
                            projectId: this.projectId,
                            pipelineName: this.pipelineName.trim()
                        })

                        if (res || this.$parent.pipelineNameList.some(item => item.pipelineName === this.pipelineName.trim())) {
                            message = '流水线名称已存在'
                            theme = 'error'
                        } else {
                            this.$emit('comfire', this.pipelineName.trim())
                        }
                    } catch (err) {
                        message = err.message || err
                        theme = 'error'
                    } finally {
                        if (message) {
                            this.$showTips({
                                message: message,
                                theme: theme
                            })
                        }

                        this.instanceDialogConfig.isLoading = false
                    }
                }
            },
            isFocus () {
                return this.showInstanceCreate
            },
            cancel () {
                this.$emit('cancel')
            }
        }
    }
</script>

<style lang='scss'>
    @import './../../scss/conf';
    .instance-pipeline-name-dialog {
        .bk-dialog-tool {
            display: none;
        }
        .bk-dialog-body {
            padding: 0px;
        }
        .create-pipeline-content {
            padding: 20px 20px 0;
        }
        .info-title {
            padding-left: 10px;
            color: #333C48;
            font-size: 16px;
        }
        .create-form {
            margin: 16px auto;
            padding-left: 10px;
            .item-label {
                margin-bottom: 4px;
            }
        }
        .form-footer {
            padding: 10px;
            text-align: right;
            background: #FAFBFD;
            border-top: 1px solid #DDE4EB;
        }
    }
</style>
