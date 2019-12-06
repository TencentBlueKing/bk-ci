package com.tencent.devops.store.service.image

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.store.dao.image.ImageLabelRelDao
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
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
                    id = it["id"] as String,
                    labelCode = it["labelCode"] as String,
                    labelName = it["labelName"] as String,
                    labelType = StoreTypeEnum.getStoreType((it["labelType"] as Byte).toInt()),
                    createTime = (it["createTime"] as LocalDateTime).timestampmilli(),
                    updateTime = (it["updateTime"] as LocalDateTime).timestampmilli()
                )
            )
        }
        return Result(imageLabelList)
    }
}
