<template>
    <div
        id="app"
        class="biz-app"
    >
        <main class="app-container">
            <router-view></router-view>
        </main>
        <portal-target name="atom-selector-popup"></portal-target>
        <portal-target name="yaml-preview-popup"></portal-target>
    </div>
</template>

<script>
    import { mapMutations, mapState } from 'vuex'

    export default {
        name: 'App',
        data () {
            return {
                stayCurrentPage: ['atomDebug', '']
            }
        },
        computed: {
            ...mapState([
                'fetchError'
            ])
        },
        watch: {
            '$route.fullPath' (val) { // 同步地址到蓝盾
                this.$syncUrl(val.replace(/^\/pipeline\//, '/'))
            },
            fetchError (error) {
                error.message && this.$showTips({
                    message: error.message,
                    theme: 'error'
                })
                if ((error.code === 404 || error.httpStatus === 404) && this.$route.name !== 'PipelineManageList') {
                    this.$router.push({
                        name: 'PipelineManageList'
                    })
                }
            }
        },
        created () {
            console.log('debug')
            window.globalVue.$on('change::$currentProjectId', data => { // 蓝盾选择项目时切换
                if (this.$route.params.projectId !== data.currentProjectId) {
                    if (this.stayCurrentPage.find(item => item === this.$route.name)) {
                        this.reflashCurrentPage(data.currentProjectId)
                    } else {
                        this.goHome(data.currentProjectId)
                    }
                }
            })

            window.globalVue.$on('order::backHome', data => { // 蓝盾选择项目时切换
                this.goHome()
            })

            window.globalVue.$on('order::syncLocale', locale => {
                this.$setLocale(locale, false)
            })
        },
        methods: {
            ...mapMutations('pipelines', [
                'updatePipelineActionState'
            ]),
            goHome (projectId) {
                const params = projectId ? { projectId } : {}
                this.updatePipelineActionState({
                    activePipeline: null,
                    isConfirmShow: false,
                    confirmType: '',
                    activePipelineList: [],
                    isSaveAsTemplateShow: false,
                    isCopyDialogShow: false,
                    addToDialogShow: false,
                    isDisableDialogShow: false
                })
                this.$router.replace({
                    name: 'PipelineManageList',
                    params: {
                        ...this.$route.params,
                        ...params
                    }
                })
            },
            reflashCurrentPage (projectId) {
                const params = Object.assign({}, this.$route.params, { projectId })
                this.$router.replace({
                    name: this.$route.name,
                    params
                })
            }
        }
    }
</script>
<style lang="scss">
    @import './scss/reset.scss';
    @import './scss/app.scss';
    @import './scss/devops-common.scss';

    .biz-app {
        .bk-dialog {
            z-index: 1610;
            .bk-dialog-footer {
                &.bk-d-footer {
                    height: 50px;
                    line-height: 50px;
                    background-color: #fafbfd;
                    border-top: 1px solid #dde4eb;
                }
                .bk-dialog-outer {
                    height: 100%;
                    padding-right: 15px;
                    font-size: 0;
                    & button {
                        width: auto;
                        height: 32px;
                        line-height: 30px;
                        padding: 0 14px;
                        min-width: 72px;
                        margin-top: 8px;
                    }
                }
            }
        }
    }
    .app-container {
        min-width: 1280px;
        height: 100%;
        position: relative;
        padding-top: 0;
        display: flex;
        background: #fafbfd;
        overflow: auto;

    }
    .app-content {
        flex: 1;
        background: #fafbfd;
    }

    .bkdevops-radio {
        > input[type=radio] {
            width: 16px;
            height: 16px;
            background-image: none;
        }
    }
</style>
