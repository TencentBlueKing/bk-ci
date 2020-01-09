<template>
    <bk-dialog
        class="share-dialog"
        v-model="shareConfig.isShow"
        :width="600"
        :title="shareConfig.title"
        :close-icon="false"
        :quick-close="false"
        :padding="'0 30px 30px 30px'"
        :ok-text="'共享'"
        @confirm="confirmHandler"
        @cancel="cancelHandler">
        <section class="bk-form bk-form-vertical">
            <div class="bk-form-item is-required">
                <label class="bk-label">共享对象(共享内容将以邮件方式发送给共享对象)</label>
                <div class="bk-form-content"
                    :class="{
                        'has-error': errors.has('members')
                    }">
                    <company-staff-input v-if="isExtendTx" :handle-change="handleChange" :value="shareMembers" v-validate="'required'" placeholder="请输入企业微信用户英文名"></company-staff-input>
                    <user-input v-else :handle-change="handleChange" name="members" :value="shareMembers" v-validate="'required'" placeholder="请输入用户名称"></user-input>
                    <div v-if="errors.has('members')" class="error-tips">请输入企业微信用户</div>
                </div>
            </div>
            <div class="bk-form-item is-required">
                <label class="bk-label">有效期</label>
                <div class="bk-form-content">
                    <input type="number"
                        style="width: 50%;"
                        class="bk-form-input"
                        placeholder="请输入有效天数"
                        v-model="shareExpire"
                        :name="'expire'"
                        v-validate="'required|numeric|min_value:1|max_value:9999'"
                        :class="{ 'is-danger': errors.has('expire') }"
                    /> 天
                    <div v-if="errors.has('expire')" class="error-tips">请填写1-9999之间的正整数</div>
                </div>
            </div>
        </section>
    </bk-dialog>
</template>

<script>
    import CompanyStaffInput from '@/components/devops/CompanyStaffInput/index.vue'
    import UserInput from '@/components/devops/UserInput/index.vue'

    export default {
        components: {
            CompanyStaffInput,
            UserInput
        },
        props: {
            shareConfig: {
                type: Object,
                required: true,
                default: {
                    isShow: false,
                    title: '',
                    fileUri: '',
                    loading: false
                }
            }
        },
        data () {
            return {
                shareMembers: [],
                shareExpire: ''
            }
        },
        computed: {
            isExtendTx () {
                return VERSION_TYPE === 'tencent'
            }
        },
        watch: {
            'shareConfig.isShow' (val) {
                this.$validator.errors.clear()
                this.shareContent = {
                    expire: '',
                    members: []
                }
            }
        },
        methods: {
            /**
             *  人员选择器选择后的回调
             */
            handleChange (name, value) {
                this.shareMembers.splice(0, this.shareMembers.length, ...value)
            },
            /**
             *  点击确定的回调函数
             */
            async confirmHandler () {
                this.$validator.validateAll().then(async result => {
                    if (result) {
                        const shareContent = {
                            members: this.shareMembers,
                            expire: this.shareExpire
                        }
                        await this.$emit('shared', shareContent)
                    }
                })
            },
            cancelHandler () {
                // this.$emit('cancelShared')
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';
    .share-dialog {
        .bk-dialog-header {
            padding-top: 20px;
        }
        .bk-dialog-title {
            text-align: left;
            overflow: hidden;
            text-overflow:ellipsis;
            white-space: nowrap;
        }
        .has-error {
            .bk-data-wrapper {
                border-color: $dangerColor;
            }
        }
    }
</style>
