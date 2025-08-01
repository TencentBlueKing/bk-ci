<template>
    <bk-tab
        :active.sync="activePanel"
        :label-height="42"
        type="card"
    >
        <bk-tab-panel
            v-for="panel in panels"
            v-bind="panel"
            render-directive="if"
            :key="panel.name"
        >
            <component :is="panel.component" />
        </bk-tab-panel>
    </bk-tab>
</template>

<script>
    import apiSetting from '@/views/manage/setting/api-setting.vue'
    import memberSetting from '@/views/manage/setting/member-setting.vue'
    import privateSetting from '@/views/manage/setting/private-setting.vue'
    import { computed, defineComponent, getCurrentInstance, ref } from 'vue'
    import publishStrategy from './publish-strategy.vue'
    import visibleRange from './visible-range.vue'

    export default defineComponent({
        components: {
            memberSetting,
            privateSetting,
            apiSetting,
            publishStrategy,
            visibleRange
        },
        setup () {
            const vm = getCurrentInstance()
            const type = computed(() => vm.proxy.$route.params.type)
            const activePanel = ref('member')
            const panels = [
                {
                    label: vm.proxy.$t('store.成员管理'),
                    name: 'member',
                    component: memberSetting
                },
                ...(
                    type.value === 'template'
                        ? [{
                            label: vm.proxy.$t('store.发布策略'),
                            name: 'strategy',
                            component: publishStrategy
                        }]
                        : []
                ),
                ...(
                    type.value === 'atom'
                        ? [{
                            label: vm.proxy.$t('store.私有配置'),
                            name: 'private',
                            component: privateSetting
                        }, {
                            label: vm.proxy.$t('store.apiSettingManage'),
                            name: 'api',
                            component: apiSetting
                        }]
                        : []
                )
            ]
            return {
                activePanel,
                panels
            }
        }
    })
</script>

<style lang="scss" scoped>
    ::v-deep .bk-tab-section {
        padding: 0;
    }
</style>
