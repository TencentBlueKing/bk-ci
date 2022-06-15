local ffi = require "ffi"
local ffi_copy = ffi.copy
local ffi_gc = ffi.gc
local ffi_new = ffi.new
local ffi_string = ffi.string
local ffi_cast = ffi.cast
local _C = ffi.C

local _M = { _VERSION = "0.2.3" }

local ngx = ngx


local CONST = {
    SHA256_DIGEST = "SHA256",
    SHA512_DIGEST = "SHA512",
    -- ref : https://github.com/openssl/openssl/blob/master/include/openssl/rsa.h
    RSA_PKCS1_PADDING = 1,
    RSA_SSLV23_PADDING = 2,
    RSA_NO_PADDING = 3,
    RSA_PKCS1_OAEP_PADDING = 4,
    RSA_X931_PADDING = 5,
    RSA_PKCS1_PSS_PADDING = 6,
    -- ref : https://github.com/openssl/openssl/blob/master/include/openssl/evp.h
    NID_rsaEncryption = 6,
    EVP_PKEY_RSA = 6,
    EVP_PKEY_ALG_CTRL = 0x1000,
    EVP_PKEY_CTRL_RSA_PADDING = 0x1000 + 1,

    EVP_PKEY_OP_TYPE_CRYPT = 768,
    EVP_PKEY_CTRL_RSA_OAEP_MD = 0x1000 + 9
}
_M.CONST = CONST


-- Reference: https://wiki.openssl.org/index.php/EVP_Signing_and_Verifying
ffi.cdef[[
// Error handling
unsigned long ERR_get_error(void);
const char * ERR_reason_error_string(unsigned long e);

// Basic IO
typedef struct bio_st BIO;
typedef struct bio_method_st BIO_METHOD;
BIO_METHOD *BIO_s_mem(void);
BIO * BIO_new(BIO_METHOD *type);
int	BIO_puts(BIO *bp,const char *buf);
void BIO_vfree(BIO *a);
int    BIO_write(BIO *b, const void *buf, int len);

// RSA
typedef struct rsa_st RSA;
int RSA_size(const RSA *rsa);
void RSA_free(RSA *rsa);
typedef int pem_password_cb(char *buf, int size, int rwflag, void *userdata);
RSA * PEM_read_bio_RSAPrivateKey(BIO *bp, RSA **rsa, pem_password_cb *cb,
								void *u);
RSA * PEM_read_bio_RSAPublicKey(BIO *bp, RSA **rsa, pem_password_cb *cb,
                                void *u);

// EC_KEY
typedef struct ec_key_st EC_KEY;
void EC_KEY_free(EC_KEY *key);
EC_KEY * PEM_read_bio_ECPrivateKey(BIO *bp, EC_KEY **key, pem_password_cb *cb,
								void *u);
EC_KEY * PEM_read_bio_ECPublicKey(BIO *bp, EC_KEY **key, pem_password_cb *cb,
                                void *u);
// EVP PKEY
typedef struct evp_pkey_st EVP_PKEY;
typedef struct engine_st ENGINE;
EVP_PKEY *EVP_PKEY_new(void);
int EVP_PKEY_set1_RSA(EVP_PKEY *pkey,RSA *key);
int EVP_PKEY_set1_EC_KEY(EVP_PKEY *pkey,EC_KEY *key);
EVP_PKEY *EVP_PKEY_new_mac_key(int type, ENGINE *e,
                               const unsigned char *key, int keylen);
void EVP_PKEY_free(EVP_PKEY *key);
int i2d_RSA(RSA *a, unsigned char **out);

// Additional typedef of ECC operations (DER/RAW sig conversion)
typedef struct bignum_st BIGNUM;
BIGNUM *BN_new(void);
void BN_free(BIGNUM *a);
int BN_num_bits(const BIGNUM *a);
int BN_bn2bin(const BIGNUM *a, unsigned char *to);
BIGNUM *BN_bin2bn(const unsigned char *s, int len, BIGNUM *ret);
char *BN_bn2hex(const BIGNUM *a);


typedef struct ECDSA_SIG_st {
    BIGNUM *r;
    BIGNUM *s;} ECDSA_SIG;
ECDSA_SIG*     ECDSA_SIG_new(void);
int            i2d_ECDSA_SIG(const ECDSA_SIG *sig, unsigned char **pp);
ECDSA_SIG*     d2i_ECDSA_SIG(ECDSA_SIG **sig, unsigned char **pp,
long len);
void           ECDSA_SIG_free(ECDSA_SIG *sig);

typedef struct ecgroup_st EC_GROUP;

EC_GROUP *EC_KEY_get0_group(const EC_KEY *key);
EC_KEY *EVP_PKEY_get0_EC_KEY(EVP_PKEY *pkey);
int EC_GROUP_get_order(const EC_GROUP *group, BIGNUM *order, void *ctx);


// PUBKEY
EVP_PKEY *PEM_read_bio_PUBKEY(BIO *bp, EVP_PKEY **x,
                              pem_password_cb *cb, void *u);

// X509
typedef struct x509_st X509;
X509 *PEM_read_bio_X509(BIO *bp, X509 **x, pem_password_cb *cb, void *u);
EVP_PKEY *      X509_get_pubkey(X509 *x);
void X509_free(X509 *a);
void EVP_PKEY_free(EVP_PKEY *key);
int i2d_X509(X509 *a, unsigned char **out);
X509 *d2i_X509_bio(BIO *bp, X509 **x);

// X509 store
typedef struct x509_store_st X509_STORE;
typedef struct X509_crl_st X509_CRL;
X509_STORE *X509_STORE_new(void );
int X509_STORE_add_cert(X509_STORE *ctx, X509 *x);
    // Use this if we want to load the certs directly from a variables
int X509_STORE_add_crl(X509_STORE *ctx, X509_CRL *x);
int     X509_STORE_load_locations (X509_STORE *ctx,
                const char *file, const char *dir);
void X509_STORE_free(X509_STORE *v);

// X509 store context
typedef struct x509_store_ctx_st X509_STORE_CTX;
X509_STORE_CTX *X509_STORE_CTX_new(void);
int X509_STORE_CTX_init(X509_STORE_CTX *ctx, X509_STORE *store,
                         X509 *x509, void *chain);
int             X509_verify_cert(X509_STORE_CTX *ctx);
void X509_STORE_CTX_cleanup(X509_STORE_CTX *ctx);
int    X509_STORE_CTX_get_error(X509_STORE_CTX *ctx);
const char *X509_verify_cert_error_string(long n);
void X509_STORE_CTX_free(X509_STORE_CTX *ctx);

// EVP Sign/Verify
typedef struct env_md_ctx_st EVP_MD_CTX;
typedef struct env_md_st EVP_MD;
typedef struct evp_pkey_ctx_st EVP_PKEY_CTX;
const EVP_MD *EVP_get_digestbyname(const char *name);

//OpenSSL 1.0
EVP_MD_CTX *EVP_MD_CTX_create(void);
void    EVP_MD_CTX_destroy(EVP_MD_CTX *ctx);

//OpenSSL 1.1
EVP_MD_CTX *EVP_MD_CTX_new(void);
void    EVP_MD_CTX_free(EVP_MD_CTX *ctx);

int     EVP_DigestInit_ex(EVP_MD_CTX *ctx, const EVP_MD *type, ENGINE *impl);
int     EVP_DigestSignInit(EVP_MD_CTX *ctx, EVP_PKEY_CTX **pctx,
                        const EVP_MD *type, ENGINE *e, EVP_PKEY *pkey);
int     EVP_DigestUpdate(EVP_MD_CTX *ctx,const void *d,
                         size_t cnt);
int     EVP_DigestSignFinal(EVP_MD_CTX *ctx,
                        unsigned char *sigret, size_t *siglen);

int     EVP_DigestVerifyInit(EVP_MD_CTX *ctx, EVP_PKEY_CTX **pctx,
                        const EVP_MD *type, ENGINE *e, EVP_PKEY *pkey);
int     EVP_DigestVerifyFinal(EVP_MD_CTX *ctx,
                        unsigned char *sig, size_t siglen);

// Fingerprints
int X509_digest(const X509 *data,const EVP_MD *type,
                unsigned char *md, unsigned int *len);

//EVP encrypt decrypt
EVP_PKEY_CTX *EVP_PKEY_CTX_new(EVP_PKEY *pkey, ENGINE *e);
void EVP_PKEY_CTX_free(EVP_PKEY_CTX *ctx);

int EVP_PKEY_CTX_ctrl(EVP_PKEY_CTX *ctx, int keytype, int optype,
                      int cmd, int p1, void *p2);

int EVP_PKEY_size(EVP_PKEY *pkey);

int EVP_PKEY_encrypt_init(EVP_PKEY_CTX *ctx);
int EVP_PKEY_encrypt(EVP_PKEY_CTX *ctx,
        unsigned char *out, size_t *outlen,
        const unsigned char *in, size_t inlen);

int EVP_PKEY_decrypt_init(EVP_PKEY_CTX *ctx);
int EVP_PKEY_decrypt(EVP_PKEY_CTX *ctx,
                        unsigned char *out, size_t *outlen,
                        const unsigned char *in, size_t inlen);


]]


local function _err(ret)
    -- The openssl error queue can have multiple items, print them all separated by ': '
    local errs = {}
    local code = _C.ERR_get_error()
    while code ~= 0 do
        table.insert(errs, 1, ffi_string(_C.ERR_reason_error_string(code)))
        code = _C.ERR_get_error()
    end

    if #errs == 0 then
        return ret, "Zero error code (null arguments?)"
    end
    return ret, table.concat(errs, ": ")
end

local ctx_new, ctx_free
local openssl11, e = pcall(function ()
    local ctx = _C.EVP_MD_CTX_new()
    _C.EVP_MD_CTX_free(ctx)
end)

ngx.log(ngx.DEBUG, "openssl11=", openssl11, " err=", e)

if openssl11 then
    ctx_new = function ()
        return _C.EVP_MD_CTX_new()
    end
    ctx_free = function (ctx)
        ffi_gc(ctx, _C.EVP_MD_CTX_free)
    end
else
    ctx_new = function ()
        local ctx = _C.EVP_MD_CTX_create()
        return ctx
    end
    ctx_free = function (ctx)
        ffi_gc(ctx, _C.EVP_MD_CTX_destroy)
    end
end

local function _new_key(self, opts)
    local bio = _C.BIO_new(_C.BIO_s_mem())
    ffi_gc(bio, _C.BIO_vfree)
    if _C.BIO_puts(bio, opts.pem_private_key) < 0 then
        return _err()
    end

    local pass
    if opts.password then
        local plen = #opts.password
        pass = ffi_new("unsigned char[?]", plen + 1)
        ffi_copy(pass, opts.password, plen)
    end

    local key = nil
    if self.algo == "RSA" then
       key = _C.PEM_read_bio_RSAPrivateKey(bio, nil, nil, pass)
       ffi_gc(key, _C.RSA_free)
    elseif self.algo == "ECDSA" then
        key = _C.PEM_read_bio_ECPrivateKey(bio, nil, nil, pass)
        ffi_gc(key, _C.EC_KEY_free)
    end

    if not key then
        return _err()
    end

    local evp_pkey = _C.EVP_PKEY_new()
    if evp_pkey == nil then
        return _err()
    end

    ffi_gc(evp_pkey, _C.EVP_PKEY_free)
    if self.algo == "RSA" then
        if _C.EVP_PKEY_set1_RSA(evp_pkey, key) ~= 1 then
           return _err()
        end
    elseif self.algo == "ECDSA" then
        if _C.EVP_PKEY_set1_EC_KEY(evp_pkey, key) ~= 1 then
            return _err()
        end
    end

    self.evp_pkey = evp_pkey
    return self, nil
end

local function _create_evp_ctx(self, encrypt)
    self.ctx = _C.EVP_PKEY_CTX_new(self.evp_pkey, nil)
    if self.ctx == nil then
        return _err()
    end

    ffi_gc(self.ctx, _C.EVP_PKEY_CTX_free)

    local md = _C.EVP_get_digestbyname(self.digest_alg)
    if ffi_cast("void *", md) == nil then
        return nil, "Unknown message digest"
    end

    if encrypt then
      if _C.EVP_PKEY_encrypt_init(self.ctx) <= 0 then
        return _err()
      end
    else
      if _C.EVP_PKEY_decrypt_init(self.ctx) <= 0 then
        return _err()
      end
    end

    if _C.EVP_PKEY_CTX_ctrl(self.ctx, CONST.EVP_PKEY_RSA, -1, CONST.EVP_PKEY_CTRL_RSA_PADDING,
                self.padding, nil) <= 0 then
            return _err()
    end

    if self.padding ==  CONST.RSA_PKCS1_OAEP_PADDING then
        if _C.EVP_PKEY_CTX_ctrl(self.ctx, CONST.EVP_PKEY_RSA, CONST.EVP_PKEY_OP_TYPE_CRYPT,
              CONST.EVP_PKEY_CTRL_RSA_OAEP_MD, 0, ffi_cast("void *", md)) <= 0 then
              return _err()
        end
    end

    return self.ctx
end

local RSASigner = {algo="RSA"}
_M.RSASigner = RSASigner

--- Create a new RSASigner
-- @param pem_private_key A private key string in PEM format
-- @param password password for the private key (if required)
-- @returns RSASigner, err_string
function RSASigner.new(self, pem_private_key, password)
    return _new_key (
        self,
        {
            pem_private_key = pem_private_key,
            password = password
        }
    )
end


--- Sign a message
-- @param message The message to sign
-- @param digest_name The digest format to use (e.g., "SHA256")
-- @returns signature, error_string
function RSASigner.sign(self, message, digest_name)
    local buf = ffi_new("unsigned char[?]", 1024)
    local len = ffi_new("size_t[1]", 1024)

    local ctx = ctx_new()
    if ctx == nil then
        return _err()
    end
    ctx_free(ctx)

    local md = _C.EVP_get_digestbyname(digest_name)
    if md == nil then
        return _err()
    end

    if _C.EVP_DigestInit_ex(ctx, md, nil) ~= 1 then
        return _err()
    end

    local ret = _C.EVP_DigestSignInit(ctx, nil, md, nil, self.evp_pkey)
    if  ret ~= 1 then
        return _err()
    end
    if _C.EVP_DigestUpdate(ctx, message, #message) ~= 1 then
         return _err()
    end
    if _C.EVP_DigestSignFinal(ctx, buf, len) ~= 1 then
        return _err()
    end
    return ffi_string(buf, len[0]), nil
end


local ECSigner = {algo="ECDSA"}
_M.ECSigner = ECSigner

--- Create a new ECSigner
-- @param pem_private_key A private key string in PEM format
-- @param password password for the private key (if required)
-- @returns ECSigner, err_string
function ECSigner.new(self, pem_private_key, password)
    return RSASigner.new(self, pem_private_key, password)
end

--- Sign a message with ECDSA
-- @param message The message to sign
-- @param digest_name The digest format to use (e.g., "SHA256")
-- @returns signature, error_string
function ECSigner.sign(self, message, digest_name)
    return RSASigner.sign(self, message, digest_name)
end

--- Converts a ASN.1 DER signature to RAW r,s
-- @param signature The ASN.1 DER signature
-- @returns signature, error_string
function ECSigner.get_raw_sig(self, signature)
    if not signature then
        return nil, "Must pass a signature to convert"
    end
    local sig_ptr = ffi_new("unsigned char *[1]")
    local sig_bin = ffi_new("unsigned char [?]", #signature)
    ffi_copy(sig_bin, signature, #signature)

    sig_ptr[0] = sig_bin
    local sig = _C.d2i_ECDSA_SIG(nil, sig_ptr, #signature)
    ffi_gc(sig, _C.ECDSA_SIG_free)

    local rbytes = math.floor((_C.BN_num_bits(sig.r)+7)/8)
    local sbytes = math.floor((_C.BN_num_bits(sig.s)+7)/8)

    -- Ensure we copy the BN in a padded form
    local ec = _C.EVP_PKEY_get0_EC_KEY(self.evp_pkey)
    local ecgroup = _C.EC_KEY_get0_group(ec)

    local order =  _C.BN_new()
    ffi_gc(order, _C.BN_free)

    -- res is an int, if 0, curve not found
    local res = _C.EC_GROUP_get_order(ecgroup, order, nil)

    -- BN_num_bytes is a #define, so have to use BN_num_bits
    local order_size_bytes = math.floor((_C.BN_num_bits(order)+7)/8)
    local resbuf_len = order_size_bytes *2
    local resbuf = ffi_new("unsigned char[?]", resbuf_len)

    -- Let's whilst preserving MSB
    _C.BN_bn2bin(sig.r, resbuf + order_size_bytes - rbytes)
    _C.BN_bn2bin(sig.s, resbuf + (order_size_bytes*2) - sbytes)

    local raw = ffi_string(resbuf, resbuf_len)
    return raw, nil
end

local RSAVerifier = {}
_M.RSAVerifier = RSAVerifier


--- Create a new RSAVerifier
-- @param key_source An instance of Cert or PublicKey used for verification
-- @returns RSAVerifier, error_string
function RSAVerifier.new(self, key_source)
    if not key_source then
        return nil, "You must pass in an key_source for a public key"
    end
    local evp_public_key = key_source.public_key
    self.evp_pkey = evp_public_key
    return self, nil
end

--- Verify a message is properly signed
-- @param message The original message
-- @param the signature to verify
-- @param digest_name The digest type that was used to sign
-- @returns bool, error_string
function RSAVerifier.verify(self, message, sig, digest_name)
    local md = _C.EVP_get_digestbyname(digest_name)
    if md == nil then
        return _err(false)
    end

    local ctx = ctx_new()
    if ctx == nil then
        return _err(false)
    end
    ctx_free(ctx)

    if _C.EVP_DigestInit_ex(ctx, md, nil) ~= 1 then
        return _err(false)
    end

    local ret = _C.EVP_DigestVerifyInit(ctx, nil, md, nil, self.evp_pkey)
    if ret ~= 1 then
        return _err(false)
    end
    if _C.EVP_DigestUpdate(ctx, message, #message) ~= 1 then
        return _err(false)
    end
    local sig_bin = ffi_new("unsigned char[?]", #sig)
    ffi_copy(sig_bin, sig, #sig)
    if _C.EVP_DigestVerifyFinal(ctx, sig_bin, #sig) == 1 then
        return true, nil
    else
        return false, "Verification failed"
    end
end

local ECVerifier = {}
_M.ECVerifier = ECVerifier
--- Create a new ECVerifier
-- @param key_source An instance of Cert or PublicKey used for verification
-- @returns ECVerifier, error_string
function ECVerifier.new(self, key_source)
    return RSAVerifier.new(self, key_source)
end

--- Verify a message is properly signed
-- @param message The original message
-- @param the signature to verify
-- @param digest_name The digest type that was used to sign
-- @returns bool, error_string
function ECVerifier.verify(self, message, sig, digest_name)
    -- We have to convert the signature back from RAW to ASN1 for verification
    local der_sig, err = self:get_der_sig(sig)
    if not der_sig then
        return nil, err
    end
    return RSAVerifier.verify(self, message, der_sig, digest_name)
end

--- Converts a RAW r,s signature to ASN.1 DER signature (ECDSA)
-- @param signature The raw signature
-- @returns signature, error_string
function ECVerifier.get_der_sig(self, signature)
    if not signature then
        return nil, "Must pass a signature to convert"
    end
    -- inspired from https://bit.ly/2yZxzxJ
    local ec = _C.EVP_PKEY_get0_EC_KEY(self.evp_pkey)
    local ecgroup = _C.EC_KEY_get0_group(ec)

    local order =  _C.BN_new()
    ffi_gc(order, _C.BN_free)

    -- res is an int, if 0, curve not found
    local res = _C.EC_GROUP_get_order(ecgroup, order, nil)

    -- BN_num_bytes is a #define, so have to use BN_num_bits
    local order_size_bytes = math.floor((_C.BN_num_bits(order)+7)/8)

    if #signature ~= 2 * order_size_bytes then
        return nil, "signature length != 2 * order length"
    end

    local sig_bytes = ffi_new("unsigned char [?]", #signature)
    ffi_copy(sig_bytes, signature, #signature)
    local ecdsa = _C.ECDSA_SIG_new()
    ffi_gc(ecdsa, _C.ECDSA_SIG_free)

    -- Those do not need to be GCed as they are cleared by the ECDSA_SIG_free()
    local r = _C.BN_bin2bn(sig_bytes, order_size_bytes, nil)
    local s = _C.BN_bin2bn(sig_bytes + order_size_bytes, order_size_bytes, nil)

    ecdsa.r = r
    ecdsa.s = s

    -- Gives us the buffer size to allocate
    local der_len = _C.i2d_ECDSA_SIG(ecdsa, nil)

    local der_sig_ptr = ffi_new("unsigned char *[1]")
    local der_sig_bin = ffi_new("unsigned char [?]", der_len)
    der_sig_ptr[0] = der_sig_bin
    der_len = _C.i2d_ECDSA_SIG(ecdsa, der_sig_ptr)

    local der_str = ffi_string(der_sig_bin, der_len)
    return der_str, nil
end


local Cert = {}
_M.Cert = Cert


--- Create a new Certificate object
-- @param payload A PEM or DER format X509 certificate
-- @returns Cert, error_string
function Cert.new(self, payload)
    if not payload then
        return nil, "Must pass a PEM or binary DER cert"
    end
    local bio = _C.BIO_new(_C.BIO_s_mem())
    ffi_gc(bio, _C.BIO_vfree)
    local x509
    if payload:find('-----BEGIN') then
        if _C.BIO_puts(bio, payload) < 0 then
            return _err()
        end
        x509 = _C.PEM_read_bio_X509(bio, nil, nil, nil)
    else
        if _C.BIO_write(bio, payload, #payload) < 0 then
            return _err()
        end
        x509 = _C.d2i_X509_bio(bio, nil)
    end
    if x509 == nil then
        return _err()
    end
    ffi_gc(x509, _C.X509_free)
    self.x509 = x509
    local public_key, err = self:get_public_key()
    if not public_key then
        return nil, err
    end

    ffi_gc(public_key, _C.EVP_PKEY_free)

    self.public_key = public_key
    return self, nil
end


--- Retrieve the DER format of the certificate
-- @returns Binary DER format, error_string
function Cert.get_der(self)
    local bufp = ffi_new("unsigned char *[1]")
    local len = _C.i2d_X509(self.x509, bufp)
    if len < 0 then
        return _err()
    end
    local der = ffi_string(bufp[0], len)
    return der, nil
end

--- Retrieve the cert fingerprint
-- @param digest_name the Type of digest to use (e.g., "SHA256")
-- @returns fingerprint_string, error_string
function Cert.get_fingerprint(self, digest_name)
    local md = _C.EVP_get_digestbyname(digest_name)
    if md == nil then
        return _err()
    end
    local buf = ffi_new("unsigned char[?]", 32)
    local len = ffi_new("unsigned int[1]", 32)
    if _C.X509_digest(self.x509, md, buf, len) ~= 1 then
        return _err()
    end
    local raw = ffi_string(buf, len[0])
    local t = {}
    raw:gsub('.', function (c) table.insert(t, string.format('%02X', string.byte(c))) end)
    return table.concat(t, ":"), nil
end

--- Retrieve the public key from the CERT
-- @returns An OpenSSL EVP PKEY object representing the public key, error_string
function Cert.get_public_key(self)
    local evp_pkey = _C.X509_get_pubkey(self.x509)
    if evp_pkey == nil then
        return _err()
    end

    return evp_pkey, nil
end

--- Verify the Certificate is trusted
-- @param trusted_cert_file File path to a list of PEM encoded trusted certificates
-- @return bool, error_string
function Cert.verify_trust(self, trusted_cert_file)
    local store = _C.X509_STORE_new()
    if store == nil then
        return _err(false)
    end
    ffi_gc(store, _C.X509_STORE_free)
    if _C.X509_STORE_load_locations(store, trusted_cert_file, nil) ~=1 then
        return _err(false)
    end

    local ctx = _C.X509_STORE_CTX_new()
    if store == nil then
        return _err(false)
    end
    ffi_gc(ctx, _C.X509_STORE_CTX_free)
    if _C.X509_STORE_CTX_init(ctx, store, self.x509, nil) ~= 1 then
        return _err(false)
    end

    if _C.X509_verify_cert(ctx) ~= 1 then
        local code = _C.X509_STORE_CTX_get_error(ctx)
        local msg = ffi_string(_C.X509_verify_cert_error_string(code))
        _C.X509_STORE_CTX_cleanup(ctx)
        return false, msg
    end
    _C.X509_STORE_CTX_cleanup(ctx)
    return true, nil

end

local PublicKey = {}
_M.PublicKey = PublicKey

--- Create a new PublicKey object
--
-- If a PEM fornatted key is provided, the key must start with
--
-- ----- BEGIN PUBLIC KEY -----
--
-- @param payload A PEM or DER format public key file
-- @return PublicKey, error_string
function PublicKey.new(self, payload)
    if not payload then
        return nil, "Must pass a PEM or binary DER public key"
    end
    local bio = _C.BIO_new(_C.BIO_s_mem())
    ffi_gc(bio, _C.BIO_vfree)
    local pkey
    if payload:find('-----BEGIN') then
        if _C.BIO_puts(bio, payload) < 0 then
            return _err()
        end
        pkey = _C.PEM_read_bio_PUBKEY(bio, nil, nil, nil)
    else
        if _C.BIO_write(bio, payload, #payload) < 0 then
            return _err()
        end
        pkey = _C.d2i_PUBKEY_bio(bio, nil)
    end
    if pkey == nil then
        return _err()
    end
    ffi_gc(pkey, _C.EVP_PKEY_free)
    self.public_key = pkey
    return self, nil
end

local RSAEncryptor= {}
_M.RSAEncryptor = RSAEncryptor

--- Create a new RSAEncryptor
-- @param key_source An instance of Cert or PublicKey used for verification
-- @param padding padding type to use
-- @param digest_alg digest algorithm to use
-- @returns RSAEncryptor, err_string
function RSAEncryptor.new(self, key_source, padding, digest_alg)
    if not key_source then
        return nil, "You must pass in an key_source for a public key"
    end
    local evp_public_key = key_source.public_key
    self.evp_pkey = evp_public_key
    self.padding = padding or CONST.RSA_PKCS1_OAEP_PADDING
    self.digest_alg = digest_alg or CONST.SHA256_DIGEST
    return self, nil
end



--- Encrypts the payload
-- @param payload plain text payload
-- @returns encrypted payload, error_string
function RSAEncryptor.encrypt(self, payload)

    local ctx, err_str = _create_evp_ctx(self, true)

    if not ctx then
        return nil, err_str
    end
    local len = ffi_new("size_t [1]")
    if _C.EVP_PKEY_encrypt(ctx, nil, len, payload, #payload) <= 0 then
        return _err()
    end
    local buf = ffi_new("unsigned char[?]", len[0])
    if _C.EVP_PKEY_encrypt(ctx, buf, len, payload, #payload) <= 0 then
        return _err()
    end

    return ffi_string(buf, len[0])

end


local RSADecryptor= {algo="RSA"}
_M.RSADecryptor = RSADecryptor

--- Create a new RSADecryptor
-- @param pem_private_key A private key string in PEM format
-- @param password password for the private key (if required)
-- @param padding padding type to use
-- @param digest_alg digest algorithm to use
-- @returns RSADecryptor, error_string
function RSADecryptor.new(self, pem_private_key, password, padding, digest_alg)
    self.padding = padding or CONST.RSA_PKCS1_OAEP_PADDING
    self.digest_alg = digest_alg or CONST.SHA256_DIGEST
    return _new_key (
        self,
        {
            pem_private_key = pem_private_key,
            password = password
        }
    )
end

--- Decrypts the cypher text
-- @param cypher_text encrypted payload
-- @param padding rsa pading mode to use, Defaults to RSA_PKCS1_PADDING
function RSADecryptor.decrypt(self, cypher_text)

    local ctx, err_code, err_str = _create_evp_ctx(self, false)

    if not ctx then
        return nil, err_code, err_str
    end

    local len = ffi_new("size_t [1]")
    if _C.EVP_PKEY_decrypt(ctx, nil, len, cypher_text, #cypher_text) <= 0 then
        return _err()
    end

    local buf = ffi_new("unsigned char[?]", len[0])
    if _C.EVP_PKEY_decrypt(ctx, buf, len, cypher_text, #cypher_text) <= 0 then
        return _err()
    end

    return ffi_string(buf, len[0])

end

return _M
