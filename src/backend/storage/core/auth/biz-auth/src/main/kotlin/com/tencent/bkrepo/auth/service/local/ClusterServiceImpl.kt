/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.auth.service.local

import com.tencent.bkrepo.auth.message.AuthMessageCode
import com.tencent.bkrepo.auth.model.TCluster
import com.tencent.bkrepo.auth.pojo.cluster.AddClusterRequest
import com.tencent.bkrepo.auth.pojo.cluster.Cluster
import com.tencent.bkrepo.auth.pojo.cluster.UpdateClusterRequest
import com.tencent.bkrepo.auth.repository.ClusterRepository
import com.tencent.bkrepo.auth.service.ClusterService
import com.tencent.bkrepo.auth.util.CertTrust
import com.tencent.bkrepo.common.api.constant.StringPool.EMPTY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import org.slf4j.LoggerFactory
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update

class ClusterServiceImpl constructor(
    private val clusterRepository: ClusterRepository,
    private val mongoTemplate: MongoTemplate
) : ClusterService {

    override fun addCluster(request: AddClusterRequest): Boolean {
        logger.info("add  cluster  request : {} ", request.toString())
        val cluster = clusterRepository.findOneByClusterId(request.clusterId)
        cluster?.let {
            logger.warn("add cluster [${request.clusterId}]  is exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_DUP_CLUSTERID)
        }

        clusterRepository.insert(
            TCluster(
                clusterId = request.clusterId,
                clusterAddr = request.clusterAddr,
                cert = request.cert,
                credentialStatus = request.credentialStatus
            )
        )
        clusterRepository.findOneByClusterId(request.clusterId) ?: return false
        return true
    }

    override fun ping(clusterId: String): Boolean {
        logger.info("ping  cluster ")
        try {
            val cluster = clusterRepository.findOneByClusterId(clusterId) ?: run {
                logger.warn("ping cluster [$clusterId]  not exist.")
                setClusterCredentialStatus(clusterId, false)
                return false
            }
            CertTrust.initClient(cluster.cert)
            var addr = cluster.clusterAddr.removeSuffix("/") + "/cluster/credential"
            CertTrust.call(addr)
            setClusterCredentialStatus(clusterId, true)
            return true
        } catch (ignored: Exception) {
            logger.warn("ping cluster [$clusterId]  failed.")
            setClusterCredentialStatus(clusterId, false)
            return false
        }
    }

    override fun delete(clusterId: String): Boolean {
        logger.info("delete  cluster clusterId : {}", clusterId)
        val result = clusterRepository.deleteByClusterId(clusterId)
        if (result == 0L) {
            logger.warn("delete cluster [$clusterId]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_CLUSTER_NOT_EXIST)
        }
        return true
    }

    override fun updateCluster(clusterId: String, request: UpdateClusterRequest): Boolean {
        logger.info("update  cluster clusterId : {} , request :{}", clusterId, request.toString())
        clusterRepository.findOneByClusterId(clusterId) ?: run {
            logger.warn("update cluster [$clusterId]  not exist.")
            throw ErrorCodeException(AuthMessageCode.AUTH_CLUSTER_NOT_EXIST)
        }

        val query = Query.query(Criteria.where(TCluster::clusterId.name).`is`(clusterId))
        val update = Update()

        request.credentialStatus?.let {
            update.set(TCluster::credentialStatus.name, request.credentialStatus!!)
        }

        if (request.cert != EMPTY) {
            update.set(TCluster::cert.name, request.cert)
        }

        if (request.clusterAddr != EMPTY) {
            update.set(TCluster::clusterAddr.name, request.clusterAddr)
        }

        val result = mongoTemplate.upsert(query, update, TCluster::class.java)
        if (result.matchedCount == 1L) return true

        return false
    }

    override fun listCluster(): List<Cluster> {
        logger.info("list  cluster ")
        return clusterRepository.findAllBy().map { transfer(it) }
    }

    private fun setClusterCredentialStatus(clusterId: String, status: Boolean): Boolean {
        logger.info("set  cluster credential status clusterId: {}, status: {}", clusterId, status)
        val query = Query()
        query.addCriteria(Criteria.where(TCluster::clusterId.name).`is`(clusterId))
        val update = Update()
        update.set("credentialStatus", status)
        val result = mongoTemplate.updateFirst(query, update, TCluster::class.java)
        if (result.modifiedCount == 1L) return true
        return false
    }

    private fun transfer(cluster: TCluster): Cluster {
        return Cluster(
            clusterId = cluster.clusterId,
            clusterAddr = cluster.clusterAddr,
            cert = cluster.cert,
            credentialStatus = cluster.credentialStatus
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClusterServiceImpl::class.java)
    }
}
