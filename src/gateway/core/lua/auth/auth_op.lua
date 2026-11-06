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

-- 配置缺失、空表或只有空字符串时拒绝访问，不再放行。
local configured = config.service_ip_whitelist
local hasConfiguredWhitelist = false
if type(configured) == "string" then
  hasConfiguredWhitelist = configured ~= ""
elseif type(configured) == "table" then
  for _, v in ipairs(configured) do
    if v ~= nil and v ~= "" then
      hasConfiguredWhitelist = true
      break
    end
  end
end

local isInServiceWhitelist = false
if not hasConfiguredWhitelist then
  err = "service_ip_whitelist is empty"
else
  local service_ip_whitelist = consulUtil.getAllWhitelistIp()
  if type(service_ip_whitelist) == "table" and next(service_ip_whitelist) ~= nil then
    isInServiceWhitelist, err = ipUtil:isInWhiteList(service_ip_whitelist)
  else
    err = "service_ip_whitelist is empty"
  end
end
if not isInServiceWhitelist then
  ngx.log(ngx.STDERR, "client ip do not in service_ip_whitelist: ", err)
  ngx.exit(403)
end 

--- 获取Cookie中bk_token
local bk_token, err = cookieUtil:get_cookie("bk_token")
if not bk_token then
  ngx.log(ngx.STDERR, "failed to read user request bk_token: ", err)
  ngx.exit(401)
end
local ticket = oauthUtil:get_ticket(bk_token)

--- 设置sid
ngx.header["x-devops-uid"] = ticket.identity.username
ngx.header["x-devops-bk-token"] = bk_token
ngx.header["x-devops-access-token"] = ticket.access_token
ngx.exit(200)
