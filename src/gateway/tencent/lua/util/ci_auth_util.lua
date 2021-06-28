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

function _M:relation_validate(user_id, bk_token, action, project_code, resource_code, resource_type)
    if user_id == nil or bk_token == nil or action == nil or project_code == nil or resource_code == nil or
        resource_type == nil then
        ngx.log(ngx.ERR,
                "null params:" .. user_id .. "," .. bk_token .. ',' .. action .. ',' .. project_code .. ',' ..
                    resource_code .. ',' .. resource_type)
        return false
    end

    -- 路由集群
    local devops_project_id = project_code
    if string.sub(project_code, 0, 4) == 'git_' then
        devops_project_id = 'gitciproject'
    end

    --- 初始化HTTP连接
    local httpc = http.new()
    --- 开始连接
    httpc:set_timeout(3000)
    httpc:connect(config.bkci.host, config.bkci.port)
    --- 发送请求
    local res, err = httpc:request({
        path = "/ms/auth/api/open/service/auth/permission/projects/" .. project_code .. "/relation/validate?action=" .. action ..
            "&resourceCode=" .. resource_code .. "&resourceType=" .. resource_type,
        method = "GET",
        headers = {
            ["X-DEVOPS-UID"] = user_id,
            ["X-DEVOPS-BK-TOKEN"] = bk_token,
            ["X-DEVOPS-PROJECT-ID"] = devops_project_id
        }
    })

    --- 设置HTTP保持连接
    httpc:set_keepalive(60000, 5)

    --- 判断是否出错了
    if not res then
        ngx.log(ngx.ERR, "failed to request relation_validate: ", err)
        return false
    end

    --- 判断返回的状态码是否是200
    if res.status ~= 200 then
        ngx.log(ngx.STDERR, "failed to request relation_validate, status: ", res.status)
        return false
    end

    --- 获取所有回复
    local responseBody = res:read_body()
    --- 转换JSON的返回数据为TABLE
    local result = json.decode(responseBody)

    --- 判断JSON转换是否成功
    if result == nil then
        ngx.log(ngx.ERR, "failed to parse relation_validate response：", responseBody)
        return false
    end

    return result.data
end

return _M
