<template>
    <bk-popover
        placement="top"
        max-width="400"
        theme="light toggle-required"
        transfer
        class="toggle-required-param-popover"
    >
        <span
            :class="[
                'icon-item',
                {
                    'is-follow': props.isFollowTemplate,
                    'is-collapsed': props.isCollapsed
                }
            ]"
            @click.stop="handleChangeStatus"
        >
            <span
                v-if="props.isLoading || initLoading"
                class="bk-icon icon-circle-2-1 spin-icon"
            >
            </span>
            <Logo
                v-else
                name="template-mode"
                size="12"
            />
        </span>
        <template slot="content">
            <div
                class="follow-popover-content"
                v-bkloading="{
                    isLoading: props.isLoading || initLoading,
                    theme: 'primary',
                    size: 'small'
                }"
            >
                <div>
                    <span class="current-status">{{ $t('template.currentStatus') }}</span>
                    <div class="option-item current">
                        {{ props.isFollowTemplate ? $t('template.follow') : $t('template.unfollow') }}
                        <div class="sub-title">
                            {{
                                props.isFollowTemplate
                                    ? $t('template.followTemplateTips', [classifyLabel || displayTypeText])
                                    : $t('template.unfollowTemplateTips', [classifyLabel || displayTypeText])
                          
                            }}
                        </div>
                    </div>
                </div>
                <div
                    class="option-item"
                    @click.stop="handleChangeStatus"
                >
                    {{ !props.isFollowTemplate ? $t('template.setFollow') : $t('template.setUnfollow') }}
                    <div class="sub-title">
                        {{
                            !props.isFollowTemplate
                                ? $t('template.followTemplateTips', [classifyLabel || displayTypeText])
                                : $t('template.unfollowTemplateTips', [classifyLabel || displayTypeText])
                        }}
                    </div>
                </div>
            </div>
        </template>
    </bk-popover>
</template>
<script setup name="ToggleRequiredParamPopover">
    import { ref, defineProps, computed } from 'vue'
    import UseInstance from '@/hook/useInstance'
    import Logo from '@/components/Logo'
    const { proxy } = UseInstance()
    const props = defineProps({
        isCollapsed: {
            type: Boolean,
            required: false
        },
        isFollowTemplate: {
            type: Boolean,
            required: true
        },
        handleChange: {
            type: Function,
            required: true
        },
        type: {
            type: String,
            required: true,
            default: 'defaultValue'
        },
        classifyLabel: {
            type: String,
            required: false,
            default: ''
        },
        isLoading: {
            type: Boolean,
            required: false,
            default: false
        }
    })
    const initLoading = ref(false)
    const displayTypeText = computed(() => {
        const typeMap = {
            'defaultValue': proxy.$t('paramDefaultValue'),
            'trigger': proxy.$t('triggerSetting'),
            'introVersion': proxy.$t('template.versionSetting')
        }
        return typeMap[props.type] || typeMap.defaultValue
    })
    function handleChangeStatus (event) {
        event.preventDefault()
        initLoading.value = true
        props.handleChange(!props.isFollowTemplate)
        setTimeout(() => {
            initLoading.value = false
        }, 200)
    }
</script>

<style lang="scss" scoped>
    .icon-item {
        position: relative;
        display: flex;
        align-items: center;
        justify-content: space-around;
        width: 22px;
        height: 22px;
        color: #4D4F56;
        background: #EAEBF0;
        border-radius: 2px;
        margin-left: 6px;
        cursor: pointer;
        &.is-follow,
        &.active{
            color: #3A84FF;
            background: #E1ECFF;
            &:hover {
                background: #CDDFFE !important;
            }
        }
        &:hover {
            background: #DCDEE5;
        }
    }
    .is-collapsed {
        background: #fff;
        &.is-follow,
        &.active{
            background: #CDDFFE !important;
        }
        // &:hover {
        //     background: #CDDFFE !important;
        // }
    }
   
</style>

<style lang="scss">
    .toggle-required-param-popover {
        display: inline-flex;
    }
    .toggle-required-theme {
        padding: 0 !important;
        .follow-popover-content {
            .current-status {
                position: relative;
                bottom: 2px;
                padding: 0 4px;
                font-size: 10px;
                color: #1768EF;
                height: 16px;
                background: #E1ECFF;
                border-radius: 0 0 2px 0;
            }
            .sub-title {
                color: #979BA5;
            }
            .option-item {
                padding: 0 20px 4px 8px;
                line-height: 20px;
                color: #4D4F56;
                font-size: 12px;
                background-color: #f5f7fa;
                cursor: pointer;
                &.current {
                    cursor: default;
                    background-color: #fff;
                }
            }
        }
    }
</style>
