package proxy

import (
	"bytes"
	"common/logs"
	"crypto/tls"
	stdlog "log"
	"net/http"
	"wsproxy/pkg/clients"
	"wsproxy/pkg/config"

	"github.com/ci-plugins/crypto-go/ssh"
	"github.com/gorilla/mux"
	"github.com/klauspost/cpuid/v2"
)

type WorkspaceProxy struct {
	Ingress               config.HostBasedIngressConfig
	Config                config.ProxyConfig
	BackendConfig         config.DevRemotingBackend
	WorkspaceRouter       WorkspaceRouter
	WorkspaceInfoProvider WorkspaceInfoProvider
	SSHHostSigners        []ssh.Signer
}

func NewWorkspaceProxy(
	ingress config.HostBasedIngressConfig,
	config config.ProxyConfig,
	backendConfig config.DevRemotingBackend,
	workspaceRouter WorkspaceRouter,
	workspaceInfoProvider WorkspaceInfoProvider,
	signers []ssh.Signer,
) *WorkspaceProxy {
	return &WorkspaceProxy{
		Ingress:               ingress,
		Config:                config,
		BackendConfig:         backendConfig,
		WorkspaceRouter:       workspaceRouter,
		WorkspaceInfoProvider: workspaceInfoProvider,
		SSHHostSigners:        signers,
	}
}

func (p *WorkspaceProxy) MustServe() {
	handler, err := p.Handler(nil)
	if err != nil {
		logs.WithError(err).Fatal("cannot initialize proxy - this is likely a configuration issue")
		return
	}
	srv := &http.Server{
		Addr:    p.Ingress.HTTPSAddress,
		Handler: handler,
		TLSConfig: &tls.Config{
			CipherSuites:             optimalDefaultCipherSuites(),
			CurvePreferences:         []tls.CurveID{tls.CurveP521, tls.CurveP384, tls.CurveP256},
			MinVersion:               tls.VersionTLS12,
			MaxVersion:               tls.VersionTLS12,
			PreferServerCipherSuites: true,
			NextProtos:               []string{"h2", "http/1.1"},
		},
		ErrorLog: stdlog.New(logrusErrorWriter{}, "", 0),
	}

	var (
		crt = p.Config.HTTPS.Certificate
		key = p.Config.HTTPS.Key
	)
	go func() {
		// 临时启动http服务
		srvhttp := &http.Server{
			Addr:    p.Ingress.HTTPAddress,
			Handler: handler,
			ErrorLog: stdlog.New(logrusErrorWriter{}, "", 0),
		}
		// err := http.ListenAndServe(p.Ingress.HTTPAddress, http.HandlerFunc(redirectToHTTPS))
		srvhttp.ListenAndServe()
		if err != nil {
			logs.WithError(err).Fatal("cannot start http proxy")
		}
	}()

	err = srv.ListenAndServeTLS(crt, key)
	if err != nil {
		logs.WithError(err).Fatal("cannot start proxy")
		return
	}
}

func (p *WorkspaceProxy) Handler(newCheckAuth checkAuth) (http.Handler, error) {
	r := mux.NewRouter()

	// install routes
	authOpt := WithDefaultAuth(clients.CheckAuthBackend)
	if newCheckAuth != nil {
		authOpt = WithDefaultAuth(newCheckAuth)
	}
	handlerConfig, err := NewRouteHandlerConfig(&p.Config, &p.BackendConfig, authOpt)
	if err != nil {
		return nil, err
	}
	ideRouter, portRouter := p.WorkspaceRouter(r, p.WorkspaceInfoProvider)
	err = installWorkspaceRoutes(ideRouter, handlerConfig, p.WorkspaceInfoProvider, p.SSHHostSigners)
	if err != nil {
		return nil, err
	}
	err = installWorkspacePortRoutes(portRouter, handlerConfig, p.WorkspaceInfoProvider)
	if err != nil {
		return nil, err
	}
	return r, nil
}

func redirectToHTTPS(w http.ResponseWriter, r *http.Request) {
	target := "https://" + r.Host + r.URL.Path
	if len(r.URL.RawQuery) > 0 {
		target += "?" + r.URL.RawQuery
	}
	logs.WithField("target", target).Debug("redirect to https")
	http.Redirect(w, r, target, http.StatusTemporaryRedirect)
}

// defaultCipherSuitesWithAESNI 假设 AES-NI 的密码套件（AES 的硬件加速）
var defaultCipherSuitesWithAESNI = []uint16{
	tls.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
	tls.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
	tls.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
	tls.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
	tls.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305,
	tls.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305,
}

// defaultCipherSuites 假定缺少 AES-NI（AES 没有硬件加速）
var defaultCipherSuitesWithoutAESNI = []uint16{
	tls.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305,
	tls.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305,
	tls.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
	tls.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
	tls.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
	tls.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
}

// optimalDefaultCipherSuites 返回一个合适的密码
// 要使用的套件取决于AES的硬件支持。
func optimalDefaultCipherSuites() []uint16 {
	if cpuid.CPU.Supports(cpuid.AESNI) {
		return defaultCipherSuitesWithAESNI
	}
	return defaultCipherSuitesWithoutAESNI
}

var tlsHandshakeErrorPrefix = []byte("http: TLS handshake error")

type logrusErrorWriter struct{}

func (w logrusErrorWriter) Write(p []byte) (int, error) {
	if bytes.Contains(p, tlsHandshakeErrorPrefix) {
		return len(p), nil
	}

	logs.Errorf("%s", string(p))
	return len(p), nil
}
