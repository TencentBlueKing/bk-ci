package com.tencent.bk.codecc.defect.resources;

import com.tencent.bk.codecc.defect.api.UserBackendParamsResource;
import com.tencent.bk.codecc.defect.vo.common.BackendParamsVO;
import com.tencent.devops.common.api.pojo.Result;
import com.tencent.devops.common.constant.ComConstants;
import com.tencent.devops.common.util.List2StrUtil;
import com.tencent.devops.common.web.RestResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * 后端配置参数接口实现类
 *
 * @version V1.0
 * @date 2019/12/14
 */
@Slf4j
@RestResource
public class UserBackendParamsResourceImpl implements UserBackendParamsResource
{
    /**
     * 特殊参数需要与规则集关联的工具
     */
    @Value("${codecc.paramJsonRelateCheckerSetTools:#{null}}")
    private String paramJsonRelateCheckerSetTools;

    /**
     * 任务语言需要与规则集关联的工具
     */
    @Value("${codecc.codeLangRelateCheckerSetTools:#{null}}")
    private String codeLangRelateCheckerSetTools;

    @Override
    public Result<BackendParamsVO> getParams()
    {
        BackendParamsVO backendParamsVO = new BackendParamsVO();
        backendParamsVO.setCodeLangRelateCheckerSetTools(List2StrUtil.fromString(codeLangRelateCheckerSetTools, ComConstants.SEMICOLON));
        backendParamsVO.setParamJsonRelateCheckerSetTools(List2StrUtil.fromString(paramJsonRelateCheckerSetTools, ComConstants.SEMICOLON));
        return new Result<>(backendParamsVO);
    }
}
