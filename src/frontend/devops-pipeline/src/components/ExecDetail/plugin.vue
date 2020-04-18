<template>
    <log-container
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
                :stages="execDetail.model.stages"
                :editable="false"
                :is-instance-template="false"
            >
            </atom-content>
        </template>
    </log-container>
</template>

<script>
    import { mapState } from 'vuex'
    import logContainer from './logContainer'
    import AtomContent from '@/components/AtomPropertyPanel/AtomContent.vue'
    import pluginLog from './pluginLog'

    export default {
        components: {
            logContainer,
            AtomContent,
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
                'editingElementPos'
            ]),

            downLoadPluginLink () {
                const editingElementPos = this.editingElementPos
                const fileName = encodeURI(encodeURI(`${editingElementPos.stageIndex + 1}-${editingElementPos.containerIndex + 1}-${editingElementPos.elementIndex + 1}-${this.currentElement.name}`))
                const tag = this.currentElement.id
                return `${AJAX_URL_PIRFIX}/log/api/user/logs/${this.$route.params.projectId}/${this.$route.params.pipelineId}/${this.execDetail.id}/download?tag=${tag}&executeCount=${this.executeCount}&fileName=${fileName}`
            },

            currentElement () {
                const {
                    editingElementPos: { stageIndex, containerIndex, elementIndex },
                    execDetail: { model: { stages } }
                } = this
                return stages[stageIndex].containers[containerIndex].elements[elementIndex]
            }
        },

        methods: {
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
        padding: 0 50px;
    }
</style>
