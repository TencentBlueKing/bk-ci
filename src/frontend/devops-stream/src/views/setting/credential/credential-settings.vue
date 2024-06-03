<template>
    <section class="credential-settings" v-bkloading="{ isLoading }">
        <header class="setting-head">
            <bk-breadcrumb separator-class="bk-icon icon-angle-right">
                <bk-breadcrumb-item v-for="(item,index) in navList" :key="index" :to="item.link">{{item.title}}</bk-breadcrumb-item>
            </bk-breadcrumb>
        </header>
        <section class="setting-body">
            <h3 class="setting-basic-head">{{$t('setting.general')}}</h3>
            <section class="setting-main">
                <section>
                    <section class="main-checkbox">
                        <bk-checkbox v-model="ticket.allowAcrossProject" class="basic-item">{{$t('setting.crossRepoUse')}}</bk-checkbox>
                    </section>
                    <p class="main-desc">{{$t('setting.crossRepoUseTips')}}</p>
                </section>
                <bk-button class="basic-btn" @click="saveSetting" :loading="isSaving">{{$t('save')}}</bk-button>
            </section>
        </section>
    </section>
</template>

<script>
    import { setting } from '@/http'
    import { mapState } from 'vuex'

    export default {
        data () {
            return {
                isLoading: false,
                isSaving: false,
                ticket: {},
                navList: [
                    { link: { name: 'credentialList' }, title: this.$t('setting.credentialSetting') },
                    { link: '', title: this.$route.params.credentialId },
                    { link: '', title: this.$t('setting.settings') }
                ]
            }
        },

        computed: {
            ...mapState(['projectId']),
            credentialId () {
                return this.$route.params.credentialId || ''
            }
        },

        created () {
            this.getTicket()
        },

        methods: {
            getTicket () {
                this.isLoading = true
                setting.getTicketDetail(this.projectId, this.credentialId).then((res) => {
                    this.ticket = res
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                })
            },
            saveSetting () {
                this.isSaving = true
                const params = {
                    allowAcrossProject: this.ticket.allowAcrossProject || false
                }
                setting.updateTicketSetting(this.projectId, params, this.credentialId).then(() => {
                    this.$bkMessage({ theme: 'success', message: 'Setting successfully' })
                    this.$router.push({
                        name: 'credentialList'
                    })
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isSaving = false
                })
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .credential-settings {
        padding: 16px;
        height: calc(100vh - 61px);
        overflow: auto;

        .setting-head {
            height: 49px;
            line-height: 49px;
            background: #fff;
            box-shadow: 0 2px 5px 0 rgba(51,60,72,0.03);
            padding: 0 25.5px;
        }

        .setting-body {
            margin-top: 16px;
            padding: 24px;
            background: #fff;
            overflow: hidden;
            .setting-basic-head {
                font-size: 16px;
                color: #313328;
            }
            .setting-main {
                margin: 10px 0 30px;
                border: 1px solid #f0f1f5;
                padding: 20px;
                display: flex;
                justify-content: space-between;
                align-items: center;
                .main-desc {
                    margin-left: 22px;
                    font-size: 12px;
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
        }
    }
    
</style>
