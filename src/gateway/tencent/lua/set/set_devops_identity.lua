if cookieUtil:get_cookie("x-devops-identity") == nil then
    return "0"
else
    return "1"
end
