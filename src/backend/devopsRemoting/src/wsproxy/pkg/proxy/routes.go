package proxy

import (
	"common/logs"
	"context"
	"fmt"
	"math/rand"
	"net"
	"net/http"
	"net/url"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"time"
	"wsproxy/pkg/config"

	"github.com/ci-plugins/crypto-go/ssh"
	"github.com/gorilla/handlers"
	"github.com/gorilla/mux"
	"github.com/sirupsen/logrus"
)

type RouteHandlerConfig struct {
	Config               *config.ProxyConfig
	BackendConfig        *config.DevRemotingBackend
	DefaultTransport     http.RoundTripper
	CorsHandler          mux.MiddlewareFunc
	WorkspaceAuthHandler mux.MiddlewareFunc
}

type RouteHandlerConfigOpt func(*config.ProxyConfig, *config.DevRemotingBackend, *RouteHandlerConfig)

func WithDefaultAuth(checkAuth checkAuth) RouteHandlerConfigOpt {
	return func(_ *config.ProxyConfig, backend *config.DevRemotingBackend, c *RouteHandlerConfig) {
		c.WorkspaceAuthHandler = WorkspaceAuthHandler(backend.HostName, backend.OauthRedirectUrl, checkAuth)
	}
}

func NewRouteHandlerConfig(config *config.ProxyConfig, backend *config.DevRemotingBackend, opts ...RouteHandlerConfigOpt) (*RouteHandlerConfig, error) {
	corsHandler, err := corsHandler(backend.HostName)
	if err != nil {
		return nil, err
	}

	cfg := &RouteHandlerConfig{
		Config:               config,
		BackendConfig:        backend,
		DefaultTransport:     createDefaultTransport(config.TransportConfig),
		CorsHandler:          corsHandler,
		WorkspaceAuthHandler: func(h http.Handler) http.Handler { return h },
	}
	for _, o := range opts {
		o(config, backend, cfg)
	}
	return cfg, nil
}

// installWorkspaceRoutes 用来代理工作空间内IDE相关请求
func installWorkspaceRoutes(r *mux.Router, config *RouteHandlerConfig, ip WorkspaceInfoProvider, _ []ssh.Signer) error {
	r.Use(logHandler)

	// 注意：路由的顺序定义了它们的优先级。
	// 先注册的路由优先于后注册的路由。
	routes := newIDERoutes(config, ip)
	// 获取remoting api服务状态接口
	routes.HandleDirectRemotingRoute(r.PathPrefix("/_remoting/api/remoting/status"), false)
	// 更新bkticket
	routes.HandleDirectRemotingRoute(r.PathPrefix("/_remoting/api/token/updateBkTicket"), true)

	rootRouter := enableCompression(r)

	routes.HandleRoot(rootRouter.NewRoute())

	return nil
}

func enableCompression(r *mux.Router) *mux.Router {
	res := r.NewRoute().Subrouter()
	res.Use(handlers.CompressHandler)
	return res
}

func newIDERoutes(config *RouteHandlerConfig, ip WorkspaceInfoProvider) *ideRoutes {
	return &ideRoutes{
		Config:       config,
		InfoProvider: ip,
	}
}

type ideRoutes struct {
	Config       *RouteHandlerConfig
	InfoProvider WorkspaceInfoProvider
}

func (ir *ideRoutes) HandleDirectRemotingRoute(route *mux.Route, authenticated bool) {
	r := route.Subrouter()
	r.Use(logRouteHandlerHandler(fmt.Sprintf("HandleDirectRemotingRoute (authenticated: %v)", authenticated)))
	r.Use(ir.Config.CorsHandler)
	if authenticated {
		r.Use(ir.Config.WorkspaceAuthHandler)
	}

	r.NewRoute().HandlerFunc(proxyPass(ir.Config, ir.InfoProvider, workspacePodRemotingResolver))
}

func (ir *ideRoutes) HandleRoot(route *mux.Route) {
	r := route.Subrouter()
	r.Use(logRouteHandlerHandler("handleRoot"))
	r.Use(ir.Config.CorsHandler)

	r.Use(ir.Config.WorkspaceAuthHandler)

	r.NewRoute().HandlerFunc(proxyPass(ir.Config, ir.InfoProvider, workspacePodResolver))
}

// corsHandler 为工作区生成 CORS 处理程序。
func corsHandler(host string) (mux.MiddlewareFunc, error) {
	hostname := strings.Split(host, "://")[1]

	domainRegex := strings.ReplaceAll(hostname, ".", "\\.")
	originRegex, err := regexp.Compile(".*" + domainRegex)
	if err != nil {
		return nil, err
	}

	return handlers.CORS(
		handlers.AllowedOriginValidator(func(origin string) bool {
			// 来源是安装主机名的子域吗？
			matches := originRegex.Match([]byte(origin))
			return matches
		}),
		handlers.AllowedMethods([]string{
			"GET",
			"POST",
			"OPTIONS",
		}),
		handlers.AllowedHeaders([]string{
			// "Accept", "Accept-Language", "Content-Language" 默认放开
			"Cache-Control",
			"Content-Type",
			"DNT",
			"If-Modified-Since",
			"Keep-Alive",
			"Origin",
			"User-Agent",
			"X-Requested-With",
		}),
		handlers.AllowCredentials(),
		// 需要能够在前端读取授权标头
		handlers.ExposedHeaders([]string{"Authorization"}),
		handlers.MaxAge(60),
		handlers.OptionStatusCode(200),
	), nil
}

func createDefaultTransport(config *config.TransportConfig) *http.Transport {
	// 这是基于 http.DefaultTransport，一些值暴露给配置
	return &http.Transport{
		Proxy: http.ProxyFromEnvironment,
		DialContext: (&net.Dialer{
			Timeout:   time.Duration(config.ConnectTimeout), // default: 30s
			KeepAlive: 30 * time.Second,
			DualStack: true,
		}).DialContext,
		ForceAttemptHTTP2:     true,
		MaxIdleConns:          config.MaxIdleConns,                   // default: 0 (无限数量的的池中链接)
		MaxIdleConnsPerHost:   config.MaxIdleConnsPerHost,            // default: 100 (池中每个主机的最大连接数)
		IdleConnTimeout:       time.Duration(config.IdleConnTimeout), // default: 90s
		TLSHandshakeTimeout:   10 * time.Second,
		ExpectContinueTimeout: 1 * time.Second,
	}
}

const (
	builtinPagePortNotFound = "port-not-found.html"
)

func installWorkspacePortRoutes(r *mux.Router, config *RouteHandlerConfig, infoProvider WorkspaceInfoProvider) error {
	showPortNotFoundPage, err := servePortNotFoundPage(config.Config)
	if err != nil {
		return err
	}

	r.Use(logHandler)
	r.Use(config.WorkspaceAuthHandler)
	// 过滤所有会话 cookie
	r.Use(sensitiveCookieHandler(config.BackendConfig.HostName))

	// 把请求发到workspaceport
	r.NewRoute().HandlerFunc(
		func(rw http.ResponseWriter, r *http.Request) {
			// 针对不区分大小写标头的服务器的解决方法，请参阅 https://github.com/gitpod-io/gitpod/issues/4047#issuecomment-856566526
			for _, name := range []string{"Key", "Extensions", "Accept", "Protocol", "Version"} {
				values := r.Header["Sec-Websocket-"+name]
				if len(values) != 0 {
					r.Header.Del("Sec-Websocket-" + name)
					r.Header["Sec-WebSocket-"+name] = values
				}
			}
			r.Header.Add("X-Forwarded-Proto", "https")
			r.Header.Add("X-Forwarded-Host", r.Host)
			r.Header.Add("X-Forwarded-Port", "443")
			proxyPass(
				config,
				infoProvider,
				workspacePodPortResolver,
				withHTTPErrorHandler(showPortNotFoundPage),
				withXFrameOptionsFilter(),
			)(rw, r)
		},
	)

	return nil
}

type wsproxyContextKey struct{}

var (
	logContextValueKey = wsproxyContextKey{}
)

// logHandler 为每个请求添加log的前缀
func logHandler(h http.Handler) http.Handler {
	return http.HandlerFunc(func(resp http.ResponseWriter, req *http.Request) {
		var (
			vars = mux.Vars(req)
			wsID = vars[workspaceIDIdentifier]
			port = vars[workspacePortIdentifier]
		)
		entry := logrus.Fields{
			"workspaceId": wsID,
			"portID":      port,
			"url":         req.URL.String(),
		}
		ctx := context.WithValue(req.Context(), logContextValueKey, entry)
		req = req.WithContext(ctx)

		h.ServeHTTP(resp, req)
	})
}

// 为每个路由代理添加日志
func logRouteHandlerHandler(routeHandlerName string) mux.MiddlewareFunc {
	return func(h http.Handler) http.Handler {
		return http.HandlerFunc(func(resp http.ResponseWriter, req *http.Request) {
			getLog(req.Context()).WithField("routeHandler", routeHandlerName).Debug("hit route handler")
			h.ServeHTTP(resp, req)
		})
	}
}

func getLog(ctx context.Context) *logrus.Entry {
	r := ctx.Value(logContextValueKey)
	rl, ok := r.(logrus.Fields)
	if rl == nil || !ok {
		return logs.Logs
	}

	return logs.WithFields(rl)
}

func sensitiveCookieHandler(domain string) func(h http.Handler) http.Handler {
	return func(h http.Handler) http.Handler {
		return http.HandlerFunc(func(resp http.ResponseWriter, req *http.Request) {
			cookies := removeSensitiveCookies(readCookies(req.Header, ""), domain)
			header := make([]string, 0, len(cookies))
			for _, c := range cookies {
				if c == nil {
					continue
				}

				cookie := c.String()
				if cookie == "" {
					// 因为我们在上面检查 nil，所以一定是 cookie 名称无效。
					// 有些语言不反对生成无效的 cookie 名称，所以我们也必须这样做。
					// 有关详细信息，请参阅 https://github.com/gitpod-io/gitpod/issues/2470。
					var (
						originalName    = c.Name
						replacementName = fmt.Sprintf("name%d%d", rand.Uint64(), time.Now().Unix())
					)
					c.Name = replacementName
					cookie = c.String()
					if cookie == "" {
						// 尽管我们尽了最大的努力，我们仍然无法呈现 cookie。 我们会放弃
						continue
					}

					cookie = strings.Replace(cookie, replacementName, originalName, 1)
					c.Name = originalName
				}

				header = append(header, cookie)
			}

			// 这里直接使用header string slice会导致多个cookie header
			// 正在发送。 请参阅 https://github.com/gitpod-io/gitpod/issues/2121。
			req.Header["Cookie"] = []string{strings.Join(header, ";")}

			h.ServeHTTP(resp, req)
		})
	}
}

// removeSensitiveCookies 列表中的所有敏感 cookie。
func removeSensitiveCookies(cookies []*http.Cookie, domain string) []*http.Cookie {
	hostnamePrefix := domain
	for _, c := range []string{" ", "-", "."} {
		hostnamePrefix = strings.ReplaceAll(hostnamePrefix, c, "_")
	}
	hostnamePrefix = "_" + hostnamePrefix + "_"

	n := 0
	for _, c := range cookies {
		if strings.EqualFold(c.Name, hostnamePrefix) {
			// skip session cookie
			continue
		}
		if strings.HasPrefix(c.Name, hostnamePrefix) && strings.HasSuffix(c.Name, "_port_auth_") {
			// skip port auth cookie
			continue
		}
		if strings.HasPrefix(c.Name, hostnamePrefix) && strings.HasSuffix(c.Name, "_owner_") {
			// skip owner token
			continue
		}
		logs.WithField("hostnamePrefix", hostnamePrefix).WithField("name", c.Name).Debug("keeping cookie")
		cookies[n] = c
		n++
	}
	return cookies[:n]
}

// workspacePodResolver 解析到工作区的pod，主要是web版vscode
func workspacePodResolver(config *config.ProxyConfig, infoProvider WorkspaceInfoProvider, req *http.Request) (url *url.URL, err error) {
	coords := getWorkspaceCoords(req)
	workspaceInfo := infoProvider.WorkspaceInfo(coords.ID)
	return buildWorkspacePodURL(workspaceInfo.IPAddress, fmt.Sprint(config.WorkspacePodConfig.VscodePort))
}

// workspacePodPortResolver 解析为工作区 pod 端口。
func workspacePodPortResolver(_ *config.ProxyConfig, infoProvider WorkspaceInfoProvider, req *http.Request) (url *url.URL, err error) {
	coords := getWorkspaceCoords(req)
	workspaceInfo := infoProvider.WorkspaceInfo(coords.ID)
	return buildWorkspacePodURL(workspaceInfo.IPAddress, coords.Port)
}

// workspacePodRemotingResolver 将请求解析到工作空间的remoting端口
func workspacePodRemotingResolver(config *config.ProxyConfig, infoProvider WorkspaceInfoProvider, req *http.Request) (url *url.URL, err error) {
	coords := getWorkspaceCoords(req)
	port := fmt.Sprint(config.WorkspacePodConfig.RemotingPort)
	workspaceInfo := infoProvider.WorkspaceInfo(coords.ID)
	if workspaceInfo == nil {
		return nil, fmt.Errorf("not found workspace %s", coords.ID)
	}
	return buildWorkspacePodURL(workspaceInfo.IPAddress, port)
}

func buildWorkspacePodURL(ipAddress string, port string) (*url.URL, error) {
	return url.Parse(fmt.Sprintf("http://%v:%v", ipAddress, port))
}

func servePortNotFoundPage(config *config.ProxyConfig) (http.Handler, error) {
	fn := filepath.Join(config.BuiltinPages.Location, builtinPagePortNotFound)
	page, err := os.ReadFile(fn)
	if err != nil {
		return nil, err
	}

	return http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusNotFound)
		_, _ = w.Write(page)
	}), nil
}
