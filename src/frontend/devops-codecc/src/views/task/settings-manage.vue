<template>
    <div style="width: 690px;padding-top: 20px;">
        <bk-form v-if="!isShow.status || isShow.status === 0" :label-width="120" :model="formData" ref="validateForm">
            <bk-form-item :label="$t('suspend.停用任务')">
                <span class="disable-label"><i class="bk-icon icon-exclamation-circle-shape"></i><span>{{$t('suspend.停用后将不再执行定时扫描')}}</span></span>
            </bk-form-item>
            <bk-form-item style="margin-top: 10px;" :rules="rules.desc" :property="'desc'">
                <bk-input
                    :placeholder="$t('suspend.请输入停用原因，不得少于10个字符')"
                    :type="'textarea'"
                    :rows="4"
                    :maxlength="100"
                    v-model="formData.desc">
                </bk-input>
            </bk-form-item>
            <bk-form-item>
                <bk-button theme="primary" :title="$t('suspend.停用')" :loading="buttonLoading" @click.stop.prevent="disable">{{$t('suspend.停用')}}</bk-button>
            </bk-form-item>
        </bk-form>
        <bk-form v-else>
            <bk-form-item :label="$t('suspend.启用任务')">
                <span class="disable-label"><i class="bk-icon icon-exclamation-circle-shape"></i><span>{{$t('suspend.任务当前状态为已停用')}}</span></span>
            </bk-form-item>
            <bk-form-item>
                <bk-button theme="primary" :title="$t('suspend.启用')" :loading="buttonLoading" @click.stop.prevent="enable">{{$t('suspend.启用')}}</bk-button>
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
                            message: this.$t('st.必填项'),
                            trigger: 'blur'
                        },
                        {
                            min: 10,
                            message: this.$t('st.不能少于x个字符', { num: 10 }),
                            trigger: 'blur'
                        },
                        {
                            max: 200,
                            message: this.$t('st.不能多于x个字符', { num: 200 }),
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
            })
        },
        methods: {
            disable () {
                this.$refs.validateForm.validate().then(validator => {
                    this.buttonLoading = true
                    const params = {
                        taskId: this.$route.params.taskId,
                        disableReason: this.formData.desc
                    }
                    const data = this.isShow
                    this.$store.dispatch('task/stopManage', params).then(res => {
                        if (res.data === true) {
                            this.$bkMessage({
                                theme: 'success',
                                message: this.$t('suspend.停用任务成功')
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
                this.$store.dispatch('task/startManage', this.$route.params.taskId).then(res => {
                    if (res.data === true) {
                        this.$bkMessage({
                            theme: 'success',
                            message: this.$t('suspend.启用任务成功')
                        })
                    }
                    this.$store.dispatch('task/status')
                }).catch(e => {
                    console.error(e)
                }).finally(() => {
                    this.$store.dispatch('task/detail', this.$route.params.taskId)
                })
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
        top: 1px;
    }
</style>
