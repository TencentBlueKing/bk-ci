-- Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
-- Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
-- BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
-- A copy of the MIT License is included in this file.
-- Terms of the MIT License:
-- ---------------------------------------------------
-- Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
-- The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
--- Github登录对接
--- 获取Cookie中bk_token
local ci_login_token, err = cookieUtil:get_cookie("X-DEVOPS-CI-LOGIN-TOKEN")
if not ci_login_token then
    ngx.log(ngx.STDERR, "failed to read user request ci_login_token: ", err)
    ngx.exit(401)
    return
end

--- 校验信息
local user_cache = ngx.shared.user_info_store
local user_cache_value = user_cache:get(ci_login_token)
if user_cache_value == nil then
    --- 初始化HTTP连接
    local httpc = http.new()
    --- 开始连接
    httpc:set_timeout(3000)
    httpc:connect(config.bkci.host, config.bkci.port)
    local res, err = httpc:request({
        path = '/auth/api/external/third/login/verifyToken',
        method = "GET",
        headers = {
            ["Host"] = config.bkci.host,
            ["Accept"] = "application/json",
            ["Content-Type"] = "application/json",
            ["X-DEVOPS-CI-LOGIN-TOKEN"] = ci_login_token
        }
    })
    --- 判断是否出错了
    if not res then
        ngx.log(ngx.ERR, "failed to request get_ticket: ", err)
        ngx.exit(401)
        return
    end
    --- 判断返回的状态码是否是200
    if res.status ~= 200 then
        ngx.log(ngx.STDERR, "failed to request get_ticket, status: ", res.status)
        ngx.exit(401)
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
    local user_id = result.data
    user_cache:set(ci_login_token, user_id, 60)
    user_cache_value = user_id
end

--- 设置用户信息
ngx.header["x-devops-uid"] = user_cache_value
ngx.header["x-devops-bk-token"] = ci_login_token
ngx.header["x-devops-access-token"] = ci_login_token
ngx.exit(200)
