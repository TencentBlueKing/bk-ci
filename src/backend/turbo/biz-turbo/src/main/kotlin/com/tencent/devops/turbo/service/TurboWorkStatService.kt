package com.tencent.devops.turbo.service


import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.TurboException
import com.tencent.devops.common.api.exception.code.TURBO_THIRDPARTY_SYSTEM_FAIL
import com.tencent.devops.common.api.util.OkhttpUtil
import com.tencent.devops.common.util.JsonUtil
import com.tencent.devops.turbo.dao.repository.TurboWorkJobStatsDataRepository
import com.tencent.devops.turbo.dao.repository.TurboWorkStatsRepository
import com.tencent.devops.turbo.dto.DistccResponse
import com.tencent.devops.turbo.dto.JobStatsDataDto
import com.tencent.devops.turbo.dto.TurboWorkStatsDto
import com.tencent.devops.turbo.model.TTurboWorkJobStatsDataEntity
import com.tencent.devops.turbo.model.TTurboWorkStatsEntity
import org.slf4j.LoggerFactory
import org.springframework.beans.BeanUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class TurboWorkStatService @Autowired constructor(
    private val turboWorkStatsRepository: TurboWorkStatsRepository,
    private val turboWorkJobStatsDataRepository: TurboWorkJobStatsDataRepository,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TurboWorkStatService::class.java)
    }

    @Value("\${tbs.rootpath}")
    private val tbsRootPath: String? = null

    /**
     * 请求接口数据反序列化为对象
     */
    fun getTbsWorkStatData(turboRecordId: String): TurboWorkStatsDto {
        val tbsWorkStatDataStr: String = try {
            OkhttpUtil.doGet(
                url = "$tbsRootPath/api/v1/disttask/work_stats/?decode_job=1&task_id=$turboRecordId"
            )
        } catch (e: Exception) {
            logger.warn("no TBS work stats data found, engine code: $turboRecordId")
            throw TurboException(errorCode = TURBO_THIRDPARTY_SYSTEM_FAIL, errorMessage = "fail to invoke request")
        }

        val response: DistccResponse<TurboWorkStatsDto> =
            JsonUtil.to(tbsWorkStatDataStr, object : TypeReference<DistccResponse<TurboWorkStatsDto>>() {})
        if (response.code != 0 || !response.result) {
            logger.warn("response not success: $response")
            throw TurboException(errorCode = TURBO_THIRDPARTY_SYSTEM_FAIL, errorMessage = "fail to invoke request")
        }
        return response.data
    }

    /**
     * 同步TBS编译加速统计数据
     */
    fun syncTbsWorkStatData(turboRecordId: String) {
        val tbsWorkStatDataDto: TurboWorkStatsDto = getTbsWorkStatData(turboRecordId)
        val jobStatsDataStr = tbsWorkStatDataDto.jobStatsData

        val jobStatsDataDto = JsonUtil.to(jobStatsDataStr, object : TypeReference<JobStatsDataDto>() {})

        var tTurboWorkJobStatsDataEntity = TTurboWorkJobStatsDataEntity()
        BeanUtils.copyProperties(jobStatsDataDto, tTurboWorkJobStatsDataEntity)

        tTurboWorkJobStatsDataEntity = turboWorkJobStatsDataRepository.save(tTurboWorkJobStatsDataEntity)

        val tTurboWorkStatsEntity =
            with(tbsWorkStatDataDto) {
                TTurboWorkStatsEntity(
                    id = id,
                    jobLocalError = jobLocalError,
                    jobLocalOk = jobLocalOk,
                    jobRemoteError = jobRemoteError,
                    jobRemoteOk = jobRemoteOk,
                    registeredTime = registeredTime,
                    scene = scene,
                    startTime = startTime,
                    endTime = endTime,
                    success = success,
                    taskId = taskId,
                    unregisteredTime = unregisteredTime,
                    workId = workId,
                    projectId = projectId,
                    jobStatsId = tTurboWorkJobStatsDataEntity.entityId!!
                )
            }
        turboWorkStatsRepository.save(tTurboWorkStatsEntity)
        logger.info("sync TBS work stats data success.")
    }
}