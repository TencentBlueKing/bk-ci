<template>
    <span class="template-name-crumb">
        <Logo
            :size="16"
            :name="typeIcon"
        />
        <span @click="goHistory">{{ templateName }}</span>
    </span>
</template>

<script>

    import Logo from '@/components/Logo'
    import { computed, defineComponent } from 'vue'
    import UseInstance from '@/hook/useInstance'

    export default defineComponent({
        components: {
            Logo
        },
        props: {
            templateType: {
                type: String,
                default: 'PIPELINE'
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
            const pipelineInfo = computed(() => proxy.$store.state?.atom.pipelineInfo)
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
