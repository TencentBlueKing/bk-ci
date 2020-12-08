<template>
    <div style="width: 690px;padding-top: 20px;">
        <bk-form v-if="!isShow.status || isShow.status === 0" :label-width="120" :model="formData" ref="validateForm">
            <bk-form-item :label="$t('停用任务')">
                <span class="disable-label"><i class="bk-icon icon-exclamation-circle-shape"></i><span>{{$t('停用后将不再执行定时扫描')}}</span></span>
            </bk-form-item>
            <bk-form-item style="margin-top: 10px;" :rules="rules.desc" :property="'desc'">
                <bk-input
                    :placeholder="$t('请输入停用原因，不得少于10个字符')"
                    :type="'textarea'"
                    :rows="4"
                    :maxlength="100"
                    v-model="formData.desc">
                </bk-input>
            </bk-form-item>
            <bk-form-item>
                <bk-button theme="primary" :title="$t('停用')" :loading="buttonLoading" @click.stop.prevent="disable">{{$t('停用')}}</bk-button>
            </bk-form-item>
        </bk-form>
        <bk-form v-else>
            <bk-form-item :label="$t('启用任务')">
                <span class="disable-label"><i class="bk-icon icon-exclamation-circle-shape"></i><span>{{$t('任务当前状态为已停用')}}</span></span>
            </bk-form-item>
            <bk-form-item>
                <bk-button v-if="isCreateFromPipeline" theme="primary" @click="goToPipeline">{{$t('去流水线启用')}}</bk-button>
                <bk-button v-else theme="primary" :title="$t('启用')" :loading="buttonLoading" @click.stop.prevent="enable">{{$t('启用')}}</bk-button>
            </bk-form-item>
        </bk-form>
    </div>
</template>
<script>
    import { mapState } from 'vuex'
    export default {
        data () {
            return {
                formData: {
                    desc: ''
                },
                rules: {
                    desc: [
                        {
                            required: true,
                            message: this.$t('必填项'),
                            trigger: 'blur'
                        },
                        {
                            min: 10,
                            message: this.$t('不能少于x个字符', { num: 10 }),
                            trigger: 'blur'
                        },
                        {
                            max: 200,
                            message: this.$t('不能多于x个字符', { num: 200 }),
                            trigger: 'blur'
                        }
                    ]
                },
                buttonLoading: false
            }
        },
        computed: {
            ...mapState('task', {
                taskDetail: 'detail',
                isShow: 'status'
            }),
            isCreateFromPipeline () {
                return this.taskDetail.createFrom === 'bs_pipeline'
            }
        },
        methods: {
            disable () {
                this.$refs.validateForm.validate().then(validator => {
                    this.buttonLoading = true
                    const params = {
                        taskId: this.taskDetail.taskId,
                        disableReason: this.formData.desc
                    }
                    const data = this.isShow
                    this.$store.dispatch('task/stopManage', params).then(res => {
                        if (res.data === true) {
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('停用任务成功')
                            })
                        }
                        this.$store.dispatch('task/status')
                        this.formData = {
                            desc: ''
                        }
                    }).catch(e => {
                        console.error(e)
                    }).finally(() => {
                        this.$store.dispatch('task/detail', data)
                        this.buttonLoading = false
                    })
                }, validator => {
                })
            },
            enable () {
                this.$store.dispatch('task/startManage', this.taskDetail.taskId).then(res => {
                    if (res.data === true) {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('启用任务成功')
                        })
                    }
                    this.$store.dispatch('task/status')
                }).catch(e => {
                    console.error(e)
                }).finally(() => {
                    this.$store.dispatch('task/detail', this.taskDetail.taskId)
                })
            },
            goToPipeline () {
                window.open(`${window.DEVOPS_SITE_URL}/console/pipeline/${this.taskDetail.projectId}/${this.taskDetail.pipelineId}/edit#${this.taskDetail.atomCode}`, '_blank')
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .disable-label {
        display: inline-block;
        width: 100%;
        font-size: 14px;
        padding: 10px;
        background: #FFF4E2;
    }
    .icon-exclamation-circle-shape {
        padding: 9px;
        color: #FFB848;
        position: relative;
        top: -1px;
    }
</style>
