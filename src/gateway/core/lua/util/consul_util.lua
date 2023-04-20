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
local _M = {}

--[[插入consul ip列表]]
function _M:getAllWhitelistIp()
    local ip_whitelist = {}
    if type(config.service_ip_whitelist) == "string" then
        ip_whitelist = {config.service_ip_whitelist}
    else
        for k, v in ipairs(config.service_ip_whitelist) do
            if v ~= "" then
                table.insert(ip_whitelist, v)
            end
        end
    end

    -- consul 白名单
    local in_container = ngx.var.namespace ~= '' and ngx.var.namespace ~= nil
    if not in_container and #ip_whitelist > 1 then
        -- 获取灰度设置
        local ns_config = nil
        if ngx.var.devops_region ~= "DEVNET" then
            ns_config = config.ns
        else
            ns_config = config.ns_devnet
        end

        local white_ip_hot_cache = ngx.shared.white_ip_hot_store
        local white_ip_cold_cache = ngx.shared.white_ip_cold_store
        local ip_cache_key = "X-DEVOPS-WHITE-IP-CONSOL"
        local responseBody = white_ip_hot_cache:get(ip_cache_key)

        if responseBody == nil then
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
            local res, err = httpc:request({path = url, method = "GET"})

            local useHttp = true
            if not res then --- 判断是否出错了
                ngx.log(ngx.ERR, "failed to request get consul ip: ", err)
                responseBody = white_ip_cold_cache:get(ip_cache_key)
                useHttp = false
            else
                if res.status ~= 200 then --- 判断返回的状态码是否是200
                    ngx.log(ngx.ERR, "failed to request get consul ip, status: ", res.status)
                    responseBody = white_ip_cold_cache:get(ip_cache_key)
                    useHttp = false
                else -- 正常
                    responseBody = res:read_body()
                    useHttp = true
                end
            end

            if responseBody == nil then
                ngx.log(ngx.ERR, "nil responseBody , please check all cache and http api")
                ngx.exit(401)
                return
            else
                if useHttp then
                    --- 热缓存5秒
                    white_ip_hot_cache:set(ip_cache_key, responseBody, 5)
                    -- 冷缓存1天
                    white_ip_cold_cache:set(ip_cache_key, responseBody, 86400)
                end
            end

            --- 设置HTTP保持连接
            httpc:set_keepalive(60000, 5)
        end

        --- 转换JSON的返回数据为TABLE
        local result = json.decode(responseBody)
        --- 判断JSON转换是否成功
        if result == nil then
            ngx.log(ngx.ERR, "failed to parse get consul ip response：", responseBody)
            ngx.exit(500)
            return
        end

        for k, v in ipairs(result) do
            table.insert(ip_whitelist, v.Address)
        end
    end

    -- kubernetes 白名单
    local kubernetes_api_token = config.kubernetes.api.token
    if ngx.var.devops_region ~= "DEVNET" and kubernetes_api_token ~= "" and kubernetes_api_token ~= nil then
        local white_ip_hot_cache = ngx.shared.white_ip_hot_store
        local white_ip_cold_cache = ngx.shared.white_ip_cold_store
        local ip_cache_key = "X-DEVOPS-WHITE-IP-K8S"
        local responseBody = white_ip_hot_cache:get(ip_cache_key)

        local kubernetes_api_host = config.kubernetes.api.host
        local kubernetes_api_port = config.kubernetes.api.port

        if responseBody == nil then
            --- 初始化HTTP连接
            local httpc = http.new()
            if httpc == nil then
                ngx.log(ngx.ERR, "k8s httpc can not nil")
            else
                --- 开始连接
                httpc:set_timeout(3000)
                local headers = {["Authorization"] = "Bearer " .. kubernetes_api_token}
                --- 发送请求
                local res, err = httpc:request_uri("https://" .. kubernetes_api_host .. ":" ..
                                                       tostring(kubernetes_api_port) .. "/api/v1/nodes",
                                                   {method = "GET", headers = headers, ssl_verify = false})
                local useHttp = true
                if not res then --- 判断是否出错了
                    ngx.log(ngx.ERR, "failed to request get k8s ip: ", err)
                    responseBody = white_ip_cold_cache:get(ip_cache_key)
                    useHttp = false
                else
                    if res.status ~= 200 then --- 判断返回的状态码是否是200
                        ngx.log(ngx.ERR, "failed to request get k8s ip, status: ", res.status)
                        responseBody = white_ip_cold_cache:get(ip_cache_key)
                        useHttp = false
                    else -- 正常
                        responseBody = res.body
                        useHttp = true
                    end
                end
                if responseBody == nil then
                    ngx.log(ngx.ERR, "nil responseBody , please check all cache and http api")
                    ngx.exit(401)
                    return
                else
                    if useHttp then
                        --- 热缓存10秒
                        white_ip_hot_cache:set(ip_cache_key, responseBody, 10)
                        -- 冷缓存1天
                        white_ip_cold_cache:set(ip_cache_key, responseBody, 86400)
                    end
                end

                --- 设置HTTP保持连接
                httpc:set_keepalive(60000, 5)
            end
        end

        --- 转换JSON的返回数据为TABLE
        local result = json.decode(responseBody)
        --- 判断JSON转换是否成功
        if result == nil then
            ngx.log(ngx.ERR, "failed to parse get k8s ip response：", responseBody)
            ngx.exit(500)
            return
        end

        --- 加到白名单
        for _, node in ipairs(result.items) do
            for _, address in ipairs(node.status.addresses) do
                if address.type == "InternalIP" then
                    table.insert(ip_whitelist, address.address)
                end
            end
        end
    end

    return ip_whitelist
end

return _M
