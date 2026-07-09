/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.common.dao.LabelDao
import com.tencent.devops.store.common.service.LabelService
import com.tencent.devops.store.pojo.common.KEY_CREATE_TIME
import com.tencent.devops.store.pojo.common.KEY_LABEL_CODE
import com.tencent.devops.store.pojo.common.KEY_LABEL_ID
import com.tencent.devops.store.pojo.common.KEY_LABEL_NAME
import com.tencent.devops.store.pojo.common.KEY_LABEL_TYPE
import com.tencent.devops.store.pojo.common.KEY_SERVICE_SCOPE
import com.tencent.devops.store.pojo.common.KEY_UPDATE_TIME
import com.tencent.devops.store.pojo.common.enums.ServiceScopeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.label.Label
import com.tencent.devops.store.pojo.common.label.LabelRequest
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

/**
 * 标签业务逻辑类
 *
 * since: 2019-03-22
 */
@Suppress("ALL")
@Service
class LabelServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val labelDao: LabelDao
) : LabelService {

    /**
     * 获取所有标签信息
     * @param type 类型
     * @param serviceScope 服务范围
     */
    override fun getAllLabel(type: Byte, serviceScope: ServiceScopeEnum?): Result<List<Label>?> {
        val atomLabelList = labelDao.getAllLabel(dslContext, type, serviceScope)?.map { labelDao.convert(it) }
        atomLabelList?.sortBy { it.labelName }
        return Result(atomLabelList)
    }

    /**
     * 根据id获取标签信息
     */
    override fun getLabel(id: String): Result<Label?> {
        val labelRecord = labelDao.getLabel(dslContext, id)
        return Result(
            if (labelRecord == null) {
                null
            } else {
                labelDao.convert(labelRecord)
            }
        )
    }

    /**
     * 保存标签信息
     */
    override fun saveLabel(labelRequest: LabelRequest, type: Byte): Result<Boolean> {
        // 校验标签代码和名称的唯一性
        val checkResult = checkLabelUniqueness(
            labelCode = labelRequest.labelCode,
            labelName = labelRequest.labelName,
            type = type,
            serviceScope = labelRequest.serviceScope
        )
        if (checkResult != null) return checkResult
        val id = UUIDUtil.generate()
        labelDao.add(dslContext, id, labelRequest, type)
        return Result(true)
    }

    /**
     * 更新标签信息
     */
    override fun updateLabel(id: String, labelRequest: LabelRequest, type: Byte): Result<Boolean> {
        // 复用公共校验方法，传入excludeId排除自身记录
        val checkResult = checkLabelUniqueness(
            labelCode = labelRequest.labelCode,
            labelName = labelRequest.labelName,
            type = type,
            serviceScope = labelRequest.serviceScope,
            excludeId = id
        )
        if (checkResult != null) return checkResult
        labelDao.update(dslContext, id, labelRequest)
        return Result(true)
    }

    /**
     * 校验标签代码和名称的唯一性（同时支持新增和更新场景）
     * @param excludeId 更新场景下传入当前记录ID，排除自身；新增场景传null
     * @return 如果存在冲突返回错误Result，否则返回null
     */
    private fun checkLabelUniqueness(
        labelCode: String,
        labelName: String,
        type: Byte,
        serviceScope: ServiceScopeEnum?,
        excludeId: String? = null
    ): Result<Boolean>? {
        // 更新场景下查询已有记录，用于判断值是否发生变化
        val existingLabel = excludeId?.let { labelDao.getLabel(dslContext, it) }
        // 判断标签代码是否已存在（更新场景下仅在值变化时校验）
        if (existingLabel == null || labelCode != existingLabel.labelCode) {
            val codeCount = labelDao.countByCode(
                dslContext = dslContext,
                labelCode = labelCode,
                type = type,
                serviceScope = serviceScope
            )
            if (codeCount > 0) {
                return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                    params = arrayOf(labelCode),
                    data = false,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                )
            }
        }
        // 判断标签名称是否已存在（更新场景下仅在值变化时校验）
        if (existingLabel == null || labelName != existingLabel.labelName) {
            val nameCount = labelDao.countByName(
                dslContext = dslContext,
                labelName = labelName,
                type = type,
                serviceScope = serviceScope
            )
            if (nameCount > 0) {
                return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.PARAMETER_IS_EXIST,
                    params = arrayOf(labelName),
                    data = false,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                )
            }
        }
        return null
    }

    /**
     * 根据id删除标签信息
     */
    override fun deleteLabel(id: String): Result<Boolean> {
        dslContext.transaction { t ->
            val context = DSL.using(t)
            labelDao.delete(context, id)
        }
        return Result(true)
    }

    /**
     * 为标签集合添加标签
     */
    override fun addLabelToLabelList(it: Record, labelList: MutableList<Label>) {
        val labelCode = it[KEY_LABEL_CODE] as String
        val labelType = StoreTypeEnum.getStoreType((it[KEY_LABEL_TYPE] as Byte).toInt())
        val labelName = it[KEY_LABEL_NAME] as String
        val labelLanName = I18nUtil.getCodeLanMessage(
            messageCode = "$labelType.label.$labelCode",
            defaultMessage = labelName
        )
        val serviceScope = it.field(KEY_SERVICE_SCOPE)?.let { field ->
            it.get(field) as? String
        }
        labelList.add(
            Label(
                id = it[KEY_LABEL_ID] as String,
                labelCode = labelCode,
                labelName = labelLanName,
                labelType = labelType,
                serviceScope = serviceScope,
                createTime = (it[KEY_CREATE_TIME] as LocalDateTime).timestampmilli(),
                updateTime = (it[KEY_UPDATE_TIME] as LocalDateTime).timestampmilli()
            )
        )
    }
}
