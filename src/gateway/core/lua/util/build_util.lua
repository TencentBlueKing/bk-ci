-- Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
-- Copyright (C) 2019 Tencent.  All rights reserved.
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

-- 外部认证API调用通用函数
function _M:call_external_auth_api(auth_type, params)
    local max_retries = 3
    local timeout = 3000 -- 3秒超时

    -- 构建请求URL
    local base_url = config.external_auth.base_url
    local api_path = "/ms/dispatch/api/external/auth/" .. auth_type
    local query_params = {}

    -- 添加参数到查询字符串
    for key, value in pairs(params) do
        if value ~= nil then
            table.insert(query_params, key .. "=" .. ngx.escape_uri(tostring(value)))
        end
    end

    local query_string = ""
    if #query_params > 0 then
        query_string = "?" .. table.concat(query_params, "&")
    end

    local full_url = base_url .. api_path .. query_string

    -- 重试逻辑
    for attempt = 1, max_retries do
        local httpc = http.new()
        httpc:set_timeout(timeout)

        -- 发送HTTP请求
        local res, err = httpc:request_uri(full_url, {
            method = "GET",
            headers = {
                ["Accept"] = "application/json",
                ["Content-Type"] = "application/json",
                ["X-DEVOPS-PROJECT-ID"] = ngx.var.project_id
            },
            ssl_verify = false
        })

        httpc:set_keepalive(60000, 5)

        if not res then
            ngx.log(ngx.ERR, "failed to request external auth API (attempt ", attempt, "/", max_retries, "): ", err)

            -- 如果不是最后一次尝试，直接重试
            if attempt < max_retries then
                -- 继续下一次循环
            else
                return nil, err
            end
        else
            if res.status ~= 200 then
                ngx.log(ngx.ERR, "external auth API returned status (attempt ", attempt, "/", max_retries, "): ",
                    res.status, ", body: ", res.body)

                -- 对于HTTP错误状态码，如果不是最后一次尝试则重试
                if attempt < max_retries then
                    -- 继续下一次循环
                else
                    return nil, "HTTP " .. res.status
                end
            else
                -- 请求成功，解析响应
                local response_data = json.decode(res.body)
                if not response_data then
                    ngx.log(ngx.ERR, "failed to decode external auth API response (attempt ", attempt, "/", max_retries,
                        ")")

                    if attempt < max_retries then
                        -- 继续下一次循环
                    else
                        return nil, "invalid response"
                    end
                else
                    if response_data.status ~= 0 then
                        ngx.log(ngx.ERR, "external auth API returned error: ", response_data.message or "unknown error")
                        return nil, response_data.message or "auth failed"
                    end

                    return response_data.data, nil
                end
            end
        end
    end
end

-- 校验第三方构建机
--[[
-- {
--  "projectId": "projectId",
--  "pipelineId": "pipelineId",
--  "buildId": "buildId",
--  "agentId": "agentId",
--  "vmSeqId": "vmSeqId",
--  "channelCode": "channelCode"?,
-- }
]]
function _M:auth_agent()
    if ngx.var.http_x_devops_agent_secret_key == nil then
        ngx.log(ngx.ERR, "lack header X-DEVOPS-AGENT-SECRET-KEY")
        ngx.exit(401)
        return
    end

    if ngx.var.http_x_devops_agent_id == nil then
        ngx.log(ngx.ERR, "lack header X-DEVOPS-AGENT-ID")
        ngx.exit(401)
        return
    end

    if ngx.var.http_x_devops_build_id == nil then
        ngx.log(ngx.ERR, "lack header X-DEVOPS-BUILD-ID")
        ngx.exit(401)
        return
    end

    local reqSecretKey = ngx.var.http_x_devops_agent_secret_key
    local reqAgentId = ngx.var.http_x_devops_agent_id
    local reqBuildId = ngx.var.http_x_devops_build_id
    local reqVmSid = ngx.var.http_x_devops_vm_sid

    local obj = nil

    -- 检查是否启用外部认证
    if buildExternalAuthUtil:enable() == true then
        -- 使用外部认证API
        local auth_token = config.external_auth.token
        local params = {
            secretKey = reqSecretKey,
            agentId = reqAgentId,
            buildId = reqBuildId,
            vmSeqId = reqVmSid,
            token = auth_token
        }

        local auth_result, err = self:call_external_auth_api("agent", params)
        if not auth_result then
            ngx.log(ngx.ERR, "external auth failed: ", err)
            ngx.exit(401)
            return
        end

        obj = auth_result
    else
        -- 使用原来的Redis校验逻辑
        local auth_cache = ngx.shared.auth_build_agent
        local cache_key = "third_party_agent_" ..
            reqSecretKey .. "_" .. reqAgentId .. "_" .. reqBuildId .. "_" .. reqVmSid
        local cache_value = auth_cache:get(cache_key)

        -- 如果没有缓存 , 则从redis里面拿
        if cache_value == nil then
            local red = redisUtil:new()
            if not red then
                ngx.log(ngx.ERR, "failed to new redis ", err)
                ngx.exit(500)
                return
            else
                --- 获取对应的buildId
                local redRes, err = red:get(cache_key)
                red:set_keepalive(config.redis.max_idle_time, config.redis.pool_size)
                if not redRes then
                    ngx.log(ngx.ERR, "failed to get redis result: ", err)
                    ngx.exit(500)
                    return
                else
                    if redRes == ngx.null then
                        ngx.log(ngx.ERR, "redis result is null")
                        ngx.exit(401)
                        return
                    else
                        auth_cache:set(cache_key, redRes, 60)
                        cache_value = redRes
                    end
                end
            end
        end

        obj = cjson.decode(cache_value)
    end

    -- parameter check
    if obj.projectId == nil then
        ngx.log(ngx.ERR, "projectId is null: ")
        ngx.exit(401)
        return
    end

    -- atom替换projectId
    if ngx.var.service_code == "atom" then
        if obj.atoms ~= nil then
            local atom_projectid = obj.atoms[ngx.var.atom_code]
            if atom_projectid ~= nil then
                obj.projectId = atom_projectid
            end
        end
    end

    if obj.pipelineId == nil then
        ngx.log(ngx.ERR, "pipelineId is null: ")
        ngx.exit(401)
        return
    end

    if obj.buildId == nil then
        ngx.log(ngx.ERR, "buildId is null: ")
        ngx.exit(401)
        return
    end

    if obj.vmSeqId == nil then
        ngx.log(ngx.ERR, "vmSeqId is null: ")
        ngx.exit(401)
        return
    end

    if obj.agentId == nil then
        ngx.log(ngx.ERR, "agentId is null: ")
        ngx.exit(401)
        return
    end

    if obj.agentId ~= reqAgentId then
        ngx.log(ngx.ERR, "agentId not match")
        ngx.exit(401)
        return
    end

    ngx.header["X-DEVOPS-PROJECT-ID"] = obj.projectId
    ngx.header["X-DEVOPS-PIPELINE-ID"] = obj.pipelineId
    ngx.header["X-DEVOPS-BUILD-ID"] = obj.buildId
    ngx.header["X-DEVOPS-AGENT-ID"] = obj.agentId
    ngx.header["X-DEVOPS-VM-SID"] = obj.vmSeqId
    ngx.header["X-DEVOPS-VM-NAME"] = obj.vmName
    ngx.header["X-DEVOPS-CHANNEL-CODE"] = obj.channelCode
    ngx.header["X-DEVOPS-AGENT-SECRET-KEY"] = reqSecretKey
    ngx.header["X-DEVOPS-SYSTEM-VERSION"] = ""
    ngx.header["X-DEVOPS-XCODE-VERSION"] = ""

    -- 重写project_id变量
    ngx.var.project_id = obj.projectId

    ngx.exit(200)
end

-- Docker构建机
--[[
-- {
--      "projectId": "a90"
--      "vmName": "sodamacos-vm-10-6-206-21",
--      "vmSeqId": "1",
--      "buildId": "983da77481c34eff982c7f1bcf993eda",
--      "pipelineId": "d577f5c2f3704c84b36559d769f472ef"
-- }
]]
function _M:auth_docker()
    if ngx.var.http_x_devops_agent_secret_key == nil then
        ngx.log(ngx.ERR, "lack header X-DEVOPS-AGENT-SECRET-KEY")
        ngx.exit(401)
        return
    end

    if ngx.var.http_x_devops_agent_id == nil then
        ngx.log(ngx.ERR, "lack header X-DEVOPS-AGENT-ID")
        ngx.exit(401)
        return
    end

    local reqSecretKey = ngx.var.http_x_devops_agent_secret_key
    local reqAgentId = ngx.var.http_x_devops_agent_id

    local obj = nil

    -- 检查是否启用外部认证
    if buildExternalAuthUtil:enable() == true then
        -- 使用外部认证API
        local auth_token = config.external_auth.token
        local params = {
            secretKey = reqSecretKey,
            agentId = reqAgentId,
            token = auth_token
        }

        local auth_result, err = self:call_external_auth_api("docker", params)
        if not auth_result then
            ngx.log(ngx.ERR, "external auth failed: ", err)
            ngx.exit(401)
            return
        end

        obj = auth_result
    else
        -- 使用原来的Redis校验逻辑
        local auth_cache = ngx.shared.auth_build_docker
        local cache_key = "docker_build_key_" .. reqAgentId .. "_" .. reqSecretKey
        local cache_value = auth_cache:get(cache_key)

        -- 如果没有缓存 , 则从redis里面拿
        if cache_value == nil then
            local red = redisUtil:new()
            if not red then
                ngx.log(ngx.ERR, "failed to new redis ", err)
                ngx.exit(500)
                return
            else
                local redRes, err = red:get(cache_key)
                red:set_keepalive(config.redis.max_idle_time, config.redis.pool_size)
                if not redRes then
                    ngx.log(ngx.ERR, "failed to get redis result: ", err)
                    ngx.exit(500)
                    return
                else
                    if redRes == ngx.null then
                        ngx.log(ngx.ERR, "redis result is null")
                        ngx.exit(401)
                        return
                    else
                        auth_cache:set(cache_key, redRes, 60)
                        cache_value = redRes
                    end
                end
            end
        end

        obj = cjson.decode(cache_value)
    end
    -- parameter check
    if obj.projectId == nil then
        ngx.log(ngx.ERR, "projectId is null: ")
        ngx.exit(401)
        return
    end

    if obj.pipelineId == nil then
        ngx.log(ngx.ERR, "pipelineId is null: ")
        ngx.exit(401)
        return
    end

    -- atom替换projectId
    if ngx.var.service_code == "atom" then
        if obj.atoms ~= nil then
            local atom_projectid = obj.atoms[ngx.var.atom_code]
            if atom_projectid ~= nil then
                obj.projectId = atom_projectid
            end
        end
    end

    if obj.buildId == nil then
        ngx.log(ngx.ERR, "buildId is null: ")
        ngx.exit(401)
        return
    end

    if obj.vmName == nil then
        ngx.log(ngx.ERR, "vmName is null: ")
        ngx.exit(401)
        return
    end

    if obj.vmSeqId == nil then
        ngx.log(ngx.ERR, "vmSeqId is null: ")
        ngx.exit(401)
        return
    end

    ngx.header["X-DEVOPS-PROJECT-ID"] = obj.projectId
    ngx.header["X-DEVOPS-PIPELINE-ID"] = obj.pipelineId
    ngx.header["X-DEVOPS-BUILD-ID"] = obj.buildId
    ngx.header["X-DEVOPS-AGENT-ID"] = reqAgentId
    ngx.header["X-DEVOPS-VM-SID"] = obj.vmSeqId
    ngx.header["X-DEVOPS-VM-NAME"] = obj.vmName
    ngx.header["X-DEVOPS-CHANNEL-CODE"] = obj.channelCode
    ngx.header["X-DEVOPS-AGENT-SECRET-KEY"] = reqSecretKey
    ngx.header["X-DEVOPS-SYSTEM-VERSION"] = ""
    ngx.header["X-DEVOPS-XCODE-VERSION"] = ""

    -- 重写project_id变量
    ngx.var.project_id = obj.projectId

    ngx.exit(200)
end

-- 校验插件构建机
--[[
-- {
--      "projectId": "a90"
--      "vmName": "sodamacos-vm-10-6-206-21",
--      "vmSeqId": "1",
--      "buildId": "983da77481c34eff982c7f1bcf993eda",
--      "pipelineId": "d577f5c2f3704c84b36559d769f472ef"
-- }
]]
function _M:auth_plugin_agent()
    if ngx.var.http_x_devops_agent_secret_key == nil then
        ngx.log(ngx.ERR, "lack header X-DEVOPS-AGENT-SECRET-KEY")
        ngx.exit(401)
        return
    end

    if ngx.var.http_x_devops_agent_id == nil then
        ngx.log(ngx.ERR, "lack header X-DEVOPS-AGENT-ID")
        ngx.exit(401)
        return
    end

    local reqSecretKey = ngx.var.http_x_devops_agent_secret_key
    local reqAgentId = ngx.var.http_x_devops_agent_id

    local obj = nil

    -- 检查是否启用外部认证
    if buildExternalAuthUtil:enable() == true then
        -- 使用外部认证API
        local auth_token = config.external_auth.token
        local params = {
            secretKey = reqSecretKey,
            agentId = reqAgentId,
            token = auth_token
        }

        local auth_result, err = self:call_external_auth_api("plugin", params)
        if not auth_result then
            ngx.log(ngx.ERR, "external auth failed: ", err)
            ngx.exit(401)
            return
        end

        obj = auth_result
    else
        -- 使用原来的Redis校验逻辑
        local auth_cache = ngx.shared.auth_build_plugin
        local cache_key = "plugin_agent_" .. reqAgentId .. "_" .. reqSecretKey
        local cache_value = auth_cache:get(cache_key)

        if cache_value == nil then
            local red = redisUtil:new()
            if not red then
                ngx.log(ngx.ERR, "failed to new redis ", err)
                ngx.exit(500)
                return
            else
                local redRes, err = red:get(cache_key)
                red:set_keepalive(config.redis.max_idle_time, config.redis.pool_size)
                if not redRes then
                    ngx.log(ngx.ERR, "failed to get redis result: ", err)
                    ngx.exit(500)
                    return
                else
                    if redRes == ngx.null then
                        ngx.log(ngx.ERR, "redis result is null")
                        ngx.exit(401)
                        return
                    else
                        auth_cache:set(cache_key, redRes, 60)
                        cache_value = redRes
                    end
                end
            end
        end

        obj = cjson.decode(cache_value)
    end

    -- parameter check
    if obj.projectId == nil then
        ngx.log(ngx.ERR, "projectId is null: ")
        ngx.exit(401)
        return
    end

    -- atom替换projectId
    if ngx.var.service_code == "atom" then
        if obj.atoms ~= nil then
            local atom_projectid = obj.atoms[ngx.var.atom_code]
            if atom_projectid ~= nil then
                obj.projectId = atom_projectid
            end
        end
    end

    if obj.pipelineId == nil then
        ngx.log(ngx.ERR, "pipelineId is null: ")
        ngx.exit(401)
        return
    end

    if obj.buildId == nil then
        ngx.log(ngx.ERR, "buildId is null: ")
        ngx.exit(401)
        return
    end

    if obj.vmName == nil then
        ngx.log(ngx.ERR, "vmName is null: ")
        ngx.exit(401)
        return
    end

    if obj.vmSeqId == nil then
        ngx.log(ngx.ERR, "vmSeqId is null: ")
        ngx.exit(401)
        return
    end

    ngx.header["X-DEVOPS-PROJECT-ID"] = obj.projectId
    ngx.header["X-DEVOPS-PIPELINE-ID"] = obj.pipelineId
    ngx.header["X-DEVOPS-BUILD-ID"] = obj.buildId
    ngx.header["X-DEVOPS-AGENT-ID"] = reqAgentId
    ngx.header["X-DEVOPS-VM-SID"] = obj.vmSeqId
    ngx.header["X-DEVOPS-VM-NAME"] = obj.vmName
    ngx.header["X-DEVOPS-CHANNEL-CODE"] = obj.channelCode
    ngx.header["X-DEVOPS-AGENT-SECRET-KEY"] = reqSecretKey
    ngx.header["X-DEVOPS-SYSTEM-VERSION"] = ""
    ngx.header["X-DEVOPS-XCODE-VERSION"] = ""

    -- 重写project_id变量
    ngx.var.project_id = obj.projectId

    ngx.exit(200)
end

-- 校验MACOS公共构建机
function _M:auth_macos(checkVersion)
    local client_ip, err = ipUtil:clientIp()
    if not client_ip then
        ngx.log(ngx.ERR, "failed to get client ip: ", err)
        ngx.exit(401)
        return
    end

    local obj = nil

    -- 检查是否启用外部认证
    if buildExternalAuthUtil:enable() == true then
        -- 使用外部认证API
        local auth_token = config.external_auth.token
        local params = {
            clientIp = client_ip,
            checkVersion = checkVersion or false,
            token = auth_token
        }

        local auth_result, err = self:call_external_auth_api("macos", params)
        if not auth_result then
            ngx.log(ngx.ERR, "external auth failed: ", err)
            ngx.exit(401)
            return
        end

        obj = auth_result
    else
        -- 使用原来的Redis校验逻辑
        local auth_cache = ngx.shared.auth_build_macos
        local cache_key = nil
        if checkVersion == true then
            cache_key = "dispatcher:devops_macos_" .. client_ip
        else
            cache_key = "devops_macos_" .. client_ip
        end
        local cache_value = auth_cache:get(cache_key)

        if cache_value == nil then
            --- redis获取IP对应的buildID
            local red = redisUtil:new()
            if not red then
                ngx.log(ngx.ERR, "failed to new redis ", err)
                ngx.exit(500)
                return
            else
                --- 获取对应的buildId
                local redRes, err = red:get(cache_key)
                red:set_keepalive(config.redis.max_idle_time, config.redis.pool_size)
                --- 处理获取到的buildID
                if not redRes then
                    ngx.log(ngx.ERR, "failed to get redis result: ", err)
                    ngx.exit(500)
                    return
                else
                    if redRes == ngx.null then
                        ngx.log(ngx.WARN, "client ip: ", client_ip, " , redis result is null")
                        ngx.exit(401)
                        return
                    else
                        auth_cache:set(cache_key, redRes, 30)
                        cache_value = redRes
                    end
                end
            end
        end

        obj = cjson.decode(cache_value)
    end
    -- parameter check
    if obj.projectId == nil then
        ngx.log(ngx.ERR, "projectId is null: ")
    end

    -- atom替换projectId
    if ngx.var.service_code == "atom" then
        if obj.atoms ~= nil then
            local atom_projectid = obj.atoms[ngx.var.atom_code]
            if atom_projectid ~= nil then
                obj.projectId = atom_projectid
            end
        end
    end

    if obj.pipelineId == nil then
        ngx.log(ngx.ERR, "pipelineId is null: ")
    end

    if obj.buildId == nil then
        ngx.log(ngx.ERR, "buildId is null: ")
    end

    if obj.vmSeqId == nil then
        ngx.log(ngx.ERR, "vmSeqId is null: ")
    end

    if obj.secretKey == nil then
        ngx.log(ngx.ERR, "secretKey is null: ")
    end

    if obj.id == nil then
        ngx.log(ngx.ERR, "id is null: ")
    end

    local systemVersion = ""
    local xcodeVersion = ""
    if checkVersion == true then
        if obj.systemVersion == nil then
            ngx.log(ngx.ERR, "systemVersion is null: ")
        end

        if obj.xcodeVersion == nil then
            ngx.log(ngx.ERR, "xcodeVersion is null: ")
        end
        systemVersion = obj.systemVersion
        xcodeVersion = obj.xcodeVersion
    end

    ngx.header["X-DEVOPS-PROJECT-ID"] = obj.projectId
    ngx.header["X-DEVOPS-PIPELINE-ID"] = obj.pipelineId
    ngx.header["X-DEVOPS-BUILD-ID"] = obj.buildId
    ngx.header["X-DEVOPS-AGENT-ID"] = obj.id
    ngx.header["X-DEVOPS-VM-SID"] = obj.vmSeqId
    ngx.header["X-DEVOPS-VM-NAME"] = obj.id
    ngx.header["X-DEVOPS-CHANNEL-CODE"] = ""
    ngx.header["X-DEVOPS-AGENT-SECRET-KEY"] = obj.secretKey
    ngx.header["X-DEVOPS-SYSTEM-VERSION"] = systemVersion
    ngx.header["X-DEVOPS-XCODE-VERSION"] = xcodeVersion

    -- 重写project_id变量
    ngx.var.project_id = obj.projectId

    ngx.exit(200)
end

-- 校验其他构建机
--[[
 -- {
 --      "projectId": "a90"
 --      "vmName": "sodamacos-vm-10-6-206-21",
 --      "vmSeqId": "1",
 --      "buildId": "983da77481c34eff982c7f1bcf993eda",
 --      "pipelineId": "d577f5c2f3704c84b36559d769f472ef"
 -- }
 ]]
function _M:auth_other()
    -- 公共构建机
    local client_ip, err = ipUtil:clientIp()
    if not client_ip then
        ngx.log(ngx.ERR, "failed to get client ip: ", err)
        ngx.exit(401)
        return
    end

    local obj = nil

    -- 检查是否启用外部认证
    if buildExternalAuthUtil:enable() == true then
        -- 使用外部认证API
        local auth_token = config.external_auth.token
        local params = {
            clientIp = client_ip,
            token = auth_token
        }

        local auth_result, err = self:call_external_auth_api("other", params)
        if not auth_result then
            ngx.log(ngx.ERR, "external auth failed: ", err)
            ngx.exit(401)
            return
        end

        obj = auth_result
    else
        -- 使用原来的Redis校验逻辑
        local auth_cache = ngx.shared.auth_build_other
        local cache_key = client_ip
        local cache_value = auth_cache:get(cache_key)

        if cache_value == nil then
            --- redis获取IP对应的buildID
            local red = redisUtil:new()
            if not red then
                ngx.log(ngx.ERR, "failed to new redis ", err)
                ngx.exit(500)
                return
            else
                --- 获取对应的buildId
                local redRes, err = red:get(cache_key)
                red:set_keepalive(config.redis.max_idle_time, config.redis.pool_size)
                --- 处理获取到的buildID
                if not redRes then
                    ngx.log(ngx.ERR, "failed to get redis result: ", err)
                    ngx.exit(500)
                    return
                else
                    if redRes == ngx.null then
                        ngx.log(ngx.ERR, "client ip: ", client_ip)
                        ngx.log(ngx.ERR, "redis result is null: ")
                        ngx.exit(401)
                        return
                    else
                        auth_cache:set(cache_key, redRes, 60)
                        cache_value = redRes
                    end
                end
            end
        end

        obj = cjson.decode(cache_value)
    end

    -- parameter check
    if obj.projectId == nil then
        ngx.log(ngx.ERR, "projectId is null: ")
    end

    -- atom替换projectId
    if ngx.var.service_code == "atom" then
        if obj.atoms ~= nil then
            local atom_projectid = obj.atoms[ngx.var.atom_code]
            if atom_projectid ~= nil then
                obj.projectId = atom_projectid
            end
        end
    end

    if obj.pipelineId == nil then
        ngx.log(ngx.ERR, "pipelineId is null: ")
    end

    if obj.buildId == nil then
        ngx.log(ngx.ERR, "buildId is null: ")
    end

    if obj.vmName == nil then
        ngx.log(ngx.ERR, "vmName is null: ")
    end

    if obj.vmSeqId == nil then
        ngx.log(ngx.ERR, "vmSeqId is null: ")
    end

    ngx.header["X-DEVOPS-PROJECT-ID"] = obj.projectId
    ngx.header["X-DEVOPS-PIPELINE-ID"] = obj.pipelineId
    ngx.header["X-DEVOPS-BUILD-ID"] = obj.buildId
    ngx.header["X-DEVOPS-AGENT-ID"] = ""
    ngx.header["X-DEVOPS-VM-SID"] = obj.vmSeqId
    ngx.header["X-DEVOPS-VM-NAME"] = obj.vmName
    ngx.header["X-DEVOPS-CHANNEL-CODE"] = obj.channelCode
    ngx.header["X-DEVOPS-AGENT-SECRET-KEY"] = ""
    ngx.header["X-DEVOPS-SYSTEM-VERSION"] = ""
    ngx.header["X-DEVOPS-XCODE-VERSION"] = ""

    -- 重写project_id变量
    ngx.var.project_id = obj.projectId

    ngx.exit(200)
    return
end

return _M
