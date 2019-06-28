<template>
    <section>
        <empty-tips
            v-if="!myPipelineList.length"
            :title="emptyTipsConfig.title"
            :desc="emptyTipsConfig.desc"
            :btns="emptyTipsConfig.btns">
        </empty-tips>

        <section v-if="myPipelineList.length" class="pipeline-list-content">
            <section v-show="layout === 'card'">
                <div class="pipeline-list-cards clearfix"
                    v-for="(mypipeline, index) in myPipelineListByGroup"
                    :key="`taskcards${index}`"
                    :index="index"
                    v-if="mypipeline.records.length"
                    >
                    <p class="pipeline-group"><span class="group-name">{{ mypipeline.groupName ? mypipeline.groupName : '默认分组' }}</span></p>
                    <task-card
                        v-for="(card, index) of mypipeline.records"
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
                    v-for="(mypipeline, index) in myPipelineListByGroup"
                    :key="`tasktable${index}`"
                    :index="index"
                    v-show="mypipeline.records.length"
                >
                    <p class="pipeline-group"><span class="group-name">{{ mypipeline.groupName ? mypipeline.groupName : '默认分组' }}</span></p>
                    <task-table
                        :list="mypipeline.records"
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
                'statusMap': 'pipelines/getStatusMap',
                'statusMapCN': 'pipelines/getStatusMapCN',
                'myPipelineList': 'pipelines/getMyPipelineList',
                'myPipelineListByGroup': 'pipelines/getMyPipelineListByGroup',
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
                layout: 'card',
                localMyPipelineListByGroup: [],
                emptyTipsConfig: {
                    title: '创建自己的第一条流水线',
                    desc: '你还没有拥有任务流水线，可以点击下方 “创建流水线” 按钮，进行创建',
                    btns: [
                        {
                            type: 'primary',
                            size: 'normal',
                            handler: () => this.createPipeline(),
                            text: '创建流水线'
                        },
                        {
                            type: 'default',
                            size: 'normal',
                            handler: () => this.changePageType('allPipeline'),
                            text: '前往所有流水线'
                        }
                    ]
                }
            }
        },
        methods: {
            changePageType (type) {
                this.$emit('changePage', type)
            },
            createPipeline () {
                this.$emit('createPipeline', true)
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
