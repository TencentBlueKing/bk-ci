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

import com.tencent.devops.common.client.Client
import com.tencent.devops.store.dao.common.CategoryDao
import com.tencent.devops.store.dao.common.ClassifyDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StoreProjectRelDao
import com.tencent.devops.store.dao.common.StoreStatisticDao
import com.tencent.devops.store.dao.image.ImageAgentTypeDao
import com.tencent.devops.store.dao.image.ImageCategoryRelDao
import com.tencent.devops.store.dao.image.ImageDao
import com.tencent.devops.store.dao.image.ImageFeatureDao
import com.tencent.devops.store.dao.image.ImageVersionLogDao
import com.tencent.devops.store.dao.image.MarketImageDao
import com.tencent.devops.store.dao.image.MarketImageFeatureDao
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.response.*
import com.tencent.devops.store.service.common.ClassifyService
import com.tencent.devops.store.service.common.StoreCommentService
import com.tencent.devops.store.service.common.StoreMemberService
import com.tencent.devops.store.service.common.StoreUserService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service

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

    fun getDefaultStoreImage(userId: String, agentType: ImageAgentTypeEnum): BaseImageInfo {
        //TODO
        return BaseImageInfo("tlinux2_2", "TLinux2.2公共镜像", "1.*", true)
    }
}
