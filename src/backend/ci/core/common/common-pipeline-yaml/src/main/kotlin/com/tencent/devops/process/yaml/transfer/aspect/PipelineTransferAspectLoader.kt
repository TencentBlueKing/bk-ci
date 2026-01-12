package com.tencent.devops.process.yaml.transfer.aspect

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.pojo.transfer.Resources
import com.tencent.devops.common.pipeline.pojo.transfer.ResourcesPools
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDispatch
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.yaml.v3.models.PreTemplateScriptBuildYamlV3Parser
import com.tencent.devops.process.yaml.v3.models.job.Job
import com.tencent.devops.process.yaml.v3.models.job.PreJob
import com.tencent.devops.process.yaml.v3.models.job.RunsOn
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

object PipelineTransferAspectLoader {
    private val cachedInstances: ConcurrentMap<String, IPipelineTransferAspect> = ConcurrentHashMap()

    fun getOrPutExtension(name: String?, createExtension: () -> IPipelineTransferAspect): IPipelineTransferAspect? {
        var instance = cachedInstances[name]
        // 从缓存中获取，如果不存在就创建
        if (instance == null) {
            synchronized(cachedInstances) {
                instance = cachedInstances[name]
                if (instance == null) {
                    instance = createExtension()
                    cachedInstances[name] = instance
                }
            }
        }
        return instance
    }

    fun yaml2ModelAspects(
        aspects: LinkedList<IPipelineTransferAspect> = LinkedList()
    ) {
        checkLockResourceJob(aspects)
        checkJobId(aspects)
    }

    private fun checkJobId(
        aspects: LinkedList<IPipelineTransferAspect> = LinkedList()
    ) {
        val jobsCheck = mutableSetOf<String/*job_id*/>()
        aspects.add(
            object : IPipelineTransferAspectJob {
                override fun after(jp: PipelineTransferJoinPoint) {
                    val job = jp.modelJob()
                    if (job?.jobId != null && !jobsCheck.add(job.jobId!!)) {
                        throw ErrorCodeException(
                            errorCode = ProcessMessageCode.ERROR_PIPELINE_JOBID_EXIST,
                            params = arrayOf(job.name, job.jobId!!)
                        )
                    }
                }
            })
    }

    /*
    * feat：第三方构建机 Job 间复用构建环境支持 Code 配置 #10254
    * 支持检查值的有效性
    * */
    private fun checkLockResourceJob(
        aspects: LinkedList<IPipelineTransferAspect> = LinkedList()
    ) {
        val jobsCheck = mutableListOf<String/*job_id*/>()
        val jobsNotCheck = mutableMapOf<String/*job_id*/, String/*child_Job_id*/>()
        aspects.add(
            object : IPipelineTransferAspectJob {
                override fun after(jp: PipelineTransferJoinPoint) {
                    val job = jp.modelJob()
                    if (job != null && job is VMBuildContainer && job.dispatchType is ThirdPartyAgentDispatch) {
                        when ((job.dispatchType as ThirdPartyAgentDispatch).agentType) {
                            AgentType.REUSE_JOB_ID -> {
                                jobsNotCheck[job.jobId!!] = (job.dispatchType as ThirdPartyAgentDispatch).value
                            }

                            else -> jobsCheck.add(job.jobId!!)
                        }
                    }
                }
            })
        aspects.add(
            object : IPipelineTransferAspectStage {
                override fun after(jp: PipelineTransferJoinPoint) {
                    // 回环检测
                    if (jobsNotCheck.isNotEmpty()) {
                        hasCycle(jobsNotCheck, jobsCheck)
                    }
                }

                private fun hasCycle(map: Map<String, String>, checked: List<String>): Boolean {
                    val visited = mutableSetOf<String>()
                    val stack = mutableSetOf<String>()

                    fun dfs(jobId: String): Boolean {
                        if (stack.contains(jobId)) {
                            return true
                        }
                        if (visited.contains(jobId)) {
                            return false
                        }

                        visited.add(jobId)
                        stack.add(jobId)

                        val childJobId = map[jobId]
                        if (childJobId != null && dfs(childJobId)) {
                            return true
                        }
                        if (childJobId == null && jobId !in checked) {
                            return true
                        }

                        stack.remove(jobId)
                        return false
                    }

                    map.forEach { (k, v) ->
                        if (!visited.contains(k) && dfs(k)) {
                            throw ErrorCodeException(
                                errorCode = ProcessMessageCode.ERROR_AGENT_REUSE_MUTEX_DEP_NULL_NODE,
                                params = arrayOf(k, v)
                            )
                        }
                    }
                    jobsCheck.addAll(map.keys)
                    jobsNotCheck.clear()
                    return false
                }
            }
        )
    }

    @Suppress("ComplexCondition")
    fun sharedEnvTransfer(
        aspects: LinkedList<IPipelineTransferAspect> = LinkedList()
    ) {
        val pools = mutableListOf<ResourcesPools>()
        aspects.add(
            object : IPipelineTransferAspectJob {
                override fun before(jp: PipelineTransferJoinPoint): Any? {
                    val job = jp.yamlJob()
                    if (job != null && job is Job && jp.yaml()?.formatResources()?.pools != null) {
                        jp.yaml()?.formatResources()?.pools?.find {
                            it.name == job.runsOn.poolName
                        }?.let { pool ->
                            job.runsOn.envProjectId = pool.from?.substringBefore("@")
                            job.runsOn.poolName = pool.from?.substringAfter("@")
                        }
                    }

                    return null
                }

                override fun after(jp: PipelineTransferJoinPoint) {
                    val job = jp.yamlPreJob()
                    if (job is PreJob && job.runsOn != null &&
                        job.runsOn is RunsOn &&
                        job.runsOn.envProjectId != null
                    ) {
                        val pool = job.runsOn
                        pools.add(
                            ResourcesPools(
                                from = "${pool.envProjectId}@${pool.poolName}",
                                name = pool.poolName
                            )
                        )
                    }
                }
            }
        )

        aspects.add(
            object : IPipelineTransferAspectModel {
                override fun after(jp: PipelineTransferJoinPoint) {
                    if (jp.yaml() != null &&
                        jp.yaml() is PreTemplateScriptBuildYamlV3Parser &&
                        pools.isNotEmpty()
                    ) {
                        val v3 = jp.yaml() as PreTemplateScriptBuildYamlV3Parser
                        v3.resources = Resources(
                            repositories = v3.resources?.repositories,
                            pools = v3.resources?.pools?.plus(pools) ?: pools
                        )
                    }
                }
            }
        )
    }

    fun initByDefaultTriggerOn(
        defaultRepo: () -> String,
        aspects: LinkedList<IPipelineTransferAspect> = LinkedList()
    ): LinkedList<IPipelineTransferAspect> {
        // 可以在此添加公共策略
        return aspects
    }

    // MODEL2YAML 时使用
    fun checkInvalidElement(
        invalidElement: MutableList<String>,
        invalidNameSpaceElement: MutableList<String>,
        aspects: LinkedList<IPipelineTransferAspect> = LinkedList()
    ): LinkedList<IPipelineTransferAspect> {
        aspects.add(
            object : IPipelineTransferAspectElement {
                override fun after(jp: PipelineTransferJoinPoint) {
                    if (jp.yamlPreStep() == null) {
                        invalidElement.add("${jp.modelElement()?.getClassType()}(${jp.modelElement()?.name})")
                    }
                }
            }
        )
        return aspects
    }
}
