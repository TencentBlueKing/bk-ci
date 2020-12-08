package com.tencent.bk.codecc.task.service.specialparam;

import com.tencent.devops.common.constant.ComConstants;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

/**
 * Gometalinter特殊参数处理实现类
 *
 * @version V1.0
 * @date 2019/12/14
 */
@Service("GOMLSpecialParamBizService")
public class GOMLSpecialParamServiceImpl extends AbstractSpecialParamServiceImpl
{
    /**
     * 修改工具配置影响规则时清空规则集
     *
     * @param reqParamJsonObj
     * @param currentParamJsonObj
     */
    @Override
    public boolean paramJsonModified(JSONObject reqParamJsonObj, JSONObject currentParamJsonObj)
    {
        String reqGoPath = reqParamJsonObj.getString(ComConstants.PARAM_GOML_GO_PATH);
        String reqRelPath = reqParamJsonObj.getString(ComConstants.PARAM_GOML_REL_PATH);
        String currentGoPath = reqParamJsonObj.getString(ComConstants.PARAM_GOML_GO_PATH);
        String currentRelPath = reqParamJsonObj.getString(ComConstants.PARAM_GOML_GO_PATH);
        return !!reqGoPath.equals(currentGoPath) || !reqRelPath.equals(currentRelPath);
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
        boolean isSameGoPath = isSameParam(paramJsonObj1, paramJsonObj2, ComConstants.PARAM_GOML_GO_PATH);
        boolean isSameRelPath = isSameParam(paramJsonObj1, paramJsonObj2, ComConstants.PARAM_GOML_REL_PATH);

        return isSameGoPath && isSameRelPath;
    }
}
