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
                class="ref-type-option"
                :title="option.label"
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
            <span
                class="selector-prepend"
                v-bk-overflow-tips
            >
                {{ !isInstanceUpgradeType ? $t('template.templateVersion') : $t('template.upgradedVersion') }}
            </span>
            <VersionSelector
                v-if="templateRefTypeById"
                is-template
                class="version-selector"
                :value="versionValue"
                @change="handleVersionChange"
                :include-draft="false"
                refresh-list-on-expand
                :show-extension="false"
                :unique-id="templateId"
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
                <template v-if="isCommitPullMode">
                    <bk-input
                        :value="templateRef"
                        :placeholder="templateRefPlaceholderMap[pullMode]"
                        @change="handelChangeTemplateRef"
                    />
                    <i
                        v-if="errorRefMsg"
                        class="bk-icon icon-exclamation-circle-shape tooltips-icon"
                        v-bk-tooltips="errorRefMsg"
                    />
                </template>
                <template v-else>
                    <bk-select
                        :clearable="false"
                        searchable
                        @change="handleChangeRefOption"
                        :remote-method="refOptionRemoteMethod"
                    >
                        <bk-option
                            v-for="option in refOptionList"
                            :key="option.name"
                            :id="option.name"
                            :name="option.name"
                        />
                    </bk-select>
                </template>
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
                                    !isInstanceUpgradeType
                                        ? $t('template.applyTemplateSettingTips.create.tips1')
                                        : $t('template.applyTemplateSettingTips.update.tips1')
                                }}
                            </li>
                            <li>
                                {{
                                    !isInstanceUpgradeType
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
            :template-version="versionValue"
            :template-pipeline="templatePipeline"
        />
    </div>
</template>

<script setup>
    import Logo from '@/components/Logo'
    import VersionSelector from '@/components/PipelineDetailTabs/VersionSelector'
    import PipelineTemplatePreview from '@/components/PipelineTemplatePreview'
    import UseInstance from '@/hook/useInstance'
    import {
        SET_TEMPLATE_DETAIL,
        UPDATE_TEMPLATE_REF,
        UPDATE_TEMPLATE_REF_TYPE,
        UPDATE_USE_TEMPLATE_SETTING
    } from '@/store/modules/templates/constants'
    import { debounce } from '@/utils/util'
    import { computed, defineProps, onMounted, ref, watch } from 'vue'
    const isShowPreview = ref(false)
    const templatePipeline = ref({})
    const errorRefMsg = ref('')
    const pullMode = ref('tag')
    const searchKey = ref('')
    const refOptionList = ref([])
    const { proxy } = UseInstance()
    const projectId = computed(() => proxy.$route.params?.projectId)
    const templateId = computed(() => proxy.$route.params?.templateId)
    const isInstanceUpgradeType = computed(() => proxy.$route.params?.type === 'upgrade')
    const useTemplateSettings = computed(() => proxy.$store?.state?.templates?.useTemplateSettings)
    const pacEnabled = computed(() => proxy.$store.getters['atom/pacEnabled'] ?? false)
    const templateRef = computed(() => proxy.$store?.state?.templates?.templateRef?.value ?? '')
    const templateRefType = computed(() => proxy.$store?.state?.templates?.templateRefType)
    const pipelineInfo = computed(() => proxy.$store?.state?.atom?.pipelineInfo)
    const versionValue = ref()
    const templateRefTypeById = computed(() => templateRefType.value === 'ID')
    const templateRefTypeList = computed(() => ([
        {
            id: 'ID',
            label: proxy.$t('template.referenceById')
        }
        // ...(
        //     pacEnabled.value ? [{
        //         id: 'PATH',
        //         label: proxy.$t('template.referenceByPath'),
        //         tips: proxy.$t('template.referenceByPathTips'),
        //     }] : []
        // )
    ]))
    const isCommitPullMode = computed(() => pullMode.value === 'commit')
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
    onMounted(() => {
        if (proxy?.$route.params?.version) {
            versionValue.value = parseInt(proxy.$route.params.version)
        } else {
            versionValue.value = pipelineInfo.value?.version
        }
    })

    watch(() => pipelineInfo.value?.version, (value) => {
        versionValue.value = value
    })
    watch(() => [pullMode.value, templateRefType.value], () => {
        proxy.$store.commit(`templates/${UPDATE_TEMPLATE_REF}`, null)
        errorRefMsg.value = ''
        if (!isCommitPullMode.value && !templateRefTypeById.value) {
            fetchRefOptionList()
        }
    }, {
        immediate: true
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
            proxy.$store.dispatch('templates/updateInstancePageLoading', true)
            versionValue.value = value
            const [templateParams, templateData] = await Promise.all([
                proxy.$store.dispatch('templates/fetchTemplateParamsById', {
                    projectId: projectId.value,
                    templateId: templateId.value,
                    version: value
                }),
                proxy.$store.dispatch('templates/fetchTemplateDetailByVersion', {
                    projectId: projectId.value,
                    templateId: templateId.value,
                    version: value
                })
            ])
            await proxy.$store.dispatch('templates/updateTemplateData', {
                data: templateParams,
                version: value
            })
            templatePipeline.value = {
                templateId: templateData.resource?.templateId,
                projectId: templateData.resource?.projectId,
                stages: templateData.resource?.model.stages,
                name: templateData.setting?.pipelineName
            }

            proxy.$router.replace({
                params: {
                    ...proxy.$route.params,
                    version: value
                }
            })
        } catch (e) {
            console.error(e)
        } finally {
            proxy.$store.dispatch('templates/updateInstancePageLoading', false)
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
        proxy.$store.commit(`templates/${UPDATE_TEMPLATE_REF}`, null)
        proxy.$store.commit(`templates/${UPDATE_TEMPLATE_REF_TYPE}`, value)
        proxy.$store.commit(`templates/${SET_TEMPLATE_DETAIL}`, {
            templateVersion: '',
            templateDetail: {}
        })
        templatePipeline.value = {}
    }
    async function fetchTemplateDateByRef (value) {
        const refAlias = getRefByPullMode(value)
        proxy.$store.commit(`templates/${UPDATE_TEMPLATE_REF}`, {
            value,
            alias: refAlias
        })
        try {
            proxy.$store.dispatch('templates/updateInstancePageLoading', true)
            const [templateParams, templateData] = await Promise.all([
                proxy.$store.dispatch('templates/fetchTemplateParamsByRef', {
                    projectId: projectId.value,
                    templateId: templateId.value,
                    ref: refAlias
                }),
                proxy.$store.dispatch('templates/fetchTemplateDetailByRef', {
                    projectId: projectId.value,
                    templateId: templateId.value,
                    ref: refAlias
                })
            ])
            const version = templateData.resource.version
            await proxy.$store.dispatch('templates/updateTemplateData', {
                data: templateParams,
                version
            })
            templatePipeline.value = {
                templateId: templateData.resource.templateId,
                projectId: templateData.resource.projectId,
                stages: templateData.resource.model.stages,
                name: templateData.setting?.pipelineName
            }
            errorRefMsg.value = ''
        } catch (e) {
            errorRefMsg.value = e.message || proxy.$t('template.notTemplate')
            proxy.$store.commit(`templates/${SET_TEMPLATE_DETAIL}`, {
                templateVersion: '',
                templateDetail: {}
            })
            proxy.$bkMessage({
                theme: 'error',
                message: errorRefMsg.value
            })
        } finally {
            proxy.$store.dispatch('templates/updateInstancePageLoading', false)
        }
    }
    function refOptionRemoteMethod (keyword) {
        searchKey.value = keyword
        proxy.$nextTick(() => {
            fetchRefOptionList()
        })
    }
    async function fetchRefOptionList () {
        const fn = pullMode.value === 'branch'
            ? 'getBranchesListByProjectId'
            : 'getTagsListByProjectId'
        try {
            const res = await proxy.$store.dispatch(`templates/${fn}`, {
                projectId: projectId.value,
                searchKey: searchKey.value,
                repoHashId: pipelineInfo.value?.yamlInfo?.repoHashId
            })
            refOptionList.value = res.data
            searchKey.value = ''
        } catch (e) {
            console.error(e)
        }
    }
    function handleChangeRefOption (value) {
        fetchTemplateDateByRef(value)
    }
    const debouncedFetchTemplate = debounce(fetchTemplateDateByRef, 300)

    const handelChangeTemplateRef = (value) => {
        debouncedFetchTemplate(value)
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
            width: 88px;
            height: 32px;
            line-height: 32px;
            text-align: center;
            padding: 0 8px;
            border: 1px solid #c4c6cc;
            border-right: none;
            border-left: none;
            background-color: #FAFBFD;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
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
    .ref-type-select {
        width: 150px;
    }
    .ref-type-option {
        .bk-option-content {
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
    }
</style>
