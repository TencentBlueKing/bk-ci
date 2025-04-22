<template>
    <bk-tab
        :active.sync="activePanel"
        :label-height="48"
        type="unborder-card"
        class="pipeline-content"
        :validate-active="false"
    >
        <bk-tab-panel
            v-for="panel in panels"
            :label="panel.label"
            :name="panel.name"
            :key="panel.name"
        >
            <component
                :is="activeComponent"
            />
        </bk-tab-panel>

        <more-route slot="setting" />
    </bk-tab>
</template>

<script>
    import MoreRoute from '@/components/MoreRoute'

    export default {
        components: {
            MoreRoute
        },
        data () {
            return {
                activePanel: 'PipelineManageList'
            }
        },
        computed: {
            projectId () {
                return this.$route.params.projectId
            },
            routeName () {
                return this.$route.name
            },
            panels () {
                return [
                    {
                        label: this.$t('pipeline'),
                        name: 'PipelineManageList',
                        component: 'router-view'
                    }
                ]
            },
            activeComponent () {
                return this.panels.find(panel => panel.name === this.activePanel)?.component
            }

        }
    }
</script>
<style lang="scss">
    @import './../../scss/conf';

    .pipeline-content {
        display: flex;
        flex-direction: column;
        height: 100%;
        .bk-tab-label-wrapper {
            text-align: center;
        }
        .bk-tab-section {
            display: flex;
            flex: 1;
            overflow: hidden;
            padding: 0;
            .bk-tab-content {
                display: flex;
                flex: 1;
                overflow: hidden;
            }
        }
        .default-link-list {
            display: flex;
            margin-right: 24px;
            .pipeline-dropdown-trigger {
                font-size: 14px;
                cursor: pointer;
                .devops-icon {
                    display: inline-block;
                    transition: all ease 0.2s;
                    margin-left: 4px;
                    font-size: 12px;
                    &.icon-flip {
                        transform: rotate(180deg);
                    }
                }
                &.active {
                    color: $primaryColor;
                }
            }
        }
    }
</style>
