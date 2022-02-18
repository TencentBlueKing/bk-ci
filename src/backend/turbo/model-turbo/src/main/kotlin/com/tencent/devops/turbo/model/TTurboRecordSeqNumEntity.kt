package com.tencent.devops.turbo.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document("t_turbo_record_seq_num_entity")
data class TTurboRecordSeqNumEntity(

    @Id
    var id: String? = null,

    @Field("project_id")
    @Indexed(background = true)
    val projectId: String? = null,

    @Field("seq_num")
    val seqNum: Int = 0
)
