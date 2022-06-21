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
                    { name: 'base', label: this.$t('environment.basicInfo') },
                    { name: 'pipeline', label: this.$t('environment.nodeInfo.buildTask') },
                    { name: 'activity', label: this.$t('environment.nodeInfo.machineActivityRecord') }
                ]
            }
        },
        mounted () {
        },
        methods: {
            tabChanged (tab) {
                this.activeName = tab
            },

            $t (message) {
                const arr = message.split('.')
                const str = arr[arr.length - 1] || message
                return str.replace(/^\S/, s => s.toUpperCase())
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
