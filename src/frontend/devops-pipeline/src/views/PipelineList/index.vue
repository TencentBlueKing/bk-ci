<template>
    <bk-tab
        :active="activePanel"
        :label-height="48"
        type="unborder-card"
        class="pipeline-content"
        @tab-change="handleTabChange"
        :validate-active="false"
    >
        <bk-tab-panel
            v-for="panel in panels"
            render-directive="if"
            :label="panel.label"
            :name="panel.name"
            :key="panel.name"
        >
            <router-view></router-view>
        </bk-tab-panel>

        <more-route slot="setting" />
    </bk-tab>
</template>

<script>
    import MoreRoute from '@/components/MoreRoute'
    import { defineComponent, getCurrentInstance, ref } from 'vue'

    export default defineComponent({
        components: {
            MoreRoute
        },
        setup () {
            const vm = getCurrentInstance()
            const activePanel = ref(vm.proxy.$route.name)

            const panels = [
                {
                    label: vm.proxy.$t('pipeline'),
                    name: 'PipelineManageList'
                },
                {
                    label: vm.proxy.$t('pipelineDataBoard'),
                    name: 'PipelineDataBoard'
                }
            ]

            function handleTabChange (name) {
                if (activePanel.value === name) return
                // 跳转到对应的路由
                this.$router.push({
                    name
                })
            }
            return {
                activePanel,
                panels,
                handleTabChange
            }
        }
    })
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
