local ffi = require "ffi"
local C = ffi.C
local ffi_str = ffi.string

require "resty.openssl.include.rand"
local ctypes = require "resty.openssl.auxiliary.ctypes"
local format_error = require("resty.openssl.err").format_error

local function bytes(length)
  if type(length) ~= "number" then
    return nil, "rand.bytes: expect a number at #1"
  end
  -- generally we don't need manually reseed rng
  -- https://www.openssl.org/docs/man1.1.1/man3/RAND_seed.html
  local buf = ctypes.uchar_array(length)
  local code = C.RAND_bytes(buf, length)
  if code ~= 1 then
    return nil, format_error("rand.bytes", code)
  end

  return ffi_str(buf, length)
end

return {
  bytes = bytes,
}
