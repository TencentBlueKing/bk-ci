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

local http_origin = ngx.var.http_origin

if not http_origin then
  return
end

if http_origin == "" then
  return
end

local allow_hosts = config.allow_hosts

local matched = false
for k, v in pairs(allow_hosts) do
    local from, to, err = ngx.re.find(http_origin, v, "jo")
    if from then
        matched = true
    end
end

if matched == false then
  ngx.log(ngx.ERR, "can not allow access: ", http_origin)
  return
end

local m = ngx.req.get_method()

if m == "OPTIONS" then
  ngx.header["Access-Control-Allow-Origin"] = http_origin
  ngx.header["Access-Control-Allow-Credentials"] = "true"
  ngx.header["Access-Control-Max-Age"] = "1728000"
  ngx.header["Access-Control-Allow-Methods"] = "GET, POST, PUT, DELETE, OPTIONS"
  ngx.header["Access-Control-Allow-Headers"] = config.allow_headers
  ngx.header["Content-Length"] = "0"
  ngx.header["Content-Type"] = "text/plain charset=UTF-8"
  ngx.exit(204)
end

ngx.header["Access-Control-Allow-Origin"] = http_origin
ngx.header["Access-Control-Allow-Credentials"] = "true"