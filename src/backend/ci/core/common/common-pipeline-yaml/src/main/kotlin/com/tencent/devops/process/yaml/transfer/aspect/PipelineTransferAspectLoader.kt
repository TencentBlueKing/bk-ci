package com.tencent.devops.process.yaml.transfer.aspect

import com.tencent.devops.common.api.constant.CommonMessageCode.BK_ELEMENT_NAMESPACE_NOT_SUPPORT
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.transfer.Resources
import com.tencent.devops.common.pipeline.pojo.transfer.ResourcesPools
import com.tencent.devops.common.pipeline.type.agent.AgentType
import com.tencent.devops.common.pipeline.type.agent.ThirdPartyAgentDispatch
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.yaml.v3.models.PreTemplateScriptBuildYamlV3Parser
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

    /*
    * feat：第三方构建机 Job 间复用构建环境支持 Code 配置 #10254
    * 支持检查值的有效性
    * */
    fun checkLockResourceJob(
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

    fun sharedEnvTransfer(
        aspects: LinkedList<IPipelineTransferAspect> = LinkedList()
    ) {
        val pools = mutableListOf<ResourcesPools>()
        aspects.add(
            object : IPipelineTransferAspectJob {
                override fun before(jp: PipelineTransferJoinPoint): Any? {
                    if (jp.yamlJob() != null && jp.yaml()?.formatResources()?.pools != null) {
                        jp.yaml()?.formatResources()?.pools?.find {
                            it.name == jp.yamlJob()!!.runsOn.poolName
                        }?.let { pool ->
                            jp.yamlJob()!!.runsOn.envProjectId = pool.from?.substringBefore("@")
                            jp.yamlJob()!!.runsOn.poolName = pool.from?.substringAfter("@")
                        }
                    }

                    return null
                }

                override fun after(jp: PipelineTransferJoinPoint) {
                    if (jp.yamlPreJob()?.runsOn != null &&
                        jp.yamlPreJob()?.runsOn is RunsOn &&
                        (jp.yamlPreJob()?.runsOn as RunsOn).envProjectId != null
                    ) {
                        val pool = jp.yamlPreJob()?.runsOn as RunsOn
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
        // val repoName = lazy { defaultRepo() }
        /*aspects.add(
            object : IPipelineTransferAspectTrigger {
                override fun before(jp: PipelineTransferJoinPoint): Any? {
                    if (jp.yamlTriggerOn() != null && jp.yamlTriggerOn()!!.repoName == null) {
                        jp.yamlTriggerOn()!!.repoName = repoName.value
                    }
                    return null
                }
            }
        )*/
        /*checkout 新增 self类型，此处暂时去掉转换 */
//        aspects.add(
//            object : IPipelineTransferAspectElement {
//                override fun before(jp: PipelineTransferJoinPoint): Any? {
//                    if (jp.yamlStep() != null && jp.yamlStep()!!.checkout == "self") {
//                        jp.yamlStep()!!.checkout = repoName.value
//                    }
//                    return null
//                }
//            }
//        )
        /*aspects.add(
            // 一个触发器时，如果为默认仓库则忽略repoName和type
            object : IPipelineTransferAspectModel {
                override fun after(jp: PipelineTransferJoinPoint) {
                    if (jp.yaml() is PreTemplateScriptBuildYamlV3 &&
                        (jp.yaml() as PreTemplateScriptBuildYamlV3).triggerOn is PreTriggerOnV3
                    ) {
                        val triggerOn = (jp.yaml() as PreTemplateScriptBuildYamlV3).triggerOn as PreTriggerOnV3
                        if (triggerOn.repoName == repoName.value) {
                            triggerOn.repoName = null
                            triggerOn.type = null
                        }
                    }
                }
            }
        )*/
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

        // feat: PAC Code 检测流水线是否使用了命名空间 #11879
        aspects.add(
            object : IPipelineTransferAspectElement {
                override fun before(jp: PipelineTransferJoinPoint): Any? {
                    if (jp.modelElement() != null &&
                        jp.modelElement() is MarketBuildAtomElement
                    ) {
                        val element = jp.modelElement() as MarketBuildAtomElement
                        val namespace = element.data["namespace"] as String? ?: return null
                        if (namespace.isNotBlank()) {
                            invalidNameSpaceElement.add(
                                I18nUtil.getCodeLanMessage(
                                    BK_ELEMENT_NAMESPACE_NOT_SUPPORT,
                                    params = arrayOf("${element.name}[${element.stepId}]")
                                )
                            )
                        }
                    }
                    return null
                }
            }
        )
        return aspects
    }
}
