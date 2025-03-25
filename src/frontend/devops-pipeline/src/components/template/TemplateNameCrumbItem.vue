<template>
    <span class="template-name-crumb">
        <Logo
            :size="16"
            :name="typeIcon"
        />
        <span>{{ templateName }}</span>
    </span>
</template>

<script>

    import Logo from '@/components/Logo'
    import { computed, defineComponent } from 'vue'

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
            const typeIcon = computed(() => {
                return `${props.templateType.toLowerCase()}-template`
            })
            return {
                typeIcon
            }
        }

    })
</script>

<style lang="scss" scoped>
    .template-name-crumb {
        display: flex;
        align-items: center;
        grid-gap: 8px;
    }
</style>
