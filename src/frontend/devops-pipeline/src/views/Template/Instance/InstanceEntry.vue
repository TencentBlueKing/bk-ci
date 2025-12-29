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
                        @click="handleGoBack"
                        outline
                    >
                        {{ $t('cancel') }}
                    </bk-button>
                    <span
                        v-bk-tooltips="{
                            disabled: templateRefTypeById ? !!templateVersion : !!templateRef,
                            content: $t('template.disabledReleaseTips')
                        }"
                    >
                        <bk-button
                            theme="primary"
                            :disabled="(templateRefTypeById ? !templateVersion : !templateRef) || (isInstanceCreateViewType && !instanceList.length) || isEditing"
                            @click="handleBatchUpgrade"
                        >
                            {{ releaseBtnText }}
                        </bk-button>
                    </span>
                </aside>
            </header>
            <main class="instance-contents">
                <template-version-selector />
                <bk-resize-layout
                    class="instance-contents-layout"
                    :initial-divide="300"
                    collapsible
                    :min="300"
                    :max="500"
                >
                    <InstanceAside
                        slot="aside"
                        :is-editing.sync="isEditing"
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
            :instance-list="instanceList"
            @change="handleBatchChange"
        />
    </section>
</template>

<script setup name="InstanceEntry">
    import ReleasePipelineSideSlider from '@/components/PipelineHeader/ReleasePipelineSideSlider'
    import TemplateBreadCrumb from '@/components/Template/TemplateBreadCrumb'
    import UseInstance from '@/hook/useInstance'
    import { allVersionKeyList } from '@/utils/pipelineConst'
    import {
        SET_INSTANCE_LIST,
        SET_RELEASE_BASE_ID,
        SET_RELEASE_ING,
        UPDATE_TEMPLATE_REF,
        UPDATE_TEMPLATE_REF_TYPE,
        INSTANCE_OPERATE_TYPE
    } from '@/store/modules/templates/constants'
    import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
    import BatchEditConfig from './BatchEditConfig'
    import InstanceAside from './InstanceAside'
    import InstanceConfig from './InstanceConfig'
    import TemplateVersionSelector from './TemplateVersionSelector'

    const { proxy } = UseInstance()
    const isLoading = ref(false)
    const showRelease = ref(false)
    const showBatchEdit = ref(false)
    const isEditing = ref(false)
    const projectId = computed(() => proxy.$route.params?.projectId)
    const templateId = computed(() => proxy.$route.params?.templateId)
    const pipeline = computed(() => proxy.$store?.state?.atom?.pipeline)
    const pipelineInfo = computed(() => proxy.$store?.state?.atom?.pipelineInfo)
    const instanceList = computed(() => proxy.$store?.state?.templates?.instanceList)
    const showTaskDetail = computed(() => proxy.$store?.state?.templates?.showTaskDetail)
    const currentVersionId = computed(() => proxy?.$route.params?.version ?? pipelineInfo.value?.version) // 路径上的模板版本号
    const templateVersion = computed(() => proxy?.$store?.state?.templates?.templateVersion) // 实例化选中的模板版本号
    const isInstanceCreateViewType = computed(() => proxy.$route.params?.type === INSTANCE_OPERATE_TYPE.CREATE)
    const useTemplateSettings = computed(() => proxy.$store?.state?.templates?.useTemplateSettings)
    const templateRef = computed(() => proxy.$store?.state?.templates?.templateRef)
    const templateRefType = computed(() => proxy.$store?.state?.templates?.templateRefType)
    const templateRefTypeById = computed(() => templateRefType.value === 'ID')

    const releaseBtnText = computed(() => {
        const type = proxy.$route.params?.type
        const textMap = {
            copy: proxy.$t('release'),
            create: proxy.$t('release'),
            upgrade: proxy.$t('template.batchUpgrade')
        }
        return textMap[type] || proxy.$t('release')
    })
    watch(() => pipeline.value, () => {
        isLoading.value = false
    }, {
        deep: true
    })
    watch(() => showTaskDetail.value, (value) => {
        if (value) {
            proxy.$store.commit(`templates/${SET_RELEASE_ING}`, true)
            setTimeout(() => {
                handleBatchUpgrade()
            }, 600)
        }
    }, {
        immediate: true
    })

    onMounted(() => {
        requestTemplateByVersion()
    })
    onBeforeUnmount(() => {
        proxy.$store.commit(`templates/${UPDATE_TEMPLATE_REF}`, null)
        proxy.$store.commit(`templates/${UPDATE_TEMPLATE_REF_TYPE}`, 'ID')
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
        
        proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, { list })
    }
    function checkInstanceListInValid () {
        let valid = true
        let message = ''
        instanceList.value.forEach(instance => {
            if (instance?.buildNo?.isRequiredParam) {
                const buildNoParams = instance.param.filter(i => allVersionKeyList.includes(i.id))
                valid = buildNoParams.every(i => !(i.defaultValue === null || i.defaultValue === ''))
                message = valid ? '' : proxy.$t('storeMap.correctPipeline')
            }
        })
        return {
            valid,
            message
        }
    }
    async function handleReleaseInstance (value) {
        const type = proxy.$route.params?.type
        const fnMap = {
            create: 'releaseInstance',
            copy: 'releaseInstance',
            upgrade: 'updateInstance'
        }
        const fn = fnMap[type]
        if (!fn) {
            proxy.$showTips({
                theme: 'error',
                message: proxy.$t(`unknown type, ${type}`)
            })
            return
        }
        const { valid, message } = checkInstanceListInValid()
        if (!valid) {
            proxy.$showTips({
                theme: 'error',
                message
            })
            throw new Error(message)
        }
        try {
            const instanceReleaseInfos = instanceList.value.map(item => {
                return {
                    pipelineId: item.pipelineId,
                    pipelineName: item.pipelineName,
                    ...(item.buildNo ? {
                        buildNo: {
                            ...item?.buildNo,
                            required: item.buildNo?.isRequiredParam
                        }
                    } : undefined),
                    param: item.param.map(i => ({
                        ...i,
                        required: i.isRequiredParam
                    })),
                    resetBuildNo: item?.resetBuildNo ?? false,
                    timerTrigger: item.timerTrigger,
                    filePath: item.filePath,
                    overrideTemplateField: item?.overrideTemplateField,
                    triggerConfigs: item?.triggerConfigs
                }
            })
            const res = await proxy.$store.dispatch(`templates/${fn}`, {
                projectId: projectId.value,
                templateId: templateId.value,
                version: templateVersion.value,
                params: {
                    templateRefType: templateRefType.value,
                    templateRef: templateRef.value?.alias ?? '',
                    useTemplateSettings: useTemplateSettings.value,
                    instanceReleaseInfos,
                    ...value
                }
            })
            proxy.$store.commit(`templates/${SET_RELEASE_BASE_ID}`, res.data)
            proxy.$store.commit(`templates/${SET_RELEASE_ING}`, true)
        } catch (e) {
            console.error(e)
        }
    }
    function handleBatchEdit () {
        showBatchEdit.value = true
    }
    function handleBatchChange (params) {
        const updateMap = new Map(params.map(item => [item.id, item.defaultValue]))

        const updatedList = instanceList.value.map(instance => ({
            ...instance,
            param: instance.param.map(p => ({
                ...p,
                defaultValue: updateMap.has(p.id) ? updateMap.get(p.id) : p.defaultValue
            }))
        }))
        
        proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, {
            list: updatedList,
            init: false
        })
    }
   
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
