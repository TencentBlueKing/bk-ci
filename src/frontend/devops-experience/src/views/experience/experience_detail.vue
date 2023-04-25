<template>
    <div class="release-detail-wrapper" v-bkloading="{ isLoading: loading.isLoading, title: loading.title }">
        <content-header>
            <div slot="left">
                <router-link :to="{ name: 'experienceList' }">
                    <i class="devops-icon icon-arrows-left"></i>
                </router-link>
                <span> {{ $route.meta.title }} </span>
                <span v-if="curReleaseDetail">
                    {{curReleaseDetail.name}}（{{curReleaseDetail.installation_package}}）
                </span>
            </div>
        </content-header>
        <section class="sub-view-port">
            <bk-tab v-if="showContent" :active.sync="curTab" @tab-change="changeTab">
                <bk-tab-panel
                    v-for="(panel, index) in panels"
                    v-bind="panel"
                    render-directive="if"
                    :key="index"
                >
                    <component :is="panel.component" v-bind="panel.bindData"></component>
                </bk-tab-panel>
            </bk-tab>
        </section>
    </div>
</template>

<script>
    import { DownloadData, BaseMessage } from '@/components/ExperienceDetailTabs'
    import { convertTime } from '@/utils/util'
    import { mapActions, mapState } from 'vuex'

    export default {
        components: {
            DownloadData,
            BaseMessage
        },
        data () {
            const curTab = this.$route.params.type === 'detail' ? 'baseMessage' : 'downloadData'
            return {
                curTab,
                curListTab: 'experienceGroups',
                showContent: false,
                defaultCover: require('@/images/qrcode_app.png'),
                loading: {
                    isLoading: false,
                    title: ''
                },
                codeLoading: {
                    isLoading: false
                },
                downloadInfo: {
                    total: 0,
                    account: 0
                },
                downloadList: []
            }
        },
        computed: {
            ...mapState('experience', [
                'curReleaseDetail'
            ]),
            pathType () {
                return this.$route.params.type
            },
            projectId () {
                return this.$route.params.projectId
            },
            experienceHashId () {
                return this.$route.params.experienceId
            },
            isMof () {
                const projectId = this.$route.params.projectId
                return this.$store.state.projectList.find(item => {
                    return (item.deptName === '魔方工作室群' && item.projectCode === projectId)
                })
            },
            isWindows () {
                return /WINDOWS/.test(window.navigator.userAgent.toUpperCase())
            },
            panels () {
                return [
                    {
                        name: 'baseMessage',
                        label: '基础信息',
                        component: 'BaseMessage',
                        bindData: {
                            curReleaseDetail: this.curReleaseDetail,
                            downloadInstallation: this.downloadInstallationAction
                        }
                    },
                    {
                        name: 'downloadData',
                        label: '下载数据',
                        component: 'DownloadData',
                        bindData: {
                            downloadInfo: this.downloadInfo,
                            localConvertTime: this.localConvertTime
                        }
                    }
                ]
            }
        },
        watch: {
            projectId () {
                this.toReleaseList()
            }
        },
        created () {
            this.init()
            this.requestStatistics()
        },
        beforeDestroy () {
            this.setCurReleaseDetail(null)
        },
        methods: {
            ...mapActions('experience', [
                'requestExperienceDetail',
                'requestDownloadCount',
                'requestDownloadUserCount',
                'setCurReleaseDetail',
                'downloadInstallation'
            ]),
            async init () {
                const {
                    loading
                } = this

                loading.isLoading = true
                loading.title = '数据加载中，请稍候'

                try {
                    this.requestRander()
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                        this.showContent = true
                    }, 1000)
                }
            },
            async requestRander () {
                try {
                    const res = await this.requestExperienceDetail({
                        projectId: this.projectId,
                        experienceHashId: this.experienceHashId
                    })
                    this.setCurReleaseDetail({
                        publish_time: this.localConvertTime(res.createDate),
                        end_time: this.localConvertTime(res.expireDate).split(' ')[0],
                        experienceGroups: res.experienceGroups.map(expGroup => expGroup.name),
                        installation_package: res.name,
                        name: res.version,
                        desc: res.remark,
                        external_list: res.outerUsers,
                        internal_list: res.innerUsers,
                        url: res.url,
                        publisher: res.creator,
                        canExperience: res.canExperience,
                        expired: res.expired,
                        online: res.online
                    })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async requestStatistics () {
                try {
                    const res = await this.requestDownloadCount({
                        projectId: this.projectId,
                        experienceHashId: this.experienceHashId
                    })

                    this.downloadInfo.total = res.downloadUsers
                    this.downloadInfo.account = res.downloadTimes
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            toReleaseList () {
                this.$router.push({
                    name: 'experienceList',
                    params: {
                        projectId: this.projectId
                    }
                })
            },
            changeTab (type) {
                this.$router.push({
                    name: 'experienceDetail',
                    params: {
                        projectId: this.projectId,
                        experienceId: this.experienceHashId,
                        type: type === 'baseMessage' ? 'detail' : 'statistics'
                    }
                })
            },
            async downloadInstallationAction (hasPermission, isExpired, isOnline, type) {
                if (hasPermission && !isExpired && isOnline) {
                    try {
                        const res = await this.downloadInstallation({
                            projectId: this.projectId,
                            experienceHashId: this.experienceHashId
                        })
                        const url = type ? `${API_URL_PREFIX}/pc/download/devops_pc_forward.html?downloadUrl=` + escape(res.url) : res.url
                        window.location.href = url
                        this.requestStatistics()
                    } catch (err) {
                        const message = err.message ? err.message : err
                        const theme = 'error'

                        this.$bkMessage({
                            message,
                            theme
                        })
                    }
                }
            },
            /**
             * 处理时间格式
             */
            localConvertTime (timestamp) {
                return convertTime(timestamp * 1000)
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    %flex {
        display: flex;
        align-items: center;
    }

    .release-detail-wrapper {
        background: white;
        height: 100%;

        .icon-arrows-left {
            margin-right: 4px;
            cursor: pointer;
            color: $iconPrimaryColor;
            font-size: 16px;
            font-weight: 600;
        }

        .inner-detail-container {
            padding: 20px;
            min-width: 1120px;
            height: 100%;

            .bk-tab2-nav {
                width: 100%;
            }
        }

        .release-detail-content {
            height: calc(100% - 62px);
        }

        .bk-tab2-small {
            height: 100%;
        }

        .bk-tab-list {
            height: 120px;
            overflow: hidden;
        }

        .download-list-table {
            height: 92%;
            overflow: auto;

            .table-head,
            .table-row {
                padding: 0 20px;
                @extend %flex;
                height: 43px;
                font-size: 14px;
                color: #333C48;
            }

            .table-head {
                color: #333C48;
            }

            .table-row {
                border-top: 1px solid $borderWeightColor;
                color: $fontWeightColor;
                font-size: 12px;

                &:last-child {
                    border-bottom: 1px solid $borderWeightColor;
                }
            }

            .download-username,
            .download-count {
                flex: 4;
            }

            .download-last-time {
                flex: 2;
            }
        }

        .bk-page .page-item .page-button {
            height: 36px;
            .devops-icon {
                line-height: 2.5;
            }
        }
    }
</style>
