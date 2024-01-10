<template>
    <div class="bkdevops-pipeline-history pb20">
        <bk-tab :active.sync="currentTab" @tab-change="switchTab" :before-toggle="beforeSwitch" class="bkdevops-pipeline-tab-card" type="unborder-card">
            <div class="bkdevops-pipeline-tab-card-setting" slot="setting">
                <i @click.stop="toggleFilterBar" class="devops-icon icon-filter-shape" :class="{ 'active': showFilterBar }"></i>
                <i @click.stop="toggleColumnsSelectPopup(true)" class="setting-icon devops-icon icon-cog-shape" :class="{ 'active': isColumnsSelectPopupVisible }"></i>
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
    import { mapGetters } from 'vuex'
    import showTooltip from '@/components/common/showTooltip'
    import customExtMixin from '@/mixins/custom-extension-mixin'
    import { HistoryTabsHooks } from '@/components/Hooks/'

    export default {
        components: {
            BuildHistoryTab,
            HistoryTabsHooks,
            showTooltip
        },
        mixins: [customExtMixin],

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
                statusMap: 'getStatusMap',
                hisPageStatus: 'getHisPageStatus'
            }),
            hooks () {
                return this.extensionTabsHooks
            },
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            extensionTabs () {
                return this.extensions.map(ext => ({
                    name: ext.serviceName,
                    label: ext.serviceName,
                    component: HistoryTabsHooks,
                    bindData: {
                        tabData: {
                            projectId: this.projectId,
                            pipelineId: this.pipelineId,
                            ...ext.props.data
                        },
                        hookIframeUrl: this.getResUrl(ext.props.entryResUrl || 'index.html', ext.baseUrl)
                    }
                }))
            },
            panels () {
                return [{
                            name: 'history',
                            label: this.$t('pipelinesHistory'),
                            component: 'BuildHistoryTab',
                            bindData: {
                                isColumnsSelectPopupVisible: this.isColumnsSelectPopupVisible,
                                showFilterBar: this.showFilterBar,
                                toggleFilterBar: this.toggleFilterBar
                            }
                        },
                        ...this.extensionTabs
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
