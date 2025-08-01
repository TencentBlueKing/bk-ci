<template>
    <div class="node-details-tab">
        <bk-tab
            :active.sync="activeName"
            type="card"
            style="margin-top: 20px;"
        >
            <bk-tab-panel
                v-for="(panel, index) in menuList"
                v-bind="panel"
                :key="index"
            >
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
                active: 'mission',
                activeName: 'base',
                menuList: [
                    { name: 'base', label: this.$t('environment.basicInfo') },
                    { name: 'envVariable', label: this.$t('environment.environmentVariable') },
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
        .bk-tab-label-wrapper {
            text-align: left;
        }
        .bk-tab-content {
            border-top: none
        }
    }
</style>
