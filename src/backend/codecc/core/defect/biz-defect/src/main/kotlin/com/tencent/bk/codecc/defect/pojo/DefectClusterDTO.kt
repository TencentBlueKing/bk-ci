package com.tencent.bk.codecc.defect.pojo

import com.tencent.bk.codecc.defect.model.BuildEntity
import com.tencent.bk.codecc.defect.model.TransferAuthorEntity
import com.tencent.bk.codecc.defect.vo.CommitDefectVO

data class DefectClusterDTO(
    val commitDefectVO: CommitDefectVO,
    val buildEntity : BuildEntity? = null,
    val transferAuthorList : List<TransferAuthorEntity.TransferAuthorPair>?,
    var inputFileName :String,
    var inputFilePath :String,
)
