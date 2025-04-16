<template>
    <span class="template-name-crumb">
        <Logo
            :size="16"
            :name="typeIcon"
        />
        <span @click="goHistory">{{ templateName }}</span>
        <pac-tag
            v-if="pacEnabled"
            :info="yamlInfo"
        />
    </span>
</template>

<script>

    import Logo from '@/components/Logo'
    import UseInstance from '@/hook/useInstance'
    import { TEMPLATE_TYPE } from '@/utils/pipelineConst'
    import { computed, defineComponent } from 'vue'
    import PacTag from '@/components/PacTag'

    export default defineComponent({
        components: {
            Logo,
            PacTag
        },
        props: {
            templateType: {
                type: String,
                default: TEMPLATE_TYPE.PIPELINE
            },
            templateName: {
                type: String,
                default: '--'
            }
        },
        setup (props) {
            const { proxy } = UseInstance()
            const typeIcon = computed(() => {
                return `${props.templateType.toLowerCase()}-template`
            })
            const pacEnabled = computed(() => proxy.$store.getters['atom/pacEnabled'] ?? false)
            const pipelineInfo = computed(() => proxy.$store.state?.atom.pipelineInfo)
            const yamlInfo = computed(() => pipelineInfo.value?.yamlInfo)
            function goHistory (e) {
                if (proxy.$route.name !== 'TemplateOverview') {
                    e.stopPropagation()

                    proxy.$router.push({
                        name: 'TemplateOverview',
                        params: {
                            ...proxy.$route.params,
                            version: pipelineInfo.value?.releaseVersion
                        }
                    })
                }
            }

            return {
                typeIcon,
                pacEnabled,
                yamlInfo,
                goHistory
            }
        }

    })
</script>

<style lang="scss" scoped>
    .template-name-crumb {
        display: flex;
        align-items: center;
        grid-gap: 8px;
        span {
            cursor: pointer;
        }
    }
</style>
