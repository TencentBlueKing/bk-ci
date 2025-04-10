<template>
    <div class="template-version-selector">
        <span class="selector-prepend">
            {{ isInstanceCreateType ? $t('template.templateVersion') : $t('template.upgradedVersion') }}
        </span>
        <bk-select class="version-selector"></bk-select>
        <bk-button
            class="preview-btn"
            outline
        >
            {{ $t('pipelinesPreview') }}
        </bk-button>
        <bk-checkbox>
            <bk-popover
                ext-cls="apply-tips-popover"
                theme="light"
            >
                <span
                    class="apply-tips"
                >
                    {{ $t('template.applyTemplateSetting') }}
                </span>
                <div slot="content">
                    <span>
                        {{ $t('template.applyTemplateSettingTips') }}
                    </span>
                    <span
                        class="jump-btn"
                        @click="handleJumpToViewDetails"
                    >
                        {{ $t('template.jumpToViewDetails') }}
                        <logo
                            name="tiaozhuan"
                            size="14"
                            style="fill:#3c96ff;position:relative;top:2px;"
                        />
                    </span>
                </div>
            </bk-popover>
        </bk-checkbox>
    </div>
</template>

<script setup>
    import { defineProps } from 'vue'
    import Logo from '@/components/Logo'
    
    defineProps({
        isInstanceCreateType: Boolean
    })
</script>

<style lang="scss">
    .template-version-selector {
        display: flex;
        align-items: center;
        height: 64px;
        padding: 0 24px;
        font-size: 12px;
        box-shadow: 0 2px 6px 0 #0000001f;
        background-color: #fff;
        .selector-prepend {
            display: inline-block;
            height: 32px;
            line-height: 32px;
            padding: 0 8px;
            border: 1px solid #c4c6cc;
            border-right: none;
        }
        .version-selector {
            width: 260px;
        }
        .preview-btn {
            margin: 0 20px 0 10px;
            &:hover {
                background: #fff !important;
                border-color: #3a84ff !important;
                color: #3a84ff !important;
            }
        }
        .apply-tips {
            border-bottom: 1px dashed #979ba5;
        }
        
    }
    .apply-tips-popover {
        .jump-btn {
            color: #3A84FF;
            cursor: pointer;
        }
    }
</style>
