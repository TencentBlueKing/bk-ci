<template>
    <aside class="template-bread-crumb-aside">
        <bk-breadcrumb
            class="template-bread-crumb"
            separator-class="devops-icon icon-angle-right"
            :back-router="manageRoute"
        >
            <template #prefix>
                <Logo
                    :size="12"
                    name="template-mode"
                />
            </template>
            <template v-if="!isLoading">
                <bk-breadcrumb-item
                    v-for="(crumb, index) in breadCrumbs"
                    class="template-bread-crumb-item"
                    :key="index"
                    :to="crumb.to"
                >
                    <component
                        v-if="crumb.slot"
                        :is="crumb.slot"
                        v-bind="crumb.slotProps"
                    />
                    <span v-else>{{ crumb.title }}</span>
                </bk-breadcrumb-item>
            </template>
            <i
                v-else
                class="devops-icon icon-circle-2-1 spin-icon"
            />
        </bk-breadcrumb>
        <span
            v-if="!!$slots.default"
            class="gap-line"
        >|</span>
        <slot></slot>
    </aside>
</template>

<script>
    import Logo from '@/components/Logo'
    import TemplateNameCrumbItem from '@/components/Template/TemplateNameCrumbItem'
    import { computed, defineComponent } from 'vue'
    import { useI18n } from 'vue-i18n-bridge'

    export default defineComponent({
        components: {
            Logo,
            TemplateNameCrumbItem
        },
        props: {
            templateName: {
                type: String,
                default: '--'
            },
            isLoading: {
                type: Boolean,
                default: true
            }
        },
        setup (props) {
            const { t } = useI18n()
            const manageRoute = {
                name: 'TemplateManageList'
            }
            const breadCrumbs = computed(() => [
                {
                    title: t('templateName'),
                    to: manageRoute
                }, {
                    slot: TemplateNameCrumbItem,
                    slotProps: {
                        templateName: props.templateName
                    }
                }
            ])

            return {
                manageRoute,
                breadCrumbs
            }
        }
    })

</script>

<style lang="scss">
.template-bread-crumb-aside {
    display: grid;
    grid-auto-flow: column;
    align-items: center;
    grid-gap: 16px;
    font-size: 14px;

    .template-bread-crumb {
        .template-bread-crumb-item {
            display: flex;
            align-items: center;
            color: #313238;
            .bk-breadcrumb-separator {
                color: #dcdee5;
                font-size: 10px;
            }
        }
    }

    .gap-line {
        color: #DCDEE5;
    }
}
</style>
