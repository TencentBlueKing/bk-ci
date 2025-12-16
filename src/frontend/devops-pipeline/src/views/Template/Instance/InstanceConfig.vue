<template>
    <section
        :class="{
            'instance-config-wrapper': true,
            'has-ref-tips': !templateRefTypeById,
            'is-create': isInstanceCreateType
        }"
        v-bkloading="{ isLoading: isLoading || instancePageLoading }"
    >
        <bk-alert
            v-if="!templateRefTypeById"
            closable
            type="warning"
        >
            <div slot="title">
                <p>
                    {{ $t('template.notSpecifiedRef.tips1') }}
                    <bk-popover
                        placement="top"
                        width="620"
                    >
                        <span class="doc-btn">
                            {{ $t('template.notSpecifiedRef.arrangingValueStrategy') }}
                        </span>
                        <div slot="content">
                            <p>{{ $t('template.arrangingValueStrategyTips.tips1') }}</p>
                            <p>{{ $t('template.arrangingValueStrategyTips.tips2') }}</p>
                            <p>{{ $t('template.arrangingValueStrategyTips.tips3') }}</p>
                            <p style="padding-left: 10px;">{{ $t('template.arrangingValueStrategyTips.tips4') }}</p>
                            <p style="padding-left: 10px;">{{ $t('template.arrangingValueStrategyTips.tips5') }}</p>
                        </div>
                    </bk-popover>
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
                    <span
                        class="instance-name"
                        v-bk-overflow-tips
                    >{{ curInstance?.pipelineName }}</span>
                </div>
                <div
                    class="right"
                    v-if="showCompareParamsNum"
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
                                :is-exec-preview="false"
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
                                        config-type="introVersion"
                                        follow-template-key="introVersion"
                                        :handle-follow-template="handleFollowTemplate"
                                        :handle-set-required="handleSetBuildNoRequired"
                                        :is-required-param="curInstance.buildNo?.isRequiredParam"
                                        :is-follow-template="curInstance.buildNo?.isFollowTemplate"
                                    >
                                        <template slot="content">
                                            <pipeline-versions-form
                                                class="mb20"
                                                ref="versionParamForm"
                                                :build-no="buildNo"
                                                is-instance
                                                :is-init-instance="isInstanceCreateType"
                                                :is-follow-template="curInstance.buildNo?.isFollowTemplate"
                                                :reset-build-no="curInstance.resetBuildNo"
                                                :build-no-changed="curInstance.buildNoChanged"
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
                                                :is-init-instance="isInstanceCreateType"
                                                disabled
                                                :build-no-changed="curInstance.buildNoChanged"
                                                :version-param-values="versionParamValues"
                                                :handle-version-change="handleParamChange"
                                                :handle-build-no-change="handleBuildNoChange"
                                                :version-param-list="versionParams"
                                                :reset-build-no="curInstance.resetBuildNo"
                                                :handle-check-change="handleCheckChange"
                                            />
                                        </template>
                                    </renderSortCategoryParams>
                                </template>
                            </pipeline-params-form>
                        </div>
                    </section>
                </template>

                <template v-if="curInstance?.triggerConfigs?.length">
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
                            <span class="trigger-tips">
                                {{ $t('template.triggersUpdateTips') }}
                            </span>
                        </header>
                        <div
                            v-if="activeName.has(4)"
                            class="params-collapse-content"
                        >
                            <renderSortCategoryParams
                                v-for="(trigger, index) in curInstance?.triggerConfigs"
                                :key="index"
                                :name="trigger.stepId ? `${trigger.name}(${trigger.stepId})` : `${trigger.name}`"
                                default-layout
                                show-follow-template-btn
                                follow-template-key="trigger"
                                check-step-id
                                v-bind="trigger"
                                config-type="trigger"
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
            v-if="!isLoading && !instancePageLoading && !isInstanceCreateType"
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
    import PipelineParamsForm from '@/components/pipelineParamsForm.vue'
    import PipelineVersionsForm from '@/components/PipelineVersionsForm.vue'
    import renderSortCategoryParams from '@/components/renderSortCategoryParams'
    import RenderTrigger from '@/components/Template/RenderTrigger.vue'
    import UseInstance from '@/hook/useInstance'
    import {
        SET_INSTANCE_LIST,
        UPDATE_INSTANCE_LIST
    } from '@/store/modules/templates/constants'
    import { allVersionKeyList } from '@/utils/pipelineConst'
    import { getParamsValuesMap } from '@/utils/util'
    import { computed, defineProps, ref, watch } from 'vue'
    import { isObject, isShallowEqual } from '@/utils/util'
    const props = defineProps({
        isInstanceCreateType: Boolean
    })
    const activeName = ref(new Set([1]))
    const { proxy } = UseInstance()
    const isLoading = ref(false)
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
    const instancePageLoading = computed(() => proxy.$store?.state?.templates?.instancePageLoading)
    const templateRef = computed(() => proxy.$store?.state?.templates?.templateRef)
    const templateRefType = computed(() => proxy.$store?.state?.templates?.templateRefType)
    const templateRefTypeById = computed(() => templateRefType.value === 'ID')
    const instanceList = computed(() => proxy.$store?.state?.templates?.instanceList)
    const initialInstanceList = computed(() => proxy.$store?.state?.templates?.initialInstanceList)
    const activeIndex = computed(() => proxy.$route?.query?.index)
    // curTemplateDetail 当前选中的某一个版本的模板数据
    // curTemplateVersion 选中的模板版本号
    const curTemplateDetail = computed(() => proxy.$store?.state?.templates?.templateDetail)
    const curTemplateVersion = computed(() => proxy.$store?.state?.templates?.templateVersion)
    const showCompareParamsNum = computed(() => {
        if (props.isInstanceCreateType) return false
        if (templateRefTypeById.value) {
            return Object.keys(curTemplateDetail.value)?.length
        }
        return Object.keys(curTemplateDetail.value)?.length && !!templateRef.value
    })
        
    watch(() => curTemplateDetail.value, (val) => {
        // 对所有实例进行数据合并并保存到 store
        if (initialInstanceList.value?.length && (curTemplateVersion.value || templateRef.value)) {
            try {
                isLoading.value = true
                const mergedInstances = initialInstanceList.value.map((instance, index) => {
                    // 设置 readOnlyCheck 为 false (readOnly只读属性在执行时才是禁用，在更新实例新建实例可以进行修改)
                    const processedInstance = {
                        ...instance,
                        param: instance.param?.map(p => ({
                           ...p,
                           readOnlyCheck: false
                       }))
                    }
                    // 如果没有模板详情，直接返回处理后的实例
                    if (!curTemplateDetail.value || Object.keys(curTemplateDetail.value).length === 0) {
                        return processedInstance
                    }
                   
                    // 进行数据比对和合并
                    let instanceParams, instanceBuildNoParams, instanceBuildNo, instanceTriggerConfigs
                   
                    if (curTemplateDetail.value?.param) {
                        instanceParams = compareParams(processedInstance, curTemplateDetail.value, index)
                    }
                    if (curTemplateDetail.value?.buildNo) {
                        const { buildNo, buildNoParam } = compareBuild(processedInstance, curTemplateDetail.value)
                        instanceBuildNo = buildNo
                        instanceBuildNoParams = buildNoParam
                    }
                    if (curTemplateDetail.value?.triggerConfigs?.length) {
                        instanceTriggerConfigs = compareTriggerConfigs(processedInstance?.triggerConfigs, curTemplateDetail.value.triggerConfigs)
                    }
                   
                    // 返回合并后的实例
                    if (instanceParams || instanceBuildNo || instanceTriggerConfigs) {
                        // 获取初始实例的参数
                        const initialInstance = initialInstanceList.value?.[index]
                        
                        // 判断 resetBuildNo：
                        // 1. buildNo.currentBuildNo 发生变化
                        // 2. 或者版本号变量（allVersionKeyList）发生变化
                        const buildNoChanged = processedInstance.buildNo?.buildNo !== instanceBuildNo?.buildNo
                        const versionParamsChanged = checkVersionParamsChanged(
                            instanceParams ?? processedInstance.param,
                            initialInstance?.param
                        )
                        
                        return {
                            ...processedInstance,
                            param: [
                                ...(instanceParams ?? processedInstance.param ?? []),
                                ...(instanceBuildNoParams ?? [])
                            ],
                            buildNo: {
                                ...instanceBuildNo,
                                currentBuildNo: buildNoChanged ? instanceBuildNo?.buildNo : processedInstance.buildNo?.currentBuildNo
                            } ?? processedInstance.buildNo,
                            triggerConfigs: instanceTriggerConfigs ?? processedInstance.triggerConfigs,
                            resetBuildNo: buildNoChanged || versionParamsChanged,
                            buildNoChanged
                        }
                    }
                    return processedInstance
                })
                // 保存到 store
                console.log(mergedInstances, 'mergedInstances')
                proxy.$store.commit(`templates/${SET_INSTANCE_LIST}`, {
                    list: mergedInstances,
                    init: false
                })
            } catch (e) {
                throw e
            } finally {
                isLoading.value = false
            }
        }
    })
    const curInstance = computed(() => instanceList.value.find((i, index) => index === activeIndex.value - 1))

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
        
        if (curInstance.value.buildNoChanged) {
            counts.changed++
        }
        if (curInstance.value.resetBuildNo) {
            counts.changed++
        }
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
    watch(() => activeIndex.value, () => {
        isLoading.value = true
    })
    watch(() => curInstance.value, (value) => {
        if (!value) return
        initData()
    }, {
        deep: true
    })
    
    // 监听 curInstance 变化，当有新增的 param、buildNo、triggerConfigs 时，同步store
    const isUpdatingStore = ref(false)
    watch(() => curInstance.value, (newInstance) => {
        if (!newInstance || isUpdatingStore.value) return
        
        const storeInstance = instanceList.value[activeIndex.value - 1]
        if (!storeInstance) return
        
        // 检查是否有新增的参数
        const hasNewParams = newInstance?.param?.some(p => p.isNew) || false
        
        // 检查是否有新增的触发器
        const hasNewTriggers = newInstance?.triggerConfigs?.some(trigger => trigger.isNew) || false
        
        // 检查 buildNo 是否有变化
        const buildNoChanged = !isShallowEqual(newInstance?.buildNo, storeInstance?.buildNo)
        
        if (!hasNewParams && !hasNewTriggers && !buildNoChanged) return
        
        // 收集需要添加到 overrideTemplateField.paramIds 的新参数 id（不跟随模板的参数）
        const newOverrideParamIds = newInstance.param
            ?.filter(p => !p.isFollowTemplate)
            .map(p => p.id) || []
        isUpdatingStore.value = true
        proxy.$nextTick(() => {
            proxy.$store.commit(`templates/${UPDATE_INSTANCE_LIST}`, {
                index: activeIndex.value - 1,
                value: {
                    ...storeInstance,
                    param: newInstance.param,
                    buildNo: newInstance.buildNo,
                    triggerConfigs: newInstance.triggerConfigs,
                    overrideTemplateField: {
                        ...storeInstance?.overrideTemplateField,
                        paramIds: [...newOverrideParamIds]
                    }
                }
            })
            setTimeout(() => {
                isUpdatingStore.value = false
            }, 0)
        })
    }, {
        deep: true,
        immediate: true
    })

    function compareParams (instance, template, instanceIndex) {
        const instanceParams = (instance?.param ?? []).map(p => ({ ...p })).filter(i => !allVersionKeyList.includes(i.id))
        const templateParams = (template?.param ?? []).filter(i => !allVersionKeyList.includes(i.id))
        
        const templateParamsMap = new Map(templateParams.map(t => [t.id, t]))
        const initialInstanceParams = initialInstanceList.value?.[instanceIndex]?.param?.reduce((acc, item) => {
            acc[item.id] = item
            return acc
        }, {})
        
        // 对比 templateParams，将新字段添加到 instanceParams，并标记为 isNew
        for (const item of templateParams) {
            const instanceParamItem = instanceParams.find(i => i.id === item.id)
            if (!instanceParamItem) {
                // 在 templateParams 中存在，但在 instanceParams 中不存在，标记为新增
                // 根据 asInstanceInput 决定新参数的 isRequiredParam 和 required 属性
                const newItem = {
                    ...item,
                    isNew: true,
                    isRequiredParam: item.required && item.asInstanceInput,
                    required: item.required
                }
                instanceParams.push(newItem) // 将新字段添加到 instanceParams
            } else {
                // 对于已存在的参数，保留 store 中的 isRequiredParam 值
                // 只在 isRequiredParam 未定义时，才使用 required 的值进行初始化
                if (instanceParamItem.isRequiredParam === undefined) {
                    instanceParamItem.isRequiredParam = instanceParamItem.required
                }
                // 更新 required 为模板的 required（模板定义的是否为入参）
                instanceParamItem.required = item.required
            }
        }
        instanceParams?.forEach(i => {
            // 常量 其他变量直接赋值为模板对应参数的值（版本号除外）
            if (i.constant || (!i.required && !allVersionKeyList.includes(i.id))) {
                const newValue = templateParamsMap.get(i.id)?.defaultValue
                i.defaultValue = newValue ?? i.defaultValue
                i.isChange = templateParamsMap.get(i.id) && newValue !== i.defaultValue
            }
        })
        for (const item of instanceParams) {
            const templateParamItem = templateParamsMap.get(item.id)
            const initialInstanceParamItem = initialInstanceParams[item.id]
           
            if (!templateParamItem) {
                // 在 instanceParams 中存在，但在 templateParams 中不存在，标记为isDelete
                item.isDelete = true
            } else {
                // 对比 defaultValue, 如果不相同则标记为isChange
                // 如果入参为跟随模板，则默认值替换为模板对应变量的默认值
                if (!allVersionKeyList.includes(item.id) && item.isFollowTemplate && item.defaultValue !== templateParamItem.defaultValue) {
                    item.defaultValue = templateParamItem.defaultValue
                    item.isChange = true
                } else {
                    const templateDefaultValue = allVersionKeyList.includes(item.id) ? Number(templateParamItem.defaultValue) : templateParamItem.defaultValue
                    const itemDefaultValue = allVersionKeyList.includes(item.id) ? Number(item.defaultValue) : item.defaultValue
                    item.hasChange = isObject(itemDefaultValue)
                        ? !isShallowEqual(itemDefaultValue, templateDefaultValue)
                        : itemDefaultValue !== templateDefaultValue
                    
                    if (!item.isNew) {
                        const initialInstanceDefaultValue = allVersionKeyList.includes(item.id) ? Number(initialInstanceParamItem?.defaultValue) : initialInstanceParamItem?.defaultValue
                        item.isChange =  isObject(itemDefaultValue)
                            ? !isShallowEqual(itemDefaultValue, initialInstanceDefaultValue)
                            : itemDefaultValue !== initialInstanceDefaultValue
                    }
                }
            }
        }
        
        return instanceParams
    }

    function compareTriggerConfigs (instanceTriggerConfigs, templateTriggerConfigs) {
        function createTriggerMap (configs) {
            return new Map(configs?.filter(i => !!i.stepId)?.map(item => [item.stepId, item]))
        }

        const instanceTriggerMap = createTriggerMap(instanceTriggerConfigs)
        const templateTriggerMap = createTriggerMap(templateTriggerConfigs)

        const result = templateTriggerConfigs?.filter(i => !!i.stepId)?.reduce((acc, item) => {
            if (!instanceTriggerMap.has(item.stepId)) {
                acc.push({ ...item, isNew: true })
            } else {
                const instanceTrigger = instanceTriggerMap.get(item.stepId)
                acc.push(instanceTrigger.isFollowTemplate ? { ...instanceTrigger, ...item } : instanceTrigger)
            }
            return acc
        }, [])

        instanceTriggerConfigs?.filter(i => !!i.stepId).forEach(item => {
            if (!templateTriggerMap.has(item.stepId)) {
                result.push({ ...item, isDelete: true })
            }
        })

        return result
    }
    function compareBuild (instance, templateDate) {
        // 模板推荐版本号为非入参时，实例始终使用模板配置
        const instanceBuildNo = instance.buildNo
        const templateBuildNo = templateDate.buildNo
        const instanceBuildNoParams = (instance?.param ?? []).map(p => ({ ...p })).filter(i => allVersionKeyList.includes(i.id))
        const templateBuildNoParams = (templateDate?.param ?? []).filter(i => allVersionKeyList.includes(i.id))
        const instanceBuildNoParamsMap = new Map(instanceBuildNoParams.map(p => [p.id, p]))
        
        if (!templateBuildNo.required) {
            // 对比模板版本号参数和实例版本号参数，如果 defaultVal 不同则加上 isChange
            const comparedBuildNoParams = templateBuildNoParams.map(templateParam => {
                const instanceParam = instanceBuildNoParamsMap.get(templateParam.id)
                const isChange = instanceParam
                    ? String(templateParam.defaultValue) !== String(instanceParam.defaultValue)
                    : false
                
                return {
                    ...templateParam,
                    ...(isChange && { isChange: true })
                }
            })
            
            return {
                buildNo: {
                    ...templateBuildNo,
                    currentBuildNo: instanceBuildNo.currentBuildNo
                },
                buildNoParam: comparedBuildNoParams
            }
        }
        
        // 模板推荐版本号为入参时
        if (templateBuildNo.required) {
            // 如果实例跟随模板，使用模板的推荐版本号配置
            if (instanceBuildNo?.isFollowTemplate) {
                // 对比模板版本号参数和实例版本号参数
                const comparedBuildNoParams = templateBuildNoParams.map(templateParam => {
                    const instanceParam = instanceBuildNoParamsMap.get(templateParam.id)
                    const isChange = instanceParam
                        ? String(templateParam.defaultValue) !== String(instanceParam.defaultValue)
                        : false
                    
                    return {
                        ...templateParam,
                        ...(isChange && { isChange: true })
                    }
                })
                
                return {
                    buildNo: {
                        ...instanceBuildNo,
                        buildNo: templateBuildNo.buildNo,
                        buildNoType: templateBuildNo.buildNoType,
                        required: templateBuildNo.required
                    },
                    buildNoParam: comparedBuildNoParams
                }
            }
            // 如果实例为不跟随模板，保持原实例的配置,但更新 required 为模板的 required
            return {
                buildNo: {
                    ...instanceBuildNo,
                    required: templateBuildNo.required
                },
                buildNoParam: instanceBuildNoParams
            }
        }
        
        // 兜底：返回实例配置
        return instanceBuildNo
    }
    
    /**
     * 检查版本号变量是否发生变化
     * @param {Array} currentParams - 当前实例的参数列表
     * @param {Array} initialParams - 初始实例的参数列表
     * @returns {boolean} - 如果版本号变量有变化返回 true，否则返回 false
     */
    function checkVersionParamsChanged (currentParams, initialParams) {
        if (!currentParams || !initialParams) {
            return false
        }
        
        const currentParamsMap = new Map(currentParams.map(p => [p.id, p]))
        const initialParamsMap = new Map(initialParams.map(p => [p.id, p]))
        
        // 遍历三个版本号变量
        for (const versionKey of allVersionKeyList) {
            const currentParam = currentParamsMap.get(versionKey)
            const initialParam = initialParamsMap.get(versionKey)
            
            if (currentParam && initialParam) {
                const currentValue = Number(currentParam.defaultValue)
                const initialValue = Number(initialParam.defaultValue)
                
                // 只要有一个版本号变量不一样，就返回 true
                if (currentValue !== initialValue) {
                    return true
                }
            }
        }
        
        return false
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
        const defaultValue = curTemplateDetail.value?.param?.find(i => i.id === id)?.defaultValue
        const initialInstanceParams = initialInstanceList.value?.[activeIndex.value - 1]?.param
        
        proxy.$store.commit(`templates/${UPDATE_INSTANCE_LIST}`, {
            index: activeIndex.value - 1,
            value: {
                ...curInstance.value,
                param: curInstance.value?.param.map(p => {
                    if (p.id === id) {
                        // 使用模板默认值
                        const initialParam = initialInstanceParams?.find(ip => ip.id === id)
                        
                        // 计算 isChange：对比模板默认值与初始值
                        let isChange = false
                        if (!p.isNew) {
                            const initialDefaultValue = initialParam?.defaultValue
                            const templateValue =  defaultValue
                            
                            isChange = isObject(templateValue)
                                ? !isShallowEqual(templateValue, initialDefaultValue)
                                : templateValue !== initialDefaultValue
                        }
                        
                        return {
                            ...p,
                            defaultValue,
                            isChange,
                            hasChange: false
                        }
                    }
                    return p
                })
            }
        })
    }
    function handleParamChange (id, value) {
        const initialInstanceParams = initialInstanceList.value?.[activeIndex.value - 1]?.param
        const templateParamsMap = new Map(curTemplateDetail.value?.param?.map(t => [t.id, t]) || [])
        
        proxy.$store.commit(`templates/${UPDATE_INSTANCE_LIST}`, {
            index: activeIndex.value - 1,
            value: {
                ...curInstance.value,
                param: curInstance.value?.param.map(p => {
                    if (p.id === id) {
                        // 更新当前参数的值
                        const initialParam = initialInstanceParams?.find(ip => ip.id === id)
                        const templateParam = templateParamsMap.get(id)
                        
                        // 计算 isChange：对比修改后的值与初始值
                        let isChange = false
                        if (!p.isNew) {
                            const initialDefaultValue = allVersionKeyList.includes(id)
                                ? Number(initialParam?.defaultValue)
                                : initialParam?.defaultValue
                            const currentValue = allVersionKeyList.includes(id)
                                ? Number(value)
                                : value
                            
                            isChange = isObject(currentValue)
                                ? !isShallowEqual(currentValue, initialDefaultValue)
                                : currentValue !== initialDefaultValue
                        }
                        
                        // 计算 hasChange：对比修改后的值与模板值
                        let hasChange = false
                        if (templateParam) {
                            const templateDefaultValue = allVersionKeyList.includes(id)
                                ? Number(templateParam.defaultValue)
                                : templateParam.defaultValue
                            const currentValue = allVersionKeyList.includes(id)
                                ? Number(value)
                                : value
                            
                            hasChange = isObject(currentValue)
                                ? !isShallowEqual(currentValue, templateDefaultValue)
                                : currentValue !== templateDefaultValue
                        }
                        
                        return {
                            ...p,
                            defaultValue: value,
                            isChange,
                            hasChange
                        }
                    }
                    return p
                })
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
                    [name]: value,
                    currentBuildNo: value
                },
                resetBuildNo: true
            }
        })
    }
    function handleCheckChange (value) {
        // 获取原始实例的 buildNo
        const initialBuildNo = initialInstanceList.value?.[activeIndex.value - 1]?.buildNo?.currentBuildNo
        
        proxy.$store.commit(`templates/${UPDATE_INSTANCE_LIST}`, {
            index: activeIndex.value - 1,
            value: {
                ...curInstance.value,
                buildNo: {
                    ...curInstance.value?.buildNo,
                    // 如果勾选，使用当前的 buildNo；如果取消勾选，恢复原始值
                    currentBuildNo: value ? curInstance.value.buildNo?.buildNo : initialBuildNo
                },
                resetBuildNo: value
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
                // 推荐版本号-取消跟随模板，存入overrideTemplateField字段为BK_CI_BUILD_NO
                target = 'BK_CI_BUILD_NO'
                index = paramIds.indexOf(target)
                const isCurrentlyFollowingTemplate = curInstance.value?.buildNo?.isFollowTemplate
                const willFollowTemplate = !isCurrentlyFollowingTemplate
                
                index > -1 ? paramIds.splice(index, 1) : paramIds.push(target)
                
                // 获取原始实例数据
                const initialInstance = initialInstanceList.value?.[activeIndex.value - 1]
                const initialBuildNo = initialInstance?.buildNo
                const initialParams = initialInstance?.param
                
                if (willFollowTemplate) {
                    // 1. 如果是跟随模板
                    const templateBuildNo = curTemplateDetail.value?.buildNo
                    const templateParams = curTemplateDetail.value?.param
                    
                    // 获取模板的版本号参数值
                    const templateVersionParams = {}
                    allVersionKeyList.forEach(versionKey => {
                        const templateParam = templateParams?.find(t => t.id === versionKey)
                        if (templateParam) {
                            templateVersionParams[versionKey] = templateParam.defaultValue
                        }
                    })
                    
                    // 检查 buildNo 或 allVersionKeyList 中任意一个参数是否与原实例的值不一样
                    let needResetBuildNo = false
                    
                    // 检查 buildNo 是否变化
                    if (templateBuildNo?.buildNo !== initialBuildNo?.buildNo) {
                        needResetBuildNo = true
                    }
                    
                    // 检查版本号参数是否有变化
                    if (!needResetBuildNo) {
                        for (const versionKey of allVersionKeyList) {
                            const initialParam = initialParams?.find(p => p.id === versionKey)
                            const templateValue = templateVersionParams[versionKey]
                            if (initialParam && String(templateValue) !== String(initialParam.defaultValue)) {
                                needResetBuildNo = true
                                break
                            }
                        }
                    }
                    
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
                                buildNo: templateBuildNo?.buildNo,
                                buildNoType: templateBuildNo?.buildNoType,
                                currentBuildNo: needResetBuildNo ? templateBuildNo?.buildNo : curInstance.value?.buildNo?.currentBuildNo,
                                isFollowTemplate: true
                            },
                            param: curInstance.value?.param.map(p => {
                                const templateParamDefault = curTemplateDetail.value.param?.find(t => t.id === p.id)?.defaultValue
                                if (allVersionKeyList.includes(p.id)) {
                                    return {
                                        ...p,
                                        defaultValue: allVersionKeyList.includes(p.id)
                                            ? templateParamDefault
                                            : p.defaultValue,
                                        isChange: p.defaultValue !== templateParamDefault,
                                        hasChange: p.defaultValue !== templateParamDefault
                                    }
                                }
                                return p
                            }),
                            resetBuildNo: needResetBuildNo,
                            buildNoChanged: templateBuildNo?.buildNo !== initialBuildNo?.buildNo
                        }
                    })
                } else {
                    // 2. 如果是取消跟随模板，恢复原实例的值
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
                                buildNo: initialBuildNo?.buildNo,
                                buildNoType: initialBuildNo?.buildNoType,
                                currentBuildNo: initialBuildNo?.currentBuildNo,
                                isFollowTemplate: false
                            },
                            param: curInstance.value?.param.map(p => {
                                if (allVersionKeyList.includes(p.id)) {
                                    const initialParam = initialParams?.find(ip => ip.id === p.id)
                                    return {
                                        ...p,
                                        defaultValue: initialParam?.defaultValue,
                                        isChange: false,
                                        hasChange: false
                                    }
                                }
                                return p
                            }),
                            resetBuildNo: false,
                            buildNoChanged: false
                        }
                    })
                }
                break

            case 'trigger':
                const temTriggerValue = curTemplateDetail.value.triggerConfigs?.find(trigger => trigger.stepId === id).disabled
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
                                disabled: trigger.stepId === id ? temTriggerValue : trigger.disabled,
                                isFollowTemplate: trigger.stepId === id ? !trigger.isFollowTemplate: trigger.isFollowTemplate
                            }
                        })
                    }
                })
                break
            case 'param':
                const temDefaultValue =  curTemplateDetail.value.param?.find(t => t.id === id)?.defaultValue
                const initialInstanceParams = initialInstanceList.value?.[activeIndex.value - 1]?.param
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
                        param: curInstance.value?.param.map(p => {
                            if (p.id === id) {
                                // 切换跟随模板状态
                                const newIsFollowTemplate = !p.isFollowTemplate
                                const initialParam = initialInstanceParams?.find(ip => ip.id === id)
                                
                                // 如果跟随模板，使用模板的默认值；否则使用原始实例的值
                                const newDefaultValue = newIsFollowTemplate
                                    ? temDefaultValue
                                    : initialParam?.defaultValue
                                
                                // 计算 isChange：对比新值与初始实例值
                                let isChange = false
                                if (!p.isNew) {
                                    const initialDefaultValue = allVersionKeyList.includes(id)
                                        ? Number(initialParam?.defaultValue)
                                        : initialParam?.defaultValue
                                    const currentValue = allVersionKeyList.includes(id)
                                        ? Number(newDefaultValue)
                                        : newDefaultValue
                                    
                                    isChange = isObject(currentValue)
                                        ? !isShallowEqual(currentValue, initialDefaultValue)
                                        : currentValue !== initialDefaultValue
                                }
                                
                                // 计算 hasChange：当前实例的值与模板默认值不同
                                // 如果跟随模板，新值就是模板值，所以 hasChange 为 false
                                // 如果不跟随模板，对比原始实例值与模板值
                                const hasChange = newIsFollowTemplate
                                    ? false
                                    : (allVersionKeyList.includes(id)
                                        ? Number(initialParam?.defaultValue) !== Number(temDefaultValue)
                                        : initialParam?.defaultValue !== temDefaultValue)
                                
                                return {
                                    ...p,
                                    defaultValue: newDefaultValue,
                                    isFollowTemplate: newIsFollowTemplate,
                                    hasChange,
                                    isChange
                                }
                            }
                            return p
                        })
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
        &.is-create {
            height: calc(100% - 98px);
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
                display: flex;
                align-items: center;
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
                max-width: 580px;
                display: inline-block;
                white-space: nowrap;
                text-overflow: ellipsis;
                overflow: hidden;
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
                .trigger-tips {
                    font-size: 12px;
                    margin-left: 10px;
                    color: #979BA5;
                    font-weight: 400;
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
        border-left: none;
    }
</style>
