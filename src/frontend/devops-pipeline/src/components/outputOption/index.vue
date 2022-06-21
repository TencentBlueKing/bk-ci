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
    import { mapActions } from 'vuex'
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
            buildNo () {
                return this.$route.params.buildNo
            },
            tabs () {
                return [
                    ...(this.hasThirdPartyReport
                        ? [{
                            id: 'thirdReport',
                            name: this.$t('details.thirdReport'),
                            component: 'thirdPartyReport',
                            componentProps: {
                                reportList: this.thirdPartyReportList
                            }
                        }]
                        : []),
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
