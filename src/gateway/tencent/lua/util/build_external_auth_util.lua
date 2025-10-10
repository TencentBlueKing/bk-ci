local _M = {}
function _M:enable()
    local isCodecc = ngx.var.project == 'codecc'
    if isCodecc then
        -- 将前缀字符串分割成表
        local prefixes = {}
        for prefix in string.gmatch(config.kubernetes.auto_prefix, "([^;]+)") do
            table.insert(prefixes, prefix)
        end
        -- 检查项目ID是否匹配任一前缀
        for _, prefix in ipairs(prefixes) do
            if ngx.var.project_id:find(prefix, 1, true) == 1 then
                return true
            end
        end
    end
    return false
end

return _M
