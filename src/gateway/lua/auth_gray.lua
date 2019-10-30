-- 获取灰度设置
local gray = ngx.var.gray
local gray_dir = ""
local devops_gray = ""

if gray ~= true then
  gray_dir = "prod"
  devops_gray = "false"
else
  gray_dir = "gray"
  devops_gray = "true"
end 

ngx.header["x-devops-devops-gray"] = devops_gray
ngx.header["x-devops-gray-dir"] = gray_dir
ngx.exit(200)