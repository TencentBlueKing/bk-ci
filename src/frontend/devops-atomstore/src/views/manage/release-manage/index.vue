<template>
    <bk-tab
        :active.sync="activePanel"
        :label-height="42"
        type="card"
    >
        <bk-tab-panel
            v-for="panel in panels"
            :name="panel.name"
            :label="panel.label"
            render-directive="if"
            :key="panel.name"
        >
            <component
                :is="panel.component"
                :has-promission="hasPromission"
            />
        </bk-tab-panel>
    </bk-tab>
</template>

<script>
    import { TYPE_ENUM } from '@/utils/constants'
    import codeCheck from '@/views/manage/release-manage/code-check.vue'
    import environment from '@/views/manage/release-manage/environment.vue'
    import version from '@/views/manage/release-manage/version.vue'
    import { computed, defineComponent, getCurrentInstance, onMounted, ref } from 'vue'

    export default defineComponent({
        components: {
            environment,
            codeCheck
        },
        setup () {
            const vm = getCurrentInstance()
            const type = computed(() => vm.proxy.$route.params.type)
            const activePanel = ref('version')
            const panels = [
                { label: vm.proxy.$t('store.版本管理'), name: 'version', component: version },
                ...(
                    type.value === TYPE_ENUM.service
                        ? [{ label: vm.proxy.$t('store.环境管理'), name: 'environment', component: environment }]
                        : []
                ),
                ...(
                    type.value === TYPE_ENUM.atom
                        ? [{ label: vm.proxy.$t('store.代码质量'), name: 'check', component: codeCheck }]
                        : []
                )
            ]
            const hasPromission = ref(false)

            onMounted(() => {
                if (type.value === 'template') {
                    getTemplateUserValidate()
                }
            })

            async function getTemplateUserValidate () {
                try {
                    const { code } = vm.proxy.$route.params
                    const res = await vm.proxy.$store.dispatch('store/templateUserValidate', {
                        templateCode: code
                    })
                    hasPromission.value = res
                } catch (err) {
                    vm.proxy.$bkMessage({
                        message: err.message || err,
                        theme: 'error'
                    })
                }
            }
            return {
                activePanel,
                hasPromission,
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
