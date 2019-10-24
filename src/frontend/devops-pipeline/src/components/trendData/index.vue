<template>
    <div class="code-record-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
        <!-- 安装包趋势 -->
        <vertical-tab v-if="trendList.length && showContent" :tabs="tabs" :init-tab-index="initIndex">
        </vertical-tab>

        <div class="artifactory-empty" v-else-if="showContent && !trendList.length">
            <div class="no-data-right">
                <img src="../../images/box.png">
                <p>{{ $t('history.noPackages') }}</p>
            </div>
        </div>
    </div>
</template>

<script>
    import VerticalTab from '../PipelineEditTabs/VerticalTab'
    export default {
        components: {
            VerticalTab
        },
        props: {
            dateRange: {
                type: Object,
                default: []
            }
        },
        data () {
            return {
                initIndex: 0,
                showContent: false,
                trendList: [],
                loading: {
                    isLoading: false,
                    title: ''
                }
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            startTime () {
                const initTime = this.dateRange[0] ? this.dateRange[0].replace('-', '/') : ''
                return parseInt(new Date(initTime) / 1000) || 0
            },
            endTime () {
                const initTime = this.dateRange[1] ? this.dateRange[1].replace('-', '/') : ''
                return parseInt(new Date(initTime) / 1000 + 24 * 3600) || 0
            },
            tabs () {
                return this.trendList.map(item => ({
                    id: item.name,
                    name: item.name,
                    component: 'TrendTable',
                    componentProps: {
                        trendData: item.trendData
                    }
                }))
            }
        },
        watch: {
            pipelineId () {
                this.init()
            },
            dateRange () {
                this.init()
            }
        },
        created () {
            this.init()
        },
        methods: {
            async init () {
                const {
                    loading,
                    pipelineId,
                    startTime,
                    endTime
                } = this

                loading.isLoading = true
                loading.title = this.$t('loadingTips')

                try {
                    if (!this.startTime || !this.endTime) return
                    const res = await this.$store.dispatch('soda/requestTrendData', {
                        pipelineId,
                        startTime,
                        endTime
                    })
                    if (res && typeof res.trendData === 'object') {
                        this.trendList = []
                        if (Object.keys(res.trendData).length) {
                            Object.keys(res.trendData).forEach(key => {
                                this.trendList.push({
                                    name: key,
                                    trendData: res.trendData[key]
                                })
                            })
                        }
                        this.tabIndex = 0
                    }
                } catch (err) {
                    this.$showTips({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                        this.showContent = true
                    }, 500)
                }
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
    .code-record-wrapper {
        height: 100%;
        .code-factory-tab {
            border: none;
            height: 100%;
            background-color: $bgHoverColor;
            .prompt-tips {
                padding: 10px 0;
                color: $fontWeightColor;
                font-size: 14px;
            }
        }
        .artifactory-empty {
            flex: 1;
            .no-data-right {
                text-align: center;
                padding-top: 226px;
                p {
                    line-height: 60px;
                }
            }
        }
        .code-records-empty {
            flex: 1;
            .no-data-right {
                text-align: center;
                padding-top: 200px;
                p {
                    line-height: 60px;
                }
            }
        }
    }
</style>
