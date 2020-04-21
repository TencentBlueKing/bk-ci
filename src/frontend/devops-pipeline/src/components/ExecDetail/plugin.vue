<template>
    <detail-container
        @closeLog="$emit('closeLog')"
        @changeExecute="changeExecute"
        @showSearchLog="showSearchLog"
        :title="currentElement.name"
        :status="currentElement.status"
        :execute-count="currentElement.executeCount"
        :down-load-link="downLoadPluginLink"
        :search-str.sync="searchStr"
        :show-time.sync="showTime"
        :current-tab="currentTab"
    >
        <span class="head-tab" slot="tab">
            <span @click="currentTab = 'log'" :class="{ active: currentTab === 'log' }">日志</span><span @click="currentTab = 'setting'" :class="{ active: currentTab === 'setting' }">配置</span>
        </span>
        <reference-variable slot="tool" class="head-tool" :global-envs="globalEnvs" :stages="stages" :container="container" v-if="currentTab === 'setting'" />
        <template v-slot:content>
            <plugin-log :id="currentElement.id"
                :build-id="execDetail.id"
                :show-time="showTime"
                :current-tab="currentTab"
                :execute-count="executeCount"
                :search-str="searchStr"
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
    import { mapState, mapActions } from 'vuex'
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
                showTime: false,
                searchStr: '',
                currentTab: 'log',
                executeCount: 1
            }
        },

        computed: {
            ...mapState('atom', [
                'execDetail',
                'editingElementPos',
                'globalEnvs'
            ]),

            downLoadPluginLink () {
                const editingElementPos = this.editingElementPos
                const fileName = encodeURI(encodeURI(`${editingElementPos.stageIndex + 1}-${editingElementPos.containerIndex + 1}-${editingElementPos.elementIndex + 1}-${this.currentElement.name}`))
                const tag = this.currentElement.id
                return `${AJAX_URL_PIRFIX}/log/api/user/logs/${this.$route.params.projectId}/${this.$route.params.pipelineId}/${this.execDetail.id}/download?tag=${tag}&executeCount=${this.executeCount}&fileName=${fileName}`
            },

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
        },

        mounted () {
            if (!this.globalEnvs) {
                this.requestGlobalEnvs()
            }
        },

        methods: {
            ...mapActions('atom', [
                'requestGlobalEnvs'
            ]),

            showSearchLog (res) {
                this.$refs.log.showSearchLog(res)
            },

            changeExecute (execute) {
                this.executeCount = execute
                this.$refs.log.changeExecute(execute)
            }
        }
    }
</script>

<style lang="scss" scoped>
    /deep/ .atom-property-panel {
        padding: 10px 50px;
    }
    /deep/ .reference-var {
        padding: 0;
    }
</style>
