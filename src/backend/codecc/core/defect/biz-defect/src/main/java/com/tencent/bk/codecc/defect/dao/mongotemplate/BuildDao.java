package com.tencent.bk.codecc.defect.dao.mongotemplate;

import com.tencent.bk.codecc.defect.dao.mongorepository.BuildRepository;
import com.tencent.bk.codecc.defect.model.BuildEntity;
import com.tencent.bk.codecc.defect.service.PipelineService;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.CommonMessageCode;
import com.tencent.devops.common.redis.lock.RedisLock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 描述
 *
 * @version V1.0
 * @date 2019/12/18
 */
@Slf4j
@Repository
public class BuildDao
{
    /**
     * 字符串锁前缀
     */
    private static final String LOCK_KEY_PREFIX = "SAVE_BUILD_INFO:";

    /**
     * 分布式锁超时时间
     */
    private static final Long LOCK_TIMEOUT = 10L;

    @Autowired
    private BuildRepository buildRepository;

    @Autowired
    private PipelineService pipelineService;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 获取并保存构建信息
     *
     * @param buildId
     * @return
     */
    public BuildEntity getAndSaveBuildInfo(String buildId)
    {
        BuildEntity buildInfo = buildRepository.findFirstByBuildId(buildId);
        if (buildInfo == null)
        {
            RedisLock lock = new RedisLock(redisTemplate, LOCK_KEY_PREFIX + buildId, LOCK_TIMEOUT);
            try
            {
                lock.lock();
                buildInfo = buildRepository.findFirstByBuildId(buildId);
                if (buildInfo == null)
                {
                    buildInfo = pipelineService.getBuildIdInfo(buildId);
                    if (buildInfo != null)
                    {
                        buildRepository.save(buildInfo);
                    }
                    else
                    {
                        String errMsg = String.format("can not get build info from pipeline, buildId: {}", buildId);
                        log.error(errMsg);
                        throw new CodeCCException(CommonMessageCode.INTERNAL_SYSTEM_FAIL, new String[]{errMsg}, null);
                    }
                }
            }
            finally
            {
                lock.unlock();
            }
        }
        return buildInfo;
    }
}
