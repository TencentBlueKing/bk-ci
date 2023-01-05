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
-- 获取Cookie中bk_ticket
local bk_ticket, err = cookieUtil:get_cookie("bk_ticket")
if not bk_ticket then
    ngx.log(ngx.STDERR, "failed to read user request bk_ticket: ", err)
    ngx.exit(401)
    return
end

local ticket = oauthUtil:get_ticket(bk_ticket)

local querysArgs = urlUtil:parseUrl(ngx.var.request_uri)

local resource_code = querysArgs["pipelineId"]
if (resource_code == "" or resource_code == nil) then
    ngx.log(ngx.ERR, "Auth docker console resource_code not found: ")
    ngx.exit(403)
    return
end
local project_code = querysArgs["projectId"]
if (project_code == "" or project_code == nil) then
    ngx.log(ngx.ERR, "Auth docker console project_code not found: ")
    ngx.exit(403)
    return
end

local verfiy = ciAuthUtil:relation_validate(ticket.user_id, "XybK7-.L*(o5lU~N?^)93H3nbV1=l>b,(3jvIAXH!7LolD&Zv<",
                                            "edit", project_code, resource_code, "pipeline")

if not verfiy then
    ngx.log(ngx.ERR, "ci auth failed")
    ngx.exit(403)
    return
end

return
