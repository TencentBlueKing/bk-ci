package com.tencent.devops.project.service

import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.project.dao.FavoriteDao
import com.tencent.devops.project.dao.ServiceDao
import com.tencent.devops.project.dao.ServiceTypeDao
import com.tencent.devops.project.service.impl.AbsUserProjectServiceServiceImpl
import org.jooq.DSLContext
import org.springframework.stereotype.Service

@Service
class SampleUserProjectServiceServiceImpl(
    dslContext: DSLContext,
    serviceTypeDao: ServiceTypeDao,
    serviceDao: ServiceDao,
    favoriteDao: FavoriteDao,
    gray: Gray,
    redisOperation: RedisOperation
) : AbsUserProjectServiceServiceImpl(dslContext, serviceTypeDao, serviceDao, favoriteDao, gray, redisOperation)