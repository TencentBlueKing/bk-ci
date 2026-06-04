<template>
    <detail-container
        @close="$emit('close')"
        :title="currentElement.name"
        :status="currentElement.status"
        :current-tab="currentTab"
        :is-hook="((currentElement.additionalOptions || {}).elementPostInfo || false)"
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
                    @click="currentTab = tab.name"
                >{{ $t(`execDetail.${tab.name}`) }}</span>
            </template>
        </span>
        <reference-variable
            slot="tool"
            class="head-tool"
            :global-envs="globalEnvs"
            :stages="stages"
            :container="container"
            v-if="currentTab === 'setting'"
        />
        <template v-slot:content>
            <error-summary
                v-if="activeErorr && currentTab === 'log'"
                :error="activeErorr"
            ></error-summary>
            <plugin-log
                :id="currentElement.id"
                :key="currentElement.id"
                :build-id="execDetail.id"
                :current-tab="currentTab"
                :exec-detail="execDetail"
                :execute-count="currentElement.executeCount"
                ref="log"
                v-if="currentTab === 'log'"
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
    import ProgressDetailPanel from '@/components/ProgressDetailPanel'

    export default {
        components: {
            detailContainer,
            ReferenceVariable,
            pluginLog,
            ErrorSummary,
            ProgressDetailPanel
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
                tabList: [
                    { name: 'progress', show: true },
                    { name: 'log', show: true },
                    { name: 'artifactory', show: false, completeLoading: false },
                    { name: 'setting', show: true },
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

            currentElement () {
                const {
                    editingElementPos: { elementIndex }
                } = this
                return this.container.elements?.[elementIndex] ?? {}
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
                    },
                    setting: {
                        component: AtomContent,
                        bindData: {
                            elementIndex: this.editingElementPos.elementIndex,
                            containerIndex: this.editingElementPos.containerIndex,
                            containerGroupIndex: this.editingElementPos.containerGroupIndex,
                            stageIndex: this.editingElementPos.stageIndex,
                            stages: this.stages,
                            editable: false,
                            isInstanceTemplate: false
                        }
                    }
                }
            },

            activeErorr () {
                return null
                // try {
                //     return this.execDetail.errorInfoList.find(error => error.taskId === this.currentElement.id)
                // } catch (error) {
                //     return null
                // }
            },
            progressHeaderMeta () {
                const buildNum = this.execDetail.buildNum ? `#${this.execDetail.buildNum}` : ''
                const stageName = `stage ${this.editingElementPos.stageIndex + 1}`
                return [buildNum, stageName].filter(Boolean).join(' - ')
            },
            defaultTab () {
                const progressFirstStatus = [
                    'RUNNING',
                    'QUEUE',
                    'WAITING',
                    'PREPARE_ENV',
                    'LOOP_WAITING',
                    'CALL_WAITING'
                ]
                return progressFirstStatus.includes(this.currentElement.status) ? 'progress' : 'log'
            },
            sortedTabList () {
                const mapping = {
                    PROGRESS: 'progress',
                    LOG: 'log',
                    ARTIFACT: 'artifactory',
                    CONFIG: 'setting'
                }

                const orderedTabs = [
                    this.tabList.find(tab => tab.name === 'progress'),
                    ...this.properties.map(prop => {
                        const tabName = mapping[prop]
                        return this.tabList.find(tab => tab.name === tabName)
                    })
                ].filter(Boolean)

                const reportTab = this.tabList.find(tab => tab.name === 'report')
                if (reportTab) {
                    orderedTabs.push(reportTab)
                }

                const visibleTabs = orderedTabs.filter(tab => tab.show)
                if (!visibleTabs.some(tab => tab.name === this.currentTab)) {
                    this.currentTab = visibleTabs.find(tab => tab.name === this.defaultTab)?.name
                        ?? visibleTabs[0]?.name
                }
                return orderedTabs
            }
        },

        watch: {
            'currentElement.id': function () {
                this.tabList = [
                    { name: 'progress', show: true },
                    { name: 'log', show: true },
                    { name: 'artifactory', show: true, completeLoading: false },
                    { name: 'setting', show: true },
                    { name: 'report', show: false, completeLoading: false }
                ]
                this.currentTab = this.defaultTab
            }
        },

        methods: {
            toggleTab (key, show = false) {
                const tab = this.sortedTabList.find(tab => tab.name === key)
                tab.show = show
            },

            completeLoading (key) {
                const tab = this.sortedTabList.find(tab => tab.name === key)
                tab.completeLoading = true
            }
        }
    }
</script>

<style lang="scss" scoped>
    ::v-deep .atom-property-panel {
        padding: 10px 50px;
        .bk-form-item.is-required .bk-label, .bk-form-inline-item.is-required .bk-label {
            margin-right: 10px;
        }
    }
    ::v-deep .reference-var {
        padding: 0;
    }
</style>
