<template>
    <div class="template-version-selector">
        <span class="selector-prepend">
            {{ isInstanceCreateType ? $t('template.templateVersion') : $t('template.upgradedVersion') }}
        </span>
        <bk-select
            v-model="versionValue"
            class="version-selector"
            :placeholder="$t('template.selectTemplateUpgradedVersion')"
            :loading="isLoading"
            :clearable="false"
            @change="handleVersionChange"
        >
            <bk-option
                v-for="(option, index) in versionLst"
                :key="index"
                :id="option.version"
                :name="option.versionName"
            />
        </bk-select>
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
    import { ref, defineProps, computed, onMounted } from 'vue'
    import Logo from '@/components/Logo'
    import UseInstance from '@/hook/useInstance'
    import PipelineTemplatePreview from '@/components/PipelineTemplatePreview'
    import { UPDATE_USE_TEMPLATE_SETTING } from '@/store/modules/templates/constants'
    defineProps({
        isInstanceCreateType: Boolean
    })
    const versionLst = ref([])
    const versionValue = ref()
    const isLoading = ref(false)
    const isShowPreview = ref(false)
    const templatePipeline = ref({})
    const { proxy } = UseInstance()
    const projectId = computed(() => proxy.$route.params?.projectId)
    const templateId = computed(() => proxy.$route.params?.templateId)
    const useTemplateSettings = computed(() => proxy.$store?.state?.templates?.useTemplateSettings)
    async function fetchVersionList () {
        try {
            isLoading.value = true
            const res = await proxy.$store.dispatch('templates/requestTemplateVersionList', {
                projectId: projectId.value,
                templateId: templateId.value,
                params: {
                    includeDraft: false,
                    projectId: projectId.value,
                    templateId: templateId.value
                }
            })
            versionLst.value = res.records
            isLoading.value = false
        } catch (e) {
            console.error(e)
        }
    }
    async function handleVersionChange (value) {
        if (!value) return
        try {
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
        const version = versionValue.value ?? versionLst.value[0].version
        window.open(`${location.origin}/console/pipeline/${projectId.value}/template/${templateId.value}/${version}/setting`)
    }
    function handlePreview () {
        if (!versionValue.value) return
        isShowPreview.value = true
    }
    onMounted(() => {
        fetchVersionList()
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
            margin-right: 20px;
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
