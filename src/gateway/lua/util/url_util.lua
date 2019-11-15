_M = {}

function Split(szFullString, szSeparator)
	local nFindStartIndex = 1
	local nSplitIndex = 1
	local nSplitArray = {}
	while true do
	   local nFindLastIndex = string.find(szFullString, szSeparator, nFindStartIndex)
	   if not nFindLastIndex then
	    nSplitArray[nSplitIndex] = string.sub(szFullString, nFindStartIndex, string.len(szFullString))
	    break
	   end
	   nSplitArray[nSplitIndex] = string.sub(szFullString, nFindStartIndex, nFindLastIndex - 1)
	   nFindStartIndex = nFindLastIndex + string.len(szSeparator)
	   nSplitIndex = nSplitIndex + 1
	end
	return nSplitArray
end

function _M:parseUrl(url)
	local t1 = nil
	--,
	t1= Split(url,',')

	--?
	url = t1[1]
	t1=Split(t1[1],'?')

	url=t1[2]
	--&

	t1=Split(t1[2],'&')
	local res = {}
	for k,v in pairs(t1) do
		i = 1
		t1 = Split(v,'=')
		res[t1[1]]={}
		res[t1[1]]=t1[2]
		i=i+1
	end
	return res
end

return _M