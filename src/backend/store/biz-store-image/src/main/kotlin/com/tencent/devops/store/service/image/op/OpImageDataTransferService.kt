/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.tencent.devops.store.service.image.op

import com.tencent.devops.common.api.exception.DataConsistencyException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.image.api.ServiceImageResource
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.dao.common.ClassifyDao
import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.pojo.common.ClassifyRequest
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.request.ApproveImageReq
import com.tencent.devops.store.pojo.image.request.ImageCreateRequest
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class OpImageDataTransferService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val imageDao: ImageDao,
    private val classifyDao: ClassifyDao,
    private val opImageService: OpImageService
) {
    companion object {
        const val CLASSIFYCODE_FROM_PROJECT_BUILD_IMAGE = "from_project_build_image"
    }

    private val logger = LoggerFactory.getLogger(OpImageDataTransferService::class.java)

    @Value("\${store.imageAdminUsers}")
    private val imageAdminUsersStr: String? = "fayewang;carlyin;jsonwan"

    private val finishedSet: HashSet<Triple<String, String, String>> = HashSet()

    private lateinit var imageAdminUsers: Set<String>

    /**
     * 生成数据迁移专有镜像分类
     */
    fun createSystemInitClassify() {
        if (null == classifyDao.getClassifyByCode(
                dslContext,
                CLASSIFYCODE_FROM_PROJECT_BUILD_IMAGE, StoreTypeEnum.IMAGE
            )
        ) {
            classifyDao.add(
                dslContext = dslContext,
                id = UUIDUtil.generate(),
                classifyRequest = ClassifyRequest(
                    classifyCode = CLASSIFYCODE_FROM_PROJECT_BUILD_IMAGE,
                    classifyName = "项目构建镜像导入",
                    weight = null
                ),
                type = StoreTypeEnum.IMAGE.type.toByte()
            )
        }
    }

    @PostConstruct
    fun init() {
        imageAdminUsers = imageAdminUsersStr!!.trim().split(";").toSet()
        logger.info("imageAdminUsers= $imageAdminUsers")
        createSystemInitClassify()
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
        interfaceName: String? = "Anon interface"
    ): Int {
        logger.info("$interfaceName:transferImage:==========$projectCode===========")
        logger.info("$interfaceName:transferImage:Input($userId,$projectCode)")
        // 0.鉴权：管理员才有权限操作
        if (!imageAdminUsers.contains(userId.trim())) {
            throw PermissionForbiddenException("Permission Denied,userId=$userId")
        }
        // 1.调image接口获取项目构建镜像信息
        val dockerBuildImageList =
            client.get(ServiceImageResource::class).listDockerBuildImages(userId, projectCode)
                .data // linux环境第三方镜像
        logger.info("$interfaceName:transferImage:Inner(dockerBuildImageList?.size=${dockerBuildImageList?.size})")
        var changedCount = 0
        dockerBuildImageList?.forEach loop@{
            val recordFeature = Triple(projectCode, it.image ?: "", it.modified ?: "")
            if (finishedSet.contains(recordFeature)) {
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
                imageNameNum += 1
                tempImageName = imageName + "_$imageNameNum"
            }
            imageName = tempImageName
            // 4.获取tag
            val imageTag = it.tag ?: "latest"
            // 5.镜像标识生成
            var imageCode = it.repo!!.removePrefix("/").removeSuffix("/").replace("paas/bkdevops", "").replace("/", "_")
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
                    classifyCode = CLASSIFYCODE_FROM_PROJECT_BUILD_IMAGE,
                    version = "1.0.0",
                    releaseType = ReleaseTypeEnum.NEW,
                    versionContent = "系统根据已拷贝的构建镜像自动生成",
                    imageSourceType = ImageType.BKDEVOPS,
                    imageRepoUrl = it.image!!.split("/")[0],
                    // 路径切回项目镜像
                    imageRepoName = it.repo!!.replace("paas/bkdevops", "paas"),
                    ticketId = null,
                    imageTag = imageTag,
                    logoUrl = "http://radosgw.open.oa.com/paas_backend/ieod/dev/file/png/random_1564495644412278667612475788088.png?v=1564495644",
                    summary = "系统根据已拷贝的构建镜像自动生成",
                    description = "系统根据已拷贝的构建镜像自动生成，可在项目流水线中选择使用，初始状态为仅本关联项目可见，拷贝的原始构建镜像地址与版本为：${it.image}",
                    publisher = creator,
                    labelIdList = null
                ),
                checkLatest = false,
                needAuth = false
            ).data
            logger.info("$interfaceName:transferImage:Inner:imageId=$imageId")
            // 7.调审核（无通知）接口
            opImageService.approveImageWithoutNotify(
                userId = userId,
                imageId = imageId!!,
                approveImageReq = ApproveImageReq(
                    imageCode = imageCode,
                    publicFlag = false,
                    recommendFlag = false,
                    certificationFlag = false,
                    weight = null,
                    result = "PASS",
                    message = "数据迁移自动通过"
                )
            )
            logger.info("$interfaceName:transferImage:Output:Approve PASS")
        }
        return changedCount
    }
}