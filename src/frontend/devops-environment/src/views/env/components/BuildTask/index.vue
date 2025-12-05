<template>
    <TaskList
        task-type="build"
        :fetch-task-list="fetchBuildTaskList"
        :fetch-task-detail="fetchBuildTaskDetail"
    />
</template>

<script>
    import TaskList from '../TaskList/index.vue'
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
            
            // 模拟数据生成 - 支持分页
            const generateMockTaskList = (page = 1, pageSize = 20) => {
                const total = 58 // 总共 58 条任务数据
                const start = (page - 1) * pageSize
                const end = Math.min(start + pageSize, total)
                
                const tasks = []
                for (let i = start; i < end; i++) {
                    tasks.push({
                        id: `job-${i + 1}`,
                        name: `这是一个 Job ${i + 1}`,
                        pipelineName: `流水线名称 ${i + 1}`,
                        executeCount: Math.floor(Math.random() * 50) + 1,
                        avgDuration: Math.floor(Math.random() * 1800) + 30, // 30秒到30分钟
                        createTime: Date.now() / 1000 - Math.random() * 30 * 24 * 3600,
                        lastExecuteTime: Date.now() / 1000 - Math.random() * 7 * 24 * 3600
                    })
                }
                
                return {
                    records: tasks,
                    count: total
                }
            }
            
            const generateMockTaskDetail = (taskId, page = 1, pageSize = 10) => {
                const statuses = ['success', 'running', 'failed']
                const users = ['daisyhong', 'zhangsan', 'lisi', 'wangwu']
                const total = 198
                
                const records = []
                const start = (page - 1) * pageSize
                const end = Math.min(start + pageSize, total)
                
                for (let i = start; i < end; i++) {
                    const num = total - i
                    const status = statuses[Math.floor(Math.random() * statuses.length)]
                    const duration = Math.floor(Math.random() * 1400) + 9 // 9秒到23分钟
                    const startTime = Date.now() / 1000 - Math.random() * 30 * 24 * 3600
                    
                    records.push({
                        buildNum: num,
                        status: status,
                        duration: duration,
                        startTime: startTime,
                        endTime: status === 'running' ? null : startTime + duration,
                        triggerUser: users[Math.floor(Math.random() * users.length)]
                    })
                }
                
                return {
                    records,
                    count: total
                }
            }
            
            // 获取构建任务列表
            const fetchBuildTaskList = async (params) => {
                try {
                    // 使用真实接口
                    // const res = await proxy.$store.dispatch('environment/requestBuildTaskList', {
                    //     projectId: projectId.value,
                    //     envHashId: envHashId.value,
                    //     params
                    // })
                    // return res || { records: [], count: 0 }
                    
                    // 暂时返回模拟数据
                    return new Promise((resolve) => {
                        setTimeout(() => {
                            resolve(generateMockTaskList(params.page, params.pageSize))
                        }, 800) // 模拟网络延迟
                    })
                } catch (err) {
                    console.error('获取构建任务列表失败:', err)
                    throw err
                }
            }
            
            // 获取构建任务详情
            const fetchBuildTaskDetail = async (taskId, params) => {
                try {
                    // 使用真实接口
                    // const res = await proxy.$store.dispatch('environment/requestBuildTaskDetail', {
                    //     projectId: projectId.value,
                    //     envHashId: envHashId.value,
                    //     taskId,
                    //     params
                    // })
                    // return res || { records: [], count: 0 }
                    
                    // 暂时返回模拟数据
                    return new Promise((resolve) => {
                        setTimeout(() => {
                            resolve(generateMockTaskDetail(taskId, params.page, params.pageSize))
                        }, 500)
                    })
                } catch (err) {
                    console.error('获取构建任务详情失败:', err)
                    throw err
                }
            }
            
            return {
                fetchBuildTaskList,
                fetchBuildTaskDetail
            }
        }
    }
</script>

<style lang="scss" scoped>

</style>
