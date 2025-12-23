<script setup>
    import { computed, defineProps } from 'vue'
    import Logo from '@/components/Logo'
    import NamingConventionTip from '@/components/namingConventionTip.vue'
    import UseInstance from '@/hook/useInstance'
    import ToggleRequiredParamPopover from '@/components/ToggleRequiredParamPopover.vue'
    import ToggleFollowTemplatePopover from '@/components/ToggleFollowTemplatePopover.vue'
    import { COMMON_PARAM_PREFIX } from '@/utils/util'

    const props = defineProps({
        label: {
            type: String,
            default: ''
        },
        inline: {
            type: Boolean,
            default: false
        },
        required: {
            type: Boolean,
            default: false
        },
        isError: {
            type: Boolean,
            default: false
        },
        errorMsg: {
            type: String,
            default: ''
        },
        hideColon: {
            type: Boolean,
            default: false
        },
        desc: {
            type: String,
            default: ''
        },
        docsLink: {
            type: String,
            default: ''
        },
        descLink: {
            type: String,
            default: ''
        },
        descLinkText: {
            type: String,
            default: ''
        },
        type: {
            type: String
        },
        labelWidth: {
            type: Number
        },
        bottomDivider: {
            type: Boolean,
            default: false
        },
        customDesc: {
            type: Boolean,
            default: false
        },
        showOperateBtn: {
            type: Boolean,
            default: false
        },
        hasChange: {
            type: Boolean,
            default: false
        },
        isDelete: {
            type: Boolean,
            default: false
        },
        isNew: {
            type: Boolean,
            default: false
        },
        isRequiredParam: {
            type: Boolean,
            default: false
        },
        isFollowTemplate: {
            type: Boolean,
            default: false
        },
        handleUseDefaultValue: {
            type: Function,
            default: () => {}
        },
        handleSetParmaRequired: {
            type: Function,
            default: () => {}
        },
        handleFollowTemplate: {
            type: Function,
            default: () => {}
        },
        // 属性更新相关
        propertyUpdates: {
            type: Array,
            default: () => []
        }
    })

    const { proxy } = UseInstance()
    const widthStyle = computed(() => {
        if (!props.labelWidth) return {}
        return {
            width: `${props.labelWidth}px`
        }
    })

    const statusTagConfig = computed(() => {
        let message, theme
        if (props.isDelete) {
            message = proxy.$t('deleted')
            theme = 'danger'
        }
        if (props.isNew) {
            message = proxy.$t('new')
            theme = 'success'
        }
        return {
            message,
            theme,
            isShow: props.isDelete || props.isNew
        }
    })

    const propertyUpdateConfig = computed(() => {
        const hasUpdates = props.propertyUpdates && props.propertyUpdates.length > 0
        return {
            isShow: hasUpdates,
            updates: props.propertyUpdates || []
        }
    })

</script>

<template>
    <div
        :class="[
            'form-field',
            {
                'bk-form-item': !props.inline,
                'form-field-group-item': props.type === 'groupItem',
                'bk-form-inline-item': props.inline,
                'is-required': props.required,
                'is-danger': props.isError
            }
        ]"
    >
        <label
            v-if="props.label"
            :title="props.label"
            class="bk-label atom-form-label"
            :style="widthStyle"
        >
            <span :class="{ deleted: props.isDelete }">{{ props.label }}</span>
            {{ props.hideColon ? '' : '：' }}
            <a
                v-if="props.docsLink"
                target="_blank"
                :href="props.docsLink"
            >
                <i class="bk-icon icon-question-circle"></i>
            </a>
            <bk-popover
                v-if="props.label.trim() && (props.desc.trim() || props.customDesc)"
                :placement="props.customDesc ? 'top-start' : 'top'"
                :theme="props.customDesc ? 'light' : 'dark'"
                :width="props.customDesc ? 892 : 'auto'"
            >
                <i
                    :class="{
                        'bk-icon': true,
                        'icon-info-circle': true
                    }"
                    :style="{
                        'margin-left': props.hideColon ? '4px' : '0',
                        color: props.hideColon ? '#979BA5' : ''
                    }"
                />
                <template slot="content">
                    <template v-if="props.customDesc">
                        <NamingConventionTip />
                    </template>
                    <template v-else>
                        <div style="font-size: 12px; max-width: 500px;">
                            <template v-if="props.desc.split('\n').length > 1">
                                <div
                                    v-for="(item, index) in props.desc.split('\n')"
                                    :key="index"
                                >{{ item }}</div>
                            </template>
                            <template v-else>
                                {{ props.desc }}
                            </template>
                            <a
                                v-if="props.descLink"
                                class="desc-link"
                                target="_blank"
                                :href="props.descLink"
                            >{{ props.descLinkText }}</a>
                        </div>
                    </template>
                </template>
            </bk-popover>
            <span
                v-if="statusTagConfig.isShow"
                :class="['status-tag', statusTagConfig.theme]"
            >
                {{ statusTagConfig.message }}
            </span>
            <bk-popover
                v-if="propertyUpdateConfig.isShow"
                placement="top"
                theme="light"
            >
                <bk-tag theme="warning">
                    {{ $t('template.propertyUpdated') }}
                </bk-tag>
                <template slot="content">
                    <div class="property-update-content">
                        <div
                            v-for="(update, index) in propertyUpdateConfig.updates"
                            :key="index"
                            class="update-item"
                        >
                            <div class="update-label">{{ update.label }}</div>
                            <div class="update-value">
                                <span
                                    class="old-value"
                                    v-bk-overflow-tips
                                >
                                    {{ update.oldValue }}
                                </span>
                                <i class="bk-icon icon-arrows-right" />
                                <span
                                    class="new-value"
                                    v-bk-overflow-tips
                                >
                                    {{ update.newValue }}
                                </span>
                            </div>
                        </div>
                    </div>
                </template>
            </bk-popover>
        </label>

        <span
            v-if="props.showOperateBtn && !props.isDelete"
            :class="[
                'operate-btn',
                { show: props.isRequiredParam || props.hasChange || props.isFollowTemplate }
            ]"
        >
            <span
                v-if="props.hasChange"
                :class="[
                    'icon-item',
                    { 'show-dot': props.hasChange }
                ]"
                @click="props.handleUseDefaultValue"
                v-bk-tooltips="{
                    theme: 'light',
                    content: $t('template.useDefaultValue')
                }"
            >
                <Logo
                    name="use-default"
                    size="18"
                />
            </span>
            <ToggleRequiredParamPopover
                :is-required-param="props.isRequiredParam"
                :handle-change="props.handleSetParmaRequired"
            />
            <ToggleFollowTemplatePopover
                :is-follow-template="props.isFollowTemplate"
                :handle-change="props.handleFollowTemplate"
                type="defaultValue"
            />
        </span>

        <div class="bk-form-content">
            <slot></slot>
            <template v-if="props.isError">
                <slot name="errorTip">
                    <p class="bk-form-help is-danger">{{ props.errorMsg?.replaceAll?.(COMMON_PARAM_PREFIX, '') }}</p>
                </slot>
            </template>
        </div>
        <div
            v-if="props.bottomDivider"
            class="bottom-border-divider"
        ></div>
    </div>
</template>

<style lang="scss">
    .form-field {
        &:hover {
            .operate-btn {
                visibility: visible;
            }
        }
        .icon-info-circle, .icon-question-circle {
            color: #C3CDD7;
            font-size: 14px;
            pointer-events: auto;
        }
        .atom-form-label {
            .deleted {
                color: #a7a9ac !important;
                text-decoration: line-through;
            }
        }
    }
    .form-field-group-item {
        display: flex;
        align-items: center;
        line-height: 32px;
        margin-top: 16px !important;
        &:first-child {
            margin-top: 0px !important;
        }
        .atom-form-label {
            text-align: right !important;
            word-break: break-all;
            align-self: self-start;
        }
        .bk-form-content {
            flex: 1;
        }
    }
    .form-field.bk-form-item {
        position: relative;
    }
    .bk-form-item,
    .bk-form-inline-item {
        .bk-label {
            position: relative;
        }
    }
    .bk-form-vertical {
        .bk-form-item.is-required .bk-label,
        .bk-form-inline-item.is-required .bk-label {
            margin-right: 10px;
        }
    }
    .desc-link {
        color: #3c96ff;
    }
    .bottom-border-divider {
        height: 1px;
        width: 100%;
        margin: 24px 0 8px;
        border-bottom: 1px solid #DCDEE5;
    }
    .status-tag {
        padding: 0 8px;
        border-radius: 2px;
        font-size: 12px;
        height: 16px;
        line-height: 16px;
        font-weight: 400;
        &.success {
            color: #299E56;
            background: #DAF6E5;
        }
        &.danger {
            color: #E71818;
            background: #FFEBEB;
        }
        &.update {
            color: #FF9C01;
            background: #FFF3E1;
            cursor: pointer;
            display: inline-flex;
            align-items: center;
            gap: 4px;
            margin-left: 4px;
            &:hover {
                background: #FFE8C3;
            }
            .icon-info-circle {
                font-size: 12px;
            }
        }
    }
    .property-update-content {
        padding: 8px 0;
        font-size: 12px;
        .update-item {
            display: flex;
            align-items: center;
            margin-bottom: 8px;
            &:last-child {
                margin-bottom: 0;
            }
        
            .update-label {
                align-items: center;
                color: #4D4F56;
                background: #f0f1f5;
                border-radius: 2px;
                display: inline-flex;
                justify-content: space-around;
                margin-right: 8px;
                max-width: 100px;
                min-width: 80px;
                overflow: hidden;
                padding: 4px 8px;
                text-align: center;
                text-overflow: ellipsis;
                white-space: nowrap;
            }
            .update-value {
                display: flex;
                align-items: center;
                gap: 8px;
                .old-value {
                    color: #C4C6CC;
                    max-width: 100px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                 }
                .icon-arrows-right {
                    color: #FF9C01;
                    font-size: 14px;
                }
                .new-value {
                    color: #313238;
                    font-weight: 500;
                    max-width: 100px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                }
            }
        }
    }
    .operate-btn {
        display: flex;
        justify-content: end;
        align-items: center;
        // visibility: hidden;
        height: 32px;
        &.show {
            visibility: visible;
        }
        .icon-item {
            position: relative;
            display: flex;
            align-items: center;
            justify-content: space-around;
            width: 22px;
            height: 22px;
            background: #EAEBF0;
            border-radius: 2px;
            margin-left: 6px;
            cursor: pointer;
            &.is-follow,
            &.active{
                background: #E1ECFF;
                &:hover {
                    background: #CDDFFE !important;
                }
            }
            &:hover {
                background: #DCDEE5;
            }
        }
        .show-dot {
            &::after {
                content: '';
                position: absolute;
                top: -2px;
                right: -2px;
                width: 5px;
                height: 5px;
                background: red;
                border-radius: 50%;
            }
        }
    }
</style>
