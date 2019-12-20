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

--[[插入consul ip列表]]
function _M:getAllWhitelistIp()
  local ip_whitelist = {}
  if type(config.service_ip_whitelist) == "string" then
    ip_whitelist = {config.service_ip_whitelist}
  else
    for k,v in ipairs(config.service_ip_whitelist) do
      table.insert(ip_whitelist, v)
    end
  end

  -- 获取灰度设置
  local devops_gray = grayUtil:get_gray()

  -- ngx.log(ngx.ERR, "devops_gray:", devops_gray )
  local ns_config = nil
  if devops_gray ~= true then
    ns_config = config.ns
    -- ngx.log(ngx.ERR, "ns_config" )
  else
    ns_config = config.ns_gray
    -- ngx.log(ngx.ERR, "ns_config_gray" )
  end 
  --- 初始化HTTP连接
  local httpc = http.new()
  --- 开始连接
  httpc:set_timeout(3000)
  local consul_ip = ""
  if type(ns_config.ip) == 'table' then
    consul_ip = ns_config.ip[1]
  else
    consul_ip = ns_config.ip
  end

  httpc:connect(consul_ip, ns_config.http_port)

  --- 发送请求
  -- local url = config.oauth.scheme .. config.oauth.ip  .. config.oauth.loginUrl .. bk_token
  local url = ns_config.nodes_url
  local res, err = httpc:request({
      path = url,
      method = "GET"
  })
  --- 判断是否出错了
  if not res then
      ngx.log(ngx.ERR, "failed to request get consul ip: ", err)
      ngx.exit(500)
      return
  end
  --- 判断返回的状态码是否是200
  if res.status ~= 200 then
      ngx.log(ngx.ERR, "failed to request get consul ip, status: ", res.status)
      ngx.exit(500)
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
      ngx.log(ngx.ERR, "failed to parse get consul ip response：", responseBody)
      ngx.exit(500)
      return
  end

  for k,v in ipairs(result) do
    table.insert(ip_whitelist, v.Address)
  end

  return ip_whitelist
end

return _M