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
--- 获取Cookie中bk_token
local ticket, token, username
local bk_token, err = cookieUtil:get_cookie("bk_token")
local bk_ticket, err = cookieUtil:get_cookie("bk_ticket")
if bk_token == nil and bk_ticket == nil then
  ngx.log(ngx.STDERR, "failed to read user request bk_token and bk_ticket: ", err)
  ngx.exit(0)
  return
elseif bk_token ~= nil then
  ticket = oauthUtil:get_ticket(bk_token,"token")
  token = bk_token
  username = ticket.identity.username
else
  token = bk_ticket
  ticket = oauthUtil:get_ticket(bk_ticket,"ticket")
  username = ticket.user_id
end

--- 设置用户信息
ngx.header["x-devops-uid"] = username
ngx.header["x-devops-bk-token"] = bk_token
ngx.header["x-devops-access-token"] = ticket.access_token
ngx.exit(200)