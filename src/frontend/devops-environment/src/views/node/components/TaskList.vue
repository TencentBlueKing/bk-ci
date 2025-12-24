<template>
    <TaskList />
</template>

<script>
    import TaskList from '@/components/TaskList/'
    export default {
        name: 'NodeTaskList',
        components: {
            TaskList
        }
    }
</script>

<style lang="scss" scoped>
.node-tasklist-container {
    padding: 20px;
    height: 100%;
    overflow-y: auto;
    
    .section-title {
        font-size: 14px;
        font-weight: 700;
        color: #63656E;
        margin-bottom: 16px;
    }
    
    .tasklist-content {
        background: #F5F7FA;
        padding: 20px;
        border-radius: 2px;
    }
}
</style>
