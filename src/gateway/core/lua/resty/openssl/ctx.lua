local ffi = require "ffi"
local C = ffi.C
local ffi_gc = ffi.gc

local format_error = require("resty.openssl.err").format_error
local OPENSSL_30 = require("resty.openssl.version").OPENSSL_30

ffi.cdef [[
  typedef struct ossl_lib_ctx_st OSSL_LIB_CTX;

  OSSL_LIB_CTX *OSSL_LIB_CTX_new(void);
  int OSSL_LIB_CTX_load_config(OSSL_LIB_CTX *ctx, const char *config_file);
  void OSSL_LIB_CTX_free(OSSL_LIB_CTX *ctx);
]]

local ossl_lib_ctx

local function new(request_context_only, conf_file)
  if not OPENSSL_30 then
    return false, "ctx is only supported from OpenSSL 3.0"
  end

  local ctx = C.OSSL_LIB_CTX_new()
  ffi_gc(ctx, C.OSSL_LIB_CTX_free)

  if conf_file and C.OSSL_LIB_CTX_load_config(ctx, conf_file) ~= 1 then
    return false, format_error("ctx.new")
  end

  if request_context_only then
    ngx.ctx.ossl_lib_ctx = ctx
  else
    ossl_lib_ctx = ctx
  end

  return true
end

local function free(request_context_only)
  if not OPENSSL_30 then
    return false, "ctx is only supported from OpenSSL 3.0"
  end

  if request_context_only then
    ngx.ctx.ossl_lib_ctx = nil
  else
    ossl_lib_ctx = nil
  end

  return true
end

return {
  new = new,
  free = free,
  get_libctx = function() return ngx.ctx.ossl_lib_ctx or ossl_lib_ctx end,
}