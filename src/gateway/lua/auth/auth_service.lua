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

-- 判断是否是白名单
local isInServiceWhitelist = false

if next(config.service_ip_whitelist) ~= nil then 
  -- 白名单为空的时候不为空的时候
  local service_ip_whitelist = consulUtil.getAllWhitelistIp()
  if next(service_ip_whitelist) ~= nil then
    isInServiceWhitelist, err = ipUtil:isInWhiteList(service_ip_whitelist)
  else
    -- 白名单为空的时候
    isInServiceWhitelist = true
  end
else
  -- 白名单为空的时候
  isInServiceWhitelist = true
end

if not isInServiceWhitelist then
  ngx.log(ngx.ERR, "client ip do not in service_ip_whitelist: ", err)
  ngx.exit(403)
end 
ngx.exit(200)