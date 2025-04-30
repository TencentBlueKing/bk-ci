<template>
    <section
        class="instance-entry"
        v-bkloading="{ isLoading }"
    >
        <template>
            <header class="instance-entry-header">
                <TemplateBreadCrumb
                    :template-name="pipeline?.name"
                    :is-loading="!pipeline"
                />
                <aside
                    class="instance-entry-aside"
                >
                    <bk-button
                        theme="primary"
                        @click="handleGoBack"
                        outline
                    >
                        {{ $t('cancel') }}
                    </bk-button>
                    <span
                        v-if="isInstanceCreateViewType"
                        v-bk-tooltips="{
                            disabled: !!templateVersion,
                            content: $t('template.disabledReleaseTips')
                        }"
                    >
                        <bk-button
                            :disabled="!templateVersion"
                            theme="primary"
                            @click="handleBatchUpgrade"
                        >
                            {{ $t('release') }}
                        </bk-button>
                    </span>
                    <span
                        v-else
                        v-bk-tooltips="{
                            disabled: !!templateVersion,
                            content: $t('template.disabledReleaseTips')
                        }"
                    >
                        <bk-button
                            theme="primary"
                            :disabled="!templateVersion"
                            @click="handleBatchUpgrade"
                        >
                            {{ $t('template.batchUpgrade') }}
                        </bk-button>
                    </span>
                </aside>
            </header>
            <main class="instance-contents">
                <template-version-selector
                    :is-instance-create-type="isInstanceCreateViewType"
                />
                <bk-resize-layout
                    class="instance-contents-layout"
                    :initial-divide="300"
                    collapsible
                    :min="300"
                    :max="500"
                >
                    <InstanceAside
                        slot="aside"
                        :is-instance-create-type="isInstanceCreateViewType"
                        @batchEdit="handleBatchEdit"
                    />
                    <bk-exception
                        v-if="isInstanceCreateViewType && !instanceList.length"
                        ext-cls="instance-contents-empty"
                        slot="main"
                        type="empty"
                        scene="part"
                    >
                        {{ $t('template.pleaseSelectTemplateVersion') }}
                    </bk-exception>
                    <InstanceConfig
                        v-else
                        slot="main"
                        :is-instance-create-type="isInstanceCreateViewType"
                    />
                </bk-resize-layout>
            </main>
        </template>
        <ReleasePipelineSideSlider
            v-model="showRelease"
            is-template-instance-mode
            :version="currentVersionId"
            :instance-list="instanceList"
            :is-instance-create-type="isInstanceCreateViewType"
            :handle-change-file-path="handleChangeFilePath"
            @release="handleReleaseInstance"
        />
        <BatchEditConfig
            v-model="showBatchEdit"
        />
    </section>
</template>

<script setup name="InstanceEntry">
    import TemplateBreadCrumb from '@/components/Template/TemplateBreadCrumb'
    import BatchEditConfig from './BatchEditConfig'
    import UseInstance from '@/hook/useInstance'
    import { computed, onMounted, ref, watch } from 'vue'
    import {
        SET_INSTANCE_LIST,
        SET_RELEASE_BASE_ID
    } from '@/store/modules/templates/constants'
    import InstanceAside from './InstanceAside'
    import InstanceConfig from './InstanceConfig'
    import TemplateVersionSelector from './TemplateVersionSelector'
    import ReleasePipelineSideSlider from '@/components/PipelineHeader/ReleasePipelineSideSlider'

    const { proxy } = UseInstance()
    const isLoading = ref(false)
    const showRelease = ref(false)
    const showBatchEdit = ref(false)
    const projectId = computed(() => proxy.$route.params?.projectId)
    const templateId = computed(() => proxy.$route.params?.templateId)
    const pipeline = computed(() => proxy.$store?.state?.atom?.pipeline)
    const pipelineInfo = computed(() => proxy.$store?.state?.atom?.pipelineInfo)
    const instanceList = computed(() => proxy.$store?.state?.templates?.instanceList)
    const currentVersionId = computed(() => proxy?.$route.params?.version ?? pipelineInfo.value?.version) // 路径上的模板版本号
    const templateVersion = computed(() => proxy?.$store?.state?.templates?.templateVersion) // 实例化选中的模板版本号
    const isInstanceCreateViewType = computed(() => proxy.$route.params?.type === 'create')
    const useTemplateSettings = computed(() => proxy.$store?.state?.templates?.useTemplateSettings)
    watch(() => pipeline.value, () => {
        isLoading.value = false
    }, {
        deep: true
    })
    async function requestTemplateByVersion (version = currentVersionId.value) {
        try {
            await proxy.$store.dispatch('atom/requestPipeline', {
                projectId: projectId.value,
                templateId: templateId.value,
                version
            })
        } catch (err) {
            proxy?.$showTips({
                theme: 'error',
                message: err.message || err
            })
        }
    }

    function handleBatchUpgrade () {
        showRelease.value = true
    }
    function handleGoBack () {
        proxy.$router.push({
            name: 'TemplateOverview',
            params: {
                type: 'instanceList',
                version: currentVersionId.value
            }
        })
    }
    function handleChangeFilePath (value, index) {
        const list = instanceList.value.map(i => i)
        proxy.$set(list[index], 'filePath', value)
        
        proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, list)
    }
    async function handleReleaseInstance (value) {
        const fn = !isInstanceCreateViewType.value ? 'templates/updateInstance' : 'templates/releaseInstance'
        try {
            const instanceReleaseInfos = instanceList.value.map(item => {
                return {
                    ...item,
                    param: item.param.map(paramItem => ({
                        ...paramItem,
                        required: paramItem.isRequiredParam
                    }))
                }
            })
            const res = await proxy.$store.dispatch(fn, {
                projectId: projectId.value,
                templateId: templateId.value,
                version: templateVersion.value,
                params: {
                    ...value,
                    useTemplateSettings: useTemplateSettings.value,
                    instanceReleaseInfos
                }
            })
            proxy.$store.commit(`templates/${SET_RELEASE_BASE_ID}`, res.data)
        } catch (e) {
            console.err(e)
        }
    }
    function handleBatchEdit () {
        console.log(123)
        showBatchEdit.value = true
    }
    onMounted(() => {
        requestTemplateByVersion()
    })
</script>

<style lang="scss">
@import './../../../scss/conf';

.instance-entry {
    .instance-entry-header {
        height: 48px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        background-color: white;
        box-shadow: 0 2px 5px 0 #333c4808;
        border-bottom: 1px solid $borderLightColor;
        padding: 0 0 0 24px;
        > aside {
            height: 100%;
            display: flex;
            align-items: center;
            grid-gap: 10px;
        }
    }
    .instance-contents,
    .instance-contents-layout {
        height: 100%;
    }
    .instance-contents-empty {
        margin-top: 15%;
    }
    .instance-entry-aside {
        margin-right: 24px;
    }
    .instance-entry-wrapper {
        overflow: hidden;
    }
}
</style>
