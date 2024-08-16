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
--- 根据service_code和resource_type来确定存储路径
if (ngx.var.service_code == nil or ngx.var.resource_type == nil) then
    ngx.log(ngx.STDERR, "service_code or resource_type is nil")
    ngx.exit(403)
    return
end

-- 频率限制
if not accessControlUtil:isAccess() then
    ngx.log(ngx.ERR, "request excess!")
    ngx.exit(429)
    return
end

if ngx.var.service_code == "pipeline" then
    ngx.var.storage_path = "generic-local/bk-archive/"
end
if ngx.var.service_code == "artifactory" then
    ngx.var.storage_path = "generic-local/bk-custom/"
end
if ngx.var.service_code == "bcs" then
    if ngx.var.resource_type == "dev_image" then
        ngx.var.storage_path = "docker-local/paas/"
    end
    if ngx.var.resource_type == "prod_image" then
        ngx.var.storage_path = "docker-prod/paas/"
    end
end
if ngx.var.service_code == "report" then
    ngx.var.storage_path = "generic-local/bk-report/"
end
