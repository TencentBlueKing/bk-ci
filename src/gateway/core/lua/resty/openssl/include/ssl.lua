local ffi = require "ffi"

require "resty.openssl.include.ossl_typ"
require "resty.openssl.include.stack"
local OPENSSL_30 = require("resty.openssl.version").OPENSSL_30

ffi.cdef [[
  // SSL_METHOD
  typedef struct ssl_method_st SSL_METHOD;

  // SSL_CIPHER
  typedef struct ssl_cipher_st SSL_CIPHER;
  const char *SSL_CIPHER_get_name(const SSL_CIPHER *cipher);
  SSL_CIPHER *SSL_get_current_cipher(const SSL *ssl);

  SSL_CTX *SSL_CTX_new(const SSL_METHOD *meth);
  void SSL_CTX_free(SSL_CTX *a);

  // SSL_SESSION
  typedef struct ssl_session_st SSL_SESSION;
  SSL_SESSION *SSL_get_session(const SSL *ssl);
  long SSL_SESSION_set_timeout(SSL_SESSION *s, long t);
  long SSL_SESSION_get_timeout(const SSL_SESSION *s);

  typedef int (*SSL_CTX_alpn_select_cb_func)(SSL *ssl,
                const unsigned char **out,
                unsigned char *outlen,
                const unsigned char *in,
                unsigned int inlen,
                void *arg);
  void SSL_CTX_set_alpn_select_cb(SSL_CTX *ctx,
                                  SSL_CTX_alpn_select_cb_func cb,
                                  void *arg);

  int SSL_select_next_proto(unsigned char **out, unsigned char *outlen,
                            const unsigned char *server,
                            unsigned int server_len,
                            const unsigned char *client,
                            unsigned int client_len);

  SSL *SSL_new(SSL_CTX *ctx);
  void SSL_free(SSL *ssl);

  int SSL_set_cipher_list(SSL *ssl, const char *str);
  int SSL_set_ciphersuites(SSL *s, const char *str);

  long SSL_set_options(SSL *ssl, long options);
  long SSL_clear_options(SSL *ssl, long options);
  long SSL_get_options(SSL *ssl);

  /*STACK_OF(SSL_CIPHER)*/ OPENSSL_STACK *SSL_get_ciphers(const SSL *ssl);
  /*STACK_OF(SSL_CIPHER)*/ OPENSSL_STACK *SSL_CTX_get_ciphers(const SSL_CTX *ctx);
  OPENSSL_STACK *SSL_get_peer_cert_chain(const SSL *ssl);

  typedef int (*verify_callback)(int preverify_ok, X509_STORE_CTX *x509_ctx);
  void SSL_set_verify(SSL *s, int mode,
                     int (*verify_callback)(int, X509_STORE_CTX *));

  int SSL_add_client_CA(SSL *ssl, X509 *cacert);
]]

if OPENSSL_30 then
  ffi.cdef [[
    X509 *SSL_get1_peer_certificate(const SSL *ssl);
  ]]
else
   ffi.cdef [[
    X509 *SSL_get_peer_certificate(const SSL *ssl);
  ]]
end
