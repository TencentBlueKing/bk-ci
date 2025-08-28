<template>
    <div class="template-version-selector">
        <bk-select
            v-model="templateRefType"
            :clearable="false"
            class="ref-type-select"
            @change="handleChangeTemplateRefType"
        >
            <bk-option
                v-for="option in templateRefTypeList"
                :key="option.id"
                :id="option.id"
                :name="option.label"
                :disabled="option.disabled"
            >
                {{ option.label }}
                <i
                    v-if="option.tips"
                    v-bk-tooltips="option.tips"
                    class="devops-icon icon-info-circle"
                />
            </bk-option>
        </bk-select>
        <template v-if="templateRefTypeById">
            <span class="selector-prepend">
                {{ isInstanceCreateType ? $t('template.templateVersion') : $t('template.upgradedVersion') }}
            </span>
            <VersionSelector
                v-if="templateRefTypeById"
                class="version-selector"
                :value="versionValue"
                @change="handleVersionChange"
                :include-draft="false"
                refresh-list-on-expand
                :show-extension="false"
            />
        </template>
        <template v-else>
            <bk-select
                v-model="pullMode"
                :clearable="false"
                class="selector-prepend"
            >
                <bk-option
                    v-for="option in pullModeList"
                    :key="option.id"
                    :id="option.id"
                    :name="option.label"
                />
            </bk-select>
            <div class="ref-input">
                <bk-input
                    v-model="templateRef"
                    :placeholder="templateRefPlaceholderMap[pullMode]"
                    @change="handelChangeTemplateRef"
                />
                <i
                    v-if="errorRefMsg"
                    class="bk-icon icon-exclamation-circle-shape tooltips-icon"
                    v-bk-tooltips="errorRefMsg"
                />
            </div>
        </template>
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
                    <div class="template-setting-tips-content">
                        <ul>
                            <li>
                                {{
                                    isInstanceCreateType
                                        ? $t('template.applyTemplateSettingTips.create.tips1')
                                        : $t('template.applyTemplateSettingTips.update.tips1')
                                }}
                            </li>
                            <li>
                                {{
                                    isInstanceCreateType
                                        ? $t('template.applyTemplateSettingTips.create.tips2')
                                        : $t('template.applyTemplateSettingTips.update.tips2')
                                }}
                            </li>
                        </ul>
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
    import { ref, defineProps, computed, watch } from 'vue'
    import Logo from '@/components/Logo'
    import UseInstance from '@/hook/useInstance'
    import VersionSelector from '@/components/PipelineDetailTabs/VersionSelector'
    import PipelineTemplatePreview from '@/components/PipelineTemplatePreview'
    import {
        UPDATE_USE_TEMPLATE_SETTING,
        UPDATE_TEMPLATE_REF_TYPE,
        UPDATE_TEMPLATE_REF,
        SET_TEMPLATE_DETAIL
    } from '@/store/modules/templates/constants'
    defineProps({
        isInstanceCreateType: Boolean
    })
    const isShowPreview = ref(false)
    const templatePipeline = ref({})
    const errorRefMsg = ref('')
    const pullMode = ref('tag')
    const { proxy } = UseInstance()
    const projectId = computed(() => proxy.$route.params?.projectId)
    const templateId = computed(() => proxy.$route.params?.templateId)
    const useTemplateSettings = computed(() => proxy.$store?.state?.templates?.useTemplateSettings)
    const pacEnabled = computed(() => proxy.$store.getters['atom/pacEnabled'] ?? false)
    const templateRef = computed(() => proxy.$store?.state?.templates?.templateRef)
    const templateRefType = computed(() => proxy.$store?.state?.templates?.templateRefType)
    const pipelineInfo = computed(() => proxy.$store?.state?.atom?.pipelineInfo)
    const versionValue = ref(proxy?.$route.params?.version ?? pipelineInfo.value?.version)
    const templateRefTypeById = computed(() => templateRefType.value === 'ID')
    const templateRefTypeList = computed(() => ([
        {
            id: 'ID',
            label: proxy.$t('template.referenceById')
        },
        {
            id: 'PATH',
            label: proxy.$t('template.referenceByPath'),
            tips: proxy.$t('template.referenceByPathTips'),
            disabled: !pacEnabled.value
        }
    ]))
    const pullModeList = computed(() => ([
        {
            id: 'tag',
            label: 'tag'
        },
        {
            id: 'branch',
            label: proxy.$t('template.branch')
        },
        {
            id: 'commit',
            label: proxy.$t('template.commit')
        }
    ]))
    watch(() => pullMode.value, () => {
        proxy.$store.commit(`templates/${UPDATE_TEMPLATE_REF}`, '')
        errorRefMsg.value = ''
    })
    const templateRefPlaceholderMap = computed(() => ({
        tag: `${proxy.$t('template.Example')} tag1`,
        branch: `${proxy.$t('template.Example')} dev`,
        commit: `${proxy.$t('template.Example')} d3b3c6a7e9e9d3b3c6a7e9e9d3`
    }))
    function getRefByPullMode (value) {
        const refMap = {
            branch: `refs/heads/${value}`,
            tag: `refs/tags/${value}`,
            commit: value
        }
        return refMap[pullMode.value]
    }
    async function handleVersionChange (value) {
        if (templateRefTypeById.value && !value) return
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
                stages: res.resource.model.stages,
                name: res.setting.pipelineName
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

    function handleChangeTemplateRefType (value) {
        errorRefMsg.value = ''
        proxy.$store.commit(`templates/${UPDATE_TEMPLATE_REF}`, '')
        proxy.$store.commit(`templates/${UPDATE_TEMPLATE_REF_TYPE}`, value)
        proxy.$store.commit(`templates/${SET_TEMPLATE_DETAIL}`, {
            templateVersion: '',
            templateDetail: {}
        })
        templatePipeline.value = {}
    }

    async function handelChangeTemplateRef (value) {
        proxy.$store.commit(`templates/${UPDATE_TEMPLATE_REF}`, value)
        try {
            const res = await proxy.$store.dispatch('templates/fetchTemplateByRef', {
                projectId: projectId.value,
                templateId: templateId.value,
                ref: getRefByPullMode(templateRef.value)
            })
            if (!res.resource) return
            templatePipeline.value = {
                templateId: res.resource.templateId,
                projectId: res.resource.projectId,
                stages: res.resource.model.stages
            }
            errorRefMsg.value = ''
        } catch (e) {
            errorRefMsg.value = e.message || proxy.$t('template.notTemplate')
            proxy.$store.commit(`templates/${SET_TEMPLATE_DETAIL}`, {
                templateVersion: '',
                templateDetail: {}
            })
            console.error(e)
        }
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
        .ref-type-select {
            width: 150px;
        }
        .ref-input {
            width: 200px;
            margin-right: 20px;
            position: relative;
            display: inline-block;
            vertical-align: middle;
             .tooltips-icon {
                position: absolute;
                z-index: 10;
                right: 8px;
                top: 8px;
                color: #ea3636;
                cursor: pointer;
                font-size: 16px;
             }
        }
        .selector-prepend {
            display: inline-block;
            height: 32px;
            line-height: 32px;
            padding: 0 8px;
            border: 1px solid #c4c6cc;
            border-right: none;
            border-left: none;
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
        .template-setting-tips-content {
            display: flex;
        }
        .jump-btn {
            color: #3A84FF;
            margin-left: 8px;
            cursor: pointer;
            position: relative;
            bottom: 1px;
        }
    }
</style>
