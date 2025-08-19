<template>
    <section
        :class="{
            'instance-config-wrapper': true,
            'has-ref-tips': !templateRefTypeById
        }"
        v-bkloading="{ isLoading: isLoading }"
    >
        <bk-alert
            v-if="!templateRefTypeById"
            closable
            type="warning"
        >
            <div slot="title">
                <p>
                    {{ $t('template.notSpecifiedRef.tips1') }}
                    <span class="doc-btn">
                        {{ $t('template.notSpecifiedRef.arrangingValueStrategy') }}
                    </span>
                </p>
                <p>
                    {{ $t('template.notSpecifiedRef.tips2') }}
                </p>
            </div>
        </bk-alert>
        <section class="instance-config-constant">
            <header class="config-header">
                <div class="left">
                    {{ $t('template.instanceConfig') }}
                    <span class="line">|</span>
                    <span class="instance-name">{{ curInstance?.pipelineName }}</span>
                </div>
                <div
                    class="right"
                    v-if="!isInstanceCreateType && (!!curTemplateVersion || !!templateRef)"
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
                                :handle-use-default-value="handleUseDefaultValue"
                                :handle-set-parma-required="handleSetParmaRequired"
                                follow-template-key="param"
                                :handle-follow-template="handleFollowTemplate"
                            >
                                <template
                                    slot="versionParams"
                                    v-if="isVisibleVersion && !hideDeletedVersionParams"
                                >
                                    <renderSortCategoryParams
                                        :name="$t('preview.introVersion')"
                                        default-layout
                                        show-follow-template-btn
                                        show-set-required-btn
                                        v-bind="versionParams"
                                        follow-template-key="introVersion"
                                        :handle-follow-template="handleFollowTemplate"
                                        :handle-set-build-no-required="handleSetBuildNoRequired"
                                        :is-required-param="curInstance.buildNo.isRequiredParam"
                                        :is-follow-template="curInstance.buildNo.isFollowTemplate"
                                    >
                                        <template slot="content">
                                            <pipeline-versions-form
                                                class="mb20"
                                                ref="versionParamForm"
                                                :build-no="buildNo"
                                                is-instance
                                                :is-reset-build-no="isResetBuildNo"
                                                :version-param-values="versionParamValues"
                                                :handle-version-change="handleParamChange"
                                                :handle-build-no-change="handleBuildNoChange"
                                                :handle-check-change="handleCheckChange"
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
                                    <renderSortCategoryParams
                                        :name="$t('preview.introVersion')"
                                        default-layout
                                        key="introVersion"
                                        v-bind="versionParams"
                                    >
                                        <template slot="content">
                                            <pipeline-versions-form
                                                class="mb20"
                                                ref="versionParamForm"
                                                :build-no="buildNo"
                                                is-instance
                                                is-init-instance
                                                disabled
                                                :version-param-values="versionParamValues"
                                                :handle-version-change="handleParamChange"
                                                :handle-build-no-change="handleBuildNoChange"
                                                :version-param-list="versionParams"
                                            />
                                        </template>
                                    </renderSortCategoryParams>
                                </template>
                            </pipeline-params-form>
                        </div>
                    </section>
                </template>

                <template>
                    <section class="params-content-item">
                        <header
                            :class="['params-collapse-trigger', {
                                'params-collapse-expand': activeName.has(4)
                            }]"
                            @click="toggleCollapse(4)"
                        >
                            <bk-icon
                                type="right-shape"
                                class="icon-angle-right"
                            />
    
                            {{ $t('template.triggers') }}
                        </header>
                        <div
                            v-if="activeName.has(4)"
                            class="params-collapse-content"
                        >
                            <renderSortCategoryParams
                                v-for="(trigger, index) in triggerConfigs"
                                :key="index"
                                :name="trigger.stepId ? `${trigger.name}(${trigger.stepId})` : `${trigger.name}`"
                                default-layout
                                show-follow-template-btn
                                follow-template-key="trigger"
                                check-step-id
                                v-bind="trigger"
                                :handle-follow-template="(key) => handleFollowTemplate(key, trigger.stepId)"
                            >
                                <template slot="content">
                                    <render-trigger
                                        :trigger="trigger"
                                        :index="index"
                                        :handle-change-trigger="handleChangeTrigger"
                                    />
                                </template>
                            </renderSortCategoryParams>
                        </div>
                    </section>
                </template>
            </div>
        </section>
        <footer
            v-if="!isLoading"
            class="config-footer"
        >
            <bk-button
                @click="handleResetInstance"
            >
                {{ $t('template.reset') }}
            </bk-button>
        </footer>
    </section>
</template>

<script setup>
    import { ref, computed, watch, defineProps } from 'vue'
    import PipelineVersionsForm from '@/components/PipelineVersionsForm.vue'
    import PipelineParamsForm from '@/components/pipelineParamsForm.vue'
    import renderSortCategoryParams from '@/components/renderSortCategoryParams'
    import RenderTrigger from '@/components/Template/RenderTrigger.vue'
    import UseInstance from '@/hook/useInstance'
    import { allVersionKeyList } from '@/utils/pipelineConst'
    import { getParamsValuesMap } from '@/utils/util'
    import {
        SET_INSTANCE_LIST,
        UPDATE_INSTANCE_LIST
    } from '@/store/modules/templates/constants'
    const props = defineProps({
        isInstanceCreateType: Boolean
    })
    const activeName = ref(new Set([1]))
    const { proxy } = UseInstance()
    const isLoading = ref(true)
    const paramsList = ref([])
    const paramsValues = ref({})
    const triggerConfigs = ref([])
    const otherParams = ref([])
    const otherValues = ref({})
    const constantParams = ref([])
    const constantValues = ref({})
    const versionParams = ref([])
    const versionParamValues = ref({})
    const buildNo = ref({})
    const hideDeleted = ref(false)
    const isResetBuildNo = ref(false)
    const templateRef = computed(() => proxy.$store?.state?.templates?.templateRef)
    const templateRefType = computed(() => proxy.$store?.state?.templates?.templateRefType)
    const templateRefTypeById = computed(() => templateRefType.value === 'ID')
    const instanceList = computed(() => proxy.$store?.state?.templates?.instanceList)
    const initialInstanceList = computed(() => proxy.$store?.state?.templates?.initialInstanceList)
    const activeIndex = computed(() => proxy.$route?.query?.index)
    // curTemplate 当前选中的某一个版本的模板数据
    // templateVersion 选中的模板版本号
    const curTemplateDetail = computed(() => proxy.$store?.state?.templates?.templateDetail)
    const curTemplateVersion = computed(() => proxy.$store?.state?.templates?.templateVersion)
    const curInstance = computed(() => {
        const instance = instanceList.value.find((i, index) => index === activeIndex.value - 1)
        if (instance?.param) {
            instance.param = instance.param.map(p => {
                return {
                    ...p,
                    readOnlyCheck: false // 取消只读参数的禁用逻辑
                }
            })
        }
        // 更新实例：如果选择了模板版本，将模板版本数据与当前实例进行比对
        let instanceParams, instanceBuildNo, instanceTriggerConfigs
        if (!props.isInstanceCreateType && instance && (curTemplateVersion.value || templateRef.value)) {
            if (curTemplateDetail.value?.params) {
                instanceParams = compareParams(instance, curTemplateDetail.value)
            }
            if (curTemplateDetail.value?.buildNo) {
                instanceBuildNo = compareBuild(instance?.buildNo, curTemplateDetail.value.buildNo)
            }
            if (templateTriggerConfigs.value.length) {
                const triggerConfigs = instance.triggerElements?.map(i => ({
                    atomCode: i.atomCode,
                    stepId: i.stepId ?? '',
                    disabled: i.additionalOptions?.enable ?? true,
                    cron: i.advanceExpression,
                    variables: i.startParams,
                    name: i.name,
                    version: i.version,
                    isFollowTemplate: !(instance?.overrideTemplateField?.triggerStepIds?.includes(i.stepId))
                }))
                instanceTriggerConfigs = compareTriggerConfigs(triggerConfigs, templateTriggerConfigs.value)
            }
        }
        if (instanceParams || instanceBuildNo || instanceTriggerConfigs) {
            return {
                ...instance,
                param: instanceParams ?? instance.param,
                buildNo: instanceBuildNo ?? instance.buildNo,
                triggerConfigs: instanceTriggerConfigs ?? instance.triggerConfigs
            }
        }
        return instance
    })
    const compareParamsNum = computed(() => {
        const counts = {
            changed: 0,
            added: 0,
            deleted: 0
        }
        // 流水线入参 新增/删除/变更统计
        curInstance?.value?.param?.forEach(item => {
            if (item.isChange) {
                counts.changed++
            }
            if (item.isNew) {
                counts.added++
            }
            if (item.isDelete) {
                counts.deleted++
            }
        })

        // 触发器新增/删除统计
        // curInstance?.value?.triggerConfigs?.forEach(item => {
        //     if (item.isNew) {
        //         counts.added++
        //     }
        //     if (item.isDelete) {
        //         counts.deleted++
        //     }
        // })

        return counts
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
    const hideDeletedVersionParams = computed(() => {
        return !hideDeleted.value && versionParams.value.every(i => i.isDelete)
    })
    const isVisibleVersion = computed(() => {
        return buildNo.value?.required
    })
    const hasOtherParams = computed(() => {
        if (!isVisibleVersion.value) {
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
    const templateTriggerConfigs = computed(() => {
        const instance = instanceList.value.find((i, index) => index === activeIndex.value - 1)
        return curTemplateDetail.value?.resource?.model?.stages[0]?.containers[0]?.elements?.map(i => ({
            atomCode: i.atomCode,
            stepId: i.stepId ?? '',
            disabled: i.additionalOptions?.enable ?? true,
            cron: i.advanceExpression,
            variables: i.startParams,
            name: i.name,
            version: i.version,
            isFollowTemplate: !(instance?.overrideTemplateField?.triggerStepIds?.includes(i.stepId))
        }))
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
    watch(() => [curTemplateVersion.value, templateRef.value], () => {
        // 切换版本，重置实例为初始状态
        isLoading.value = true
        if (props.isInstanceCreateType) {
            proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, instanceList.value.map((instance) => {
                return {
                    ...instance,
                    param: curTemplateDetail.value.params,
                    buildNo: curTemplateDetail.value.buildNo
                }
            }))
        } else {
            proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, initialInstanceList.value)
        }
    })
    function compareParams (instance, template) {
        const instanceParams = instance?.param
        const templateParams = template?.params
        const instanceBuildNo = instance?.buildNo
        const templateBuildNo = template?.buildNo

        // 非入参的参数直接赋值模板配置的入参默认值
        if (instanceBuildNo?.required && !templateBuildNo?.required) {
            instanceParams?.forEach(i => {
                if (allVersionKeyList.includes(i.id)) {
                    const newValue = templateParams.find(t => t.id === i.id)?.defaultValue
                    i.defaultValue = newValue ?? i.defaultValue
                }
            })
        }

        instanceParams?.forEach(i => {
            // 常量 其他变量直接赋值为模板对应参数的值（版本号除外）
            if (i.constant || (!i.required && !allVersionKeyList.includes(i.id))) {
                const newValue = templateParams.find(t => t.id === i.id)?.defaultValue
                i.defaultValue = newValue ?? i.defaultValue
            }
        })

        const templateParamsMap = templateParams.reduce((acc, item) => {
            acc[item.id] = item
            return acc
        }, {})
        for (const item of instanceParams) {
            const templateParamItem = templateParamsMap[item.id]

            if (!templateParamItem) {
                // 在 instanceParams 中存在，但在 templateParams 中不存在，标记为isDelete
                item.isDelete = true
            } else {
                // 对比 defaultValue, 如果不同则标记为isChange
                item.isChange = item.defaultValue !== templateParamItem.defaultValue
            }
        }

        // 对比 templateParams，将新字段添加到 instanceParams，并标记为 isNew
        for (const item of templateParams) {
            const instanceParamItem = instanceParams.find(i => i.id === item.id)
            if (!instanceParamItem) {
                // 在 templateParams 中存在，但在 instanceParams 中不存在，标记为新增
                const newItem = { ...item, isNew: true }
                instanceParams.push(newItem) // 将新字段添加到 instanceParams
            }
        }
        
        return instanceParams
    }

    function compareTriggerConfigs (instanceTriggerConfigs, templateTriggerConfigs) {
        const instanceTriggerMap = new Map(instanceTriggerConfigs.map(item => [item.stepId, item]))
        const result = templateTriggerConfigs.map(item => {
            if (!instanceTriggerMap.has(item.stepId)) {
                return { ...item, isNew: true }
            }
            const instanceTrigger = instanceTriggerMap.get(item.stepId)
            return { ...item, ...instanceTrigger }
        })

        const templateTriggerMap = new Map(templateTriggerConfigs.map(item => [item.stepId, item]))

        instanceTriggerConfigs.forEach(item => {
            if (!templateTriggerMap.has(item.stepId)) {
                result.push({ ...item, isDelete: true })
            }
        })
        return result
    }
    function compareBuild (instanceBuildNo, templateBuildNo) {
        if (!instanceBuildNo && !!templateBuildNo) {
            // 将模板的推荐版本号配置覆盖实例推荐版本号
            return {
                ...instanceBuildNo,
                ...templateBuildNo
            }
        }
        if (instanceBuildNo && templateBuildNo) {
            return {
                ...instanceBuildNo,
                required: templateBuildNo.required
            }
        }
        return instanceBuildNo
    }
    
    function handleSetParmaRequired (id) {
        proxy.$store.commit(`templates/${UPDATE_INSTANCE_LIST}`, {
            index: activeIndex.value - 1,
            value: {
                ...curInstance.value,
                param: curInstance.value?.param.map(p => ({
                    ...p,
                    isRequiredParam: p.id === id ? !p.isRequiredParam : p.isRequiredParam
                }))
            }
        })
    }
    
    function handleUseDefaultValue (id) {
        const defaultValue = curTemplateDetail.value?.params?.find(i => i.id === id)?.defaultValue
        proxy.$store.commit(`templates/${UPDATE_INSTANCE_LIST}`, {
            index: activeIndex.value - 1,
            value: {
                ...curInstance.value,
                param: curInstance.value?.param.map(p => ({
                    ...p,
                    defaultValue: p.id === id ? defaultValue : p.defaultValue
                }))
            }
        })
    }
    function handleParamChange (id, value) {
        proxy.$store.commit(`templates/${UPDATE_INSTANCE_LIST}`, {
            index: activeIndex.value - 1,
            value: {
                ...curInstance.value,
                param: curInstance.value?.param.map(p => ({
                    ...p,
                    defaultValue: p.id === id ? value : p.defaultValue
                }))
            }
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

    function handleResetInstance () {
        const instanceIndex = activeIndex.value - 1
        curInstance.value = initialInstanceList.value[instanceIndex]
        proxy.$store.commit(`templates/${UPDATE_INSTANCE_LIST}`, {
            index: instanceIndex,
            value: initialInstanceList.value[instanceIndex]
        })
    }

    function handleBuildNoChange (name, value) {
        proxy.$store.commit(`templates/${UPDATE_INSTANCE_LIST}`, {
            index: activeIndex.value - 1,
            value: {
                ...curInstance.value,
                buildNo: {
                    ...curInstance.value?.buildNo,
                    [name]: value
                }
            }
        })
    }
    function handleCheckChange (value) {
        isResetBuildNo.value = value
        if (!value) return
        proxy.$store.commit(`templates/${UPDATE_INSTANCE_LIST}`, {
            index: activeIndex.value - 1,
            value: {
                ...curInstance.value,
                buildNo: {
                    ...curInstance.value?.buildNo,
                    currentBuildNo: curInstance.value.buildNo?.buildNo
                }
            }
        })
    }
    function handleSetBuildNoRequired () {
        proxy.$store.commit(`templates/${UPDATE_INSTANCE_LIST}`, {
            index: activeIndex.value - 1,
            value: {
                ...curInstance.value,
                buildNo: {
                    ...curInstance.value?.buildNo,
                    isRequiredParam: !curInstance.value.buildNo.isRequiredParam
                }
            }
        })
    }
    function handleChangeTrigger (name, index, value) {
        proxy.$store.commit(`templates/${UPDATE_INSTANCE_LIST}`, {
            index: activeIndex.value - 1,
            value: {
                ...curInstance.value,
                triggerConfigs: curInstance.value?.triggerConfigs.map((trigger, idx) => {
                    if (idx === index) {
                        return {
                            ...trigger,
                            [name]: value
                        }
                    }
                    return trigger
                })
            }
        })
    }
    /**
     *
     * @param key 区分 推荐版本号/流水线参数/触发器
     * @param id 流水线入参 id |  触发器 stepId
     */
    function handleFollowTemplate (key, id) {
        const paramIds = [...curInstance.value.overrideTemplateField?.paramIds ?? []]
        let target = id, index
        switch (key) {
            case 'introVersion':
                target = 'BK_CI_BUILD_NO'
                index = paramIds.indexOf(target)
                index > -1 ? paramIds.splice(index, 1) : paramIds.push(target)
                proxy.$store.commit(`templates/${UPDATE_INSTANCE_LIST}`, {
                    index: activeIndex.value - 1,
                    value: {
                        ...curInstance.value,
                        overrideTemplateField: {
                            ...curInstance.value.overrideTemplateField,
                            paramIds
                        },
                        buildNo: {
                            ...curInstance.value?.buildNo,
                            isFollowTemplate: !curInstance.value.buildNo.isFollowTemplate
                        }
                    }
                })
                break

            case 'trigger':
                // const temTriggerValue = templateTriggerConfigs.value?.find(trigger => trigger.stepId === id).disabled
                // console.log(temTriggerValue, 'templateTriggerConfigs.value')
                const triggerStepIds = [...(curInstance.value.overrideTemplateField?.triggerStepIds ?? [])]
                index = triggerStepIds.indexOf(target)
                index > -1 ? triggerStepIds.splice(index, 1) : triggerStepIds.push(target)
                proxy.$store.commit(`templates/${UPDATE_INSTANCE_LIST}`, {
                    index: activeIndex.value - 1,
                    value: {
                        ...curInstance.value,
                        overrideTemplateField: {
                            ...curInstance.value.overrideTemplateField,
                            triggerStepIds
                        },

                        triggerConfigs: curInstance.value?.triggerConfigs.map(trigger => {
                            return {
                                ...trigger,
                                // disabled: trigger.stepId === id ? temTriggerValue : trigger.disabled,
                                isFollowTemplate: trigger.stepId === id ? !trigger.isFollowTemplate: trigger.isFollowTemplate
                            }
                        })
                    }
                })
                break
            case 'param':
                const temDefaultValue =  curTemplateDetail.value.params?.find(t => t.id === id)?.defaultValue
                index = paramIds.indexOf(target)
                index > -1 ? paramIds.splice(index, 1) : paramIds.push(target)
                proxy.$store.commit(`templates/${UPDATE_INSTANCE_LIST}`, {
                    index: activeIndex.value - 1,
                    value: {
                        ...curInstance.value,
                        overrideTemplateField: {
                            ...curInstance.value.overrideTemplateField,
                            paramIds
                        },
                        param: curInstance.value?.param.map(p => ({
                            ...p,
                            defaultValue: p.id === id && !p.isFollowTemplate ? temDefaultValue : p.defaultValue,
                            isFollowTemplate: p.id === id ? !p.isFollowTemplate : p.isFollowTemplate
                        }))
                    }
                })
                break
            default:
                break
        }
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
        buildNo.value = {
            ...curInstance.value?.buildNo,
            isRequiredParam: curInstance.value?.buildNo?.required ?? false
        } || {}
        triggerConfigs.value = curInstance.value?.triggerConfigs || []
        getParamsValue()
        setTimeout(() => {
            isLoading.value = false
        })
    }
</script>

<style lang="scss">
    $header-height: 36px;
    .instance-config-wrapper {
        height: calc(100% - 148px);
        &.has-ref-tips {
            height: calc(100% - 188px);
        }
        .doc-btn {
            color: #3a84ff;
            cursor: pointer;
        }
    }
    .instance-config-constant {
        height: 100%;
        overflow: auto;
        padding: 20px;
        margin-bottom: 48px;
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
    .config-footer {
        position: fixed;
        bottom: 0;
        width: 100%;
        height: 48px;
        line-height: 48px;
        padding: 0 20px;
        background: #FFFFFF;
        border: 1px solid #DCDEE5;
        z-index: 100;
    }
</style>
