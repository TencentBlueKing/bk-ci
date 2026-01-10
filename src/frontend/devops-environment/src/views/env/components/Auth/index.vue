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

    export default {
        name: 'AuthManage',
        props: {
            curEnvDetail: Object
        },
        setup (props) {
            const { proxy } = useInstance()
            const resourceType = ref('environment')
            const projectCode = computed(() => proxy.$route.params.projectId)
            const resourceCode = computed(() => proxy.$route.params.envId)
            const resourceName = computed(() => props.curEnvDetail?.envName || '')
            return {
                resourceType,
                projectCode,
                resourceCode,
                resourceName
            }
        }
    }
</script>
