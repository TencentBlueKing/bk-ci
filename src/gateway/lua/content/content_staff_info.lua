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

--- 获取ckey的头部信息
local headers_tab = ngx.req.get_headers()
local ckey = headers_tab["x-ckey"]

if not ckey then
  ngx.log(ngx.ERR, "request does not has header=x-ckey.")
  ngx.exit(401)
  return
end


--- 请求itlogin后台查询用户信息
local staff_info = itloginUtil:get_staff_info(ckey)
local result = {
  status = 0,
  data = {
    englishName = staff_info.EnglishName,
    chineseName = staff_info.ChineseName,
    avatars = "https://v2.bkdevops.qq.com/avatars/" .. staff_info.EnglishName,
    departmentId = staff_info.DepartmentId,
    staffId = staff_info.StaffId,
    departmentName = staff_info.DepartmentName
  }
}
ngx.say(json.encode(result))
-- ngx.say(json.encode(staff_info))