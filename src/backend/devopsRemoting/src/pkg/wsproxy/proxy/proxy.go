package proxy

import (
	"devopsRemoting/common/logs"
	"devopsRemoting/src/pkg/wsproxy/clients"
	"devopsRemoting/src/pkg/wsproxy/config"
	"net/http"

	"github.com/ci-plugins/crypto-go/ssh"
	"github.com/gorilla/mux"
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
		Addr:    p.Ingress.HTTPAddress,
		Handler: handler,
		// TODO: 未来有证书了再添加https
		// TLSConfig: &tls.Config{
		// 	CipherSuites:             optimalDefaultCipherSuites(),
		// 	CurvePreferences:         []tls.CurveID{tls.CurveP521, tls.CurveP384, tls.CurveP256},
		// 	MinVersion:               tls.VersionTLS12,
		// 	MaxVersion:               tls.VersionTLS12,
		// 	PreferServerCipherSuites: true,
		// 	NextProtos:               []string{"h2", "http/1.1"},
		// },
	}

	// var (
	// 	crt = p.Config.HTTPS.Certificate
	// 	key = p.Config.HTTPS.Key
	// )
	// if tproot := os.Getenv("TELEPRESENCE_ROOT"); tproot != "" {
	// 	crt = filepath.Join(tproot, crt)
	// 	key = filepath.Join(tproot, key)
	// }
	// go func() {
	// 	err := http.ListenAndServe(p.Ingress.HTTPAddress, http.HandlerFunc(redirectToHTTPS))
	// 	if err != nil {
	// 		logs.WithError(err).Fatal("cannot start http proxy")
	// 	}
	// }()

	err = srv.ListenAndServe()
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
