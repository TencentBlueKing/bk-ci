<template>
    <section>
        <project-header></project-header>
        <router-view v-if="!isLoading" class="project-content"></router-view>
    </section>
</template>

<script>
    import { common, notifications, pipelines } from '@/http'
    import { mapActions, mapState } from 'vuex'
    import streamWebSocket from '@/utils/websocket'
    import register from '@/utils/websocket-register'
    import { setCookie, getCookie, deleteCookie } from '@/utils'
    import projectHeader from '@/components/project-header'

    export default {
        components: {
            projectHeader
        },

        data () {
            return {
                isLoading: false
            }
        },

        computed: {
            ...mapState(['exceptionInfo', 'projectInfo', 'projectId', 'permission'])
        },

        created () {
            this.initData()
        },

        beforeDestroy () {
            register.unInstallWsMessage('notify')
        },

        methods: {
            ...mapActions(['setProjectInfo', 'setExceptionInfo', 'setPermission', 'setMessageNum']),

            initData () {
                this.isLoading = true
                Promise.all([this.getProjectInfo()]).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                })
            },

            getProjectInfo () {
                return new Promise((resolve, reject) => {
                    const projectPath = (location.hash || '').slice(1)

                    if (projectPath) {
                        common.getProjectInfo(projectPath).then((projectInfo = {}) => {
                            if (projectInfo.id) {
                                if (getCookie(X_DEVOPS_PROJECT_ID) !== projectInfo.projectCode) {
                                    // 清除 projectid cookie
                                    deleteCookie(X_DEVOPS_PROJECT_ID)
                                    // 设置 projectid cookie
                                    setCookie(X_DEVOPS_PROJECT_ID, projectInfo.projectCode, location.hostname)
                                    if (getCookie(ROUTER_TAG) !== projectInfo.routerTag) {
                                        // 清除是否灰度 cookie
                                        deleteCookie(ROUTER_TAG)
                                        // 设置是否灰度 cookie
                                        setCookie(ROUTER_TAG, projectInfo.routerTag, location.hostname)
                                        location.reload()
                                    }
                                }
                                this.setProjectInfo(projectInfo)
                                this.loopGetNotifications()
                                this.getPermission()
                                this.setExceptionInfo({ type: 200 })
                                streamWebSocket.changeRoute(this.$route)
                            }
                            resolve()
                        }).catch((err) => {
                            resolve()
                            this.setExceptionInfo({ type: 500, message: err.message || err })
                        })
                    } else {
                        this.setExceptionInfo({ type: 520 })
                        resolve()
                    }
                })
            },

            getPermission () {
                return pipelines.requestPermission(this.projectId).then((res) => {
                    this.setPermission(res)
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                })
            },

            getNotifications () {
                return notifications.getUnreadNotificationNum(this.projectId).then((res) => {
                    this.setMessageNum(res || 0)
                })
            },

            loopGetNotifications () {
                this.getNotifications()
                register.installWsMessage(this.getNotifications, 'NOTIFYstream', 'notify')
            }
        }
    }
</script>

<style lang="postcss" scoped>
    .project-content {
        height: calc(100vh - 61px);
        width: 100%;
    }
</style>
