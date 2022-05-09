local ffi = require "ffi"
local C = ffi.C
local ffi_str = ffi.string
local ffi_sizeof = ffi.sizeof

require "resty.openssl.include.objects"

local buf = ffi.new('char[?]', 100)

local function obj2table(obj)
  local nid = C.OBJ_obj2nid(obj)

  local len = C.OBJ_obj2txt(buf, ffi_sizeof(buf), obj, 1)
  local oid = ffi_str(buf, len)

  return {
    id = oid,
    nid = nid,
    sn = ffi_str(C.OBJ_nid2sn(nid)),
    ln = ffi_str(C.OBJ_nid2ln(nid)),
  }
end

local function nid2table(nid)
  return obj2table(C.OBJ_nid2obj(nid))
end

local function txt2nid(txt)
  if type(txt) ~= "string" then
    return nil, "objects.txt2nid: expect a string at #1"
  end
  local nid = C.OBJ_txt2nid(txt)
  if nid == 0 then
    return nil, "objects.txt2nid: invalid NID text " .. txt
  end
  return nid
end

local function txtnid2nid(txt_nid)
  local nid
  if type(txt_nid) == "string" then
    nid = C.OBJ_txt2nid(txt_nid)
    if nid == 0 then
      return nil, "objects.txtnid2nid: invalid NID text " .. txt_nid
    end
  elseif type(txt_nid) == "number" then
    nid = txt_nid
  else
    return nil, "objects.txtnid2nid: expect string or number at #1"
  end
  return nid
end

return {
  obj2table = obj2table,
  nid2table = nid2table,
  txt2nid = txt2nid,
  txtnid2nid = txtnid2nid,
  create = C.OBJ_create,
}