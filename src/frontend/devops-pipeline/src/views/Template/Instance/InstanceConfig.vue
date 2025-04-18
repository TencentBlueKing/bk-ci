<template>
    <section
        class="instance-config-constant"
        v-bkloading="{ isLoading }"
    >
        <header class="config-header">
            <div class="left">
                {{ $t('template.instanceConfig') }}
                <span class="line">|</span>
                <span class="instance-name">{{ curInstance.pipelineName }}</span>
            </div>
            <div class="right">
                todo..
            </div>
        </header>
        <div
            class="config-content"
        >
            <template>
                <section class="params-content-item">
                    <header
                        :class="['params-collapse-trigger', {
                            'params-collapse-expand': activeName.has(1)
                        }]"
                        @click="toggleCollapse(1)"
                    >
                        <bk-icon
                            type="right-shape"
                            class="icon-angle-right"
                        />

                        {{ $t('template.pipelineBuildParams') }}
                    </header>
                    <div
                        v-if="activeName.has(1)"
                        class="params-collapse-content"
                    >
                        <pipeline-params-form
                            v-if="hasPipelineParams"
                            ref="paramsForm"
                            :param-values="paramsValues"
                            :handle-param-change="handleParamChange"
                            :params="paramsList"
                            sort-category
                        >
                            <template
                                slot="versionParams"
                                v-if="isVisibleVersion"
                            >
                                <renderSortCategoryParams
                                    :name="$t('preview.introVersion')"
                                >
                                    <template slot="content">
                                        <pipeline-versions-form
                                            class="mb20"
                                            ref="versionParamForm"
                                            :build-no="buildNo"
                                            is-preview
                                            :version-param-values="versionParamValues"
                                            :handle-version-change="handleVersionChange"
                                            :handle-build-no-change="handleBuildNoChange"
                                            :version-param-list="versionParams"
                                        />
                                    </template>
                                </renderSortCategoryParams>
                            </template>
                        </pipeline-params-form>
                        <bk-exception
                            v-else
                            type="empty"
                            scene="part"
                        >
                            {{ $t('noParams') }}
                        </bk-exception>
                    </div>
                </section>
            </template>

            <template v-if="constantParams.length > 0">
                <section class="params-content-item">
                    <header
                        :class="['params-collapse-trigger', {
                            'params-collapse-expand': activeName.has(2)
                        }]"
                        @click="toggleCollapse(2)"
                    >
                        <bk-icon
                            type="right-shape"
                            class="icon-angle-right"
                        />
                        {{ $t('newui.const') }}
                    </header>
                    <div
                        v-if="activeName.has(2)"
                        class="params-collapse-content"
                    >
                        <pipeline-params-form
                            ref="constParamsForm"
                            disabled
                            :param-values="constantValues"
                            :params="constantParams"
                            sort-category
                        />
                    </div>
                </section>
            </template>

            <template v-if="hasOtherParams">
                <section class="params-content-item">
                    <header
                        :class="['params-collapse-trigger', {
                            'params-collapse-expand': activeName.has(3)
                        }]"
                        @click="toggleCollapse(3)"
                    >
                        <bk-icon
                            type="right-shape"
                            class="icon-angle-right"
                        />

                        {{ $t('newui.pipelineParam.otherVar') }}
                    </header>
                    <div
                        v-if="activeName.has(3)"
                        class="params-collapse-content"
                    >
                        <pipeline-params-form
                            ref="otherParamsForm"
                            disabled
                            :param-values="otherValues"
                            :params="otherParams"
                            sort-category
                        >
                            <template
                                slot="versionParams"
                                v-if="!isVisibleVersion && versionParamValues.length"
                            >
                                <pipeline-versions-form
                                    class="mb20"
                                    ref="versionParamForm"
                                    :build-no="buildNo"
                                    is-preview
                                    disabled
                                    :version-param-values="versionParamValues"
                                    :handle-version-change="handleVersionChange"
                                    :handle-build-no-change="handleBuildNoChange"
                                    :version-param-list="versionParams"
                                />
                            </template>
                        </pipeline-params-form>
                    </div>
                </section>
            </template>
        </div>
    </section>
</template>

<script setup>
    import { ref, computed, watch, onMounted } from 'vue'
    import PipelineVersionsForm from '@/components/PipelineVersionsForm.vue'
    import PipelineParamsForm from '@/components/pipelineParamsForm.vue'
    import renderSortCategoryParams from '@/components/renderSortCategoryParams'
    import UseInstance from '@/hook/useInstance'
    import { allVersionKeyList } from '@/utils/pipelineConst'
    import { getParamsValuesMap } from '@/utils/util'
    const activeName = ref(new Set([1]))
    const { proxy } = UseInstance()
    const isLoading = ref(true)
    const paramsList = ref([])
    const paramsValues = ref({})
    const otherParams = ref([])
    const otherValues = ref({})
    const constantParams = ref([])
    const constantValues = ref({})
    const versionParams = ref([])
    const versionParamValues = ref({})
    const buildNo = ref({})
    const instanceList = computed(() => proxy.$store?.state?.templates?.instanceList)
    const activePipelineId = computed(() => proxy.$route?.query?.pipelineId)
    const curInstance = computed(() => instanceList.value.find(i => i.pipelineId === activePipelineId.value) || {})
    const isVisibleVersion = computed(() => buildNo.value?.required ?? false)
    const hasOtherParams = computed(() => {
        if (isVisibleVersion.value) {
            return [...otherParams.value, ...versionParams.value].length
        }
        return otherParams.value.length
    })
    const hasPipelineParams = computed(() => {
        if (isVisibleVersion.value) {
            return [...paramsList.value, ...versionParams.value].length
        }
        return paramsList.value.length
    })
    watch(() => curInstance.value, (value) => {
        if (!value) return
        initData()
    })
    function toggleCollapse (id) {
        if (activeName.value.has(id)) {
            activeName.value.delete(id)
        } else {
            activeName.value.add(id)
        }
        activeName.value = new Set(activeName.value)
    }
    function getParamsValue () {
        const key = 'defaultValue'
        paramsValues.value = getParamsValuesMap(paramsList.value, key)
        versionParamValues.value = getParamsValuesMap(versionParams.value, key)
        constantValues.value = getParamsValuesMap(constantParams.value, key)
        otherValues.value = getParamsValuesMap(otherParams.value, key)
    }
    function initData () {
        isLoading.value = true
        paramsList.value = curInstance.value?.param?.filter(p => !p.constant && p.required && !allVersionKeyList.includes(p.id) && p.propertyType !== 'BUILD').map(p => ({
            ...p,
            label: `${p.id}${p.name ? `(${p.name})` : ''}`
        }))
        otherParams.value = curInstance.value?.param?.filter(p => !p.constant && !p.required && !allVersionKeyList.includes(p.id) && p.propertyType !== 'BUILD').map(p => ({
            ...p,
            label: `${p.id}${p.name ? `(${p.name})` : ''}`
        }))
        constantParams.value = curInstance.value?.param?.filter(p => p.constant).map(p => ({
            ...p,
            label: `${p.id}${p.name ? `(${p.name})` : ''}`
        }))
        versionParams.value = curInstance.value?.param?.filter(p => allVersionKeyList.includes(p.id)).map(p => ({
            ...p,
            isChanged: p.defaultValue !== p.value
        }))
        buildNo.value = curInstance.value?.buildNo || {}
        getParamsValue()
        setTimeout(() => {
            isLoading.value = false
        })
    }
    onMounted(() => {
        console.log(1)
    })
</script>

<style lang="scss">
    $header-height: 36px;

    .instance-config-constant {
        height: calc(100% - 100px);
        overflow: auto;
        padding: 20px;
        .config-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            .left {
                font-weight: 700;
                font-size: 14px;
                color: #313238;
            }
            .line {
                display: inline-block;
                margin: 0 10px;
                color: #DCDEE5;
            }
            .instance-name {
                color: #979BA5;
                font-weight: 400;
            }
        }
        .config-content {
            overflow: auto !important;
            margin-top: 20px;

            .params-content-item {
                background: #FFFFFF;
                box-shadow: 0 2px 4px 0 #1919290d;
                border-radius: 2px;
                margin-bottom: 20px;
            }

            .params-collapse-content {
                padding: 16px 24px;
            }

            @for $i from 1 through 6 {
                :nth-child(#{$i} of .params-collapse-trigger) {
                    top: $header-height * ($i - 1);
                }
            }

            .params-collapse-trigger {
                display: flex;
                flex-shrink: 0;
                align-items: center;
                font-size: 14px;
                font-weight: 700;
                height: $header-height;
                cursor: pointer;
                top: 0;
                margin: 0 24px;
                position: sticky;
                grid-gap: 10px;
                color: #313238;
                background-color: white;
                z-index: 6;

                &.params-collapse-expand {
                    .icon-angle-right {
                        transform: rotate(90deg);
                    }
                }

                .icon-angle-right {
                    transition: all 0.3 ease;
                    color: #4D4F56;
                }
            }
        }
    }
</style>
