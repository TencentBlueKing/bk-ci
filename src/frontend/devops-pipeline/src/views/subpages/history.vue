<template>
    <div class="bkdevops-pipeline-history pb20">
        <bk-tab :active.sync="currentTab" @tab-change="switchTab" :before-toggle="beforeSwitch" class="bkdevops-pipeline-tab-card" type="unborder-card">
            <div class="bkdevops-pipeline-tab-card-setting" slot="setting" v-if="currentTab === 'trendData'">
                <bk-date-picker :placeholder="'选择日期范围'" :value="dateRange" :type="'daterange'" @change="changeDateRange" :shortcuts="shortcuts" :options="dateOptions"></bk-date-picker>
            </div>
            <div class="bkdevops-pipeline-tab-card-setting" slot="setting" v-else>
                <i @click.stop="toggleFilterBar" class="bk-icon icon-filter-shape" :class="{ 'active': showFilterBar }"></i>
                <i @click.stop="toggleColumnsSelectPopup(true)" class="setting-icon bk-icon icon-cog-shape" :class="{ 'active': isColumnsSelectPopupVisible }"></i>
            </div>
            <bk-tab-panel
                v-for="panel in panels"
                tab-change="tab"
                render-directive="if"
                v-bind="{ name: panel.name, label: panel.label }"
                :key="panel.name">
                <component :is="panel.component" v-bind="panel.bindData" @hideColumnPopup="toggleColumnsSelectPopup(false)"></component>
            </bk-tab-panel>
        </bk-tab>
    </div>
</template>

<script>
    import BuildHistoryTab from '@/components/BuildHistoryTab'
    import PipelineLog from '@/components/Log'
    import { mapGetters } from 'vuex'
    import showTooltip from '@/components/common/showTooltip'
    import TrendData from '@/components/trendData'
    import trendMixins from '@/components/trendData/trendMixins'

    export default {
        components: {
            PipelineLog,
            BuildHistoryTab,
            showTooltip,
            TrendData
        },
        mixins: [trendMixins],

        props: {
            execHandler: Function
        },

        data () {
            return {
                isColumnsSelectPopupVisible: false,
                showFilterBar: false
            }
        },

        computed: {
            ...mapGetters('pipelines', {
                'statusMap': 'getStatusMap',
                'pipelineList': 'getPipelineList',
                'curPipeline': 'getCurPipeline',
                'hisPageStatus': 'getHisPageStatus'
            }),
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            panels () {
                return [{
                            name: 'history',
                            label: '执行历史',
                            component: 'BuildHistoryTab',
                            bindData: {
                                isColumnsSelectPopupVisible: this.isColumnsSelectPopupVisible,
                                showFilterBar: this.showFilterBar,
                                toggleFilterBar: this.toggleFilterBar
                            }
                        },
                        {
                            name: 'trendData',
                            label: '安装包趋势',
                            component: 'TrendData',
                            bindData: {
                                dateRange: this.dateRange
                            }
                        }
                // {
                //     name: 'buildAnalysis',
                //     label: '数据分析',
                //     disabled: true,
                //     bindData: {}
                // },
                // {
                //     name: 'buildDiff',
                //     label: '构建对比',
                //     disabled: true,
                //     bindData: {}
                // }
                ]
            },
            currentTab () {
                return this.$route.params.type || 'history'
            }
        },
        methods: {
            switchTab (tabType = '') {
                this.$router.push({
                    name: 'pipelinesHistory',
                    params: {
                        ...this.$route.params,
                        type: tabType !== 'history' ? tabType : undefined
                    }
                })
            },
            toggleColumnsSelectPopup (isShow = false) {
                this.isColumnsSelectPopupVisible = isShow
            },
            toggleFilterBar () {
                this.showFilterBar = !this.showFilterBar
            },
            beforeSwitch (tabName) {
                const tab = this.panels.find(panel => panel.name === tabName)
                const isDisabled = tab ? tab.disabled : false
                return !isDisabled
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';

    .bkdevops-pipeline-history.biz-content {
        padding: 7px 25px 0 25px;
        height: 100%;
        overflow: auto;
        .bk-picker-panel-body.bk-picker-panel-body-date {
            width: 530px;
        }
        .bk-date-picker-prev-btn-arrow-double {
            margin-left: 0px;
        }
        .bkdevops-pipeline-tab-card {
            height: 100%;
            .bk-tab-section {
                height: calc(100% - 60px);
                padding-bottom: 10px;
            }
        }
    }
</style>
