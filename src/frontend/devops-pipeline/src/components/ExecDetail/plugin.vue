<template>
    <detail-container @close="$emit('close')"
        :title="currentElement.name"
        :status="currentElement.status"
        :current-tab="currentTab"
        :is-hook="((currentElement.additionalOptions || {}).elementPostInfo || false)"
    >
        <span class="head-tab" slot="tab">
            <span @click="currentTab = 'log'" :class="{ active: currentTab === 'log' }">{{ $t('execDetail.log') }}</span><span @click="currentTab = 'setting'" :class="{ active: currentTab === 'setting' }">{{ $t('execDetail.setting') }}</span>
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
            <atom-content v-show="currentTab === 'setting'"
                :element-index="editingElementPos.elementIndex"
                :container-index="editingElementPos.containerIndex"
                :stage-index="editingElementPos.stageIndex"
                :stages="stages"
                :editable="false"
                :is-instance-template="false"
            >
            </atom-content>
        </template>
    </detail-container>
</template>

<script>
    import { mapState } from 'vuex'
    import detailContainer from './detailContainer'
    import AtomContent from '@/components/AtomPropertyPanel/AtomContent.vue'
    import ReferenceVariable from '@/components/AtomPropertyPanel/ReferenceVariable'
    import pluginLog from './log/pluginLog'

    export default {
        components: {
            detailContainer,
            AtomContent,
            ReferenceVariable,
            pluginLog
        },

        data () {
            return {
                currentTab: 'log'
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
