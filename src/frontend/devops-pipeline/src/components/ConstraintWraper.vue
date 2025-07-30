<template>
    <div
        class="template-constraint-tips-popover"
    >
        <div
            :class="['template-constraint-title', {
                'space-between': spaceBetween
            }]"
        >
            <slot
                name="constraint-title"
                v-bind:props="{ isOverride: isOverrideField, toggleConstraint: toggleConstraint }"
            >
                <label
                    v-if="label"
                    class="constraint-title-text"
                >
                    {{ label }}
                </label>
            </slot>
            <bk-popover
                v-if="instanceFromTemplate"
                ref="constraintTips"
                transfer
            >
                <span
                    :class="['template-constraint-mode-icon', { 'is-override': isOverrideField }]"
                    @click="toggleConstraint"
                >
                    <logo
                        name="template-mode"
                        size="12"
                    />
                </span>
                <div
                    class="template-constraint-tips"
                    slot="content"
                >
                    {{ constraintTipsContent }}
                </div>
            </bk-popover>
        </div>
        <div
            v-if="showConstraintArea"
            :class="{
                'template-constraint-area': showConstraintAreaBg && instanceFromTemplate,
                'is-override': isOverrideField
            }"
            :isOverride="isOverrideField"
        >
            <slot
                name="constraint-area"
                v-bind:props="{ isOverride: isOverrideField, toggleConstraint: toggleConstraint }"
            >
            </slot>
        </div>
    </div>
</template>

<script>
    import Logo from '@/components/Logo/index.vue'
    import useTemplateConstraint from '@/hook/useTemplateConstraint'
    import { computed, getCurrentInstance, ref, watch } from 'vue'
    import { useI18n } from 'vue-i18n-bridge'
    export default {
        emits: ['toggleConstraint'],
        components: {
            Logo
        },
        props: {
            spaceBetween: {
                type: Boolean,
                default: true
            },
            showConstraintArea: {
                type: Boolean,
                default: true
            },
            showConstraintAreaBg: {
                type: Boolean,
                default: true
            },
            label: {
                type: String,
                default: ''
            },
            classify: {
                type: String,
                default: ''
            },
            field: {
                type: String,
                default: ''
            }
        },
        setup (props, ctx) {
            const { isOverrideTemplate, toggleConstraint } = useTemplateConstraint()
            const vm = getCurrentInstance()
            const { t } = useI18n()
            const constraintTips = ref(null)
            const fieldMap = {
                buildNumRule: 'CUSTOM_BUILD_NUM',
                label: 'LABEL',
                notices: 'NOTICES',
                parallelSetting: 'CONCURRENCY',
                failIfVariableInvalid: 'FAIL_IF_VARIABLE_INVALID'
            }
            const instanceFromTemplate = computed(() => {
                return vm.proxy.$store.getters['atom/instanceFromTemplate'] ?? false
            })
            const fieldAlias = computed(() => {
                return fieldMap[props.field] ?? props.field
            })
            const classifyLabel = computed(() => {
                const classify = props.classify ?? ''
                if (fieldAlias.value === 'NOTICES') {
                    return t('noticeConf')
                }
            
                const labelMap = {
                    triggerStepIds: 'triggerSetting',
                    paramIds: 'paramDefaultValue',
                    settingGroups: 'template.settings',
                }
                return t(labelMap[classify] ?? 'unknown')
            })
            const isOverrideField = computed(() => {
                return isOverrideTemplate(props.classify, fieldAlias.value)
            })
            const isTriggerClassify = computed(() => {
                return props.classify === 'triggerStepIds'
            })

            const constraintTipsContent = computed(() => {
                const prefix = isOverrideField.value ? 'to' : 'un'
                if (isTriggerClassify.value) {
                    return t(`${prefix}TriggerConstraintTips`)
                }
                
                return t(`${prefix}ConstraintTips`, [classifyLabel.value])
            })

            watch(() => isOverrideField.value, (isOverride) => {
                ctx.emit('toggleConstraint', isOverride)
            })
            
            return {
                toggleConstraint: (instance) => {
                    if (isTriggerClassify.value && !fieldAlias.value) {
                        vm.proxy.$bkMessage({
                            theme: 'error',
                            message: t('triggerStepIdNotSet')
                        })
                        return
                    }
                    if (fieldAlias.value) {
                        toggleConstraint(props.classify, fieldAlias.value)
                        instance?.hide?.()
                    }
                    
                },
                instanceFromTemplate,
                constraintTips,
                isOverrideField,
                constraintTipsContent
            }
        }
    }
</script>

<style lang="scss">
    .template-constraint-tips-popover {
        border-radius: 2px;
        width: 100%;
        .template-constraint-title {
            display: flex;
            
            align-items: center;
            font-size: 14px;
            color: #606266;
            padding: 4px 0;
            grid-gap: 8px;
            &.space-between {
                justify-content: space-between;
            }
            .constraint-title-text {
                font-size: 12px;
            }
            .template-constraint-mode-icon {
                display: flex;
                align-items: center;
                justify-content: center;
                width: 24px;
                height: 24px;
                border-radius: 2px;
                background: #E1ECFF;
                color: #3A84FF;
                cursor: pointer;
                &.is-override {
                    background: #EAEBF0;
                    color: #4D4F56;
                }
            }
        }
        .template-constraint-area {
            padding: 8px;
            background-color: transparent;
            &:not(.is-override) {
                background-color: #FAFBFD;
            }
        }
        .bk-tooltip-ref {
            width: 100%;
        }
    }
    
</style>