/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.project.service.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.enums.CrudEnum
import com.tencent.devops.common.api.enums.SystemModuleEnum
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ShardingRoutingRule
import com.tencent.devops.common.api.pojo.ShardingRuleTypeEnum
import com.tencent.devops.common.api.util.ShardingUtil
import com.tencent.devops.common.event.pojo.sharding.ShardingRoutingRuleBroadCastEvent
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.project.dao.ShardingRoutingRuleDao
import com.tencent.devops.project.dispatch.ShardingRoutingRuleDispatcher
import com.tencent.devops.project.service.ShardingRoutingRuleService
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import java.util.concurrent.TimeUnit

abstract class AbsShardingRoutingRuleServiceImpl @Autowired constructor(
    val dslContext: DSLContext,
    val redisOperation: RedisOperation,
    private val shardingRoutingRuleDao: ShardingRoutingRuleDao,
    private val shardingRoutingRuleDispatcher: ShardingRoutingRuleDispatcher
) : ShardingRoutingRuleService {
    companion object {
        private val DEFAULT_RULE_REDIS_CACHE_TIME = TimeUnit.DAYS.toSeconds(14) // 分片规则在redis默认缓存时间
        private val logger = LoggerFactory.getLogger(AbsShardingRoutingRuleServiceImpl::class.java)
    }

    /**
     * 添加分片路由规则
     * @param userId 用户ID
     * @param shardingRoutingRule 片路由规则
     * @return 布尔值
     */
    override fun addShardingRoutingRule(userId: String, shardingRoutingRule: ShardingRoutingRule): Boolean {
        return try {
            doAddShardingRoutingRuleIfAbsent(userId, shardingRoutingRule).first
        } catch (ignored: Throwable) {
            logger.warn("Add ShardingRoutingRule failed", ignored)
            false
        }
    }

    /**
     * 添加分片路由规则，若规则已存在则复用已入库的规则
     * @param userId 用户ID
     * @param shardingRoutingRule 待添加的分片路由规则
     * @return 最终生效的分片路由规则
     */
    override fun addShardingRoutingRuleIfAbsent(
        userId: String,
        shardingRoutingRule: ShardingRoutingRule
    ): ShardingRoutingRule {
        return doAddShardingRoutingRuleIfAbsent(userId, shardingRoutingRule).second
    }

    /**
     * @return first-本次调用是否真正把规则写入db，second-最终生效的分片路由规则
     */
    private fun doAddShardingRoutingRuleIfAbsent(
        userId: String,
        shardingRoutingRule: ShardingRoutingRule
    ): Pair<Boolean, ShardingRoutingRule> {
        val key = ShardingUtil.getShardingRoutingRuleKey(
            clusterName = shardingRoutingRule.clusterName,
            moduleCode = shardingRoutingRule.moduleCode.name,
            ruleType = shardingRoutingRule.type.name,
            routingName = shardingRoutingRule.routingName,
            tableName = shardingRoutingRule.tableName
        )
        logger.info("$userId addShardingRoutingRule params: rule:$shardingRoutingRule|" +
                "clusterName:${CommonUtils.getDbClusterName()}")
        val lock = RedisLock(redisOperation, "$key:add", 30)
        try {
            lock.lock()
            // 持锁后重新查库，保证并发分配时所有调用方拿到的都是同一条规则
            getPersistedShardingRoutingRule(shardingRoutingRule)?.let { persistedRule ->
                logger.warn("Sharding routing rule($key) already exists, reuse rule:${persistedRule.routingRule}")
                cacheShardingRoutingRule(key, persistedRule.routingRule)
                return false to persistedRule
            }
            try {
                shardingRoutingRuleDao.add(dslContext, userId, shardingRoutingRule)
            } catch (duplicated: DuplicateKeyException) {
                // redis不可用时RedisLock不保证互斥，此时靠唯一索引兜底，冲突则复用已入库的规则
                val persistedRule = getPersistedShardingRoutingRule(shardingRoutingRule) ?: throw duplicated
                logger.warn("Sharding routing rule($key) duplicated, reuse rule:${persistedRule.routingRule}")
                cacheShardingRoutingRule(key, persistedRule.routingRule)
                return false to persistedRule
            }
            // 入库成功后再操作缓存和事件（避免入库失败导致缓存脏数据）
            cacheShardingRoutingRule(key, shardingRoutingRule.routingRule)
            // 发送规则新增事件消息
            shardingRoutingRuleDispatcher.dispatch(
                ShardingRoutingRuleBroadCastEvent(routingName = key, actionType = CrudEnum.CREATAE)
            )
            return true to shardingRoutingRule
        } finally {
            lock.unlock()
        }
    }

    /**
     * 查询已入库的分片路由规则，db是路由规则的唯一数据源
     */
    private fun getPersistedShardingRoutingRule(shardingRoutingRule: ShardingRoutingRule): ShardingRoutingRule? {
        val record = shardingRoutingRuleDao.get(
            dslContext = dslContext,
            clusterName = shardingRoutingRule.clusterName,
            moduleCode = shardingRoutingRule.moduleCode,
            type = shardingRoutingRule.type,
            routingName = shardingRoutingRule.routingName,
            tableName = shardingRoutingRule.tableName
        ) ?: return null
        return shardingRoutingRule.copy(
            dataSourceName = record.dataSourceName ?: shardingRoutingRule.dataSourceName,
            routingRule = record.routingRule
        )
    }

    private fun cacheShardingRoutingRule(key: String, routingRule: String) {
        redisOperation.set(key = key, value = routingRule, expiredInSecond = DEFAULT_RULE_REDIS_CACHE_TIME)
    }

    /**
     * 删除分片路由规则
     * @param userId 用户ID
     * @param id 规则ID
     * @return 布尔值
     */
    override fun deleteShardingRoutingRule(userId: String, id: String): Boolean {
        val shardingRoutingRuleRecord = shardingRoutingRuleDao.getById(dslContext, id)
        if (shardingRoutingRuleRecord != null) {
            // 删除db中规则信息
            shardingRoutingRuleDao.delete(dslContext, id)
            val routingName = shardingRoutingRuleRecord.routingName
            // 删除redis中规则信息
            val key = ShardingUtil.getShardingRoutingRuleKey(
                clusterName = CommonUtils.getDbClusterName(),
                moduleCode = shardingRoutingRuleRecord.moduleCode,
                ruleType = shardingRoutingRuleRecord.type,
                routingName = routingName,
                tableName = shardingRoutingRuleRecord.tableName
            )
            redisOperation.delete(key)
            // 发送规则删除事件消息
            shardingRoutingRuleDispatcher.dispatch(
                ShardingRoutingRuleBroadCastEvent(routingName = key, actionType = CrudEnum.DELETE)
            )
        }
        return true
    }

    /**
     * 更新分片路由规则
     * @param userId 用户ID
     * @param id 规则ID
     * @param shardingRoutingRule 路由规则
     * @return 布尔值
     */
    override fun updateShardingRoutingRule(
        userId: String,
        id: String,
        shardingRoutingRule: ShardingRoutingRule
    ): Boolean {
        val routingName = shardingRoutingRule.routingName
        val key = ShardingUtil.getShardingRoutingRuleKey(
            clusterName = shardingRoutingRule.clusterName,
            moduleCode = shardingRoutingRule.moduleCode.name,
            ruleType = shardingRoutingRule.type.name,
            routingName = routingName,
            tableName = shardingRoutingRule.tableName
        )
        val lock = RedisLock(redisOperation, "$key:update", 30)
        try {
            lock.lock()
            var isUpdated = false
            dslContext.transaction { t ->
                val context = DSL.using(t)
                val nameCount = shardingRoutingRuleDao.countByName(
                    dslContext = context,
                    clusterName = shardingRoutingRule.clusterName,
                    moduleCode = shardingRoutingRule.moduleCode,
                    type = shardingRoutingRule.type,
                    routingName = routingName,
                    tableName = shardingRoutingRule.tableName
                )
                if (nameCount > 0) {
                    val rule = shardingRoutingRuleDao.getById(context, id)
                    if (null != rule && routingName != rule.routingName) {
                        // 抛出错误提示
                        throw ErrorCodeException(
                            errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                            params = arrayOf(routingName)
                        )
                    }
                }
                // 更新db中规则信息
                shardingRoutingRuleDao.update(context, id, shardingRoutingRule)
                isUpdated = true
            }
            if (isUpdated) {
                // 更新redis缓存规则信息
                redisOperation.set(
                    key = key,
                    value = shardingRoutingRule.routingRule,
                    expiredInSecond = DEFAULT_RULE_REDIS_CACHE_TIME
                )
                // 发送规则更新事件消息
                shardingRoutingRuleDispatcher.dispatch(
                    ShardingRoutingRuleBroadCastEvent(
                        routingName = key,
                        routingRule = shardingRoutingRule.routingRule,
                        actionType = CrudEnum.UPDATE
                    )
                )
            }
        } catch (ignored: Throwable) {
            logger.warn("Update ShardingRoutingRule failed", ignored)
            return false
        } finally {
            lock.unlock()
        }
        return true
    }

    /**
     * 更新分片路由规则
     * @param userId 用户ID
     * @param shardingRoutingRule 片路由规则
     * @return 布尔值
     */
    override fun updateShardingRoutingRule(
        userId: String,
        shardingRoutingRule: ShardingRoutingRule
    ): Boolean {
        val record = shardingRoutingRuleDao.get(
            dslContext = dslContext,
            clusterName = shardingRoutingRule.clusterName,
            moduleCode = shardingRoutingRule.moduleCode,
            type = shardingRoutingRule.type,
            routingName = shardingRoutingRule.routingName,
            tableName = shardingRoutingRule.tableName
        )
        record?.let {
            updateShardingRoutingRule(userId, record.id, shardingRoutingRule)
        }
        return true
    }

    /**
     * 根据ID查找分片路由规则
     * @param id 规则ID
     * @return 分片路由规则信息
     */
    override fun getShardingRoutingRuleById(id: String): ShardingRoutingRule? {
        val record = shardingRoutingRuleDao.getById(dslContext, id)
        return if (record != null) {
            ShardingRoutingRule(
                clusterName = record.clusterName,
                moduleCode = SystemModuleEnum.valueOf(record.moduleCode),
                dataSourceName = record.dataSourceName,
                tableName = record.tableName,
                type = ShardingRuleTypeEnum.valueOf(record.type),
                routingName = record.routingName,
                routingRule = record.routingRule
            )
        } else {
            null
        }
    }

    /**
     * 根据规则名称查找分片路由规则
     * @param moduleCode 模块标识
     * @param ruleType 规则类型
     * @param routingName 规则名称
     * @param tableName 数据库表名称
     * @return 分片路由规则信息
     */
    override fun getShardingRoutingRuleByName(
        moduleCode: SystemModuleEnum,
        ruleType: ShardingRuleTypeEnum,
        routingName: String,
        tableName: String?
    ): ShardingRoutingRule? {
        // 获取集群名称
        val clusterName = CommonUtils.getDbClusterName()
        // 从redis缓存中获取规则信息
        val key = ShardingUtil.getShardingRoutingRuleKey(
            clusterName = clusterName,
            moduleCode = moduleCode.name,
            ruleType = ruleType.name,
            routingName = routingName,
            tableName = tableName
        )
        val routingRule = redisOperation.get(key)
        return if (routingRule.isNullOrBlank()) {
            // redis缓存中未取到规则信息则从db查
            val record = shardingRoutingRuleDao.get(
                dslContext = dslContext,
                clusterName = clusterName,
                moduleCode = moduleCode,
                type = ruleType,
                routingName = routingName,
                tableName = tableName
            )
            if (record != null) {
                // 更新redis缓存规则信息
                redisOperation.set(
                    key = key,
                    value = record.routingRule,
                    expiredInSecond = DEFAULT_RULE_REDIS_CACHE_TIME
                )
                ShardingRoutingRule(
                    clusterName = record.clusterName ?: "",
                    moduleCode = moduleCode,
                    dataSourceName = record.dataSourceName ?: "",
                    tableName = record.tableName,
                    type = ruleType,
                    routingName = routingName,
                    routingRule = record.routingRule
                )
            } else {
                null
            }
        } else {
            if (tableName.isNullOrBlank()) {
                // 生成db的分片规则
                ShardingRoutingRule(
                    clusterName = clusterName,
                    moduleCode = moduleCode,
                    dataSourceName = routingRule,
                    type = ruleType,
                    routingName = routingName,
                    routingRule = routingRule
                )
            } else {
                // 生成数据库表的分片规则
                generateTableShardingRoutingRule(
                    moduleCode = moduleCode,
                    ruleType = ruleType,
                    routingName = routingName,
                    routingRule = routingRule,
                    tableName = tableName
                )
            }
        }
    }

    private fun generateTableShardingRoutingRule(
        moduleCode: SystemModuleEnum,
        ruleType: ShardingRuleTypeEnum,
        routingName: String,
        routingRule: String,
        tableName: String?
    ): ShardingRoutingRule? {
        val dbRuleType = if (ruleType == ShardingRuleTypeEnum.ARCHIVE_TABLE) {
            ShardingRuleTypeEnum.ARCHIVE_DB
        } else {
            ShardingRuleTypeEnum.DB
        }
        val dbShardingRoutingRule = getShardingRoutingRuleByName(
            moduleCode = moduleCode,
            ruleType = dbRuleType,
            routingName = routingName
        )
        return if (dbShardingRoutingRule != null) {
            ShardingRoutingRule(
                clusterName = CommonUtils.getDbClusterName(),
                moduleCode = moduleCode,
                dataSourceName = dbShardingRoutingRule.dataSourceName,
                tableName = tableName,
                type = ruleType,
                routingName = routingName,
                routingRule = routingRule
            )
        } else {
            null
        }
    }
}
