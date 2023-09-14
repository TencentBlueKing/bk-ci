local is_devx = string.find(ngx.var.http_host, "devx") ~= nil
if is_devx then
    return ngx.var.http_host
else
    return config.bkci.host
end
