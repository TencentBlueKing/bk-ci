local ffi = require "ffi"

require "resty.openssl.include.ossl_typ"
require "resty.openssl.include.x509v3"
require "resty.openssl.include.x509"
local asn1_macro = require "resty.openssl.include.asn1"

asn1_macro.declare_asn1_functions("X509_EXTENSION")

ffi.cdef [[
  struct v3_ext_ctx {
      int flags;
      X509 *issuer_cert;
      X509 *subject_cert;
      X509_REQ *subject_req;
      X509_CRL *crl;
      /*X509V3_CONF_METHOD*/ void *db_meth;
      void *db;
  };
  int X509_EXTENSION_set_data(X509_EXTENSION *ex, ASN1_OCTET_STRING *data);
  int X509_EXTENSION_set_object(X509_EXTENSION *ex, const ASN1_OBJECT *obj);
]]