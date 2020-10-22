package com.tencent.bk.codecc.coverity.component;

import com.tencent.bk.codecc.coverity.constant.CoverityMessageCode;
import com.tencent.devops.common.api.exception.CodeCCException;
import com.tencent.devops.common.constant.ComConstants;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * coverity的负载均衡器
 *
 * @version V1.0
 * @date 2019/10/1
 */
@Slf4j
public class CovPlatformLoadBalancer
{
    public static String getRegisterSelectedPlatform(String algorithmType, String createFrom)
    {
        log.info("getRegisterSelectedPlatform, algorithmType: {}, createFrom: {}", algorithmType, createFrom);

        Map<String, CoverityService> platformInstMap;
        if (StringUtils.isNotEmpty(createFrom) && ComConstants.BsTaskCreateFrom.GONGFENG_SCAN.value().equalsIgnoreCase(createFrom))
        {
            platformInstMap = CoverityService.getAllowRegisterOpenSourcePlatformInst();
        }
        else
        {
            platformInstMap = CoverityService.getAllowRegisterPlatformInst();
        }

        if (platformInstMap == null || platformInstMap.size() == 0)
        {
            log.error("coverity platform instance map is empty!");
            throw new CodeCCException(CoverityMessageCode.GET_COV_PLATFORM_INST_FAIL);
        }

        if (StringUtils.isEmpty(algorithmType))
        {
            algorithmType = LBConstants.LB_ALGOL.RANDOM.value();
        }

        String platformIp;
        String[] ips = platformInstMap.keySet().toArray(new String[platformInstMap.size()]);
        if (LBConstants.LB_ALGOL.PERFORMANCE_BALANCE.value().equals(algorithmType))
        {
            // TODO
//            CoverityService.getSystemOverview()
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
