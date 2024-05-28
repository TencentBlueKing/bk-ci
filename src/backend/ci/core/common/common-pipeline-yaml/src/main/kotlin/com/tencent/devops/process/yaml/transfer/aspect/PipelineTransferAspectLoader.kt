package com.tencent.devops.process.yaml.transfer.aspect

import com.tencent.devops.common.pipeline.pojo.transfer.Resources
import com.tencent.devops.common.pipeline.pojo.transfer.ResourcesPools
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

    fun sharedEnvTransfer(
        aspects: LinkedList<IPipelineTransferAspect> = LinkedList()
    ): LinkedList<IPipelineTransferAspect> {
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
        return aspects
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

    fun checkInvalidElement(
        invalidElement: MutableList<String>,
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
