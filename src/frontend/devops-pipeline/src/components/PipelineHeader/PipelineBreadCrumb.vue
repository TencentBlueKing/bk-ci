<template>
    <aside class="pipeline-bread-crumb-aside">
        <bk-breadcrumb class="pipeline-bread-crumb" separator-class="devops-icon icon-angle-right" :back-router="manageRoute">
            <template #prefix>
                <span class="devops-icon icon-pipeline"></span>
            </template>
            <template v-if="!isLoading">
                <bk-breadcrumb-item v-for="(crumb, index) in breadCrumbs" :key="index" :to="crumb.to">
                    <component v-if="crumb.slot" :is="crumb.slot" v-bind="crumb.slotProps" />
                    <span v-else>{{ crumb.title }}</span>
                </bk-breadcrumb-item>
            </template>
            <i v-else class="devops-icon icon-circle-2-1 spin-icon" />
        </bk-breadcrumb>
        <span v-if="!!$slots.default" class="gap-line">|</span>
        <slot></slot>
    </aside>
</template>

<script>
    import PipelineNameCrumbItem from './PipelineNameCrumbItem'
    import BuildNumSwitcher from './BuildNumSwitcher'
    import { mapGetters, mapState } from 'vuex'

    export default {
        components: {
            PipelineNameCrumbItem,
            BuildNumSwitcher
        },
        props: {
            showRecordEntry: Boolean,
            showBuildNumSwitch: Boolean,
            pipelineName: String,
            isLoading: Boolean,
            showPacTag: {
                type: Boolean,
                default: true
            }
        },
        data () {
            return {
                pipelineListSearching: false,
                pipelineList: []
            }
        },
        computed: {
            ...mapState('atom', [
                'pipelineSetting',
                'pipelineInfo',
                'execDetail'
            ]),
            ...mapGetters({
                pipelineHistoryViewable: 'atom/pipelineHistoryViewable',
                pacEnabled: 'atom/pacEnabled'
            }),
            yamlInfo () {
                return this.pipelineInfo?.yamlInfo
            },
            manageRoute () {
                return {
                    name: 'PipelineManageList'
                }
            },
            breadCrumbs () {
                return [{
                            title: this.$t('pipeline'),
                            to: this.manageRoute
                        }, this.$route.name === 'pipelineImportEdit'
                            ? {
                                title: this.pipelineSetting?.pipelineName ?? '--'
                            }
                            : {
                                slot: PipelineNameCrumbItem,
                                slotProps: {
                                    pipelineName: this.pipelineName ?? this.pipelineInfo?.pipelineName ?? '--',
                                    showPacTag: this.showPacTag
                                }
                            },
                        ...(
                            this.showRecordEntry
                                ? [{
                                    title: this.$t('draftExecRecords'),
                                    to: {
                                        name: 'draftDebugRecord',
                                        params: {
                                            ...this.$route.params,
                                            version: this.pipelineInfo?.version
                                        }
                                    }
                                }]
                                : []
                        ),
                        ...(
                            this.showBuildNumSwitch
                                ? [{
                                    slot: BuildNumSwitcher,
                                    slotProps: {
                                        isDebug: this.showRecordEntry,
                                        latestBuildNum: this.execDetail?.latestBuildNum ?? 1,
                                        currentBuildNum: this.execDetail?.buildNum ?? 1,
                                        version: this.pipelineInfo?.[this.showRecordEntry ? 'version' : 'releaseVersion']
                                    }

                                }]
                                : []
                        )
                ]
            }
        },
        // <build-num-switcher v-bind="buildNumConf" />
        watch: {
            'pipelineInfo.pipelineName': {
                handler (val) {
                    const title = val ? `${val} | ${this.$t('pipeline')}` : ''
                    this.$updateTabTitle?.({ title })
                },
                immediate: true
            }
        }
    }
</script>

<style lang="scss">
.pipeline-bread-crumb-aside {
    display: grid;
    grid-auto-flow: column;
    align-items: center;
    grid-gap: 16px;
    font-size: 14px;
    .gap-line {
        color: #DCDEE5;
    }
    .pipeline-bread-crumb {
        .bk-breadcrumb-item {
            display: flex;
            align-items: center;
            color: #313238;
        }
        .devops-icon.icon-angle-right {
            color: #DCDEE5;
            margin: 0 8px;
            font-size: 12px;
        }
        .devops-icon.icon-pipeline {
            font-size: 16px;
            color: #63656E;
            font-weight: 700;
        }
        .build-num-switcher-wrapper {
            display: grid;
            grid-auto-flow: column;
            grid-gap: 6px;
        }
    }
}
</style>
