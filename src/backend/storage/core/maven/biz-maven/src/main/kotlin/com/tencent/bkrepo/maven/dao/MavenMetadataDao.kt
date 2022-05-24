package com.tencent.bkrepo.maven.dao

import com.tencent.bkrepo.common.mongo.dao.simple.SimpleMongoDao
import com.tencent.bkrepo.maven.model.TMavenMetadataRecord
import org.springframework.stereotype.Repository

@Repository
class MavenMetadataDao : SimpleMongoDao<TMavenMetadataRecord>()
