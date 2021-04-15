<template>
    <detail-container @close="$emit('close')"
        :title="currentElement.name"
        :status="currentElement.status"
        :current-tab="currentTab"
        :is-hook="((currentElement.additionalOptions || {}).elementPostInfo || false)"
    >
        <span class="head-tab" slot="tab">
            <span v-for="tab in tabList"
                :key="tab"
                :class="{ active: currentTab === tab }"
                @click="currentTab = tab"
            >{{ $t(`execDetail.${tab}`) }}</span>
        </span>
        <reference-variable slot="tool" class="head-tool" :global-envs="globalEnvs" :stages="stages" :container="container" v-if="currentTab === 'setting'" />
        <template v-slot:content>
            <plugin-log :id="currentElement.id"
                :build-id="execDetail.id"
                :current-tab="currentTab"
                :execute-count="currentElement.executeCount"
                ref="log"
                v-show="currentTab === 'log'"
            />
            <component :is="(componentList[currentTab] || {}).component" v-bind="(componentList[currentTab] || {}).bindData"></component>
        </template>
    </detail-container>
</template>

<script>
    import { mapState } from 'vuex'
    import detailContainer from './detailContainer'
    import AtomContent from '@/components/AtomPropertyPanel/AtomContent.vue'
    import ReferenceVariable from '@/components/AtomPropertyPanel/ReferenceVariable'
    import pluginLog from './log/pluginLog'
    import Report from './Report'
    import Artifactory from './Artifactory'

    export default {
        components: {
            detailContainer,
            ReferenceVariable,
            pluginLog
        },

        data () {
            return {
                currentTab: 'log',
                tabList: ['log', 'artifactory', 'report', 'setting']
            }
        },

        computed: {
            ...mapState('atom', [
                'execDetail',
                'editingElementPos',
                'globalEnvs'
            ]),

            stages () {
                return this.execDetail.model.stages
            },

            container () {
                const {
                    editingElementPos: { stageIndex, containerIndex },
                    execDetail: { model: { stages } }
                } = this
                return stages[stageIndex].containers[containerIndex]
            },

            currentElement () {
                const {
                    editingElementPos: { stageIndex, containerIndex, elementIndex },
                    execDetail: { model: { stages } }
                } = this
                return stages[stageIndex].containers[containerIndex].elements[elementIndex]
            },

            componentList () {
                return {
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
                            stageIndex: this.editingElementPos.stageIndex,
                            stages: this.stages,
                            editable: false,
                            isInstanceTemplate: false
                        }
                    }
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    /deep/ .atom-property-panel {
        padding: 10px 50px;
        .bk-form-item.is-required .bk-label, .bk-form-inline-item.is-required .bk-label {
            margin-right: 10px;
        }
    }
    /deep/ .reference-var {
        padding: 0;
    }
</style>
