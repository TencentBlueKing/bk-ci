local ffi = require "ffi"

ffi.cdef [[
  int RAND_bytes(unsigned char *buf, int num);
]]
