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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.service.template.impl

import com.tencent.devops.common.api.constant.APPROVE
import com.tencent.devops.common.api.constant.BEGIN
import com.tencent.devops.common.api.constant.COMMIT
import com.tencent.devops.common.api.constant.DOING
import com.tencent.devops.common.api.constant.END
import com.tencent.devops.common.api.constant.FAIL
import com.tencent.devops.common.api.constant.NUM_FOUR
import com.tencent.devops.common.api.constant.NUM_ONE
import com.tencent.devops.common.api.constant.NUM_THREE
import com.tencent.devops.common.api.constant.NUM_TWO
import com.tencent.devops.common.api.constant.SUCCESS
import com.tencent.devops.common.api.constant.UNDO
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.pojo.common.ReleaseProcessItem
import com.tencent.devops.store.pojo.common.enums.DeptStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.template.enums.TemplateStatusEnum
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import com.tencent.devops.store.service.template.TemplateVisibleDeptService
import com.tencent.devops.store.service.template.TxTemplateReleaseService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxTemplateReleaseServiceImpl : TxTemplateReleaseService, TemplateReleaseServiceImpl() {

    @Autowired
    private lateinit var storeVisibleDeptService: StoreVisibleDeptService

    @Autowired
    private lateinit var templateVisibleDeptService: TemplateVisibleDeptService

    private val logger = LoggerFactory.getLogger(TxTemplateReleaseServiceImpl::class.java)

    override fun handleProcessInfo(isNormalUpgrade: Boolean, status: Int): List<ReleaseProcessItem> {
        logger.info("handleProcessInfo isNormalUpgrade: $isNormalUpgrade,status: $status")
        val processInfo = initProcessInfo(isNormalUpgrade)
        val totalStep = if (isNormalUpgrade) NUM_THREE else NUM_FOUR
        when (status) {
            TemplateStatusEnum.INIT.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_TWO, DOING)
            }
            TemplateStatusEnum.AUDITING.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_THREE, DOING)
            }
            TemplateStatusEnum.AUDIT_REJECT.status -> {
                storeCommonService.setProcessInfo(processInfo, totalStep, NUM_THREE, FAIL)
            }
            TemplateStatusEnum.RELEASED.status -> {
                val currStep = if (isNormalUpgrade) NUM_THREE else NUM_FOUR
                storeCommonService.setProcessInfo(processInfo, totalStep, currStep, SUCCESS)
            }
        }
        logger.info("handleProcessInfo processInfo: $processInfo")
        return processInfo
    }

    override fun validateTemplateVisibleDept(templateCode: String) {
        val templateDeptInfos = storeVisibleDeptService.getVisibleDept(
            storeCode = templateCode,
            storeType = StoreTypeEnum.TEMPLATE,
            deptStatus = DeptStatusEnum.APPROVED
        ).data?.deptInfos
        templateVisibleDeptService.validateTemplateVisibleDept(
            templateCode = templateCode,
            deptInfos = templateDeptInfos
        )
    }

    /**
     * 初始化进度
     */
    private fun initProcessInfo(isNormalUpgrade: Boolean): List<ReleaseProcessItem> {
        val processInfo = mutableListOf<ReleaseProcessItem>()
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
            messageCode = BEGIN,
            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())), BEGIN, NUM_ONE, SUCCESS))
        processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
            messageCode = COMMIT,
            language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())), COMMIT, NUM_TWO, UNDO))
        if (isNormalUpgrade) {
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
                messageCode = END,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())), END, NUM_THREE, UNDO))
        } else {
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
                messageCode = APPROVE,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())), APPROVE, NUM_THREE, UNDO))
            processInfo.add(ReleaseProcessItem(I18nUtil.getCodeLanMessage(
                messageCode = END,
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())), END, NUM_FOUR, UNDO))
        }
        return processInfo
    }
}
