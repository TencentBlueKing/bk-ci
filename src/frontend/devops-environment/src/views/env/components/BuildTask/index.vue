<template>
    <TaskList
        task-type="build"
    />
</template>

<script>
    import TaskList from '@/components/TaskList/'
    import useInstance from '@/hooks/useInstance'
    import useEnvDetail from '@/hooks/useEnvDetail'
    
    export default {
        name: 'BuildTask',
        components: {
            TaskList
        },
        setup () {
            const { proxy } = useInstance()
            const { projectId, envHashId } = useEnvDetail()
           
            return {
                projectId,
                envHashId
            }
        }
    }
</script>

<style lang="scss" scoped>

</style>
