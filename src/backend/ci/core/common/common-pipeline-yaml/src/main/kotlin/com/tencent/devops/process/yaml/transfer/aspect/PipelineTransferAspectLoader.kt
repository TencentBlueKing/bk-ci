package com.tencent.devops.process.yaml.transfer.aspect

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
