package com.tencent.devops.common.cos.request;

import com.tencent.devops.common.cos.model.enums.HttpMethodEnum;
import com.tencent.devops.common.cos.model.enums.SignTypeEnum;
import okhttp3.RequestBody;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.Map;

public interface IRequest {
    /**
     * 返回请求的方法
     * @return 默认方法：GET，无数据提交
     */
    default Pair<HttpMethodEnum, RequestBody> getMethod() {
        return Pair.of(HttpMethodEnum.GET, null);
    }

    /**
     * 返回请求的查询参数
     * @return 默认方法：空
     */
    default Map<String, String> getQueryParams() {
        return new HashMap<>();
    }

    /**
     * 返回请求的头参数
     * @return 默认方法：空
     */
    default Map<String, String> getHeaderParams() {
        return new HashMap<>();
    }

    /**
     * 返回请求的路径
     * @return 默认方法：/（表示根目录）
     */
    default String getPath() {
        return "/";
    }

    /**
     * 返回请求是否需要签名
     * @return 默认方法：需要签名
     */
    default boolean isNeedSign() {
        return true;
    }

    /**
     * 返回请求签名的方式
     * @return 默认方法：签名在头部
     */
    default SignTypeEnum getSignType() {
        return SignTypeEnum.HEADER;
    }

    /**
     * 返回请求签名的有效期秒数
     * @return 默认方法：24小时
     */
    default long getSignExpireSeconds() {
        return 24 * 60 * 60;
    }

}
