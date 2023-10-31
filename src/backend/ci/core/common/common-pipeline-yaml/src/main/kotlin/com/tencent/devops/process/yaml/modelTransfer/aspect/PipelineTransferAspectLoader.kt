package com.tencent.devops.process.yaml.modelTransfer.aspect

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
        defaultRepo: String,
        aspects: LinkedList<IPipelineTransferAspect> = LinkedList()
    ): LinkedList<IPipelineTransferAspect> {
        aspects.add(
            object : IPipelineTransferAspectTrigger {
                override fun before(jp: PipelineTransferJoinPoint): Any? {
                    if (jp.yamlTriggerOn() != null && jp.yamlTriggerOn()!!.repoName == null) {
                        jp.yamlTriggerOn()!!.repoName = defaultRepo
                    }
                    return null
                }
            }
        )
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
