local ffi = require "ffi"
local C = ffi.C
local ffi_gc = ffi.gc
local ffi_str = ffi.string

local x509_vfy_macro = require "resty.openssl.include.x509_vfy"
local x509_lib = require "resty.openssl.x509"
local chain_lib = require "resty.openssl.x509.chain"
local crl_lib = require "resty.openssl.x509.crl"
local format_error = require("resty.openssl.err").format_all_error
local format_all_error = require("resty.openssl.err").format_error

local _M = {}
local mt = { __index = _M }

local x509_store_ptr_ct = ffi.typeof('X509_STORE*')

function _M.new()
  local ctx = C.X509_STORE_new()
  if ctx == nil then
    return nil, "x509.store.new: X509_STORE_new() failed"
  end
  ffi_gc(ctx, C.X509_STORE_free)

  local self = setmetatable({
    ctx = ctx,
    _elem_refs = {},
    _elem_refs_idx = 1,
  }, mt)

  return self, nil
end

function _M.istype(l)
  return l and l.ctx and ffi.istype(x509_store_ptr_ct, l.ctx)
end

function _M:use_default()
  if C.X509_STORE_set_default_paths(self.ctx) ~= 1 then
    return false, format_all_error("x509.store:use_default")
  end
  return true
end

function _M:add(item)
  local dup
  local err
  if x509_lib.istype(item) then
    dup = C.X509_dup(item.ctx)
    if dup == nil then
      return false, "x509.store:add: X509_dup() failed"
    end
    -- ref counter of dup is increased by 1
    if C.X509_STORE_add_cert(self.ctx, dup) ~= 1 then
      err = format_all_error("x509.store:add: X509_STORE_add_cert")
    end
    -- decrease the dup ctx ref count immediately to make leak test happy
    C.X509_free(dup)
  elseif crl_lib.istype(item) then
    dup = C.X509_CRL_dup(item.ctx)
    if dup == nil then
      return false, "x509.store:add: X509_CRL_dup() failed"
    end
    -- ref counter of dup is increased by 1
    if C.X509_STORE_add_crl(self.ctx, dup) ~= 1 then
      err = format_all_error("x509.store:add: X509_STORE_add_crl")
    end

    -- define X509_V_FLAG_CRL_CHECK                   0x4
    -- enables CRL checking for the certificate chain leaf certificate.
    -- An error occurs if a suitable CRL cannot be found.
    -- Note: this does not check for certificates in the chain.
    C.X509_STORE_set_flags(self.ctx, 0x4)
    -- decrease the dup ctx ref count immediately to make leak test happy
    C.X509_CRL_free(dup)
  else
    return false, "x509.store:add: expect an x509 or crl instance at #1"
  end

  if err then
    return false, err
  end

  -- X509_STORE doesn't have stack gc handler, we need to gc by ourselves
  self._elem_refs[self._elem_refs_idx] = dup
  self._elem_refs_idx = self._elem_refs_idx + 1

  return true
end

function _M:load_file(path)
  if type(path) ~= "string" then
    return false, "x509.store:load_file: expect a string at #1"
  else
    if C.X509_STORE_load_locations(self.ctx, path, nil) ~= 1 then
      return false, format_all_error("x509.store:load_file")
    end
  end

  return true
end

function _M:load_directory(path)
  if type(path) ~= "string" then
    return false, "x509.store:load_directory expect a string at #1"
  else
    if C.X509_STORE_load_locations(self.ctx, nil, path) ~= 1 then
      return false, format_all_error("x509.store:load_directory")
    end
  end

  return true
end

function _M:verify(x509, chain, return_chain)
  if not x509_lib.istype(x509) then
    return nil, "x509.store:verify: expect a x509 instance at #1"
  elseif chain and not chain_lib.istype(chain) then
    return nil, "x509.store:verify: expect a x509.chain instance at #1"
  end

  local ctx = C.X509_STORE_CTX_new()
  if ctx == nil then
    return nil, "x509.store:verify: X509_STORE_CTX_new() failed"
  end

  ffi_gc(ctx, C.X509_STORE_CTX_free)

  local chain_dup_ctx
  if chain then
    local chain_dup, err = chain_lib.dup(chain.ctx)
    if err then
      return nil, err
    end
    chain_dup_ctx = chain_dup.ctx
  end

  if C.X509_STORE_CTX_init(ctx, self.ctx, x509.ctx, chain_dup_ctx) ~= 1 then
    return nil, format_error("x509.store:verify: X509_STORE_CTX_init")
  end

  local code = C.X509_verify_cert(ctx)
  if code == 1 then -- verified
    if not return_chain then
      return true, nil
    end
    local ret_chain_ctx = x509_vfy_macro.X509_STORE_CTX_get0_chain(ctx)
    return chain_lib.dup(ret_chain_ctx)
  elseif code == 0 then -- unverified
    local vfy_code = C.X509_STORE_CTX_get_error(ctx)

    return nil, ffi_str(C.X509_verify_cert_error_string(vfy_code))
  end

  -- error
  return nil, format_error("x509.store:verify: X509_verify_cert", code)

end

return _M
