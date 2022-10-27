package com.tencent.devops.turbo.dao.repository

import com.tencent.devops.turbo.model.TTurboWorkStatsEntity
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
interface TurboWorkStatsRepository : MongoRepository<TTurboWorkStatsEntity, String>
