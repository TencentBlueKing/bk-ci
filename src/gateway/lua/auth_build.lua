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

local build_type = ngx.var.http_x_devops_build_type
local cjson = require("cjson")

if build_type == "AGENT" then
    --- 第三方构建机
    -- if build_type == "AGENT" then 
    -- --- agentId + secretkey 验证
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

    local red = redisUtil:new()
    if not red then
        ngx.log(ngx.ERR, "failed to new redis ", err)
        ngx.exit(500)
        return
    else
        --- 获取对应的buildId
        local redRes, err = red:get("third_party_agent_" .. reqSecretKey .. "_" .. reqAgentId .. "_" .. reqBuildId .. "_" .. reqVmSid)
        -- ngx.log(ngx.ERR, "third_party_agent_" .. reqSecretKey .. "_" .. reqAgentId)
        --- 将redis连接放回pool中
        local ok, err = red:set_keepalive(config.redis.max_idle_time, config.redis.pool_size)
        if not ok then
            ngx.say("failed to set keepalive: ", err)
        end

        if not redRes then
            ngx.log(ngx.ERR, "failed to get redis result: ", err)
            ngx.exit(500)
            return
        else
            if redRes == ngx.null then
                ngx.log(ngx.ERR, "redis result is null")
                ngx.exit(404)
                return
            else
                local obj = cjson.decode(redRes)
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
            
                -- parameter check
                if obj.projectId == nil then
                    ngx.log(ngx.ERR, "projectId is null: ")
                    ngx.exit(500)
                    return
                end

                if obj.pipelineId == nil then
                    ngx.log(ngx.ERR, "pipelineId is null: ")
                    ngx.exit(500)
                    return
                end

                if obj.buildId == nil then
                    ngx.log(ngx.ERR, "buildId is null: ")
                    ngx.exit(500)
                    return
                end

                if obj.vmSeqId == nil then
                    ngx.log(ngx.ERR, "vmSeqId is null: ")
                    ngx.exit(500)
                    return
                end

                if obj.agentId == nil then
                    ngx.log(ngx.ERR, "agentId is null: ")
                    ngx.exit(500)
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
                ngx.exit(200)
                return
            end
        end
    end


elseif build_type == "DOCKER" then
    -- --- Docker构建机
    -- if build_type == "DOCKETR" then 
    -- --- agentId + secretkey 验证
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

    local red = redisUtil:new()
    if not red then
        ngx.log(ngx.ERR, "failed to new redis ", err)
        ngx.exit(500)
        return
    else
        local redRes, err = red:get("docker_build_key_" .. reqAgentId .. "_" .. reqSecretKey)
        -- ngx.log(ngx.ERR, "docker_build_key_" .. reqAgentId .. "_" .. reqSecretKey)
        --- 将redis连接放回pool中
        local ok, err = red:set_keepalive(config.redis.max_idle_time, config.redis.pool_size)
        if not ok then
            ngx.say("failed to set keepalive: ", err)
        end

        if not redRes then
            ngx.log(ngx.ERR, "failed to get redis result: ", err)
            ngx.exit(500)
            return
        else
            if redRes == ngx.null then
                ngx.log(ngx.ERR, "redis result is null")
                ngx.exit(404)
                return
            else
                local obj = cjson.decode(redRes)
                --[[              
                -- {
                --      "projectId": "a90"
                --      "vmName": "sodamacos-vm-10-6-206-21",
                --      "vmSeqId": "1",
                --      "buildId": "983da77481c34eff982c7f1bcf993eda",
                --      "pipelineId": "d577f5c2f3704c84b36559d769f472ef"
                -- }
                ]]
            
                -- parameter check
                if obj.projectId == nil then
                    ngx.log(ngx.ERR, "projectId is null: ")
                    ngx.exit(500)
                    return
                end

                if obj.pipelineId == nil then
                    ngx.log(ngx.ERR, "pipelineId is null: ")
                    ngx.exit(500)
                    return
                end

                if obj.buildId == nil then
                    ngx.log(ngx.ERR, "buildId is null: ")
                    ngx.exit(500)
                    return
                end

                if obj.vmName == nil then
                    ngx.log(ngx.ERR, "vmName is null: ")
                    ngx.exit(500)
                    return
                end

                if obj.vmSeqId == nil then
                    ngx.log(ngx.ERR, "vmSeqId is null: ")
                    ngx.exit(500)
                    return
                end

                ngx.header["X-DEVOPS-PROJECT-ID"] = obj.projectId
                ngx.header["X-DEVOPS-PIPELINE-ID"] = obj.pipelineId
                ngx.header["X-DEVOPS-BUILD-ID"] = obj.buildId
                ngx.header["X-DEVOPS-AGENT-ID"] = reqAgentId
                ngx.header["X-DEVOPS-VM-SID"] = obj.vmSeqId
                ngx.header["X-DEVOPS-VM-NAME"] = obj.vmName
                ngx.header["X-DEVOPS-CHANNEL-CODE"] = obj.channelCode
                ngx.exit(200)
                return
            end
        end
    end

    
elseif build_type == "PLUGIN_AGENT" then
    -- --- Docker构建机
    -- if build_type == "PLUGIN_AGENT" then 
    -- --- agentId + secretkey 验证
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

    local red = redisUtil:new()
    if not red then
        ngx.log(ngx.ERR, "failed to new redis ", err)
        ngx.exit(500)
        return
    else
        local redRes, err = red:get("plugin_agent_" .. reqAgentId .. "_" .. reqSecretKey)
        -- ngx.log(ngx.ERR, "plugin_agent_" .. reqAgentId .. "_" .. reqSecretKey)
        --- 将redis连接放回pool中
        local ok, err = red:set_keepalive(config.redis.max_idle_time, config.redis.pool_size)
        if not ok then
            ngx.say("failed to set keepalive: ", err)
        end

        if not redRes then
            ngx.log(ngx.ERR, "failed to get redis result: ", err)
            ngx.exit(500)
            return
        else
            if redRes == ngx.null then
                ngx.log(ngx.ERR, "redis result is null")
                ngx.exit(404)
                return
            else
                local obj = cjson.decode(redRes)
                --[[              
                -- {
                --      "projectId": "a90"
                --      "vmName": "sodamacos-vm-10-6-206-21",
                --      "vmSeqId": "1",
                --      "buildId": "983da77481c34eff982c7f1bcf993eda",
                --      "pipelineId": "d577f5c2f3704c84b36559d769f472ef"
                -- }
                ]]
            
                -- parameter check
                if obj.projectId == nil then
                    ngx.log(ngx.ERR, "projectId is null: ")
                    ngx.exit(500)
                    return
                end

                if obj.pipelineId == nil then
                    ngx.log(ngx.ERR, "pipelineId is null: ")
                    ngx.exit(500)
                    return
                end

                if obj.buildId == nil then
                    ngx.log(ngx.ERR, "buildId is null: ")
                    ngx.exit(500)
                    return
                end

                if obj.vmName == nil then
                    ngx.log(ngx.ERR, "vmName is null: ")
                    ngx.exit(500)
                    return
                end

                if obj.vmSeqId == nil then
                    ngx.log(ngx.ERR, "vmSeqId is null: ")
                    ngx.exit(500)
                    return
                end

                ngx.header["X-DEVOPS-PROJECT-ID"] = obj.projectId
                ngx.header["X-DEVOPS-PIPELINE-ID"] = obj.pipelineId
                ngx.header["X-DEVOPS-BUILD-ID"] = obj.buildId
                ngx.header["X-DEVOPS-AGENT-ID"] = reqAgentId
                ngx.header["X-DEVOPS-VM-SID"] = obj.vmSeqId
                ngx.header["X-DEVOPS-VM-NAME"] = obj.vmName
                ngx.header["X-DEVOPS-CHANNEL-CODE"] = obj.channelCode
                ngx.exit(200)
                return
            end
        end
    end


else
    -- --- 公共构建机
    -- if build_type == "WORKER" then 
    --- 构建机IP验证
    local client_ip, err = ipUtil:clientIp()
    if not client_ip then
        ngx.log(ngx.ERR, "failed to get client ip: ", err)
        ngx.exit(500)
        return
    end
    --- redis获取IP对应的buildID
    local red = redisUtil:new()
    if not red then
        ngx.log(ngx.ERR, "failed to new redis ", err)
        ngx.exit(500)
        return
    else
        --- 获取对应的buildId
        local redRes, err = red:get(client_ip)
        --- 将redis连接放回pool中
        local ok, err = red:set_keepalive(config.redis.max_idle_time, config.redis.pool_size)
        if not ok then
            ngx.say("failed to set keepalive: ", err)
        end
        --- 处理获取到的buildID
        if not redRes then
            ngx.log(ngx.ERR, "failed to get redis result: ", err)
            ngx.exit(500)
            return
        else
            if redRes == ngx.null then
                ngx.log(ngx.ERR, "client ip: ", client_ip)
                ngx.log(ngx.ERR, "redis result is null: ")
                ngx.exit(500)
                return
            else
                local obj = cjson.decode(redRes)
                --[[              
                -- {
                --      "projectId": "a90"
                --      "vmName": "sodamacos-vm-10-6-206-21",
                --      "vmSeqId": "1",
                --      "buildId": "983da77481c34eff982c7f1bcf993eda",
                --      "pipelineId": "d577f5c2f3704c84b36559d769f472ef"
                -- }
                ]]

                -- parameter check
                if obj.projectId == nil then
                    ngx.log(ngx.ERR, "projectId is null: ")
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
                ngx.exit(200)
                return
            end
        end
    end
end

   