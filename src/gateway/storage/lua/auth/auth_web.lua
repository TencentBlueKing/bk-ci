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

--- 蓝鲸平台登录对接
--- 获取Cookie中bk_token 和 bk_ticket
local bk_ticket, err = cookieUtil:get_cookie("bk_ticket")
local bk_token, err = cookieUtil:get_cookie("bk_token")
local bkrepo_token, bkrepo_err = cookieUtil:get_cookie("bkrepo_ticket")
local ticket, token, username

--- standalone模式下校验bkrepo_ticket
if config.mode == "standalone" or config.mode == "" or config.mode == nil then
    --- 跳过登录请求
    start_i = string.find(ngx.var.request_uri, "login")
    start_i2 = string.find(ngx.var.request_uri, "rsa")
    if start_i ~= nil or start_i2 ~= nil then
        return
    end
    if bkrepo_token == nil then
        ngx.exit(401)
        return
    end
    ticket = oauthUtil:verify_bkrepo_token(bkrepo_token)
    username = ticket.user_id
elseif bk_token ~= nil then
    ticket = oauthUtil:get_ticket(bk_token, "token")
    token = bk_token
    username = ticket.identity.username
else
    local devops_access_token = ngx.var.http_x_devops_access_token
    if bk_ticket == nil and devops_access_token == nil then
        ngx.exit(401)
        return
    end
    if devops_access_token ~= nill then
        ticket = oauthUtil:verify_token(devops_access_token)
    else
        ticket = oauthUtil:get_ticket(bk_ticket, "ticket")
    end
    token = bk_ticket
    username = ticket.user_id
end

--- 其它模式校验bk_ticket


--- 设置用户信息
ngx.header["x-bkrepo-uid"] = username
ngx.header["x-bkrepo-bk-token"] = token
ngx.header["x-bkrepo-access-token"] = ticket.access_token
ngx.exit(200)
