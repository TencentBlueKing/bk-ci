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

--- 是否在白名单里面
if (ngx.var.whitelist_deny and ngx.var.whitelist_deny ~= "") then
  ngx.log(ngx.ERR, "owner_uin is not in whitelist!")
  ngx.exit(423)
  return
end


-- 访问限制的工具
local access_util = nil

-- 用户请求类型访问频率限制
if ngx.var.access_type == 'user' then
  access_util = require 'access_control_user'
end
-- ip地址请求类型访问频率限制
if ngx.var.access_type == 'build' or ngx.var.access_type == 'external' then
  access_util = require 'access_control_ip'
end

-- 限制访问频率
if access_util then 
  local access_result,err = access_util:isAccess()
  if not access_result then
    ngx.log(ngx.ERR, "request excess!")
    ngx.exit(503)
    return
  end
end 

ngx.var.target = config.job.domain