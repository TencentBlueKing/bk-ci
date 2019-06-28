<template>
    <section>
        <empty-tips
            v-if="!collectList.length"
            :imgType="emptyTipsConfig.imgType"
            :title="emptyTipsConfig.title"
            :desc="emptyTipsConfig.desc"
            :btns="emptyTipsConfig.btns">
        </empty-tips>

        <section v-if="collectList.length" class="pipeline-list-content">
            <div class="pipeline-list-cards clearfix"
                v-show="layout === 'card'">
                <task-card
                    v-for="(card, index) of collectList"
                    :style="{
                        marginRight: card.feConfig && card.feConfig.marginRight,
                        width: card.feConfig && card.feConfig.width
                    }"
                    :hasPermission="card.hasListPermission"
                    :config="card.feConfig"
                    :index="index"
                    :key="`taskCard${index}`"
                    :canManualStartup='card.canManualStartup'>
                </task-card>
            </div>

            <div class="pipeline-list-table"
                v-show="layout === 'table'">
                <task-table
                    :list="collectList">
                </task-table>
            </div>
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
            },
            titleClickHandler: {
                type: Function,
                default: () => () => {}
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
                'collectList': 'pipelines/getCollectPipelineList',
                'myPipelineList': 'pipelines/getMyPipelineList'
            }),
            projectId () {
                return this.$route.params.projectId
            }
        },
        data () {
            return {
                // layout: 'card',
                emptyTipsConfig: {
                    title: '您尚未添加任何流水线至收藏夹',
                    desc: '将鼠标悬停在项目上会出现菜单，请单击收藏',
                    imgType: 'noCollect'
                }
            }
        },
        methods: {

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
