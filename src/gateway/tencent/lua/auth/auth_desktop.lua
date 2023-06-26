-- Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
-- Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
-- BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
-- A copy of the MIT License is included in this file.
-- Terms of the MIT License:
-- ---------------------------------------------------
-- Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
-- The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
--- 蓝鲸平台登录对接
--- 获取Cookie中bk_token
local bk_token, err = cookieUtil:get_cookie("bk_token")
if bk_token == nil then
    bk_token = ngx.var.http_bk_token
end
if bk_token == nil then
    ngx.log(ngx.STDERR, "failed to read user request bk_token ", err)
    ngx.exit(401)
    return
end

-- 创建HTTP客户端实例
local httpc = http.new()

-- 发送HTTP GET请求
local res, err = httpc:request_uri("http://login.bkdevops.woa.com/login/accounts/is_login/?bk_token=" .. bk_token,
                                   {method = "GET"})

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
local responseBody = res.body
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
if result.code ~= "00" then
    ngx.log(ngx.STDERR, "invalid get_ticket: ", result.message)
    ngx.exit(401)
    return
end

--- 设置用户信息
local ticket = result.data
ngx.header["x-devops-uid"] = ticket.username
ngx.header["x-devops-bk-token"] = bk_token
ngx.exit(200)
