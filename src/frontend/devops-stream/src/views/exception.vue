<template>
    <bk-exception :type="infoMap.typeMap[exceptionType] || '403'" class="exception-home">
        <section class="exception-content">
            <span>{{ infoMap.titleMap[exceptionType] }}</span>
            <span class="exception-title">
                {{ exceptionInfo.message || infoMap.messageMap[exceptionType] || 'System error, please try again later!' }}
                <bk-link theme="primary" target="_blank" :href="LINK_CONFIG.STREAM" v-if="exceptionType === 520">了解更多</bk-link>
                <bk-link theme="primary" target="_blank" :href="LINK_CONFIG.STREAM_PERMISSION" v-if="exceptionType === 403">Learn more</bk-link>
            </span>
            <div v-bk-tooltips="{ content: 'Permission denied', disabled: permission }" v-if="exceptionType === 419">
                <bk-button theme="primary" @click="enable" :loading="isSaving" :disabled="!permission">Enable</bk-button>
            </div>
            <bk-button theme="primary" v-if="exceptionType === 418" @click="oauth" :loading="isSaving">OAUTH Authorization</bk-button>
            <bk-button theme="primary" v-if="[500, 403].includes(exceptionType)" @click="refresh">Refresh</bk-button>
        </section>
    </bk-exception>
</template>

<script>
    import { common, setting } from '@/http'
    import { mapState } from 'vuex'
    import LINK_CONFIG from '@/conf/link-config.js'

    export default {
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
                        403: 'No permission',
                        404: 'Not found',
                        418: 'No permission',
                        419: 'CI is not enabled',
                        499: 'No project information found',
                        500: 'System Error',
                        520: 'Welcome to Stream.'
                    },
                    messageMap: {
                        403: `There is no access permission for the project ${this.projectPath}, please join the project first!`,
                        404: 'Page not found',
                        418: 'The OAUTH authorization has not been carried out yet, please authorize first!',
                        419: 'CI has not been enabled yet, please enable it first!',
                        499: `The information of the project ${this.projectPath} is not queried, please modify and try again`,
                        500: 'System error, please try again later',
                        520: 'Stream 服务可以在主流的操作系统上持续快速地编译、测试、部署你的服务。'
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
        margin-top: calc(50vh - 300px);
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
