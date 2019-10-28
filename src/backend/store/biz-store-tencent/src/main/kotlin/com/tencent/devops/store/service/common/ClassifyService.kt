package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.dao.common.ClassifyDao
import com.tencent.devops.store.pojo.common.Classify
import com.tencent.devops.store.pojo.common.ClassifyRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 分类业务逻辑类
 * author: carlyin
 * since: 2018-12-20
 */
@Service
class ClassifyService {

    private val logger = LoggerFactory.getLogger(ClassifyService::class.java)

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var classifyDao: ClassifyDao

    /**
     * 获取所有分类信息
     * @param type 0:原子 1：模板
     */
    fun getAllClassify(type: Byte): Result<List<Classify>> {
        val classifyList = classifyDao.getAllClassify(dslContext, type).map { classifyDao.convert(it) }
        return Result(classifyList)
    }

    /**
     * 根据id获取分类信息
     */
    fun getClassify(id: String): Result<Classify?> {
        val classifyRecord = classifyDao.getClassify(dslContext, id)
        logger.info("the pipelineContainerRecord is :{}", classifyRecord)
        return Result(if (classifyRecord == null) {
            null
        } else {
            classifyDao.convert(classifyRecord)
        })
    }

    /**
     * 保存分类信息
     */
    fun saveClassify(classifyRequest: ClassifyRequest, type: Byte): Result<Boolean> {
        logger.info("the save classifyRequest is:$classifyRequest,type is:$type")
        val classifyCode = classifyRequest.classifyCode
        // 判断分类代码是否存在
        val codeCount = classifyDao.countByCode(dslContext, classifyCode, type)
        if (codeCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(classifyCode), false)
        }
        val classifyName = classifyRequest.classifyName
        // 判断分类名称是否存在
        val nameCount = classifyDao.countByName(dslContext, classifyName, type)
        if (nameCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(classifyName), false)
        }
        val id = UUIDUtil.generate()
        classifyDao.add(dslContext, id, classifyRequest, type)
        return Result(true)
    }

    /**
     * 更新分类信息
     */
    fun updateClassify(id: String, classifyRequest: ClassifyRequest, type: Byte): Result<Boolean> {
        logger.info("the update id is :$id,the update classifyRequest is:$classifyRequest,type is:$type")
        val classifyCode = classifyRequest.classifyCode
        // 判断分类是否存在
        val codeCount = classifyDao.countByCode(dslContext, classifyCode, type)
        if (codeCount > 0) {
            // 判断更新分类名称是否属于自已
            val classify = classifyDao.getClassify(dslContext, id)
            if (null != classify && classifyCode != classify.classifyCode) {
                // 抛出错误提示
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(classifyCode), false)
            }
        }
        val classifyName = classifyRequest.classifyName
        // 判断类型分类是否存在
        val count = classifyDao.countByName(dslContext, classifyName, type)
        if (count > 0) {
            // 判断更新的分类名称是否属于自已
            val classify = classifyDao.getClassify(dslContext, id)
            if (null != classify && classifyName != classify.classifyName) {
                // 抛出错误提示
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(classifyName), false)
            }
        }
        classifyDao.update(dslContext, id, classifyRequest)
        return Result(true)
    }

    /**
     * 根据id删除分类信息
     */
    fun deleteClassify(id: String): Result<Boolean> {
        logger.info("the delete id is :$id")
        val classifyRecord = classifyDao.getClassify(dslContext, id)
        var flag = false
        if (null != classifyRecord) {
            val classifyType = classifyRecord.type
            val classifyService = getStoreClassifyService(StoreTypeEnum.getStoreType(classifyType.toInt()))
            flag = classifyService.getDeleteClassifyFlag(id)
        }
        if (flag) {
            classifyDao.delete(dslContext, id)
        } else {
            return MessageCodeUtil.generateResponseDataObject(StoreMessageCode.USER_CLASSIFY_IS_NOT_ALLOW_DELETE, false)
        }
        return Result(true)
    }

    private fun getStoreClassifyService(storeType: String): AbstractClassifyService {
        return SpringContextUtil.getBean(AbstractClassifyService::class.java, "${storeType}_CLASSIFY_SERVICE")
    }
}
