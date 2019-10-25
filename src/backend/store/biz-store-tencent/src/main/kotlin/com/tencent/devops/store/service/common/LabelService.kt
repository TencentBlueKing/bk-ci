package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.common.LabelDao
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.LabelRequest
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 标签业务逻辑类
 * author: carlyin
 * since: 2019-03-22
 */
@Service
class LabelService @Autowired constructor(
    private val dslContext: DSLContext,
    private val labelDao: LabelDao
) {
    private val logger = LoggerFactory.getLogger(LabelService::class.java)

    /**
     * 获取所有标签信息
     * @param type 0:原子 1：模板
     */
    fun getAllLabel(type: Byte): Result<List<Label>?> {
        val atomLabelList = labelDao.getAllLabel(dslContext, type)?.map { labelDao.convert(it) }
        return Result(atomLabelList)
    }

    /**
     * 根据id获取标签信息
     */
    fun getLabel(id: String): Result<Label?> {
        val labelRecord = labelDao.getLabel(dslContext, id)
        logger.info("the pipelineContainerRecord is :{}", labelRecord)
        return Result(if (labelRecord == null) {
            null
        } else {
            labelDao.convert(labelRecord)
        })
    }

    /**
     * 保存标签信息
     */
    fun saveLabel(labelRequest: LabelRequest, type: Byte): Result<Boolean> {
        logger.info("the save LabelRequest is:$labelRequest,type is:$type")
        val labelCode = labelRequest.labelCode
        // 判断标签代码是否存在
        val codeCount = labelDao.countByCode(dslContext, labelCode, type)
        if (codeCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(labelCode), false)
        }
        val labelName = labelRequest.labelName
        // 判断标签名称是否存在
        val nameCount = labelDao.countByName(dslContext, labelName, type)
        if (nameCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(labelName), false)
        }
        val id = UUIDUtil.generate()
        labelDao.add(dslContext, id, labelRequest, type)
        return Result(true)
    }

    /**
     * 更新标签信息
     */
    fun updateLabel(id: String, labelRequest: LabelRequest, type: Byte): Result<Boolean> {
        logger.info("the update id is :$id,the update labelRequest is:$labelRequest,type is:$type")
        val labelCode = labelRequest.labelCode
        // 判断标签是否存在
        val codeCount = labelDao.countByCode(dslContext, labelCode, type)
        if (codeCount > 0) {
            // 判断更新标签名称是否属于自已
            val label = labelDao.getLabel(dslContext, id)
            if (null != label && labelCode != label.labelCode) {
                // 抛出错误提示
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(labelCode), false)
            }
        }
        val labelName = labelRequest.labelName
        // 判断类型标签是否存在
        val count = labelDao.countByName(dslContext, labelName, type)
        if (count > 0) {
            // 判断更新的标签名称是否属于自已
            val label = labelDao.getLabel(dslContext, id)
            if (null != label && labelName != label.labelName) {
                // 抛出错误提示
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(labelName), false)
            }
        }
        labelDao.update(dslContext, id, labelRequest)
        return Result(true)
    }

    /**
     * 根据id删除标签信息
     */
    fun deleteLabel(id: String): Result<Boolean> {
        logger.info("the delete id is :$id")
        dslContext.transaction { t ->
            val context = DSL.using(t)
            labelDao.delete(context, id)
        }
        return Result(true)
    }
}
