package com.tencent.devops.store.service.image

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.dao.image.ImageLabelRelDao
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.service.common.LabelService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ImageLabelService @Autowired constructor(
    private val dslContext: DSLContext,
    private val imageLabelRelDao: ImageLabelRelDao,
    private val labelService: LabelService
) {
    private val logger = LoggerFactory.getLogger(ImageLabelService::class.java)

    /**
     * 查找镜像标签
     */
    fun getLabelsByImageId(imageId: String): Result<List<Label>?> {
        logger.info("the imageId is :$imageId")
        val imageLabelList = mutableListOf<Label>()
        val imageLabelRecords = imageLabelRelDao.getLabelsByImageId(dslContext, imageId) // 查询镜像标签信息
        imageLabelRecords?.forEach {
            labelService.addLabelToLabelList(it, imageLabelList)
        }
        return Result(imageLabelList)
    }

    fun updateImageLabels(dslContext: DSLContext, userId: String, imageId: String, labelIdList: List<String>) {
        // 更新标签信息
        dslContext.transaction { configuration ->
            val context = DSL.using(configuration)
            imageLabelRelDao.deleteByImageId(context, imageId)
            if (labelIdList.isNotEmpty()) {
                imageLabelRelDao.batchAdd(
                    dslContext = context,
                    userId = userId,
                    imageId = imageId,
                    labelIdList = labelIdList
                )
            }
        }
    }
}
