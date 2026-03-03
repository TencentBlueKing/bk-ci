<template>
    <bk-permission
        :key="resourceCode"
        :resource-type="resourceType"
        :resource-code="resourceCode"
        :project-code="projectCode"
        :show-create-group="false"
        :resource-name="resourceName"
    />
</template>

<script>
    import { ref, computed } from 'vue'
    import useInstance from '@/hooks/useInstance'
    import useEnvDetail from '@/hooks/useEnvDetail'

    export default {
        name: 'AuthManage',
        setup () {
            const { proxy } = useInstance()
            const {
                currentEnv
            } = useEnvDetail()
            const resourceType = ref('environment')
            const projectCode = computed(() => proxy.$route.params.projectId)
            const resourceCode = computed(() => proxy.$route.params.envId)
            const resourceName = computed(() => currentEnv.value?.name || '')
            return {
                resourceType,
                projectCode,
                resourceCode,
                resourceName
            }
        }
    }
</script>
