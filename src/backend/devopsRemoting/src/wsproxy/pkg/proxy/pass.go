package proxy

import (
	"common/logs"
	"fmt"
	"net"
	"net/http"
	"net/http/httputil"
	"net/url"
	"strings"
	"syscall"
	"wsproxy/pkg/config"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"
)

type proxyPassConfig struct {
	TargetResolver  targetResolver
	ResponseHandler []responseHandler
	ErrorHandler    errorHandler
	Transport       http.RoundTripper
	UseTargetHost   bool
}

func (ppc *proxyPassConfig) appendResponseHandler(handler responseHandler) {
	ppc.ResponseHandler = append(ppc.ResponseHandler, handler)
}

// proxyPassOpt 允许组合 ProxyHandler 选项。
type proxyPassOpt func(h *proxyPassConfig)

// errorHandler 是一个处理 HTTP 请求代理期间发生的错误的函数。
type errorHandler func(http.ResponseWriter, *http.Request, error)

// targetResolver 是一个函数，用于确定将给定 HTTP 请求转发到哪个目标。
type targetResolver func(*config.ProxyConfig, WorkspaceInfoProvider, *http.Request) (*url.URL, error)

type responseHandler func(*http.Response, *http.Request) error

// proxyPass 是从配置、解析器和各种选项组装 ProxyHandler 并返回 http.HandlerFunc 的函数。
func proxyPass(config *RouteHandlerConfig, infoProvider WorkspaceInfoProvider, resolver targetResolver, opts ...proxyPassOpt) http.HandlerFunc {
	h := proxyPassConfig{
		Transport: config.DefaultTransport,
	}
	for _, o := range opts {
		o(&h)
	}
	h.TargetResolver = resolver

	if h.ErrorHandler != nil {
		oeh := h.ErrorHandler
		h.ErrorHandler = func(w http.ResponseWriter, req *http.Request, connectErr error) {
			logs.Debugf("could not connect to backend %s: %s", req.URL.String(), connectErrorToCause(connectErr))
			oeh(w, req, connectErr)
		}
	}

	return func(w http.ResponseWriter, req *http.Request) {
		targetURL, err := h.TargetResolver(config.Config, infoProvider, req)
		if err != nil {
			if h.ErrorHandler != nil {
				h.ErrorHandler(w, req, err)
			} else {
				logs.WithError(err).Errorf("Unable to resolve targetURL: %s", req.URL.String())
			}
			return
		}

		originalURL := *req.URL

		proxy := NewSingleHostReverseProxy(targetURL, h.UseTargetHost)
		proxy.Transport = h.Transport
		proxy.ModifyResponse = func(resp *http.Response) error {
			url := resp.Request.URL
			if url == nil {
				return errors.Errorf("response's request without URL")
			}

			if logs.Logs.Level <= logrus.DebugLevel && resp.StatusCode >= http.StatusBadRequest {
				dmp, _ := httputil.DumpRequest(resp.Request, false)
				logs.WithField("url", url.String()).WithField("req", dmp).WithField("status", resp.Status).Debug("proxied request failed")
			}

			// execute response handlers in order of registration
			for _, handler := range h.ResponseHandler {
				err := handler(resp, req)
				if err != nil {
					return err
				}
			}

			return nil
		}

		proxy.ErrorHandler = func(rw http.ResponseWriter, req *http.Request, err error) {
			if h.ErrorHandler != nil {
				req.URL = &originalURL
				h.ErrorHandler(w, req, err)
				return
			}

			rw.WriteHeader(http.StatusBadGateway)
		}

		getLog(req.Context()).WithField("targetURL", targetURL.String()).Debug("proxy-passing request")
		proxy.ServeHTTP(w, req)
	}
}

func connectErrorToCause(err error) string {
	if err == nil {
		return ""
	}

	if netError, ok := err.(net.Error); ok && netError.Timeout() {
		return "Connect timeout"
	}

	switch t := err.(type) {
	case *net.OpError:
		if t.Op == "dial" {
			return fmt.Sprintf("Unknown host: %s", err.Error())
		} else if t.Op == "read" {
			return fmt.Sprintf("Connection refused: %s", err.Error())
		}

	case syscall.Errno:
		if t == syscall.ECONNREFUSED {
			return "Connection refused"
		}
	}

	return err.Error()
}

func NewSingleHostReverseProxy(target *url.URL, useTargetHost bool) *httputil.ReverseProxy {
	targetQuery := target.RawQuery
	director := func(req *http.Request) {
		req.URL.Scheme = target.Scheme
		req.URL.Host = target.Host
		if useTargetHost {
			req.Host = target.Host
		}
		req.URL.Path, req.URL.RawPath = joinURLPath(target, req.URL)
		if targetQuery == "" || req.URL.RawQuery == "" {
			req.URL.RawQuery = targetQuery + req.URL.RawQuery
		} else {
			req.URL.RawQuery = targetQuery + "&" + req.URL.RawQuery
		}
		if _, ok := req.Header["User-Agent"]; !ok {
			// 显式禁用 User-Agent，因此它不会设置为默认值
			req.Header.Set("User-Agent", "")
		}
	}
	return &httputil.ReverseProxy{Director: director}
}

func joinURLPath(a, b *url.URL) (path, rawpath string) {
	if a.RawPath == "" && b.RawPath == "" {
		return singleJoiningSlash(a.Path, b.Path), ""
	}
	// 同singleJoiningSlash，但是使用EscapedPath来判断
	// 是否应该添加斜线
	apath := a.EscapedPath()
	bpath := b.EscapedPath()

	aslash := strings.HasSuffix(apath, "/")
	bslash := strings.HasPrefix(bpath, "/")

	switch {
	case aslash && bslash:
		return a.Path + b.Path[1:], apath + bpath[1:]
	case !aslash && !bslash:
		return a.Path + "/" + b.Path, apath + "/" + bpath
	}
	return a.Path + b.Path, apath + bpath
}

func singleJoiningSlash(a, b string) string {
	aslash := strings.HasSuffix(a, "/")
	bslash := strings.HasPrefix(b, "/")
	switch {
	case aslash && bslash:
		return a + b[1:]
	case !aslash && !bslash:
		return a + "/" + b
	}
	return a + b
}

func withHTTPErrorHandler(h http.Handler) proxyPassOpt {
	return func(cfg *proxyPassConfig) {
		cfg.ErrorHandler = func(w http.ResponseWriter, req *http.Request, _ error) {
			h.ServeHTTP(w, req)
		}
	}
}

func withXFrameOptionsFilter() proxyPassOpt {
	return func(cfg *proxyPassConfig) {
		cfg.appendResponseHandler(func(resp *http.Response, _ *http.Request) error {
			resp.Header.Del("X-Frame-Options")
			return nil
		})
	}
}
