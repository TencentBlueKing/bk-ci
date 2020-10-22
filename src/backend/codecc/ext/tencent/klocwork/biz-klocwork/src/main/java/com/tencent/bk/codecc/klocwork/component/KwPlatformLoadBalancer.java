package com.tencent.bk.codecc.klocwork.component;

import com.tencent.bk.codecc.klocwork.constant.KlocworkMessageCode;
import com.tencent.bk.codecc.task.vo.PlatformVO;
import com.tencent.devops.common.api.exception.CodeCCException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * klocwork的负载均衡器
 *
 * @version V1.0
 * @date 2019/10/1
 */
@Slf4j
public class KwPlatformLoadBalancer
{
    public static String getRegisterSelectedPlatform(String algorithmType)
    {
        log.error("getRegisterSelectedPlatform：{}", algorithmType);
        Map<String, PlatformVO> platformInst = KlocworkAPIService.getAllowRegisterPlatformInst();

        if (platformInst.size() == 0)
        {
            log.error("klocwork platform instance map is empty!");
            throw new CodeCCException(KlocworkMessageCode.GET_KW_PLATFORM_INST_FAIL);
        }

        if (StringUtils.isEmpty(algorithmType))
        {
            algorithmType = LBConstants.LB_ALGOL.RANDOM.value();
        }

        String platformIp;
        String[] ips = platformInst.keySet().toArray(new String[platformInst.size()]);
        if (LBConstants.LB_ALGOL.PERFORMANCE_BALANCE.value().equals(algorithmType))
        {
            platformIp = ips[0];
        }
        else
        {
            int chooseIndex = (int) (System.currentTimeMillis() % ips.length);
            platformIp = ips[chooseIndex];
        }

        log.info("select platform:{}", platformIp);
        return platformIp;
    }
}
