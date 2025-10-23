package com.tencent.devops.common.pipeline

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
        model: Model,
        referId: String,
        referType: String,
        referVersion: Int
    )
}
