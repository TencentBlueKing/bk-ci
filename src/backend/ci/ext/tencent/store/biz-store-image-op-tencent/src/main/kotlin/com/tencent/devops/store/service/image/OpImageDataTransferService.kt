package com.tencent.devops.store.service.image

import com.tencent.devops.common.api.exception.DataConsistencyException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.image.api.ServiceImageResource
import com.tencent.devops.image.pojo.DockerTag
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import com.tencent.devops.store.dao.common.CategoryDao
import com.tencent.devops.store.dao.common.ClassifyDao
import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.pojo.common.ClassifyRequest
import com.tencent.devops.store.pojo.common.StoreMemberReq
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.request.ImageCreateRequest
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class OpImageDataTransferService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val imageDao: ImageDao,
    private val classifyDao: ClassifyDao,
    private val categoryDao: CategoryDao,
    private val opImageService: OpImageService,
    private val imageMemberService: ImageMemberService
) {
    companion object {
        const val CLASSIFYCODE_OTHER = "OTHER"
        const val CATEGORY_PIPELINE_JOB = "PIPELINE_JOB"
    }

    private val logger = LoggerFactory.getLogger(OpImageDataTransferService::class.java)

    @Value("\${store.imageAdminUsers}")
    private val imageAdminUsersStr: String? = null

    private val finishedSet: HashSet<Triple<String, String, String>> = HashSet()

    private lateinit var imageAdminUsers: Set<String>

    /**
     * 生成数据迁移专有镜像分类
     */
    fun createSystemInitClassify(classifyCode: String, classifyName: String) {
        if (null == classifyDao.getClassifyByCode(
                dslContext,
                classifyCode,
                StoreTypeEnum.IMAGE
            )
        ) {
            try {
                classifyDao.add(
                    dslContext = dslContext,
                    id = UUIDUtil.generate(),
                    classifyRequest = ClassifyRequest(
                        classifyCode = classifyCode,
                        classifyName = classifyName,
                        weight = null
                    ),
                    type = StoreTypeEnum.IMAGE.type.toByte()
                )
            } catch (e: DuplicateKeyException) {
                // 并发创建可能引起的主键冲突直接忽略
            }
        }
    }

    /**
     * 生成数据迁移专有镜像范畴
     */
    fun createSystemInitCategory(categoryCode: String, categoryName: String) {
        if (null == categoryDao.getCategoryByCodeAndType(
                dslContext,
                categoryCode,
                StoreTypeEnum.IMAGE.type.toByte()
            )
        ) {
            try {
                categoryDao.add(
                    dslContext = dslContext,
                    id = UUIDUtil.generate(),
                    categoryCode = categoryCode,
                    categoryName = categoryName,
                    iconUrl = "http://radosgw.open.oa.com/paas_backend/ieod/prod/file/png/random_15649905585375037820270514184859.png?v=1564990558",
                    type = StoreTypeEnum.IMAGE.type.toByte()
                )
            } catch (e: DuplicateKeyException) {
                // 并发创建可能引起的主键冲突直接忽略
            }
        }
    }

    @PostConstruct
    fun init() {
        imageAdminUsers = imageAdminUsersStr!!.trim().split(";").toSet()
        logger.info("imageAdminUsers= $imageAdminUsers")
    }

    fun initClassifyAndCategory(
        userId: String,
        classifyCode: String?,
        classifyName: String?,
        categoryCode: String?,
        categoryName: String?,
        interfaceName: String? = "Anon interface"
    ): Int {
        logger.info("$interfaceName:initClassifyAndCategory:Input($classifyCode,$classifyName,$categoryCode,$categoryName)")
        createSystemInitClassify(classifyCode ?: CLASSIFYCODE_OTHER, classifyName ?: "其它")
        logger.info("$interfaceName:initClassifyAndCategory:Inner:createSystemInitClassify end,begin to createSystemInitCategory")
        createSystemInitCategory(categoryCode ?: CATEGORY_PIPELINE_JOB, categoryName ?: "流水线Job")
        logger.info("$interfaceName:initClassifyAndCategory:Output:createSystemInitCategory end")
        return 0
    }

    /**
     * 清空已迁移数据记录，释放内存
     */
    fun clearFinishedSet(
        userId: String,
        interfaceName: String? = "Anon interface"
    ): Int {
        // 0.鉴权：管理员才有权限操作
        if (!imageAdminUsers.contains(userId.trim())) {
            throw PermissionForbiddenException("Permission Denied,userId=$userId")
        }
        val size = finishedSet.size
        logger.info("$interfaceName:clearFinishedSet:finishedSet.size=$size")
        finishedSet.clear()
        return size
    }

    /**
     * 迁移一个项目的历史数据
     */
    fun transferImage(
        userId: String,
        projectCode: String,
        classifyCode: String?,
        categoryCode: String?,
        interfaceName: String? = "Anon interface"
    ): Int {
        val realClassifyCode = classifyCode ?: CLASSIFYCODE_OTHER
        val realCategoryCode = categoryCode ?: CATEGORY_PIPELINE_JOB
        logger.info("$interfaceName:transferImage:==========$projectCode===========")
        logger.info("$interfaceName:transferImage:Input($userId,$projectCode)")
        // 0.鉴权：管理员才有权限操作
        if (!imageAdminUsers.contains(userId.trim())) {
            throw PermissionForbiddenException("Permission Denied,userId=$userId")
        }
        var changedCount = 0
        // 1.调image接口获取项目构建镜像信息
        val dockerBuildImageList =
            client.get(ServiceImageResource::class).listDockerBuildImages(userId, projectCode)
                .data // linux环境第三方镜像
        logger.info("$interfaceName:transferImage:Inner(dockerBuildImageList?.size=${dockerBuildImageList?.size})")
        logger.info("$interfaceName:transferImage:Inner(dockerBuildImageList=${dockerBuildImageList?.map { it.image }})")
        changedCount += transferImageList(
            projectCode = projectCode,
            realClassifyCode = realClassifyCode,
            realCategoryCode = realCategoryCode,
            // Docker镜像默认在Devnet与IDC均可使用
            agentTypeList = listOf(ImageAgentTypeEnum.DOCKER, ImageAgentTypeEnum.IDC),
            imageList = dockerBuildImageList!!,
            interfaceName = interfaceName
        )
        // 1.调image接口获取项目构建镜像信息
        val devCloudImageList =
            client.get(ServiceImageResource::class).listDevCloudImages(userId, projectCode, false)
                .data // linux环境第三方镜像
        logger.info("$interfaceName:transferImage:Inner(devCloudImageList?.size=${devCloudImageList?.size})")
        logger.info("$interfaceName:transferImage:Inner(devCloudImageList=${devCloudImageList?.map { it.image }})")
        changedCount += transferImageList(
            projectCode = projectCode,
            realClassifyCode = realClassifyCode,
            realCategoryCode = realCategoryCode,
            agentTypeList = listOf(ImageAgentTypeEnum.PUBLIC_DEVCLOUD),
            imageList = devCloudImageList!!,
            interfaceName = interfaceName
        )
        return changedCount
    }

    fun transferImageList(
        projectCode: String,
        realClassifyCode: String,
        realCategoryCode: String,
        agentTypeList: List<ImageAgentTypeEnum>,
        imageList: List<DockerTag>,
        interfaceName: String? = "Anon interface"
    ): Int {
        var changedCount = 0
        imageList?.forEach loop@{
            val recordFeature = Triple(projectCode, it.image ?: "", it.modified ?: "")
            logger.info("$interfaceName:transferImage:Inner:Start to transefer:($projectCode,${it.image})")
            if (finishedSet.contains(recordFeature)) {
                logger.info("$interfaceName:transferImage:Inner:already transfered:($projectCode,${it.image})")
                return@loop
            } else {
                finishedSet.add(recordFeature)
                ++changedCount
            }
            // 2.调project接口获取项目负责人信息
            val projectInfo =
                client.get(ServiceProjectResource::class).listByProjectCode(setOf(projectCode)).data?.get(0)
                    ?: throw DataConsistencyException(
                        srcData = "projectCode=$projectCode",
                        targetData = "projectDetail",
                        message = "Fail to get projectDetail by projectCode($projectCode)"
                    )
            val creator = projectInfo.creator
            // 3.镜像名称生成
            val fromProjectClassify = classifyDao.getClassifyByCode(
                dslContext = dslContext,
                classifyCode = realClassifyCode,
                type = StoreTypeEnum.IMAGE
            )
            val index = it.repo!!.lastIndexOf("/")
            var imageName = if (-1 != index) {
                it.repo!!.substring(index + 1)
            } else {
                it.repo
            }
            // 重复处理
            var imageNameNum = 0
            var tempImageName = imageName
            while (imageDao.countByName(dslContext, tempImageName) > 0) {
                logger.warn("$interfaceName:transferImage:Inner:imageName $tempImageName already exists")
                if (imageDao.countByNameAndClassifyId(dslContext, tempImageName, fromProjectClassify!!.id) > 0) {
                    // 该条数据已迁移过，不再处理
                    logger.warn("$interfaceName:transferImage:Inner:imageName $tempImageName already processed")
                    return@loop
                }
                imageNameNum += 1
                tempImageName = imageName + "_$imageNameNum"
            }
            imageName = tempImageName
            // 4.获取tag
            val imageTag = it.tag ?: "latest"
            // 5.镜像标识生成
            var imageCode =
                it.repo!!.removePrefix("/").removeSuffix("/").replace("paas/bkdevops/", "").replace("/", "_")
            // 重复处理
            var imageCodeNum = 0
            var tempImageCode = imageCode
            while (imageDao.countByCode(dslContext, tempImageCode) > 0) {
                logger.warn("$interfaceName:transferImage:Inner:imageCode $tempImageCode already exists")
                imageCodeNum += 1
                tempImageCode = imageCode + "_$imageCodeNum"
            }
            imageCode = tempImageCode
            // 6.调OP新增镜像接口
            // image字段是含repoUrl、repoName、tag的完整字段
            logger.info("$interfaceName:transferImage:Inner:ImageCreateRequest($creator,$projectCode,$imageName,$imageCode,${it.image},${it.repo},$imageTag)")
            val imageId = opImageService.addImage(
                accessToken = "",
                userId = creator!!,
                imageCreateRequest = ImageCreateRequest(
                    projectCode = projectCode,
                    imageName = imageName!!,
                    imageCode = imageCode,
                    classifyCode = realClassifyCode,
                    category = realCategoryCode,
                    agentTypeScope = agentTypeList,
                    version = "1.0.0",
                    releaseType = ReleaseTypeEnum.NEW,
                    versionContent = "容器镜像商店上线，历史镜像数据自动生成",
                    imageSourceType = ImageType.BKDEVOPS,
                    imageRepoUrl = it.image!!.split("/")[0],
                    imageRepoName = it.repo!!,
                    ticketId = null,
                    imageTag = imageTag,
                    logoUrl = "http://radosgw.open.oa.com/paas_backend/ieod/prod/file/png/random_15755397330026456632033301754111.png?v=1575539733",
                    summary = "旧版的构建镜像，通过拷贝为构建镜像入口生成。\n" +
                        "已自动转换为容器镜像商店数据，请项目管理员在研发商店工作台进行管理。",
                    description = "旧版的构建镜像，通过蓝盾版本仓库“拷贝为构建镜像”入口生成。\n" +
                        "容器镜像商店上线后，旧版入口已下线。因历史原因，此类镜像没有办法对应到实际的镜像推送人，暂时先挂到项目管理员名下。\n" +
                        "项目管理员可在研发商店工作台进行上架/升级/下架等操作，或者交接给实际负责人进行管理。",
                    publisher = creator,
                    labelIdList = null
                ),
                checkLatest = false,
                needAuth = false,
                // 批量迁移不发送通知，避免打扰
                sendCheckResultNotify = false
            ).data
            logger.info("$interfaceName:transferImage:Inner:imageId=$imageId")
            logger.info("$interfaceName:transferImage:Output:Start validating")
            // 将项目所有管理员添加为镜像管理员
            val managers = client.get(ServiceTxProjectResource::class).getProjectManagers(projectCode).data
            if (managers != null) {
                imageMemberService.add(creator, StoreMemberReq(managers, StoreMemberTypeEnum.ADMIN, imageCode), StoreTypeEnum.IMAGE)
            }
        }
        return changedCount
    }
}