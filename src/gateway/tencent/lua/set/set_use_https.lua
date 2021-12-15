if config.openHttps == 'true' and ngx.var.agent_type == 'brower' and ngx.var.http_x_forwarded_proto == 'http' then
    return '1'
else
    return '0'
end
