package com.tencent.devops.environment.utils

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.environment.dao.NodeDao
import com.tencent.devops.environment.dao.ProjectConfigDao
import com.tencent.devops.environment.dao.StaticData
import com.tencent.devops.environment.pojo.BcsVmParam
import org.jooq.DSLContext

object BcsVmParamCheckUtils {

    fun checkAndGetVmCreateParam(dslContext: DSLContext, projectConfigDao: ProjectConfigDao, nodeDao: NodeDao, projectId: String, userId: String, vmParam: BcsVmParam): Triple<String, String, String> {
        val projectConfig = projectConfigDao.get(dslContext, projectId, userId)
        if (!projectConfig.bcsvmEnalbed) {
            throw OperationException("项目[$projectId]没有开通过BCS虚拟机功能，请联系【蓝盾助手】申请资源")
        }

        val bcsVmImageMap = StaticData.getBcsImageList().associateBy { it.imageId }
        if (!bcsVmImageMap.containsKey(vmParam.imageId)) {
            throw OperationException("无效的 imageId")
        }

        val bcsVmModelMap = StaticData.getBcsVmModelList().associateBy { it.moduleId }
        if (!bcsVmModelMap.containsKey(vmParam.vmModelId)) {
            throw OperationException("无效的 imageId")
        }

        val usedCount = nodeDao.countBcsVm(dslContext, projectId)
        val limit = projectConfig.bcsvmQuota
        if (vmParam.instanceCount > limit - usedCount) {
            throw OperationException("bcs虚拟机配额不足")
        }

        val bcsVmModel = bcsVmModelMap[vmParam.vmModelId]!!
        return if (System.getProperty("spring.profiles.active") == "prod") {
            Triple(bcsVmImageMap[vmParam.imageId]!!.image, bcsVmModel.resCpu, bcsVmModel.resMemory)
        } else {
            Triple(bcsVmImageMap[vmParam.imageId]!!.image, "0.1", "100")
        }
    }
}