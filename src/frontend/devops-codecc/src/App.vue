<template>
    <div id="app" v-bkloading="{ isLoading: appLoading, opacity: 0.1 }">
        <!-- eslint-disable-next-line vue/require-component-is -->
        <!-- <div class="banner" v-if="!isBannerClose">
            亲爱的用户，CodeCC将于2020-07-25（周六）20:00至24:00进行数据迁移，届时网站和CodeCC插件将出现不可用，给您造成不便敬请谅解。
            <i class="bk-icon icon-close f22" @click="handleBanner"></i>
        </div> -->
        <component :is="layout">
            <div style="height: 100%" v-bkloading="{ isLoading: mainContentLoading, opacity: 0.3 }">
                <router-view class="main-content" :key="$route.fullPath" />
            </div>
        </component>
        <app-auth ref="bkAuth"></app-auth>
    </div>
</template>
<script>
    import { mapGetters, mapState } from 'vuex'
    import { bus } from './common/bus'
    import { toggleLang } from './i18n'
    import { getToolMeta, getToolList, getTaskList } from './common/preload'

    export default {
        name: 'app',
        data () {
            return {
                appLoading: false
            }
        },
        computed: {
            ...mapGetters(['mainContentLoading', 'isBannerClose']),
            ...mapState('task', {
                status: 'status'
            }),
            layout () {
                return `layout-${this.$route.meta.layout}`
            },
            projectId () {
                return this.$route.params.projectId
            },
            taskId () {
                return this.$route.params.taskId
            }
        },
        watch: {
            async '$route.fullPath' (val) { // 同步地址到蓝盾
                if (window.self !== window.top) { // iframe嵌入
                    devopsUtil.syncUrl(val.replace(/^\/codecc\//, '/')) // eslint-disable-line
                }
                // 进到具体项目页面，项目停用跳转到任务管理
                if (this.taskId) {
                    const res = await this.$store.dispatch('task/status')
                    if (res.status === 1) {
                        this.$router.push({ name: 'task-settings-manage' })
                    }
                    if (!res.status.gongfengProjectId) { // 工蜂开源项目少调一个接口
                        getTaskList()
                    }
                }
                getToolMeta()
                getToolList()
            }
        },
        created () {
            // 蓝盾切换项目
            window.addEventListener('change::$currentProjectId', data => {
                if (this.$route.params.projectId && this.$route.params.projectId !== data.detail.currentProjectId) {
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

            bus.$on('show-content-loading', () => {
                this.$store.commit('setMainContentLoading', true, { root: true })
            })
            bus.$on('hide-content-loading', () => {
                this.$store.commit('setMainContentLoading', false, { root: true })
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
            },
            handleBanner () {
                this.$store.commit('updateBannerStatus', true)
                window.localStorage.setItem('codecc-banner-271', 1)
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
<style scoped lang="postcss">
    .banner {
        background-color: #fdf6e2;
        border-bottom: 1px solid #d5dbe0;
        color: #ec531d;
        font-size: 14px;
        height: 30px;
        line-height: 30px;
        padding: 0 20px;
        text-align: center;
        width: 100%;
        .icon-close {
            float: right;
            line-height: 30px;
            cursor: pointer;
        }
    }
</style>
