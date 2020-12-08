package com.tencent.bk.codecc.task.service.specialparam;

import com.tencent.bk.codecc.defect.vo.CheckerDetailVO;
import com.tencent.devops.common.constant.ComConstants;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

/**
 * PHPCS特殊参数处理器实现类
 *
 * @version V4.0
 * @date 2019/3/12
 */
@Service("PHPCSSpecialParamBizService")
public class PHPCSSpecialParamServiceImpl extends AbstractSpecialParamServiceImpl
{
    /**
     * 修改工具配置影响规则时清空规则集
     * @param reqParamJsonObj
     * @param currentParamJsonObj
     */
    @Override
    public boolean paramJsonModified(JSONObject reqParamJsonObj, JSONObject currentParamJsonObj)
    {
        return !reqParamJsonObj.getString(ComConstants.KEY_PHPCS_STANDARD).equals(currentParamJsonObj.getString(ComConstants.KEY_PHPCS_STANDARD));
    }

    /**
     * 特殊参数是否相同
     *
     * @param toolName
     * @param paramJson1
     * @param paramJson2
     */
    @Override
    public boolean isSameParam(String toolName, String paramJson1, String paramJson2)
    {
        JSONObject paramJsonObj1 = new JSONObject(paramJson1);
        JSONObject paramJsonObj2 = new JSONObject(paramJson2);

        return isSameParam(paramJsonObj1, paramJsonObj2, ComConstants.KEY_PHPCS_STANDARD);
    }

    /**
     * 规则与参数是否相符
     *
     * @param toolName
     * @param checkerModel
     * @param paramJson
     * @return
     */
    @Override
    public boolean checkerMatchParam(String toolName, CheckerDetailVO checkerModel, String paramJson)
    {
        JSONObject parseObj = StringUtils.isBlank(paramJson) ? new JSONObject() : new JSONObject(paramJson);
        if (parseObj.has(ComConstants.KEY_PHPCS_STANDARD))
        {
            String phpcsStandard = parseObj.getString(ComConstants.KEY_PHPCS_STANDARD);
            int standCode = ComConstants.PHPCSStandardCode.valueOf(phpcsStandard).code();
            if ((standCode & checkerModel.getStandard()) == 0)
            {
                return false;
            }
        }
        return true;
    }
}
