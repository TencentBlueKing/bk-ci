/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
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

package com.tencent.devops.store.service.image

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.DataConsistencyException
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.model.store.tables.records.TImageRecord
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.USER_IMAGE_VERSION_NOT_EXIST
import com.tencent.devops.store.dao.common.CategoryDao
import com.tencent.devops.store.dao.common.ClassifyDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.dao.image.Constants
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_PUBLIC_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_FEATURE_RECOMMEND_FLAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_ID
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_LOGO_URL
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_RD_TYPE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_REPO_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_REPO_URL
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SIZE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SOURCE_TYPE
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_STATUS
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_SUMMARY
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_TAG
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_VERSION
import com.tencent.devops.store.dao.image.ImageAgentTypeDao
import com.tencent.devops.store.dao.image.ImageCategoryRelDao
import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.dao.image.ImageFeatureDao
import com.tencent.devops.store.dao.image.ImageVersionLogDao
import com.tencent.devops.store.dao.image.MarketImageDao
import com.tencent.devops.store.dao.image.MarketImageFeatureDao
import com.tencent.devops.store.exception.image.CategoryNotExistException
import com.tencent.devops.store.exception.image.ImageNotExistException
import com.tencent.devops.store.pojo.common.HOTTEST
import com.tencent.devops.store.pojo.common.KEY_CATEGORY_CODE
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_ID
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_CREATOR
import com.tencent.devops.store.pojo.common.KEY_MODIFIER
import com.tencent.devops.store.pojo.common.KEY_PUBLISHER
import com.tencent.devops.store.pojo.common.KEY_PUB_TIME
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.LATEST
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.STORE_IMAGE_STATUS
import com.tencent.devops.store.pojo.common.VersionInfo
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageRDTypeEnum
import com.tencent.devops.store.pojo.image.enums.ImageStatusEnum
import com.tencent.devops.store.pojo.image.enums.MarketImageSortTypeEnum
import com.tencent.devops.store.pojo.image.exception.UnknownImageSourceType
import com.tencent.devops.store.pojo.image.request.ImageBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.image.request.ImageFeatureUpdateRequest
import com.tencent.devops.store.pojo.image.response.*
import com.tencent.devops.store.service.common.ClassifyService
import com.tencent.devops.store.service.common.StoreCommentService
import com.tencent.devops.store.service.common.StoreMemberService
import com.tencent.devops.store.service.common.StoreUserService
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.math.ceil

@RefreshScope
@Service
abstract class ImagePipelineService @Autowired constructor() {
    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var imageDao: ImageDao
    @Autowired
    lateinit var imageCategoryRelDao: ImageCategoryRelDao
    @Autowired
    lateinit var classifyDao: ClassifyDao
    @Autowired
    lateinit var categoryDao: CategoryDao
    @Autowired
    lateinit var imageFeatureDao: ImageFeatureDao
    @Autowired
    lateinit var imageAgentTypeDao: ImageAgentTypeDao
    @Autowired
    lateinit var imageVersionLogDao: ImageVersionLogDao
    @Autowired
    lateinit var marketImageDao: MarketImageDao
    @Autowired
    lateinit var marketImageFeatureDao: MarketImageFeatureDao
    @Autowired
    lateinit var storeMemberDao: StoreMemberDao
    @Autowired
    lateinit var storeProjectRelDao: StoreProjectRelDao
    @Autowired
    lateinit var storeStatisticDao: StoreStatisticDao
    @Autowired
    lateinit var imageCommonService: ImageCommonService
    @Autowired
    lateinit var storeCommentService: StoreCommentService
    @Autowired
    lateinit var storeUserService: StoreUserService
    @Autowired
    @Qualifier("imageMemberService")
    lateinit var storeMemberService: StoreMemberService
    @Autowired
    lateinit var classifyService: ClassifyService
    @Autowired
    lateinit var supportService: SupportService
    @Autowired
    lateinit var marketImageStatisticService: MarketImageStatisticService
    @Autowired
    lateinit var imageLabelService: ImageLabelService
    @Autowired
    lateinit var imageCategoryService: ImageCategoryService
    @Autowired
    lateinit var client: Client

    @Value("\${store.baseImageDocsLink}")
    private lateinit var baseImageDocsLink: String

    private val logger = LoggerFactory.getLogger(ImagePipelineService::class.java)

    fun getDefaultStoreImage(userId: String, agentType: ImageAgentTypeEnum): SimpleImageInfo {
        //TODO
        return SimpleImageInfo("tlinux2_2", "TLinux2.2公共镜像", "1.*")
    }
}
