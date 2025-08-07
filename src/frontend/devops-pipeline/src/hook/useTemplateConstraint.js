// 转化为composition API 的 mixin

import { allVersionKeyList } from '@/utils/pipelineConst'
import { computed, getCurrentInstance, ref } from 'vue'


export const CLASSIFY_ENUM = {
    PARAM: 'paramIds',
    SETTING: 'settingGroups',
    TRIGGER: 'triggerStepIds'
}

export default function useTemplateConstraint () {
    const fieldMap = {
        buildNumRule: 'CUSTOM_BUILD_NUM',
        labels: 'LABEL',
        notices: 'NOTICES',
        parallelSetting: 'CONCURRENCY',
        failIfVariableInvalid: 'FAIL_IF_VARIABLE_INVALID',
        buildNo: 'BK_CI_BUILD_NO'
    }
    
    const labelMap = {
        [CLASSIFY_ENUM.TRIGGER]: 'triggerSetting',
        [CLASSIFY_ENUM.PARAM]: 'paramDefaultValue',
        [CLASSIFY_ENUM.SETTING]: 'template.settings',
        BK_CI_BUILD_NO: 'template.versionSetting'
    }
    const vm = getCurrentInstance()
    const reverting = ref(false)
    const overrideTemplateGroups = computed(() => vm.proxy.$store.state.atom.pipeline?.overrideTemplateField ?? {})

    const instanceFromTemplate = computed(() => vm.proxy.$store.getters['atom/instanceFromTemplate'])

    function isOverrideTemplate (classify, field) {
        if (!instanceFromTemplate.value) return true
        
        return vm.proxy.$route.meta.edit && overrideTemplateGroups.value[classify]?.includes(field)
    }

    function partialRevertPipelineSetting (setting, field) {
        console.log('partialRevertPipeline', setting, field)
        const param = {}
        if (field === 'notices') {
            Object.assign(param, {
                failSubscriptionList: setting.failSubscriptionList,
                successSubscriptionList: setting.successSubscriptionList
            })
        } else if (field === 'parallelSetting') {
            Object.assign(param, [
                'maxConRunningQueueSize',
                'maxQueueSize',
                'runLockType',
                'waitQueueTimeMinute',
                'concurrencyCancelInProgress',
                'concurrencyGroup'
            ].reduce((acc, cur) => {
                acc[cur] = setting[cur]
                return acc
            }, {})
            )
        } else {
            param[field] = setting[field]
        }
        vm.proxy.$store.dispatch('atom/updatePipelineSetting', {
            setting: vm.proxy.$store.state.atom.pipelineSetting,
            param
        })
    }

    function partialRevertPipelineModel (model, classify, field) {
        console.log('partialRevertPipeline', model, classify, field)
        const currentTriggerContainer = vm.proxy.$store.state.atom.pipeline.stages[0].containers[0]
        const constraintTriggerContainer = model.stages[0].containers[0]
        if (classify === CLASSIFY_ENUM.TRIGGER) {
            const currentAtom = findTrigger(currentTriggerContainer, field)
            const constraintAtom = findTrigger(constraintTriggerContainer, field)
            
            vm.proxy.$store.dispatch('atom/updateAtom', {
                element: currentAtom,
                newParam: {
                    ...constraintAtom,
                    startParams: constraintAtom.startParams ?? null
                }
            })
        } else if (classify === CLASSIFY_ENUM.PARAM) {
            const { params } = currentTriggerContainer
            if (field === 'buildNo') {
                const { buildNo = null, params: contraintParams } = constraintTriggerContainer
                const otherParams = params.filter(p => !allVersionKeyList.includes(p.id))
                const allVersionParams = contraintParams.filter(p => allVersionKeyList.includes(p.id))
                console.log(buildNo, otherParams, allVersionParams)
                vm.proxy.$store.dispatch('atom/updateContainer', {
                    container: currentTriggerContainer,
                    newParam: {
                        buildNo,
                        params: [
                            ...otherParams,
                            ...allVersionParams
                        ]
                    }
                })
            }
            const paramIndex = constraintTriggerContainer.params.findIndex(item => item.id === field)
            if (paramIndex === -1) return
            vm.proxy.$store.dispatch('atom/updateContainer', {
                container: currentTriggerContainer,
                newParam: {
                    params: [
                        ...params.slice(0, paramIndex),
                        constraintTriggerContainer.params[paramIndex],
                        ...params.slice(paramIndex + 1)
                    ]
                }
            })
        }
    }

    function findTrigger (container, stepId) {
        return container.elements.find(item => item.stepId === stepId)
    }

    function partialRevert (modelAndSetting, classify, field) {
        if (classify === CLASSIFY_ENUM.SETTING) {
            partialRevertPipelineSetting(modelAndSetting.setting, field)
        } else {
            partialRevertPipelineModel(modelAndSetting.model, classify, field)
        }
    }

    async function revertTemplateConstraint (classify, field) {
        try {
            reverting.value = true
            const templateRes = await vm.proxy.$store.dispatch('atom/revertPipelineConstraint', vm.proxy.$route.params)
            partialRevert({
                model: templateRes.resource.model,
                setting: templateRes.setting
            }, classify, field)
        } catch (error) {
            console.error(error)
        } finally {
            reverting.value = false
        }
    }
    async function toggleConstraint (classify, fieldAlias, field) {
        if (reverting.value) return
        let constraintList = overrideTemplateGroups.value[classify] || []
        const pos = constraintList.indexOf(fieldAlias)
      
        if (pos === -1) {
            constraintList = [
                ...constraintList,
                fieldAlias
            ]
        } else {
            constraintList = [
                ...constraintList.slice(0, pos),
                ...constraintList.slice(pos + 1)
            ]
            await revertTemplateConstraint(classify, field)
        }
        
        vm.proxy.$store.dispatch('atom/updatePipelineConstraintGroup', {
            classify,
            constraintList
        })
    }

    return {
        vm,
        instanceFromTemplate,
        isOverrideTemplate,
        toggleConstraint,
        overrideTemplateGroups,
        fieldMap,
        labelMap,
        reverting
    }
}


