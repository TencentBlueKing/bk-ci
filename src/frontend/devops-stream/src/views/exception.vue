<template>
    <section>
        <dashboard-header></dashboard-header>
        <bk-exception :type="infoMap.typeMap[exceptionType] || '403'" class="exception-home">
            <section class="exception-content">
                <span>{{ infoMap.titleMap[exceptionType] }}</span>
                <span class="exception-title">
                    {{ exceptionInfo.message || infoMap.messageMap[exceptionType] || $t('exception.systemErrTips') }}
                    <bk-link theme="primary" target="_blank" :href="LINK_CONFIG.STREAM" v-if="exceptionType === 520">{{$t('exception.learnMore')}}</bk-link>
                    <bk-link theme="primary" target="_blank" :href="LINK_CONFIG.STREAM_PERMISSION" v-if="exceptionType === 403">{{$t('exception.learnMore')}}</bk-link>
                </span>
                <div v-bk-tooltips="{ content: $t('exception.permissionDeny'), disabled: permission }" v-if="exceptionType === 419">
                    <bk-button theme="primary" @click="enable" :loading="isSaving" :disabled="!permission">{{$t('exception.enable')}}</bk-button>
                </div>
                <bk-button theme="primary" v-if="exceptionType === 418" @click="oauth" :loading="isSaving">{{$t('exception.oauthAuth')}}</bk-button>
                <bk-button theme="primary" v-if="[500, 403].includes(exceptionType)" @click="refresh">{{$t('refresh')}}</bk-button>
            </section>
        </bk-exception>
    </section>
</template>

<script>
    import { common, setting } from '@/http'
    import { mapState } from 'vuex'
    import LINK_CONFIG from '@/conf/link-config.js'
    import dashboardHeader from '@/components/dashboard-header'

    export default {
        components: {
            dashboardHeader
        },

        data () {
            return {
                isSaving: false,
                LINK_CONFIG
            }
        },

        computed: {
            ...mapState(['exceptionInfo', 'projectId', 'projectInfo', 'permission']),

            projectPath () {
                return (this.$route.hash || '').slice(1)
            },

            exceptionType () {
                return +this.exceptionInfo.type || 404
            },

            infoMap () {
                return {
                    typeMap: {
                        404: 404,
                        500: 500,
                        520: 'login'
                    },
                    titleMap: {
                        403: this.$t('exception.noPermission'),
                        404: this.$t('exception.title404'),
                        418: this.$t('exception.noPermission'),
                        419: this.$t('exception.title419'),
                        499: this.$t('exception.title499'),
                        500: this.$t('exception.title500'),
                        520: this.$t('exception.title520')
                    },
                    messageMap: {
                        403: this.$t('exception.tips403'),
                        404: this.$t('exception.tips404'),
                        418: this.$t('exception.tips418'),
                        419: this.$t('exception.tips419'),
                        499: this.$t('exception.tips499'),
                        500: this.$t('exception.tips500'),
                        520: this.$t('exception.tips520')
                    }
                }
            }
        },

        methods: {
            oauth () {
                this.isSaving = true
                common.oauth(location.href).then((res) => {
                    if (res.url) {
                        location.href = res.url
                    }
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isSaving = false
                })
            },

            enable () {
                this.isSaving = true
                setting.toggleEnableCi(true, this.projectInfo).then(() => {
                    this.refresh()
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isSaving = false
                })
            },

            refresh () {
                location.reload()
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .exception-home {
        padding-top: calc(50vh - 300px);
    }
    .exception-content {
        display: flex;
        flex-direction: column;
        align-items: center;
        .exception-title {
            font-size: 14px;
            margin: 15px 0;
        }
    }
</style>
