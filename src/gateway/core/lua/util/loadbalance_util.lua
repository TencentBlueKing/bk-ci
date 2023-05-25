-- Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
-- Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
-- BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
-- A copy of the MIT License is included in this file.
-- Terms of the MIT License:
-- ---------------------------------------------------
-- Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
-- documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
-- rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
-- permit persons to whom the Software is furnished to do so, subject to the following conditions:
-- The above copyright notice and this permission notice shall be included in all copies or substantial portions of
-- the Software.
-- THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
-- LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
-- NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
-- WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
-- SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
_M = {}
-- 获取目标ip:port
function _M:getTarget(devops_tag, service_name, cache_tail, ns_config)
    local in_container = ngx.var.namespace ~= '' and ngx.var.namespace ~= nil
    local gateway_project = ngx.var.project

    -- 不走容器化的服务
    local no_container = false
    for index, value in ipairs(no_container_svr) do
        if value == service_name then
            no_container = true
        end
    end
    if no_container and string.find(devops_tag, '^kubernetes-') then
        devops_tag = string.sub(devops_tag, 12)
    end

    -- 转发到容器环境里
    if not in_container and string.find(devops_tag, '^kubernetes-') then
        if config.gw_token ~= nil and ngx.var.devops_token == '' then
            ngx.var.devops_token = config.gw_token
        end
        local kubernetes_domain = nil
        if gateway_project == 'codecc' then
            kubernetes_domain = config.kubernetes.codecc.domain
        else
            kubernetes_domain = config.kubernetes.domain
        end
        return kubernetes_domain .. "/ms/" .. service_name
    end

    -- 容器环境
    if in_container then
        if ngx.var.multi_cluster == 'true' then -- 多集群场景
            local dns = resolver:new{
                nameservers = resolvUtil.nameservers,
                retrans = 5,
                timeout = 2000 -- 2 sec
            }
            -- 先查询当前ns下的服务
            if string.find(devops_tag, '^kubernetes-') then
                devops_tag = string.sub(devops_tag, 12) -- 去掉 "kubernetes-" 头部
            end
            local prefix = nil
            if gateway_project == 'codecc' then
                prefix = 'bk-codecc-' .. service_name
                devops_tag = 'ieg-codeccsvr-codecc-' .. devops_tag
            else
                prefix = service_name .. '-' .. ngx.var.chart_name .. '-' .. service_name
            end
            local domain = prefix .. '.' .. devops_tag .. '.svc.cluster.local'
            local records = dns:query(domain, {qtype = dns.TYPE_A})
            -- 兜底策略
            if ngx.var.default_namespace ~= '' and ngx.var.default_namespace ~= nil and not records then
                domain = prefix .. ngx.var.default_namespace .. '.svc.cluster.local'
            end
            return domain
        else -- 单一集群场景
            return
                ngx.var.release_name .. '-' .. ngx.var.chart_name .. '-' .. service_name .. '.' .. ngx.var.namespace ..
                    '.svc.cluster.local'
        end
    end

    -- 获取consul查询域名
    local query_subdomain = devops_tag .. "." .. service_name .. ns_config.suffix .. ".service." .. ns_config.domain

    local ips = {} -- address
    local port = nil -- port

    local router_srv_key = query_subdomain .. cache_tail
    local router_srv_cache = ngx.shared.router_srv_store
    local router_srv_value = router_srv_cache:get(router_srv_key)

    if router_srv_value == nil then
        -- 获取consul dns服务器
        local dnsIps = {}
        if type(ns_config.ip) == 'table' then
            for i, v in ipairs(ns_config.ip) do
                table.insert(dnsIps, {v, ns_config.port})
            end
        else
            table.insert(dnsIps, {ns_config.ip, ns_config.port})
        end

        -- 连接consul dns
        local dns, err = resolver:new{nameservers = dnsIps, retrans = 5, timeout = 2000}

        if not dns then
            ngx.log(ngx.ERR, "failed to instantiate the resolver: ", err)
            return nil
        end

        -- 查询dns
        local records, err = dns:query(query_subdomain, {qtype = dns.TYPE_SRV, additional_section = true})

        if not records then
            ngx.log(ngx.ERR, "failed to query the DNS server: ", err)
            return nil
        end

        if records.errcode then
            if records.errcode == 3 then
                ngx.log(ngx.ERR, "DNS error code #" .. records.errcode .. ": ", records.errstr, " , query_subdomain : ",
                        query_subdomain)
                return nil
            else
                ngx.log(ngx.ERR, "DNS error #" .. records.errcode .. ": ", err, " , query_subdomain : ", query_subdomain)
                return nil
            end
        end

        -- 将ip 放到列表中
        for _, v in pairs(records) do
            if v.section == dns.SECTION_AN then
                port = v.port
            end

            if v.section == dns.SECTION_AR then
                table.insert(ips, v.address)
            end
        end

        local ip_len = #ips
        if ip_len == 0 or port == nil then
            ngx.log(ngx.ERR, "DNS answer didn't include ip or a port , service :" .. service_name)
            return nil
        end

        -- set cache
        router_srv_cache:set(router_srv_key, table.concat(ips, ",") .. ":" .. port, 1)

    else
        local func_itor = string.gmatch(router_srv_value, "([^:]+)")
        local ips_str = func_itor()
        port = func_itor()

        for ip in string.gmatch(ips_str, "([^,]+)") do
            table.insert(ips, ip)
        end

    end

    return ips[math.random(#ips)] .. ":" .. port
end

return _M
