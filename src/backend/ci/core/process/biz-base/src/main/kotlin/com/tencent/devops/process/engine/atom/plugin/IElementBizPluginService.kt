package com.tencent.devops.process.engine.atom.plugin

import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.atom.BeforeDeleteParam
import com.tencent.devops.common.pipeline.pojo.element.atom.ElementBatchCheckParam
import com.tencent.devops.common.pipeline.pojo.element.atom.ElementHolder

/**
 * 插件扩展点服务类
 */
interface IElementBizPluginService {

    /**
     * 支持的插件
     */
    fun supportElement(element: Element): Boolean

    fun supportAtomCode(atomCode: String): Boolean

    /**
     * 在删除[element]插件之前，根据[param]参数调用删除前的预处理
     */
    fun beforeDelete(element: Element, param: BeforeDeleteParam)

    fun batchCheck(
        elements: List<ElementHolder>,
        param: ElementBatchCheckParam
    )
}
