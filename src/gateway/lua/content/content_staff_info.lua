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
    avatars = "https://dev.bkdevops.qq.com/avatars/" .. staff_info.EnglishName,
    departmentId = staff_info.DepartmentId,
    staffId = staff_info.StaffId,
    departmentName = staff_info.DepartmentName
  }
}
ngx.say(json.encode(result))
-- ngx.say(json.encode(staff_info))