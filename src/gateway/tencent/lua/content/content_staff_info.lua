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
--- 获取ckey的头部信息
local x_ckey = ngx.var.http_x_ckey -- 内部用户
if nil == x_ckey then
    x_ckey = ngx.var.arg_cKey
end

local x_otoken = ngx.var.http_x_otoken -- 外部用户
if nil == x_otoken then
    x_otoken = ngx.var.arg_oToken
end

if x_ckey == nil and x_otoken == nil then
    ngx.log(ngx.STDERR, "request does not has header=x-ckey or header=x-otoken")
    ngx.exit(401)
    return
end

local result = nil
if x_ckey ~= nil then
    local staff_info = itloginUtil:get_staff_info(x_ckey)
    result = {
        status = 0,
        data = {
            englishName = staff_info.EnglishName,
            chineseName = staff_info.ChineseName,
            avatars = "https://" .. config.externalHost .. "/avatars/" .. staff_info.EnglishName,
            departmentId = staff_info.DepartmentId,
            staffId = staff_info.StaffId,
            departmentName = staff_info.DepartmentName,
            email = staff_info.EnglishName .. '@tencent.com'
        }
    }
else
    local outer_profile = outerloginUtil:getProfile(x_otoken)
    result = {
        status = 0,
        data = {
            englishName = outer_profile.data.username,
            chineseName = outer_profile.data.username,
            avatars = outer_profile.data.logo,
            departmentId = 0,
            staffId = 0,
            departmentName = "outer",
            email = outer_profile.data.email
        }
    }
end
ngx.say(json.encode(result))

