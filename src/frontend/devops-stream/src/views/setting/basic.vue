<template>
    <article class="setting-basic-home" v-bkloading="{ isLoading }">
        <h3 class="setting-basic-head">General</h3>
        <section class="basic-main">
            <h5 class="main-title">Config listening events</h5>
            <section class="main-checkbox">
                <bk-checkbox v-model="form.buildPushedBranches" class="basic-item">Build pushed branches</bk-checkbox>
                <bk-checkbox v-model="form.buildPushedPullRequest" class="basic-item">Build pushed merge request</bk-checkbox>
            </section>

            <h5 class="main-title">Config merge request</h5>
            <section class="main-checkbox">
                <bk-checkbox v-model="form.enableMrBlock" class="basic-item">Lock MR merge</bk-checkbox>
            </section>
            <bk-button theme="primary" class="basic-btn" @click="saveSetting" :loading="isSaving">Save</bk-button>
        </section>

        <h3 class="setting-basic-head">CI Authorization</h3>
        <section class="basic-main">
            <h5 class="main-title">Authorized by @{{form.enableUserId}}. When running, it will use {{form.enableUserId}}'s permission to checkout current repository.</h5>
            <section class="main-checkbox">
                <bk-button @click="resetAuthorization" :loading="isReseting">Reset Authorization</bk-button>
            </section>
            <h5 class="main-title">{{ form.enableCi ? 'After disable CI, all trigger events will be ignored.' : 'Build, test and deploy your code. Continuous delivery of your product faster, easier, with fewer bugs.' }}</h5>
            <section class="main-checkbox">
                <bk-button :theme="form.enableCi ? 'danger' : 'primary'" :loading="isToggleEnable" @click="toggleEnable">{{ form.enableCi ? 'Disable CI' : 'Enable CI' }}</bk-button>
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
                isSaving: false,
                isLoading: false,
                isToggleEnable: false,
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
        overflow: hidden;
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
        .basic-item {
            margin-right: 100px;
        }
    }
    .basic-btn {
        width: 88px;
    }
</style>
