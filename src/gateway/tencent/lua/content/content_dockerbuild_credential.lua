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


--- 初始化HTTP连接
local httpc = http.new()
--- 开始连接
httpc:set_timeout(3000)
httpc:connect(config.artifactory.domain, config.artifactory.port)
--- 发送请求
local auth_code = "Basic " .. ngx.encode_base64(config.artifactory.user .. ":" .. config.artifactory.password)
local res, err = httpc:request({
  path = "/api/plugins/execute/createDockerUser?params=projectCode=" .. ngx.var.projectId .. ";permanent=false",
  method = "GET",
  headers = {
    ["Host"] = config.artifactory.domain,
    ["Authorization"] = auth_code,
  }
})
httpc:set_keepalive(60000, 5)
--- 判断是否出错了
if not res then
  ngx.log(ngx.STDERR, "failed to request dockerbuild/credential: ", err)
  ngx.exit(500)
  return
end

--- 判断返回的状态码是否是200
if res.status ~= 200 then
  ngx.log(ngx.STDERR, "Log dockerbuild/credential response http code: ", res.status)
  ngx.log(ngx.STDERR, "Log dockerbuild/credential response http body: ", res:read_body())
  ngx.exit(500)
  return
end

--- 读取返回结果
local responseBody = res:read_body()
--- 转换JSON的返回数据为TABLE
local result = json.decode(responseBody)
--- 判断JSON转换是否成功
if result == nil then
  ngx.log(ngx.STDERR, "failed to parse core response：", responseBody)
  ngx.exit(500)
  return
end
--- 判断返回码
if result.status ~= 0 then
  ngx.log(ngx.STDERR, "invalid dockerbuild/credential: ", result.message)
  ngx.exit(500)
  return
end
local return_result = {
  user = result.data.user,
  password = result.data.password,
  host = config.artifactory.domain,
  port = config.artifactory.docker
}
ngx.say(json.encode(return_result))
