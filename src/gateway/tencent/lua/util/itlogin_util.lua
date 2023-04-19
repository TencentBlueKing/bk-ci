--[[
Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.

Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.

BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.

A copy of the MIT License is included in this file.


Terms of the MIT License:
---------------------------------------------------
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
]] _M = {}

function _M:get_staff_info(ckey)
    if ckey == nil then
        ngx.log(ngx.ERR, "ckey is null")
        ngx.exit(500)
        return
    end

    local itlogin_cache = ngx.shared.itlogin_store
    local itlogin_value = itlogin_cache:get(ckey)

    if itlogin_value == nil then
        --- 初始化HTTP连接
        local httpc = http.new()
        if httpc == nil then
            ngx.log(ngx.ERR, "get httpc failed")
            ngx.exit(500)
            return
        end
        --- 开始连接
        httpc:set_timeout(3000)
        httpc:connect(config.itlogin.host, config.itlogin.port)

        --- RIO鉴权
        local timestamp = ngx.time()
        local token = "f471eda88cd6107cd9eb3ea3c168a13d363a6fe63f310193f086"
        local sn = timestamp .. token .. timestamp
        local resty_sha256 = require "resty.sha256"
        local resty_str = require "resty.string"
        local sha256 = resty_sha256:new()
        sha256:update(sn)
        local digest = sha256:final()
        local signature = resty_str.str_to_hex(digest)

        --- 发送请求
        local res, err = httpc:request({
            path = "/devops-itlogin/dm/api/user/credentialkey.php",
            method = "POST",
            headers = {
                ["Host"] = config.itlogin.host,
                ["Timestamp"] = tostring(timestamp),
                ["Signature"] = signature,
                ["x-rio-seq"] = "",
                ["Accept"] = "application/json",
                ["Content-Type"] = "application/x-www-form-urlencoded",
                ["X-Protocol-Version"] = "ITLoginV1.1",
                ["X-System-Key"] = "d4699d2782a6edb09a1837cfdc2df110"
            },
            body = "key=" .. ckey
        })

        --- 设置HTTP保持连接
        httpc:set_keepalive(60000, 5)

        --- 判断是否出错了
        if not res then
            ngx.log(ngx.ERR, "failed to request ckey info: ", err)
            ngx.exit(500)
            return
        end

        --- 判断返回的状态码是否是200
        if res.status ~= 200 then
            ngx.log(ngx.STDERR, "failed to request ckey info, status: ", res.status)
            ngx.exit(res.status)
            return
        end

        --- 获取所有回复
        local responseBody = res:read_body()
        --- 转换JSON的返回数据为TABLE
        local result = json.decode(responseBody)

        --- 判断JSON转换是否成功
        if result == nil then
            ngx.log(ngx.ERR, "failed to parse ckey info response：", responseBody)
            ngx.exit(500)
            return
        end

        --- 判断返回码是否为0
        if result.ReturnFlag ~= 0 then
            ngx.log(ngx.STDERR, "invalid ckey info ", responseBody)
            ngx.exit(401)
            return
        end

        --- 设置3分钟缓存
        itlogin_cache:set(ckey, responseBody, 180)

        return result
    else
        return json.decode(itlogin_value)
    end
end

return _M
