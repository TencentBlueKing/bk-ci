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
                        @click="handle "
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
                    :min="300"
                    :max="500"
                >
                    <InstanceAside
                        slot="aside"
                        :is-instance-create-type="isInstanceCreateViewType"
                    />
                    <router-view slot="main" />
                </bk-resize-layout>
            </main>
        </template>
    </section>
</template>

<script setup name="InstanceEntry">
    import TemplateBreadCrumb from '@/components/template/TemplateBreadCrumb'
    import TemplateVersionSelector from './TemplateVersionSelector'
    import InstanceAside from './InstanceAside'
    import { ref, computed, onMounted, watch } from 'vue'
    import UseInstance from '@/hook/useInstance'
    const { proxy } = UseInstance()

    const isLoading = ref(false)
    const projectId = computed(() => proxy.$route.params?.projectId)
    const templateId = computed(() => proxy.$route.params?.templateId)
    const pipeline = computed(() => proxy.$store?.state?.atom?.pipeline)
    const pipelineInfo = computed(() => proxy.$store?.state?.atom?.pipelineInfo)
    const currentVersionId = computed(() => proxy?.$route.params?.version ?? pipelineInfo.value?.version)
    const isInstanceCreateViewType = computed(() => proxy.$route.params?.type === 'create')
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
    function handleGoBack () {
        proxy.$router.push({
            name: 'TemplateOverview',
            params: {
                type: 'instanceList',
                version: currentVersionId.value
            }
        })
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
        background-color: white;
        box-shadow: 0 2px 5px 0 #333c4808;
        border-bottom: 1px solid $borderLightColor;
        padding: 0 0 0 24px;
        > aside {
            height: 100%;
            margin-left: auto;
            display: flex;
            justify-self: flex-end;
            align-items: center;
            grid-gap: 10px;
        }
    }
    .instance-contents,
    .instance-contents-layout {
        height: 100%;
    }
    .instance-entry-aside {
        margin-right: 24px;
    }
    .instance-entry-wrapper {
        overflow: hidden;
    }
}
</style>
