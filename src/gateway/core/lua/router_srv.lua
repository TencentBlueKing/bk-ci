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
  ngx.log(ngx.STDERR, "owner_uin is not in whitelist!")
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

-- report服务和bkrepo服务不做频率限制
if ngx.var.service == 'report' or ngx.var.service == 'bkrepo' or ngx.var.service == 'schedule' or ngx.var.service == 'task' then
  access_util = nil
end

-- 限制访问频率
if access_util then 
  local access_result,err = access_util:isAccess()
  if not access_result then
    ngx.log(ngx.STDERR, "request excess!")
    ngx.exit(503)
    return
  end
end 

local service_name = ngx.var.service
if config.service_name ~= nil and config.service_name ~= "" then
  service_name = config.service_name
end

if not service_name then
  ngx.log(ngx.STDERR, "failed with no service name")
  ngx.exit(503)
  return
end

if service_name == "" then
  ngx.log(ngx.STDERR, "failed with empty service name")
  ngx.exit(503)
  return
end

-- 当服务器为job的时候指向job的域名
if service_name == "job" then
  ngx.var.target = config.job.domain
  return 
end

if service_name == "bkrepo" then
  ngx.var.target = config.bkrepo.domain
  return
end


-- 获取灰度设置
local devops_gray = grayUtil:get_gray()

local cache_tail = ""
local ns_config = nil
if devops_gray ~= true then
  if ngx.var.devops_region ~= "DEVNET" then
    ns_config = config.ns
    cache_tail = ".normal.idc"
  else
    ns_config = config.ns_devnet
    cache_tail = ".normal.devnet"
  end
else
  if ngx.var.devops_region ~= "DEVNET" then
    ns_config = config.ns_gray
    cache_tail = ".gray.idc"
  else
    ns_config = config.ns_devnet_gray
    cache_tail = ".gray.devnet"
  end
end 

local query_subdomain = ns_config.tag .. "." .. service_name .. ns_config.suffix .. ".service." .. ns_config.domain


if not ns_config.ip then
  ngx.log(ngx.ERR, "DNS ip not exist!")
  ngx.exit(503)
  return
end 

local dnsIps = {}
if type(ns_config.ip) == 'table' then
  for i,v in ipairs(ns_config.ip) do
    table.insert(dnsIps,{v, ns_config.port})
  end
else
  table.insert(dnsIps,{ns_config.ip, ns_config.port})
end


local dns, err = resolver:new{
  nameservers = dnsIps,
  retrans = 5,
  timeout = 2000
}

if not dns then
  ngx.log(ngx.ERR, "failed to instantiate the resolver: ", err)
  ngx.exit(503)
  return
end

local ips = {} -- address
local port = nil -- port

local router_srv_cache = ngx.shared.router_srv_store
local router_srv_value = router_srv_cache:get(query_subdomain .. cache_tail)

if router_srv_value == nil then
  local records, err = dns:query(query_subdomain, {qtype = dns.TYPE_SRV, additional_section=true})

  if not records then
    ngx.log(ngx.ERR, "failed to query the DNS server: ", err)
    ngx.exit(503)
    return
  end

  if records.errcode then
    if records.errcode == 3 then
      ngx.log(ngx.ERR, "DNS error code #" .. records.errcode .. ": ", records.errstr)
      ngx.exit(503)
      return
    else
      ngx.log(ngx.ERR, "DNS error #" .. records.errcode .. ": ", err)
      ngx.exit(503)
      return
    end
  end

  for i, v in pairs(records) do
    if v.section == dns.SECTION_AN then
      port = v.port
    end

    if v.section == dns.SECTION_AR then
      table.insert(ips, v.address)
    end
  end

  local ip_len = table.getn(ips)
  if ip_len == 0 or port == nil then
    ngx.log(ngx.ERR, "DNS answer didn't include ip or a port , ip len" .. ip_len .. " port " .. port)
    ngx.exit(503)
    return
  end

  -- set cache
  router_srv_cache:set(query_subdomain, table.concat(ips, ",") .. ":" .. port, 1)

else
  local func_itor = string.gmatch(router_srv_value, "([^:]+)")
  local ips_str = func_itor()
  port = func_itor()

  for ip in string.gmatch(ips_str, "([^,]+)") do
      table.insert(ips, ip)
  end

end

ngx.var.target = ips[math.random(table.getn(ips))] .. ":" .. port