<template>
    <div class="template-version-selector">
        <span class="selector-prepend">
            {{ isInstanceCreateType ? $t('template.templateVersion') : $t('template.upgradedVersion') }}
        </span>
        <VersionSelector
            v-if="versionValue"
            class="version-selector"
            :value="versionValue"
            @change="handleVersionChange"
            :include-draft="false"
            refresh-list-on-expand
            :show-extension="false"
        />
        <bk-button
            :class="{
                'preview-btn': versionValue
            }"
            :disabled="!versionValue"
            @click="handlePreview"
        >
            {{ $t('pipelinesPreview') }}
        </bk-button>
        <bk-checkbox
            v-model="useTemplateSettings"
            class="apply-checkbox"
            @change="handleChangeUseTemplateSettings"
        >
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
                        @click="handleToViewDetails"
                    >
                        {{ $t('template.viewDetails') }}
                        <logo
                            name="tiaozhuan"
                            size="14"
                            style="fill:#3c96ff;position:relative;top:2px;"
                        />
                    </span>
                </div>
            </bk-popover>
        </bk-checkbox>
        <pipeline-template-preview
            v-model="isShowPreview"
            :template-pipeline="templatePipeline"
        />
    </div>
</template>

<script setup>
    import { ref, defineProps, computed } from 'vue'
    import Logo from '@/components/Logo'
    import UseInstance from '@/hook/useInstance'
    import VersionSelector from '@/components/PipelineDetailTabs/VersionSelector'
    import PipelineTemplatePreview from '@/components/PipelineTemplatePreview'
    import { UPDATE_USE_TEMPLATE_SETTING } from '@/store/modules/templates/constants'
    defineProps({
        isInstanceCreateType: Boolean
    })
    const isShowPreview = ref(false)
    const templatePipeline = ref({})
    const { proxy } = UseInstance()
    const projectId = computed(() => proxy.$route.params?.projectId)
    const templateId = computed(() => proxy.$route.params?.templateId)
    const useTemplateSettings = computed(() => proxy.$store?.state?.templates?.useTemplateSettings)
    const pipelineInfo = computed(() => proxy.$store?.state?.atom?.pipelineInfo)
    const versionValue = ref(proxy?.$route.params?.version ?? pipelineInfo.value?.version)
    
    async function handleVersionChange (value) {
        if (!value) return
        try {
            versionValue.value = value
            const res = await proxy.$store.dispatch('templates/fetchTemplateByVersion', {
                projectId: projectId.value,
                templateId: templateId.value,
                version: value
            })
            if (!res.resource) return
            templatePipeline.value = {
                templateId: res.resource.templateId,
                projectId: res.resource.projectId,
                stages: res.resource.model.stages
            }
        } catch (e) {
            console.error(e)
        }
    }
    function handleChangeUseTemplateSettings (value) {
        proxy.$store.commit(`templates/${UPDATE_USE_TEMPLATE_SETTING}`, value)
    }
    function handleToViewDetails () {
        window.open(`${location.origin}/console/pipeline/${projectId.value}/template/${templateId.value}/${versionValue.value}/setting`)
    }
    function handlePreview () {
        if (!versionValue.value) return
        isShowPreview.value = true
    }
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
            height: 32px;
            margin-right: 20px;
            .pipeline-version-dropmenu-trigger {
                height: 32px;
                background: #fff;
                border: 1px solid #c4c6cc;
            }
        }
        .preview-btn {
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
    .apply-checkbox {
        margin-left: 15px;
    }
    .apply-tips-popover {
        .jump-btn {
            color: #3A84FF;
            cursor: pointer;
        }
    }
</style>
