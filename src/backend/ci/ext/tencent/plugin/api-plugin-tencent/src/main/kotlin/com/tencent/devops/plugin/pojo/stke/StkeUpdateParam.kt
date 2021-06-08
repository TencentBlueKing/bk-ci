package com.tencent.devops.plugin.pojo.stke

data class StkeUpdateParam(
    val metadata: MetadataParam,
    val spec: SpecParam
) {

    data class MetadataParam(
        val annotations: HashMap<String, Any> = HashMap<String, Any>()
    )

    data class SpecParam(
        val updateStrategy: UpdateStrategy,
        val template: TemplateParam
    )

    data class UpdateStrategy(
        val type: String = "RollingUpdate",
        val rollingUpdate: RollingUpdate = RollingUpdate(0)
    )

    data class RollingUpdate(
        val partition: Int
    )

    data class TemplateParam(
        val spec: TemplateSpecParam
    )

    data class TemplateSpecParam(
        val containers: List<ContainerParam>
    )

    data class ContainerParam(
        val image: String,
        val name: String
    )
}
