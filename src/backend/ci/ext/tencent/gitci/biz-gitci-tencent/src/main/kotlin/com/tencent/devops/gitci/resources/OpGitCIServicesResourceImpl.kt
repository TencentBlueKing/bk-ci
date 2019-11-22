package com.tencent.devops.gitci.resources

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.gitci.api.OpGitCIServicesResource
import com.tencent.devops.gitci.pojo.GitCIServicesConf
import com.tencent.devops.gitci.service.GitCIServicesConfService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpGitCIServicesResourceImpl @Autowired constructor(private val gitCIServicesConfService: GitCIServicesConfService) : OpGitCIServicesResource {
    override fun create(userId: String, gitCIServicesConf: GitCIServicesConf): Result<Boolean> {
        return try {
            Result(gitCIServicesConfService.create(userId, gitCIServicesConf))
        } catch (e: CustomException) {
            Result(e.status.statusCode, e.message ?: "")
        } catch (e: Exception) {
            Result(1, "op git ci create service failed.")
        }
    }

    override fun update(userId: String, id: Long, enable: Boolean?): Result<Boolean> {
        return try {
            Result(gitCIServicesConfService.update(userId, id, enable))
        } catch (e: CustomException) {
            Result(e.status.statusCode, e.message ?: "")
        } catch (e: Exception) {
            Result(1, "op git ci update service failed.")
        }
    }

    override fun delete(userId: String, id: Long): Result<Boolean> {
        return try {
            Result(gitCIServicesConfService.delete(userId, id))
        } catch (e: CustomException) {
            Result(e.status.statusCode, e.message ?: "")
        } catch (e: Exception) {
            Result(1, "op git ci delete service failed.")
        }
    }

    override fun list(userId: String): Result<List<GitCIServicesConf>> {
        return try {
            Result(gitCIServicesConfService.list(userId))
        } catch (e: CustomException) {
            Result(e.status.statusCode, e.message ?: "")
        } catch (e: Exception) {
            Result(1, "op git ci list service failed.")
        }
    }
}
