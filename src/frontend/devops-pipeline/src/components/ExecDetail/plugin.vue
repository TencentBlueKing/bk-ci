<template>
    <div>
        <detail-container
            @close="$emit('close')"
            :title="currentElement.name || ''"
            :position="panelPosition"
            :status="currentElement.status"
            :current-tab="currentTab"
            :is-hook="((currentElement.additionalOptions || {}).elementPostInfo || false)"
            :ignore-close="configOpen"
        >
            <span
                class="head-tab"
                slot="tab"
                v-if="isGetPluginHeadTab"
            >
                <template v-for="tab in sortedTabList">
                    <span
                        v-if="tab.show"
                        :key="tab.name"
                        :class="{ active: currentTab === tab.name }"
                        @click="selectTab(tab.name)"
                    >{{ $t(`execDetail.${tab.name}`) }}</span>
                </template>
            </span>
            <template v-slot:content>
                <error-summary
                    v-if="activeErorr && currentTab === 'log' && useLegacyLog"
                    :error="activeErorr"
                ></error-summary>
                <step-log-panel
                    v-if="currentTab === 'log' && !useLegacyLog"
                    :id="currentElement.id"
                    :key="'v2-' + currentElement.id"
                    :build-id="execDetail.id"
                    :execute-count="currentElement.executeCount"
                    :exec-detail="execDetail"
                    :element="currentElement"
                    :job="container"
                    ref="log"
                    @fallback="useLegacyLog = true"
                />
                <plugin-log
                    :id="currentElement.id"
                    :key="currentElement.id"
                    :build-id="execDetail.id"
                    :current-tab="currentTab"
                    :exec-detail="execDetail"
                    :execute-count="currentElement.executeCount"
                    ref="log"
                    v-else-if="currentTab === 'log'"
                />
                <log-params-view
                    v-if="currentTab === 'property'"
                    :model="paramsModel"
                    @view-config="configOpen = true"
                />
                <component
                    v-show="currentTab === key"
                    :is="value.component"
                    v-bind="value.bindData"
                    v-for="(value, key) in componentList"
                    :key="key"
                    :ref="key"
                    @toggle="(show) => toggleTab(key, show)"
                    @complete="completeLoading(key)"
                ></component>
            </template>
        </detail-container>
        <bk-sideslider
            :is-show.sync="configOpen"
            :width="640"
            :quick-close="true"
            :z-index="2100"
            class="step-plugin-config-slider"
        >
            <header
                class="plugin-config-hd"
                slot="header"
            >
                <span
                    class="plugin-config-name"
                    :title="currentElement.name"
                >{{ currentElement.name }}</span>
                <span
                    class="plugin-config-ro"
                    :title="$t('logPanel.readonly')"
                >
                    <i class="devops-icon icon-eye"></i>
                    {{ $t('logPanel.readonly') }}
                </span>
                <reference-variable
                    class="plugin-config-ref"
                    :global-envs="globalEnvs"
                    :stages="stages"
                    :container="container"
                />
            </header>
            <atom-content
                v-if="configOpen"
                slot="content"
                :element-index="editingElementPos.elementIndex"
                :container-index="editingElementPos.containerIndex"
                :container-group-index="editingElementPos.containerGroupIndex"
                :stage-index="editingElementPos.stageIndex"
                :stages="stages"
                :editable="false"
                :is-instance-template="false"
            />
        </bk-sideslider>
    </div>
</template>

<script>
    import AtomContent from '@/components/AtomPropertyPanel/AtomContent.vue'
    import ReferenceVariable from '@/components/AtomPropertyPanel/ReferenceVariable'
    import ErrorSummary from '@/components/ExecDetail/ErrorSummary'
    import { mapState } from 'vuex'
    import Artifactory from './Artifactory'
    import Report from './Report'
    import detailContainer from './detailContainer'
    import pluginLog from './log/pluginLog'
    import StepLogPanel from './log-panel/StepLogPanel'
    import LogParamsView from './log-panel/LogParamsView'
    import { positionCode, buildParamsModel } from './log-panel/logPanelAdapter'
    import ProgressDetailPanel from '@/components/ProgressDetailPanel'

    export default {
        components: {
            detailContainer,
            ReferenceVariable,
            pluginLog,
            StepLogPanel,
            LogParamsView,
            ErrorSummary,
            AtomContent,
            ProgressDetailPanel,
            Artifactory,
            Report
        },
        props: {
            execDetail: {
                type: Object,
                required: true
            },
            editingElementPos: {
                type: Object,
                required: true
            },
            properties: {
                type: Array,
                default: () => ['LOG', 'ARTIFACT', 'CONFIG']
            }
        },
        data () {
            return {
                currentTab: null,
                userSelectedTab: false,
                useLegacyLog: false,
                configOpen: false,
                tabList: [
                    { name: 'progress', show: false },
                    { name: 'log', show: true },
                    { name: 'artifactory', show: false, completeLoading: false },
                    { name: 'property', show: true },
                    { name: 'report', show: false, completeLoading: false }
                ]
            }
        },

        computed: {
            ...mapState('atom', [
                'globalEnvs',
                'isGetPluginHeadTab'
            ]),

            stages () {
                return this.execDetail.model.stages
            },

            container () {
                const {
                    editingElementPos: { stageIndex, containerIndex, containerGroupIndex },
                    execDetail: { model: { stages } }
                } = this
                try {
                    if (containerGroupIndex !== undefined) {
                        return stages[stageIndex].containers[containerIndex].groupContainers[containerGroupIndex]
                    } else {
                        return stages[stageIndex].containers[containerIndex]
                    }
                } catch (_) {
                    return {}
                }
            },

            panelPosition () {
                return positionCode(this.editingElementPos, 3)
            },
            currentElement () {
                const {
                    editingElementPos: { elementIndex }
                } = this
                return this.container.elements?.[elementIndex] ?? {}
            },
            paramsModel () {
                return buildParamsModel(this.currentElement)
            },

            componentList () {
                return {
                    progress: {
                        component: ProgressDetailPanel,
                        bindData: {
                            buildId: this.execDetail.id,
                            taskId: this.currentElement.id,
                            executeCount: this.currentElement.executeCount,
                            taskStatus: this.currentElement.status,
                            showEmpty: true,
                            showHeader: true,
                            headerMeta: this.progressHeaderMeta
                        }
                    },
                    artifactory: {
                        component: Artifactory,
                        bindData: {
                            taskId: this.currentElement.id
                        }
                    },
                    report: {
                        component: Report,
                        bindData: {
                            taskId: this.currentElement.id
                        }
                    }
                }
            },

            activeErorr () {
                return null
            },
            progressHeaderMeta () {
                const buildNum = this.execDetail.buildNum ? `#${this.execDetail.buildNum}` : ''
                const stageName = `stage ${this.editingElementPos.stageIndex + 1}`
                return [buildNum, stageName].filter(Boolean).join(' - ')
            },
            isRunningStatus () {
                const progressFirstStatus = [
                    'RUNNING',
                    'QUEUE',
                    'WAITING',
                    'PREPARE_ENV',
                    'LOOP_WAITING',
                    'CALL_WAITING'
                ]
                return progressFirstStatus.includes(this.currentElement.status)
            },
            hasProgressTab () {
                return !!this.tabList.find(tab => tab.name === 'progress')?.show
            },
            defaultTab () {
                return (this.isRunningStatus && this.hasProgressTab) ? 'progress' : 'log'
            },
            sortedTabList () {
                // 对齐原型：日志 → 进度 → 制品 → 报告 → 参数（进度仅沿用现网组件，不做完整重做）
                const order = ['log', 'progress', 'artifactory', 'report', 'property']
                return order.map(name => this.tabList.find(tab => tab.name === name)).filter(Boolean)
            },
            visibleTabList () {
                return this.sortedTabList.filter(tab => tab.show)
            },
            visibleTabKey () {
                return this.visibleTabList.map(tab => tab.name).join('|')
            }
        },

        watch: {
            isGetPluginHeadTab: {
                immediate: true,
                handler (visible) {
                    if (visible) this.ensureCurrentTab()
                }
            },
            visibleTabKey: {
                immediate: true,
                handler () {
                    if (this.isGetPluginHeadTab) this.ensureCurrentTab()
                }
            },
            'currentElement.id': function () {
                this.userSelectedTab = false
                this.configOpen = false
                this.tabList = [
                    { name: 'progress', show: false },
                    { name: 'log', show: true },
                    { name: 'artifactory', show: true, completeLoading: false },
                    { name: 'property', show: true },
                    { name: 'report', show: false, completeLoading: false }
                ]
                this.currentTab = this.defaultTab
            }
        },

        methods: {
            selectTab (name) {
                this.userSelectedTab = true
                this.currentTab = name
            },

            ensureCurrentTab () {
                if (!this.visibleTabList.some(tab => tab.name === this.currentTab)) {
                    this.currentTab = this.visibleTabList.find(tab => tab.name === this.defaultTab)?.name
                        ?? this.visibleTabList[0]?.name
                }
            },

            toggleTab (key, show = false) {
                const tab = this.tabList.find(tab => tab.name === key)
                if (!tab) return
                tab.show = show
                if (key === 'progress' && show && this.isRunningStatus && !this.userSelectedTab) {
                    this.currentTab = 'progress'
                }
            },

            completeLoading (key) {
                const tab = this.sortedTabList.find(tab => tab.name === key)
                if (tab) tab.completeLoading = true
            }
        }
    }
</script>

<style lang="scss" scoped>
    ::v-deep .atom-property-panel {
        padding: 10px 24px 24px;
        .bk-form-item.is-required .bk-label, .bk-form-inline-item.is-required .bk-label {
            margin-right: 10px;
        }
    }
    ::v-deep .reference-var {
        padding: 0;
    }
    .plugin-config-hd {
        display: flex;
        align-items: center;
        gap: 8px;
        width: calc(100% - 30px);
        min-width: 0;
    }
    .plugin-config-name {
        flex: 1;
        min-width: 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        font-size: 16px;
        color: #313238;
    }
    .plugin-config-ro {
        flex-shrink: 0;
        display: inline-flex;
        align-items: center;
        gap: 4px;
        height: 20px;
        padding: 0 6px;
        border-radius: 2px;
        background: #f0f1f5;
        color: #63656e;
        font-size: 12px;
        line-height: 20px;
    }
    .plugin-config-ref {
        margin-left: auto;
    }
    .lp-output-tab,
    ::v-deep .detail-artifactory-home,
    ::v-deep .detail-report-home {
        flex: 1;
        min-height: 0;
        width: 100%;
        background: #fff;
    }
</style>
<style lang="scss">
    .step-plugin-config-slider {
        .bk-sideslider-content {
            overflow: auto;
            background: #fff;
        }
    }
</style>
