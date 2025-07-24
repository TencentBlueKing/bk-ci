// 转化为composition API 的 mixin

import { computed, getCurrentInstance } from 'vue'

export default function useTemplateConstraint () {
    const vm = getCurrentInstance()
    const overrideTemplateGroups = computed(() => vm.proxy.$store.state.atom.pipeline?.overrideTemplateField ?? {})

    const instanceFromTemplate = computed(() => vm.proxy.$store.getters['atom/instanceFromTemplate'])

    function isOverrideTemplate (classify, field) {
        return !!(instanceFromTemplate.value && overrideTemplateGroups.value?.[classify]?.includes(field))
    }
    function unConstraint (classify, field) {
        return vm.proxy.$store.dispatch('atom/updatePipelineConstraintGroup', {
            classify,
            field
        })
    }

    return {
        vm,
        instanceFromTemplate,
        isOverrideTemplate,
        unConstraint,
        overrideTemplateGroups,
    }
}


