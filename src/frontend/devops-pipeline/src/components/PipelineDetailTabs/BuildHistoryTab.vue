<template>
    <div class="build-history-tab-content">
        <empty-tips v-if="hasNoPermission" :show-lock="true" v-bind="emptyTipsConfig"></empty-tips>
        <build-history-table v-else :show-log="showLog" />
    </div>
</template>

<script>
    import BuildHistoryTable from '@/components/BuildHistoryTable/'
    import emptyTips from '@/components/devops/emptyTips'
    import { mapGetters, mapActions, mapState } from 'vuex'
    import { AUTH_URL_PREFIX } from '@/store/constants'
    import pipelineConstMixin from '@/mixins/pipelineConstMixin'
    import webSocketMessage from '@/utils/webSocketMessage'

    export default {
        name: 'build-history-tab',
        components: {
            BuildHistoryTable,
            emptyTips
        },

        mixins: [pipelineConstMixin],

        data () {
            return {
                hasNoPermission: false,
                currentBuildNo: '',
                currentBuildNum: '',
                currentShowStatus: false
            }
        },

        computed: {
            ...mapGetters({
                historyPageStatus: 'pipelines/getHistoryPageStatus'
            }),
            ...mapState('atom', [
                'isPropertyPanelVisible'
            ]),
            scrollBoxCls () {
                return 'bkdevops-pipeline-history'
            },
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            queryStr () {
                return this.historyPageStatus?.queryStr ?? ''
            },
            emptyTipsConfig () {
                const { hasNoPermission } = this
                const title = hasNoPermission ? this.$t('noPermission') : this.$t('history.noBuildRecords')
                const desc = hasNoPermission ? this.$t('history.noPermissionTips') : this.$t('history.buildEmptyDesc')
                const btns = hasNoPermission
                    ? [{
                        theme: 'primary',
                        size: 'normal',
                        handler: this.changeProject,
                        text: this.$t('changeProject')
                    }, {
                        theme: 'success',
                        size: 'normal',
                        handler: this.toApplyPermission,
                        text: this.$t('applyPermission')
                    }]
                    : [{
                        theme: 'primary',
                        size: 'normal',
                        disabled: this.executeStatus,
                        loading: this.executeStatus,
                        handler: () => {
                            !this.executeStatus && this.$router.push({
                                name: 'pipelinesPreview',
                                ...this.$route.params
                            })
                        },
                        text: this.$t('history.startBuildTips')
                    }]
                return {
                    title,
                    desc,
                    btns
                }
            }
        },
        watch: {
            queryStr (newStr) {
                let hashParam = ''
                if (this.$route.hash && /^#b-+/.test(this.$route.hash)) hashParam = this.$route.hash
                const url = `${this.$route.path}${newStr ? `?${newStr}` : ''}${hashParam}`
                if (url !== this.$route.fullPath) {
                    this.$router.push(url)
                }
            }
        },

        async mounted () {
            if (this.$route.hash) { // 带上buildId时，弹出日志弹窗
                const isBuildId = /^#b-+/.test(this.$route.hash) // 检查是否是合法的buildId
                isBuildId && this.showLog(this.$route.hash.slice(1), '', true)
            }
            webSocketMessage.installWsMessage(this.refreshBuildHistoryList)
        },

        updated () {
            if (!this.isPropertyPanelVisible) {
                this.currentBuildNo = ''
                this.currentBuildNum = ''
                this.currentShowStatus = false
            }
        },

        beforeDestroy () {
            webSocketMessage.unInstallWsMessage()
        },

        methods: {
            ...mapActions('pipelines', [
                'requestExecPipeline'
            ]),
            ...mapActions('atom', [
                'togglePropertyPanel'
            ]),

            changeProject () {
                this.$toggleProjectMenu(true)
            },
            async toApplyPermission () {
                try {
                    const { projectId } = this.$route.params
                    const redirectUrl = await this.$ajax.post(`${AUTH_URL_PREFIX}/user/auth/permissionUrl`, [{
                        actionId: this.$permissionActionMap.view,
                        resourceId: this.$permissionResourceMap.pipeline,
                        instanceId: [{
                            id: projectId,
                            type: this.$permissionResourceTypeMap.PROJECT
                        }, {
                            id: this.pipelineId,
                            name: this.pipelineId,
                            type: this.$permissionResourceTypeMap.PIPELINE_DEFAULT
                        }]
                    }])
                    console.log('redirectUrl', redirectUrl)
                    window.open(redirectUrl, '_blank')
                    this.$bkInfo({
                        title: this.$t('permissionRefreshtitle'),
                        subTitle: this.$t('permissionRefreshSubtitle'),
                        okText: this.$t('permissionRefreshOkText'),
                        cancelText: this.$t('close'),
                        confirmFn: () => {
                            location.reload()
                        }
                    })
                } catch (e) {
                    console.error(e)
                }
            },

            showLog (buildId, buildNum, status) {
                this.togglePropertyPanel({
                    isShow: true
                })

                this.currentBuildNo = buildId
                this.currentBuildNum = buildNum
                this.currentShowStatus = status
            }
        }
    }
</script>

<style lang="scss">
    .build-history-tab-content {
        height: 100%;
        overflow: hidden;
        padding: 16px 24px;
        display: flex;
        flex-direction: column;
        align-items: stretch;
        .bk-sideslider-wrapper {
            top: 0;
            padding-bottom: 0;
             .bk-sideslider-content {
                height: calc(100% - 60px);
            }
        }
    }
</style>
