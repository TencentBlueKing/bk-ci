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
                    <bk-button
                        v-if="isInstanceCreateViewType"

                        theme="primary"
                        @click="handleBatchUpgrade"
                    >
                        {{ $t('release') }}
                    </bk-button>
                    <bk-button
                        v-else
                        theme="primary"
                        @click="handleBatchUpgrade"
                    >
                        {{ $t('template.batchUpgrade') }}
                    </bk-button>
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
            is-template-mode
            :instance-list="instanceList"
            :is-instance-create-type="isInstanceCreateViewType"
            :handle-change-file-path="handleChangeFilePath"
            @release="handleReleaseInstance"
        />
    </section>
</template>

<script setup name="InstanceEntry">
    import TemplateBreadCrumb from '@/components/Template/TemplateBreadCrumb'
    import UseInstance from '@/hook/useInstance'
    import { computed, onMounted, ref, watch } from 'vue'
    import {
        SET_INSTANCE_LIST
    } from '@/store/modules/templates/constants'
    import InstanceAside from './InstanceAside'
    import InstanceConfig from './InstanceConfig'
    import TemplateVersionSelector from './TemplateVersionSelector'
    import ReleasePipelineSideSlider from '@/components/PipelineHeader/ReleasePipelineSideSlider'

    const { proxy } = UseInstance()
    const isLoading = ref(false)
    const showRelease = ref(false)
    const projectId = computed(() => proxy.$route.params?.projectId)
    const templateId = computed(() => proxy.$route.params?.templateId)
    const pipeline = computed(() => proxy.$store?.state?.atom?.pipeline)
    const pipelineInfo = computed(() => proxy.$store?.state?.atom?.pipelineInfo)
    const instanceList = computed(() => proxy.$store?.state?.templates?.instanceList)
    const currentVersionId = computed(() => proxy?.$route.params?.version ?? pipelineInfo.value?.version)
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
        try {
            await proxy.$store.dispatch('templates/updateInstance', {
                useTemplateSettings: useTemplateSettings.value,
                projectId: projectId.value,
                templateId: templateId.value,
                version: currentVersionId.value,
                params: {
                    ...value,
                    instanceReleaseInfos: instanceList.value
                }
            })
        } catch (e) {
            console.err(e)
        }
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
