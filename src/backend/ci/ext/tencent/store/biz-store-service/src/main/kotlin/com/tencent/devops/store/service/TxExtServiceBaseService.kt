package com.tencent.devops.store.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.repository.pojo.enums.VisibilityLevelEnum
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.StoreBuildInfoDao
import com.tencent.devops.store.pojo.enums.ExtServicePackageSourceTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.dto.InitExtServiceDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class TxExtServiceBaseService: ExtServiceBaseService() {

    @Autowired
    override lateinit var client: Client

    @Value("\${git.service.nameSpaceId}")
    private lateinit var serviceNameSpaceId: String

    @Autowired
    lateinit var storeBuildInfoDao: StoreBuildInfoDao

    override fun handleAtomPackage(
        extensionInfo: InitExtServiceDTO,
        userId: String,
        serviceCode: String
    ): Result<Map<String, String>?> {
        logger.info("handleAtomPackage marketServiceCreateRequest is:$extensionInfo,serviceCode is:$serviceCode,userId is:$userId")
        extensionInfo.authType ?: return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PARAMETER_IS_NULL,
            arrayOf("authType"),
            null
        )
        extensionInfo.visibilityLevel ?: return MessageCodeUtil.generateResponseDataObject(
            CommonMessageCode.PARAMETER_IS_NULL,
            arrayOf("visibilityLevel"),
            null
        )
        val repositoryInfo: RepositoryInfo?
        if (extensionInfo.visibilityLevel == VisibilityLevelEnum.PRIVATE) {
            if (extensionInfo.privateReason.isNullOrBlank()) {
                return MessageCodeUtil.generateResponseDataObject(
                    CommonMessageCode.PARAMETER_IS_NULL,
                    arrayOf("privateReason"),
                    null
                )
            }
        }
        // 远程调工蜂接口创建代码库
        try {
            val createGitRepositoryResult = client.get(ServiceGitRepositoryResource::class).createGitCodeRepository(
                userId,
                extensionInfo.projectCode,
                serviceCode,
                storeBuildInfoDao.getStoreBuildInfoByLanguage(
                    dslContext,
                    extensionInfo.language,
                    StoreTypeEnum.SERVICE
                ).sampleProjectPath,
                serviceNameSpaceId.toInt(),
                extensionInfo.visibilityLevel,
                TokenTypeEnum.PRIVATE_KEY
            )
            logger.info("the createGitRepositoryResult is :$createGitRepositoryResult")
            if (createGitRepositoryResult.isOk()) {
                repositoryInfo = createGitRepositoryResult.data
            } else {
                return Result(createGitRepositoryResult.status, createGitRepositoryResult.message, null)
            }
        } catch (e: Exception) {
            logger.info("createGitCodeRepository error  is :$e", e)
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_CREATE_REPOSITORY_FAIL)
        }
        if (null == repositoryInfo) {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_CREATE_REPOSITORY_FAIL)
        }
        return Result(mapOf("repositoryHashId" to repositoryInfo.repositoryHashId!!, "codeSrc" to repositoryInfo.url))
    }

    override fun getExtServicePackageSourceType(atomCode: String): ExtServicePackageSourceTypeEnum {
        // 内部版暂时只支持代码库打包的方式，后续支持用户传可执行包的方式
        return ExtServicePackageSourceTypeEnum.REPO
    }

    override fun getRepositoryInfo(projectCode: String?, repositoryHashId: String?): Result<Repository?> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}