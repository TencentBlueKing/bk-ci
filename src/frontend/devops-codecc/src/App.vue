<template>
    <div id="app" v-bkloading="{ isLoading: appLoading, opacity: 0.1 }">
        <!-- eslint-disable-next-line vue/require-component-is -->
        <component :is="layout">
            <div style="height: 100%" v-bkloading="{ isLoading: mainContentLoading, opacity: 0.3 }">
                <router-view v-show="!mainContentLoading" class="main-content" :key="$route.fullPath" />
            </div>
        </component>
        <app-auth ref="bkAuth"></app-auth>
    </div>
</template>
<script>
    import { mapGetters } from 'vuex'
    import { bus } from './common/bus'
    import { toggleLang } from './i18n'

    export default {
        name: 'app',
        data () {
            return {
                appLoading: false
            }
        },
        computed: {
            ...mapGetters(['mainContentLoading']),
            layout () {
                return `layout-${this.$route.meta.layout}`
            },
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {
            '$route.fullPath' (val) { // 同步地址到蓝盾
                devopsUtil.syncUrl(val.replace(/^\/codecc\//, '/'))
            },
        },
        created () {
            // 蓝盾切换项目
            window.addEventListener('change::$currentProjectId', data => {
                if (this.$route.params.projectId !== data.detail.currentProjectId) {
                    this.goHome(data.detail.currentProjectId)
                }
            })
            // 蓝盾回到首页
            window.addEventListener('order::backHome', data => {
                if (this.$route.name !== 'task-list') {
                    this.goHome()
                }
            })
        },
        mounted () {
            const self = this
            bus.$on('show-login-modal', () => {
                self.$refs.bkAuth.showLoginModal()
            })
            bus.$on('close-login-modal', () => {
                self.$refs.bkAuth.hideLoginModal()
                setTimeout(() => {
                    window.location.reload()
                }, 0)
            })

            bus.$on('show-app-loading', () => {
                self.appLoading = true
            })
            bus.$on('hide-app-loading', () => {
                self.appLoading = false
            })
        },
        methods: {
            /**
             * router 跳转
             *
             * @param {string} idx 页面指示
             */
            goPage (idx) {
                this.$router.push({
                    name: idx
                })
            },
            handleToggleLang () {
                toggleLang()
            },
            goHome (projectId) {
                const params = projectId ? { projectId } : {}
                this.$router.replace({
                    name: 'task-list',
                    params
                })
            }
        }
    }
</script>

<style>
    @import './css/reset.css';
    @import './css/app.css';

    #app {
        min-width: 1280px;
        color: #737987;

        > .bk-loading {
            z-index: 999999;
        }
    }
</style>
