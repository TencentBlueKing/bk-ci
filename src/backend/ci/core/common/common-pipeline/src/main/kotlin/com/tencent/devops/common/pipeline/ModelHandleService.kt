package com.tencent.devops.common.pipeline

import com.tencent.devops.common.pipeline.pojo.BuildFormProperty

interface ModelHandleService {

    /**
     * 处理模型参数
     * 该方法负责对流水线模型参数进行统一处理
     * @param projectId 项目ID
     * @param model 模型对象
     * @param referId 引用ID，标识参数引用的具体资源或对象
     * @param referType 引用类型，描述引用ID对应的资源类型（如模板、流水线等）
     * @param referVersion 引用版本，标识引用资源的版本号
     */
    fun handleModelParams(
        projectId: String,
        modelPublicVarHandleContext: ModelPublicVarHandleContext
    ): List<BuildFormProperty>

    /**
     * 处理模型中所有变量的引用
     * @param projectId 项目ID
     * @param model 模型对象
     * @param resourceId 资源ID
     * @param resourceType 资源类型
     * @param resourceVersion 资源版本
     */
    fun handleModelVarReferences(
        userId: String,
        projectId: String,
        resourceId: String,
        resourceType: String,
        model: Model? = null,
        resourceVersion: Int
    )
}
