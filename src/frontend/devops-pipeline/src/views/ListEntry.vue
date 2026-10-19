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
    import { getCacheViewId, getTemplateCacheViewId } from '@/utils/util'
    import { computed, defineComponent, getCurrentInstance } from 'vue'

    export default defineComponent({
        components: {
            MoreRoute
        },
        setup () {
            const vm = getCurrentInstance()
            const activePanel = computed(() => {
                if (vm.proxy.$route.name === 'PipelineListAuth') {
                    return 'PipelineManageList'
                }
                return vm.proxy.$route.name
            })

            const panels = [
                {
                    label: vm.proxy.$t('pipeline'),
                    name: 'PipelineManageList'
                },
                {
                    label: vm.proxy.$t('templateName'),
                    name: 'TemplateManageList'
                },
                {
                    label: vm.proxy.$t('publicVar.publicVariable'),
                    name: 'PublicVarList'
                }
            ]

            function handleTabChange (name) {
                if (activePanel.value === name) return
                // 跳转到对应的路由
                vm.proxy.$router.push({
                    name,
                    params: getParamsByRouterName(name)
                })
            }
            function getParamsByRouterName (name) {
                const paramsMap = {
                    PipelineManageList: {
                        viewId: getCacheViewId(vm.proxy.$route.params.projectId)
                    },
                    TemplateManageList: {
                        templateViewId: getTemplateCacheViewId()
                    },
                    PublicVarList: {}
                }
                return paramsMap[name]
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
    @import '@/scss/conf';

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
    }
    .bk-tab-header-setting {
        border-left: none !important;
    }
</style>
