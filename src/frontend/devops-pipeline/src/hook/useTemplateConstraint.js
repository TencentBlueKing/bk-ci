// 转化为composition API 的 mixin

import { computed, getCurrentInstance, ref } from 'vue'

export default function useTemplateConstraint () {
    const fieldMap = {
        buildNumRule: 'CUSTOM_BUILD_NUM',
        labels: 'LABEL',
        notices: 'NOTICES',
        parallelSetting: 'CONCURRENCY',
        failIfVariableInvalid: 'FAIL_IF_VARIABLE_INVALID'
    }
    
    const labelMap = {
        triggerStepIds: 'triggerSetting',
        paramIds: 'paramDefaultValue',
        settingGroups: 'template.settings',
    }
    const vm = getCurrentInstance()
    const reverting = ref(false)
    const overrideTemplateGroups = computed(() => vm.proxy.$store.state.atom.pipeline?.overrideTemplateField ?? {})

    const instanceFromTemplate = computed(() => vm.proxy.$store.getters['atom/instanceFromTemplate'])

    function isOverrideTemplate (classify, field) {
        return !!(instanceFromTemplate.value && overrideTemplateGroups.value?.[classify]?.includes(field))
    }

    function partialRevertPipelineSetting (setting, field) {
        console.log('partialRevertPipeline', setting, field)
        const param = {}
        if (field === 'notices') {
            Object.assign(param, {
                failSubscriptionList: setting.failSubscriptionList,
                successSubscriptionList: setting.successSubscriptionList
            })
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
        if (classify === 'triggerStepIds') {
            const currentAtom = findTrigger(currentTriggerContainer, field)
            const constraintAtom = findTrigger(constraintTriggerContainer, field)
            
            vm.proxy.$store.dispatch('atom/updateAtom', {
                element: currentAtom,
                newParam: {
                    ...constraintAtom
                }
            })
        } else if (classify === 'paramIds') {
            const { params } = currentTriggerContainer
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
        if (classify === 'settingGroups') {
            partialRevertPipelineSetting(modelAndSetting.setting, field)
        } else {
            partialRevertPipelineModel(modelAndSetting.model, classify, field)
        }
    }

    async function revertTemplateConstraint (classify, field) {
        try {
            reverting.value = true
            const { pipeline } = vm.proxy.$store.state.atom
            const templateRes = await vm.proxy.$store.dispatch('atom/fetchTemplateByVersion', {
                projectId: vm.proxy.$route.params.projectId,
                templateId: pipeline.parsedTemplateId,
                version: pipeline.parsedTemplateVersion
            })
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


