package com.tencent.devops.common.pipeline.pojo.element

class EmptyElement(
    override val name: String = "unknownType",
    override var id: String? = null
) : Element(name, id) {
    override fun getClassType() = "unknownType"
}
