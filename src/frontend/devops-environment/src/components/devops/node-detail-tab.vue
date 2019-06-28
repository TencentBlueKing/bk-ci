<template>
    <div class="node-details-tab">
        <bk-tab :active.sync="activeName" :type="currentType" style="margin-top: 20px;">
            <bk-tab-panel
                v-for="(panel, index) in menuList"
                v-bind="panel"
                :key="index">
                <basic-information v-if="panel.name === 'base'"></basic-information>
                <env-variable v-if="panel.name === 'envVariable'"></env-variable>
                <pipeline-list v-if="panel.name === 'pipeline'"></pipeline-list>
                <machine-record v-if="panel.name === 'activity'"></machine-record>
            </bk-tab-panel>
        </bk-tab>
    </div>
</template>

<script>
    import BasicInformation from '@/components/devops/environment/basic-information'
    import EnvVariable from '@/components/devops/environment/env-variable'
    import PipelineList from '@/components/devops/environment/pipeline-list'
    import MachineRecord from '@/components/devops/environment/machine-record'

    export default {
        components: {
            BasicInformation,
            EnvVariable,
            PipelineList,
            MachineRecord
        },
        data () {
            return {
                panels: [
                    { name: 'mission', label: ' 任务报表', count: 10 },
                    { name: 'config', label: '加速配置', count: 20 },
                    { name: 'hisitory', label: '历史版本', count: 30 },
                    { name: 'deleted', label: '已归档加速任务', count: 40 }
                ],
                active: 'mission',
                type: ['card', 'border-card', 'unborder-card'],
                currentType: 'card',
                activeName: 'base',
                menuList: [
                    { name: 'base', label: '基本信息' },
                    { name: 'envVariable', label: '环境变量' },
                    { name: 'pipeline', label: '构建任务' },
                    { name: 'activity', label: '机器上下线记录' }
                ]
            }
        },
        mounted () {
        },
        methods: {
            tabChanged (tab) {
                this.activeName = tab
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';
    .node-details-tab {
        margin-top: 20px;
        .bk-tab-label-wrapper .bk-tab-label-list .active {
            background-color: #FBFBFB;
        }
        .bk-tab-section {
            padding: 0;
        }
    }
</style>
