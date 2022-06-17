<template>
    <div class="output-option-wrapper">
        <div class="output-option-empty" v-if="isEmptyNav">
            <div class="no-data-right">
                <img src="../../images/box.png">
                <p>{{ $t('details.noOutputReport') }}</p>
            </div>
        </div>
        <vertical-tab v-else :tabs="tabs"></vertical-tab>
    </div>
</template>

<script>
    import { mapGetters, mapActions } from 'vuex'
    import VerticalTab from '../PipelineEditTabs/VerticalTab'

    export default {
        components: {
            VerticalTab
        },
        props: {
            curPipeline: Object
        },
        data () {
            return {
                optionList: [],
                hasWetestTab: false,
                hasThirdPartyReport: false,
                pipelineReportList: [],
                thirdPartyReportList: [],
                customizeList: [],
                loading: {
                    isLoading: false,
                    title: ''
                }
            }
        },
        computed: {
            ...mapGetters({
                checkHasCodecc: 'common/getHasAtomCheck'
            }),
            hasCodecc () {
                return this.checkHasCodecc(this.curPipeline.stages, 'linuxPaasCodeCCScript')
            },
            buildNo () {
                return this.$route.params.buildNo
            },
            tabs () {
                return [
                    ...(this.hasCodecc
                        ? [{
                            id: 'codeCheck',
                            name: this.$t('details.codeCheck'),
                            component: 'codeCheck',
                            componentProps: {

                            }
                        }]
                        : []
                    ),
                    ...(this.hasWetestTab
                        ? [{
                            id: 'wetestReport',
                            name: this.$t('details.wetestReportName'),
                            component: 'wetestReport',
                            componentProps: {
                                pipelineReportList: this.pipelineReportList
                            }
                        }]
                        : []
                    ),
                    ...(this.hasThirdPartyReport
                        ? [{
                            id: 'thirdReport',
                            name: this.$t('details.thirdReport'),
                            component: 'thirdPartyReport',
                            componentProps: {
                                reportList: this.thirdPartyReportList
                            }
                        }]
                        : []
                    ),
                    ...this.customizeList.map(item => ({
                        ...item,
                        name: item.name,
                        component: 'IframeReport',
                        componentProps: {
                            ...item
                        },
                        isCustomize: true
                    }))

                ]
            },
            isEmptyNav () {
                return this.tabs.length === 0
            }
        },
        watch: {
            buildNo () {
                this.init()
            }
        },
        created () {
            this.init()
        },
        methods: {
            ...mapActions('common', [
                'requestWetestReport',
                'requestReportList'
            ]),
            async init () {
                try {
                    const { projectId, pipelineId, buildNo } = this.$route.params
                    const params = {
                        projectId,
                        pipelineId,
                        buildId: buildNo
                    }
                    const [reportRes] = await Promise.all([
                        this.requestReportList(params)
                    ])
                    // 先把wetest报告相关注释
                    // const [wetestRes, reportRes] = await Promise.all([
                    //     this.requestWetestReport(params),
                    //     this.requestReportList(params)
                    // ])

                    // if (wetestRes.records && wetestRes.records.length) {
                    //     this.hasWetestTab = true
                    //     this.pipelineReportList = [
                    //         ...wetestRes.records
                    //     ]
                    // }

                    if (reportRes.length) {
                        this.thirdPartyReportList = []
                        this.customizeList = []
                        reportRes.map(item => item.type === 'THIRDPARTY' ? this.thirdPartyReportList.push(item) : this.customizeList.push(item))
                        this.hasThirdPartyReport = this.thirdPartyReportList.length > 0
                    }
                } catch (err) {
                    this.$showTips({
                        theme: 'error',
                        message: err.message || err
                    })
                }
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';
    .output-option-wrapper {
        height: 100%;
        position: relative;
        .output-nav {
            height: 100%;
            .bkdevops-output-nav-tab {
                height: 100%;
                display: flex;
                flex-direction: column;
            }
        }
        .report-iframe-content {
            // overflow: hidden;
            height: 95%;
            width: 100%;
            // min-width: 1280px;
            .iframe-over-layout {
                height: 100%;
                width: 100%;
                position: absolute;
                z-index: 2;
            }
            iframe {
                width: 100%;
                height: 100%;
                border: 0;
            }
        }
        .output-option-empty {
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
