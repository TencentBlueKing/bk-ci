<template>
    <section>
        <empty-tips
            v-if="!myPipelineList.length"
            :title="emptyTipsConfig.title"
            :desc="emptyTipsConfig.desc"
            :btns="emptyTipsConfig.btns">
        </empty-tips>

        <section v-if="myPipelineList.length" class="all pipeline-list-content">
            <section v-show="layout === 'card'">
                <div class="pipeline-list-cards clearfix"
                    v-for="(allpipeline, index) in allPipelineListByGroup"
                    :key="`taskcards${index}`"
                    :index="index"
                    v-if="allpipeline.records.length"
                    >
                    <p class="pipeline-group"><span class="group-name">{{ allpipeline.groupName ? allpipeline.groupName : '默认分组' }}</span></p>
                    <task-card
                        v-for="(card, index) of allpipeline.records"
                        :style="{
                            marginRight: card.feConfig && card.feConfig.marginRight,
                            width: card.feConfig && card.feConfig.width
                        }"
                        :hasPermission="card.hasListPermission"
                        :config="card.feConfig"
                        :index="index"
                        :key="`taskCard${index}`"
                        :canManualStartup='card.canManualStartup'
                        >
                    </task-card>
                </div>
            </section>

            <section v-show="layout === 'table'">
                <div class="pipeline-list-table"
                    v-for="(allpipeline, index) in allPipelineListByGroup"
                    :key="`tasktable${index}`"
                    :index="index"
                    v-show="allpipeline.records.length"
                >
                    <p class="pipeline-group"><span class="group-name">{{ allpipeline.groupName ? allpipeline.groupName : '默认分组' }}</span></p>
                    <task-table
                        :list="allpipeline.records"
                        >
                    </task-table>
                </div>
            </section>
        </section>
    </section>
</template>

<script>
    import { mapGetters } from 'vuex'
    import emptyTips from '@/components/pipelineList/imgEmptyTips'
    import taskCard from '@/components/pipelineList/taskCard'
    import taskTable from '@/components/pipelineList/taskTable'
    export default {
        props: {
            layout: {
                type: String,
                default: 'card'
            }
        },
        components: {
            emptyTips,
            taskCard,
            taskTable
        },
        computed: {
            ...mapGetters({
                'pipelineList': 'pipelines/getPipelineList',
                'collectList': 'pipelines/getCollectPipelineList',
                'myPipelineList': 'pipelines/getMyPipelineList',
                'myPipelineListByGroup': 'pipelines/getMyPipelineListByGroup',
                'allPipelineListByGroup': 'pipelines/getAllPipelineListByGroup',
                'groupList': 'pipelines/getGroupList'
            }),
            projectId () {
                return this.$route.params.projectId
            }
        },
        watch: {

        },
        data () {
            return {
                emptyTipsConfig: {
                    title: '创建项目的第一条流水线',
                    desc: '该项目下还没有拥有任务流水线，可以点击下方 “创建流水线” 按钮，进行创建',
                    btns: [
                        {
                            theme: 'primary',
                            size: 'normal',
                            handler: () => this.createPipeline(),
                            text: '创建流水线'
                        },
                        {
                            theme: 'default',
                            size: 'normal',
                            handler: this.gotoDocs,
                            text: '了解更多'
                        }
                    ]
                }
            }
        },
        methods: {
            createPipeline () {
                this.$emit('createPipeline', true)
            },
            gotoDocs () {
                this.$emit('gotoDocs')
            }
        },
        mounted () {
            this.$emit('calcPage')
            this.$emit('calcMargin')
        }
    }
</script>

<style lang="scss">
    @import './../../../scss/conf';
</style>
