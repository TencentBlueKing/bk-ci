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

local _M = {}

--[[获取客户端的IP地址]]
function _M:clientIp()
  local headers=ngx.req.get_headers()
  -- ngx.log(ngx.ERR, "X-REAL-IP:", headers["X-REAL-IP"])
  -- ngx.log(ngx.ERR, "X_FORWARDED_FOR:", headers["X_FORWARDED_FOR"])
  -- ngx.log(ngx.ERR, "remote_addr:", ngx.var.remote_addr)
  local ip=headers["X-REAL-IP"] or headers["X_FORWARDED_FOR"] or ngx.var.remote_addr or "0.0.0.0"
  return ip
end

--[[判断IP是否在名名单中]]
function _M:isInWhiteList(whiteList)
  local headers=ngx.req.get_headers()
  
  -- 打印相关IP
  -- ngx.log(ngx.ERR, "X-REAL-IP:", headers["X-REAL-IP"])
  -- ngx.log(ngx.ERR, "X_FORWARDED_FOR:", headers["X_FORWARDED_FOR"])
  -- ngx.log(ngx.ERR, "remote_addr:", ngx.var.remote_addr)


  local result = false
  local clientIp = ""
  -- 将白名单转为table格式
  if type(whiteList) == "string" then
    whiteList = {whiteList}
  end
  -- 判断X-REAL-IP是否在名单中（单IP）
  clientIp = headers["X-REAL-IP"] or ""
  if arrayUtil:isInArray(clientIp,whiteList) then
    return true
  end
  -- 判断remote_addr是否在名单中（单IP）
  clientIp = ngx.var.remote_addr or ""
  if arrayUtil:isInArray(clientIp,whiteList) then
    return true
  end
  -- 判断X_FORWARDED_FOR是否在名单中（多IP，逗号分隔）
  clientIp = stringUtil:split(headers["X_FORWARDED_FOR"] or "",",")
  for k,v in ipairs(clientIp) do
    if arrayUtil:isInArray(v,whiteList) then
      return true
    end
  end
  return result
end

return _M