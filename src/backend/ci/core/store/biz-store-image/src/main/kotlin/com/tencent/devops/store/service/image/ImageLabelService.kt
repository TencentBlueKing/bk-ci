package com.tencent.devops.store.service.image

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.store.dao.image.Constants.KEY_CREATE_TIME
import com.tencent.devops.store.dao.image.Constants.KEY_LABEL_CODE
import com.tencent.devops.store.dao.image.Constants.KEY_LABEL_ID
import com.tencent.devops.store.dao.image.Constants.KEY_LABEL_NAME
import com.tencent.devops.store.dao.image.Constants.KEY_LABEL_TYPE
import com.tencent.devops.store.dao.image.Constants.KEY_UPDATE_TIME
import com.tencent.devops.store.dao.image.ImageLabelRelDao
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class ImageLabelService @Autowired constructor(
    private val dslContext: DSLContext,
    private val imageLabelRelDao: ImageLabelRelDao
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
            imageLabelList.add(
                Label(
                    id = it[KEY_LABEL_ID] as String,
                    labelCode = it[KEY_LABEL_CODE] as String,
                    labelName = it[KEY_LABEL_NAME] as String,
                    labelType = StoreTypeEnum.getStoreType((it[KEY_LABEL_TYPE] as Byte).toInt()),
                    createTime = (it[KEY_CREATE_TIME] as LocalDateTime).timestampmilli(),
                    updateTime = (it[KEY_UPDATE_TIME] as LocalDateTime).timestampmilli()
                )
            )
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
