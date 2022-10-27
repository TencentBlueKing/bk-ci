<template>
    <article class="setting-basic-home" v-bkloading="{ isLoading }">
        <h3 class="setting-basic-head">{{$t('setting.general')}}</h3>
        <section class="basic-main">
            <h5 class="main-title">{{$t('setting.configListeningEvents')}}</h5>
            <section class="main-checkbox">
                <bk-checkbox v-model="form.buildPushedBranches" class="basic-item">{{$t('setting.buildPushedBranches')}}</bk-checkbox>
                <bk-checkbox v-model="form.buildPushedPullRequest" class="basic-item">{{$t('setting.buildPushedMergeRequest')}}</bk-checkbox>
            </section>

            <h5 class="main-title">{{$t('setting.configMergeRequest')}}</h5>
            <section class="main-checkbox">
                <bk-checkbox v-model="form.enableMrBlock" class="basic-item">{{$t('setting.lockMrMerge')}}</bk-checkbox>
            </section>
            <bk-button theme="primary" class="basic-btn" @click="saveSetting" :loading="isSaving">{{$t('save')}}</bk-button>
        </section>

        <h3 class="setting-basic-head">{{$t('setting.ciAuthorization')}}</h3>
        <section class="basic-main">
            <h5 class="main-title">{{ $t('setting.authBy', [form.enableUserId])}}</h5>
            <section class="main-checkbox">
                <bk-button @click="resetAuthorization" :loading="isReseting">{{$t('setting.resetAuthorization')}}</bk-button>
            </section>
            <h5 class="main-title">{{ form.enableCi ? $t('setting.disableTips') : $t('setting.enableTips') }}</h5>
            <section class="main-checkbox">
                <bk-button :theme="form.enableCi ? 'danger' : 'primary'" :loading="isToggleEnable" @click="toggleEnable">{{ form.enableCi ? $t('setting.disableCi') : $t('setting.enableCi') }}</bk-button>
            </section>
        </section>

        <h3 class="setting-basic-head">{{$t('setting.mrRunPerm')}}</h3>
        <section class="basic-main">
            <section class="form-item">
                <bk-checkbox v-model="triggerSetting.memberNoNeedApproving" class="basic-item">{{$t('setting.mrNoApproval')}}</bk-checkbox>
                <p class="desc desc-padding">{{$t('setting.mrNoApprovalTips')}}</p>
            </section>
            <section class="form-item">
                <p>{{$t('setting.whiteList')}}</p>
                <bk-input
                    :placeholder="$t('setting.whiteListPlaceholder')"
                    :type="'textarea'"
                    :rows="3"
                    :maxlength="255"
                    v-model="triggerSetting.whitelistStr">
                </bk-input>
                <p class="desc">{{$t('setting.whiteListTips')}}</p>
            </section>
            <section class="main-checkbox">
                <bk-button theme="primary" :loading="isSavingTrigginSetting" @click="saveTriggerSetting">{{ $t('save') }}</bk-button>
            </section>
        </section>
    </article>
</template>

<script>
    import { setting } from '@/http'
    import { mapState, mapActions } from 'vuex'

    export default {
        data () {
            return {
                form: {
                    buildPushedBranches: false,
                    buildPushedPullRequest: false,
                    enableMrBlock: false
                },
                triggerSetting: {
                    memberNoNeedApproving: true,
                    whitelistStr: ''
                },
                isSaving: false,
                isLoading: false,
                isToggleEnable: false,
                isSavingTrigginSetting: false,
                isReseting: false
            }
        },

        computed: {
            ...mapState(['projectId', 'projectInfo'])
        },

        created () {
            this.getSetting()
        },

        methods: {
            ...mapActions(['setProjectSetting']),
            getSetting () {
                this.isLoading = true
                setting.getSetting(this.projectId).then((res = {}) => {
                    Object.assign(this.form, res)
                    this.triggerSetting = {
                        memberNoNeedApproving: res.triggerReviewSetting?.memberNoNeedApproving !== undefined ? res.triggerReviewSetting?.memberNoNeedApproving : true,
                        whitelistStr: (res.triggerReviewSetting?.whitelist || []).join(',') || ''
                    }
                    this.setProjectSetting(res)
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            saveSetting () {
                this.isSaving = true
                setting.saveSetting(this.projectId, this.form).then(() => {
                    this.$bkMessage({ theme: 'success', message: 'Saved successfully' })
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isSaving = false
                })
            },

            saveTriggerSetting () {
                this.isSavingTrigginSetting = true
                const whitelist = this.triggerSetting.whitelistStr.trim().split(',').map(item => item.trim())
                const data = {
                    memberNoNeedApproving: this.triggerSetting.memberNoNeedApproving,
                    whitelist
                }
                setting.saveTriggerSetting(this.projectId, data).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isSavingTrigginSetting = false
                })
            },

            toggleEnable () {
                this.isToggleEnable = true
                setting.toggleEnableCi(!this.form.enableCi, this.projectInfo).then(() => {
                    this.getSetting()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isToggleEnable = false
                })
            },

            resetAuthorization () {
                this.isReseting = true
                setting.resetAuthorization(this.projectInfo.id).then(() => {
                    this.getSetting()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isReseting = false
                })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .setting-basic-home {
        margin: 16px;
        padding: 24px;
        background: #fff;
        overflow: auto;
    }
    .setting-basic-head {
        font-size: 16px;
        color: #313328;
    }
    .basic-main {
        margin: 10px 0 30px;
        border: 1px solid #f0f1f5;
        padding: 20px;
        .main-title {
            margin-bottom: 20px;
        }
        .main-checkbox {
            display: flex;
            margin-bottom: 20px;
            &:last-child {
                margin-bottom: 0;
            }
        }
        .form-item {
            margin-bottom: 20px;
            &:last-child {
                margin-bottom: 0;
            }
            p {
                line-height: 24px;
            }
            .desc {
                color: #979BA5;
                font-size: 12px;
            }
            .desc-padding {
                padding-left: 22px;
            }
        }
        .basic-item {
            margin-right: 100px;
        }
    }
    .basic-btn {
        width: 88px;
    }
</style>
