<template>
    <div class="node-details-tab">
        <bk-tab :active.sync="activeName" :type="currentType" style="margin-top: 20px;">
            <bk-tab-panel
                v-for="(panel, index) in menuList"
                v-bind="panel"
                :key="index">
                <basic-information v-if="panel.name === 'base'"></basic-information>
                <pipeline-list v-if="panel.name === 'pipeline'"></pipeline-list>
                <machine-record v-if="panel.name === 'activity'"></machine-record>
            </bk-tab-panel>
        </bk-tab>
    </div>
</template>

<script>
    import BasicInformation from '@/components/setting/agent-detail/basic-information'
    import PipelineList from '@/components/setting/agent-detail/pipeline-list'
    import MachineRecord from '@/components/setting/agent-detail/machine-record'

    export default {
        components: {
            BasicInformation,
            PipelineList,
            MachineRecord
        },
        data () {
            return {
                active: 'mission',
                type: ['card', 'border-card', 'unborder-card'],
                currentType: 'card',
                activeName: 'base',
                menuList: [
                    { name: 'base', label: this.$t('setting.nodeInfo.basicInfo') },
                    { name: 'pipeline', label: this.$t('setting.nodeInfo.buildTask') },
                    { name: 'activity', label: this.$t('setting.nodeInfo.machineActivityRecord') }
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

<style lang="postcss">
    @import '@/css/conf';
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
