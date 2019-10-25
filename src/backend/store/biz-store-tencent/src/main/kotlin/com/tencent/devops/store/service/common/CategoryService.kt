package com.tencent.devops.store.service.common

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.store.dao.common.CategoryDao
import com.tencent.devops.store.pojo.common.Category
import com.tencent.devops.store.pojo.common.CategoryRequest
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 范畴业务逻辑类
 * author: carlyin
 * since: 2019-03-22
 */
@Service
class CategoryService @Autowired constructor(
    private val dslContext: DSLContext,
    private val categoryDao: CategoryDao
) {
    private val logger = LoggerFactory.getLogger(CategoryService::class.java)

    /**
     * 获取所有范畴信息
     * @param type 0:原子 1：模板
     */
    fun getAllCategory(type: Byte): Result<List<Category>?> {
        val atomCategoryList = categoryDao.getAllCategory(dslContext, type)?.map { categoryDao.convert(it) }
        return Result(atomCategoryList)
    }

    /**
     * 根据id获取范畴信息
     */
    fun getCategory(id: String): Result<Category?> {
        val categoryRecord = categoryDao.getCategory(dslContext, id)
        logger.info("the pipelineContainerRecord is :{}", categoryRecord)
        return Result(if (categoryRecord == null) {
            null
        } else {
            categoryDao.convert(categoryRecord)
        })
    }

    /**
     * 保存范畴信息
     */
    fun saveCategory(categoryRequest: CategoryRequest, type: Byte): Result<Boolean> {
        logger.info("the save categoryRequest is:$categoryRequest,type is:$type")
        val categoryCode = categoryRequest.categoryCode
        // 判断范畴代码是否存在
        val codeCount = categoryDao.countByCode(dslContext, categoryCode, type)
        if (codeCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(categoryCode), false)
        }
        val categoryName = categoryRequest.categoryName
        // 判断范畴名称是否存在
        val nameCount = categoryDao.countByName(dslContext, categoryName, type)
        if (nameCount > 0) {
            // 抛出错误提示
            return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(categoryName), false)
        }
        val id = UUIDUtil.generate()
        categoryDao.add(dslContext, id, categoryRequest, type)
        return Result(true)
    }

    /**
     * 更新范畴信息
     */
    fun updateCategory(id: String, categoryRequest: CategoryRequest, type: Byte): Result<Boolean> {
        logger.info("the update id is :$id,the update CategoryRequest is:$categoryRequest,type is:$type")
        val categoryCode = categoryRequest.categoryCode
        // 判断范畴是否存在
        val codeCount = categoryDao.countByCode(dslContext, categoryCode, type)
        if (codeCount > 0) {
            // 判断更新范畴名称是否属于自已
            val category = categoryDao.getCategory(dslContext, id)
            if (null != category && categoryCode != category.categoryCode) {
                // 抛出错误提示
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(categoryCode), false)
            }
        }
        val categoryName = categoryRequest.categoryName
        // 判断类型范畴是否存在
        val count = categoryDao.countByName(dslContext, categoryName, type)
        if (count > 0) {
            // 判断更新的范畴名称是否属于自已
            val category = categoryDao.getCategory(dslContext, id)
            if (null != category && categoryName != category.categoryName) {
                // 抛出错误提示
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.PARAMETER_IS_EXIST, arrayOf(categoryName), false)
            }
        }
        categoryDao.update(dslContext, id, categoryRequest)
        return Result(true)
    }

    /**
     * 根据id删除范畴信息
     */
    fun deleteCategory(id: String): Result<Boolean> {
        logger.info("the delete id is :$id")
        dslContext.transaction { t ->
            val context = DSL.using(t)
            categoryDao.delete(context, id)
        }
        return Result(true)
    }
}
