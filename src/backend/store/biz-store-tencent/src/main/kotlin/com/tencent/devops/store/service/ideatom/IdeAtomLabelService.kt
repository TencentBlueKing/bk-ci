package com.tencent.devops.store.service.ideatom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.store.dao.ideatom.IdeAtomLabelRelDao
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class IdeAtomLabelService @Autowired constructor(
    private val dslContext: DSLContext,
    private val ideAtomLabelRelDao: IdeAtomLabelRelDao
) {
    private val logger = LoggerFactory.getLogger(IdeAtomLabelService::class.java)

    /**
     * 查找IDE插件标签
     */
    fun getLabelsByAtomId(atomId: String): Result<List<Label>?> {
        logger.info("the atomId is :$atomId")
        val ideAtomLabelList = mutableListOf<Label>()
        val ideAtomLabelRecords = ideAtomLabelRelDao.getLabelsByAtomId(dslContext, atomId) // 查询IDE插件标签信息
        ideAtomLabelRecords?.forEach {
            ideAtomLabelList.add(
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
        return Result(ideAtomLabelList)
    }
}
