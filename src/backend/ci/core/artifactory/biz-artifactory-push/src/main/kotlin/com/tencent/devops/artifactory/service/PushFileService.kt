package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.constant.PushMessageCode
import com.tencent.devops.artifactory.pojo.FileResourceInfo
import com.tencent.devops.artifactory.pojo.RemoteResourceInfo
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthPermissionApi
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.code.PipelineAuthServiceCode
import com.tencent.devops.common.service.utils.MessageCodeUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PushFileService @Autowired constructor(
    private val authPermissionApi: AuthPermissionApi,
    private val pipelineAuthServiceCode: PipelineAuthServiceCode,
    private val envService: EnvService

) {
    fun pushFileByJob(
        userId: String,
        pushResourceInfo: RemoteResourceInfo,
        fileResourceInfo: FileResourceInfo
    ): String {
        val projectId = fileResourceInfo.projectId
        val pipelineId = fileResourceInfo.pipelineId
        // 校验用户是否有权限下载文件
        val validatePermission = authPermissionApi.validateUserResourcePermission(userId, pipelineAuthServiceCode, AuthResourceType.PIPELINE_DEFAULT, projectId, pipelineId, AuthPermission.DOWNLOAD)
        if(!validatePermission) {
            throw RuntimeException(MessageCodeUtil.getCodeMessage(PushMessageCode.FUSH_FILE_VALIDATE_FAIL, null))
        }
        // 解析目标机器，需校验用户是否有权限操作目标机
        val envSet = envService.parsingAndValidateEnv(pushResourceInfo, userId)
        // 下载目标文件到本地


        // 执行分发动作
    }
}