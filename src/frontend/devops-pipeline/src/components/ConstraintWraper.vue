<template>
    <bk-popover
        class="template-constraint-tips-popover"
        ref="constraintTips"
        :disabled="disabled"
        transfer
    >
        <slot :isOverride="isOverrideField">
        </slot>
        <div
            class="template-constraint-tips"
            slot="content"
        >
            <span>{{ $t('settings.concurrentTips') }}</span>
            <a
                class="text-link"
                @click="unConstraint"
            >{{ $t('template.convertToCustom') }}</a>
        </div>
    </bk-popover>
</template>

<script>
    import useTemplateConstraint from '@/hook/useTemplateConstraint'
    import { computed, ref } from 'vue'
    export default {
        props: {
            classify: {
                type: String,
                default: ''
            },
            field: {
                type: String,
                default: ''
            }
        },
        setup (props) {
            const { isOverrideTemplate, unConstraint } = useTemplateConstraint()
            const constraintTips = ref(null)
            const fieldMap = {
                buildNumRule: 'CUSTOM_BUILD_NUM',
                label: 'LABEL',
                notices: 'NOTICES',
                parallelSetting: 'CONCURRENCY',
                failIfVariableInvalid: 'FAIL_IF_VARIABLE_INVALID'
            }
            const isOverrideField = computed(() => {
                return isOverrideTemplate(props.classify, fieldMap[props.field])
            })
            const disabled = computed(() => {
                return !fieldMap[props.field] || isOverrideField.value
            })
            return {
                unConstraint: () => {
                    if (fieldMap[props.field]) {
                        unConstraint(props.classify, fieldMap[props.field])
                        constraintTips.value.instance.hide()
                    }
                },
                constraintTips,
                disabled,
                isOverrideField
            }
        }
    }
</script>

<style lang="scss">
    .template-constraint-tips-popover {
        width: 100%;
        .bk-tooltip-ref {
            width: 100%;
        }
    }
    
</style>