package com.tencent.bk.codecc.task.component;

import com.tencent.bk.codecc.task.constant.TaskMessageCode;
import com.tencent.bk.codecc.task.vo.PlatformVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * klocwork的负载均衡器
 *
 * @version V1.0
 * @date 2019/10/1
 */
@Slf4j
public class PlatformLoadBalancer
{
    public static String getRegisterSelectedPlatform(String algorithmType, List<PlatformVO> platformList)
    {
        log.info("getRegisterSelectedPlatform：{}", algorithmType);

        if (platformList.size() == 0)
        {
            log.error("platform instance is empty!");
            throw new CodeCCException(TaskMessageCode.REGISTER_TASK_FAIL);
        }

        if (StringUtils.isEmpty(algorithmType))
        {
            algorithmType = LBConstants.LB_ALGOL.RANDOM.value();
        }

        String platformIp;
        PlatformVO[] platforms = platformList.toArray(new PlatformVO[platformList.size()]);
        if (LBConstants.LB_ALGOL.PERFORMANCE_BALANCE.value().equals(algorithmType))
        {
            platformIp = platforms[0].getIp();
        }
        else
        {
            int chooseIndex = (int) (System.currentTimeMillis() % platforms.length);
            platformIp = platforms[chooseIndex].getIp();
        }

        log.info("select platform:{}", platformIp);
        return platformIp;
    }
}
