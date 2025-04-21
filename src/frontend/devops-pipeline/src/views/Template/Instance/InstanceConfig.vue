<template>
    <section
        class="instance-config-constant"
        v-bkloading="{ isLoading }"
    >
        <header class="config-header">
            <div class="left">
                {{ $t('template.instanceConfig') }}
                <span class="line">|</span>
                <span class="instance-name">{{ curInstance?.pipelineName }}</span>
            </div>
            <div
                class="right"
                v-if="!isInstanceCreateType"
            >
                <ul class="params-compare-content">
                    <bk-checkbox
                        class="hide-params-btn"
                        v-if="compareParamsNum.deleted"
                        v-model="hideDeleted"
                    >
                        {{ $t('template.hideDeletedParam') }}
                    </bk-checkbox>
                    <li
                        v-for="item in renderCompareParamsNum"
                        :key="item.key"
                        class="num-item"
                    >
                        <span :class="['status-icon', item.class]"></span>
                        <span> {{ item.label }}</span>
                        <span :class="['status-value', item.class]">
                            {{ item.value }}
                        </span>
                    </li>
                </ul>
            </div>
        </header>
        <div
            class="config-content"
            :key="activeIndex"
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
                            show-operate-btn
                            :hide-deleted="hideDeleted"
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
                            :hide-deleted="hideDeleted"
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
                            :hide-deleted="hideDeleted"
                        >
                            <template
                                slot="versionParams"
                                v-if="!isVisibleVersion && versionParams.length"
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
    import { ref, computed, watch, onBeforeUnmount, defineProps } from 'vue'
    import PipelineVersionsForm from '@/components/PipelineVersionsForm.vue'
    import PipelineParamsForm from '@/components/pipelineParamsForm.vue'
    import renderSortCategoryParams from '@/components/renderSortCategoryParams'
    import UseInstance from '@/hook/useInstance'
    import { allVersionKeyList } from '@/utils/pipelineConst'
    import { getParamsValuesMap } from '@/utils/util'
    import {
        SET_TEMPLATE_DETAIL,
        SET_INSTANCE_LIST,
        UPDATE_INSTANCE_LIST,
        UPDATE_USE_TEMPLATE_SETTING
    } from '@/store/modules/templates/constants'
    const props = defineProps({
        isInstanceCreateType: Boolean
    })
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
    const hideDeleted = ref(false)
    const compareParamsNum = ref({})
    const instanceList = computed(() => proxy.$store?.state?.templates?.instanceList)
    const initialInstanceList = computed(() => proxy.$store?.state?.templates?.initialInstanceList)
    const activeIndex = computed(() => proxy.$route?.query?.index)
    // curTemplate 当前选中的某一个版本的模板数据
    // templateVersion 选中的模板版本
    const curTemplateDetail = computed(() => proxy.$store?.state?.templates?.templateDetail)
    const curTemplateVersion = computed(() => proxy.$store?.state?.templates?.templateVersion)
    const curInstance = computed(() => {
        const instance = instanceList.value.find((i, index) => index === activeIndex.value - 1)
        // 如果选择了模板版本，将模板版本数据与当前实例进行比对
        let instanceParams
        if (instance && instance?.param && curTemplateVersion.value && curTemplateDetail.value?.params && !props.isInstanceCreateType) {
            const res = compareParams(instance.param, curTemplateDetail.value.params)
            instanceParams = res.instanceParams
            compareParamsNum.value = res.result
        }
        if (instanceParams) {
            return {
                ...instance,
                param: instanceParams
            }
        }
        return instance
    })
    const renderCompareParamsNum = computed(() => {
        const itemMap = {
            changed: {
                label: proxy.$t('template.paramChanged'),
                class: 'changed'
            },
            added: {
                label: proxy.$t('template.paramAdded'),
                class: 'added'
            },
            deleted: {
                label: proxy.$t('template.paramDeleted'),
                class: 'deleted'
            }
        }
        
        return Object.keys(compareParamsNum.value).map(key => {
            return {
                value: compareParamsNum.value[key],
                key,
                ...itemMap[key]
            }
        })
    })
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
    watch(() => activeIndex.value, () => {
        isLoading.value = true
    })
    watch(() => curInstance.value, (value) => {
        if (!value) return
        initData()
    }, {
        deep: true
    })
    watch(() => curTemplateVersion.value, () => {
        // 切换版本，重置实例为初始状态
        isLoading.value = true
        proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, initialInstanceList.value)
    })
    function compareParams (instanceParams, templateParams) {
        const result = {
            changed: 0,
            added: 0,
            deleted: 0
        }
        const templateParamsMap = templateParams.reduce((acc, item) => {
            acc[item.id] = item // 按 id 进行映射
            return acc
        }, {})
        for (const item of instanceParams) {
            const templateParamItem = templateParamsMap[item.id]

            if (!templateParamItem) {
                // 在 instanceParams 中存在，但在 templateParams 中不存在，标记为isDelete
                item.isDelete = true
                result.deleted += 1
            } else {
                // 对比 defaultValue, 如果不同则标记为isChange
                item.isChange = item.defaultValue !== templateParamItem.defaultValue
                if (item.defaultValue !== templateParamItem.defaultValue) {
                    result.changed += 1
                }
            }
        }

        // 对比 templateParams，将新字段添加到 instanceParams，并标记为 isNew
        for (const item of templateParams) {
            const instanceParamItem = instanceParams.find(i => i.id === item.id)
            if (!instanceParamItem) {
                // 在 templateParams 中存在，但在 instanceParams 中不存在，标记为新增
                const newItem = { ...item, isNew: true }
                instanceParams.push(newItem) // 将新字段添加到 instanceParams
                result.added += 1
            }
        }
        
        return {
            instanceParams,
            result
        }
    }
    function handleParamChange (id, value) {
        curInstance.value.param.forEach(i => {
            if (i.id === id) i.defaultValue = value
        })
        proxy.$store.commit(`templates/${UPDATE_INSTANCE_LIST}`, {
            index: activeIndex.value - 1,
            value: curInstance.value
        })
    }
    function toggleCollapse (id) {
        if (activeName.value.has(id)) {
            activeName.value.delete(id)
        } else {
            activeName.value.add(id)
        }
        activeName.value = new Set(activeName.value)
    }
    function getParamsValue (key = 'defaultValue') {
        paramsValues.value = getParamsValuesMap(paramsList.value, key)
        versionParamValues.value = getParamsValuesMap(versionParams.value, key)
        constantValues.value = getParamsValuesMap(constantParams.value, key)
        otherValues.value = getParamsValuesMap(otherParams.value, key)
    }
    function initData () {
        const params = curInstance.value?.param
        if (!params) return
        paramsList.value = params.filter(p => !p.constant && p.required && !allVersionKeyList.includes(p.id) && p.propertyType !== 'BUILD').map(p => ({
            ...p,
            label: `${p.id}${p.name ? `(${p.name})` : ''}`
        }))
        otherParams.value = params.filter(p => !p.constant && !p.required && !allVersionKeyList.includes(p.id) && p.propertyType !== 'BUILD').map(p => ({
            ...p,
            label: `${p.id}${p.name ? `(${p.name})` : ''}`
        }))
        constantParams.value = params.filter(p => p.constant).map(p => ({
            ...p,
            label: `${p.id}${p.name ? `(${p.name})` : ''}`
        }))
        versionParams.value = params.filter(p => allVersionKeyList.includes(p.id)).map(p => ({
            ...p,
            isChanged: p.defaultValue !== p.value
        }))
        buildNo.value = curInstance.value?.buildNo || {}
        getParamsValue()
        setTimeout(() => {
            isLoading.value = false
        })
    }
    onBeforeUnmount(() => {
        proxy.$store.commit(`templates/${SET_TEMPLATE_DETAIL}`, {
            templateVersion: '',
            templateDetail: {}
        })
        proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, [])
        proxy.$store.commit(`templates/${UPDATE_USE_TEMPLATE_SETTING}`, false)
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
        .params-compare-content {
            display: flex;
            align-items: center;
            font-size: 12px;
            .hide-params-btn {
                margin-right: 20px;
            }
            .num-item {
                display: flex;
                align-items: center;
                margin-right: 12px;
            }
            .status-icon {
                display: inline-block;
                width: 12px;
                height: 12px;
                background: #FDF4E8;
                margin-right: 4px;
                &.changed {
                    border: 1px solid #F8B64F;
                }
                &.added {
                    border: 1px solid #2CAF5E;
                }
                &.deleted {
                    border: 1px solid #FF5656;
                }
            }
            .status-value {
                font-weight: 700;
                margin-left: 2px;
                &.changed {
                    color:#F8B64F;
                }
                &.added {
                    color:#2CAF5E;
                }
                &.deleted {
                    color:#FF5656;
                }
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
