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
]]

_M = {}

function _M:get_ticket(bk_token)
    --- 初始化HTTP连接
    local httpc = http.new()
    --- 开始连接
    httpc:set_timeout(3000)
    httpc:connect(config.oauth.ip, config.oauth.port)

    --- 组装请求body
    local requestBody = {
        grant_type =  "authorization_code",  
        id_provider = "bk_login", 
        bk_token = bk_token
    }

    --- 转换请求内容
    local requestBodyJson = json.encode(requestBody)
    if requestBodyJson == nil then
        ngx.log(ngx.ERR, "failed to encode auth/token request body: ", logUtil:dump(requestBody))
        ngx.exit(500)
        return
    end

    --- 发送请求
    -- local url = config.oauth.scheme .. config.oauth.ip  .. config.oauth.loginUrl .. bk_token
    local url = config.oauth.url
    ngx.log(ngx.ERR, "get_ticket url: ", url)
    local res, err = httpc:request({
        path = url,
        method = "POST",
        headers = {
        ["Host"] = config.oauth.host,
        ["Accept"] = "application/json",
        ["Content-Type"] = "application/json",
        ["X-BK-APP-CODE"] = config.oauth.app_code,
        ["X-BK-APP-SECRET"] = config.oauth.app_secret
        }, 
        body = requestBodyJson
    })
    --- 判断是否出错了
    if not res then
        ngx.log(ngx.ERR, "failed to request get_ticket: ", err)
        ngx.exit(500)
        return
    end
    --- 判断返回的状态码是否是200
    if res.status ~= 200 then
        ngx.log(ngx.ERR, "failed to request get_ticket, status: ", res.status)
        ngx.exit(500)
        return
    end
    --- 获取所有回复
    local responseBody = res:read_body()
    --- 设置HTTP保持连接
    httpc:set_keepalive(60000, 5)
    --- 转换JSON的返回数据为TABLE
    local result = json.decode(responseBody)
    --- 判断JSON转换是否成功
    if result == nil then 
        ngx.log(ngx.ERR, "failed to parse get_ticket response：", responseBody)
        ngx.exit(500)
        return
    end

    --- 判断返回码:Q!
    if result.code ~= 0 then
        ngx.log(ngx.ERR, "invalid get_ticket: ", result.message)
        ngx.exit(401)
        return
    end
    -- 记录用户的访问情况
    -- ngx.log(ngx.ERR, "access user‘s rtx :", result.data.user_id)
    return result.data
end


return _M