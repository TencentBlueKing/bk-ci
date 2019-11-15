--- 组装用户的菜单

local user_id = ngx.var.sid
local whiteList = config.op.whiteList
local menu = {}
for k,v in pairs(whiteList) do
  --- 当时超级管理的时候，或者是对应的服务的管理的时候
  if ( arrayUtil:isInArray(user_id,whiteList['admin']['staffs']) or arrayUtil:isInArray(user_id,v["staffs"]) ) then
    table.insert(menu, k)
  end
end

local return_result = {
  status = 0,
  data = {
    englishName = ngx.var.sid,
    profile = "http://localhost/avatars/" .. ngx.var.sid .. "/profile.jpg",
    menu = menu
  }
}
ngx.say(json.encode(return_result))