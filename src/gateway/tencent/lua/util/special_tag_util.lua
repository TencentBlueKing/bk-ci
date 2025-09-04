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
-- SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE._M = {}
local _M = {}
-- 获取特殊tag
function _M:get_special_tag(gateway_project, devops_project_id, x_gateway_tag)
    local auto_cluster_prefixes = config.kubernetes.auto_prefix
    if x_gateway_tag == nil and gateway_project ~= 'codecc' then
        -- 将前缀字符串分割成表
        local prefixes = {}
        for prefix in string.gmatch(auto_cluster_prefixes, "([^;]+)") do
            table.insert(prefixes, prefix)
        end
        -- 检查项目ID是否匹配任一前缀
        for _, prefix in ipairs(prefixes) do
            if string.find(devops_project_id, "^" .. prefix) then
                x_gateway_tag = config.redis.auto_redis.tag
                break
            end
        end
    end
    return x_gateway_tag
end
return _M
