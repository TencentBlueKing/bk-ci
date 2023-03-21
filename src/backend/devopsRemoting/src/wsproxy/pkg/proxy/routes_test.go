package proxy

import (
	"common/logs"
	"common/util"
	"context"
	"fmt"
	"io"
	"net"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
	"wsproxy/pkg/config"

	"github.com/google/go-cmp/cmp"
	"github.com/sirupsen/logrus"
)

const (
	hostBasedHeader = "x-host-header"
	wsHostSuffix    = ".test.com"
	wsHostNameRegex = "\\.test\\.com"
)

var (
	workspaces = []WorkspaceInfo{
		{
			WorkspaceID: "remoting-12138",
		},
	}

	workspacePortUrl = "http://3000-remoting-12138.test.com/"

	ideServerHost = "localhost:20000"
	workspacePort = uint16(20001)
	remotingPort  = uint16(20002)
	workspaceHost = fmt.Sprintf("localhost:%d", workspacePort)
	portServeHost = fmt.Sprintf("localhost:%d", 3000)
	blobServeHost = "localhost:20003"

	proxyConfig = config.ProxyConfig{
		TransportConfig: &config.TransportConfig{
			ConnectTimeout:      util.Duration(10 * time.Second),
			IdleConnTimeout:     util.Duration(60 * time.Second),
			MaxIdleConns:        0,
			MaxIdleConnsPerHost: 100,
		},
		BuiltinPages: config.BuiltinPagesConfig{
			Location: "../../public",
		},
	}
	devRemotingBackend = config.DevRemotingBackend{
		HostName: "http://test.com",
	}
)

type Target struct {
	Status  int
	Handler func(w http.ResponseWriter, r *http.Request, requestCount uint8)
}

type testTarget struct {
	Target       *Target
	RequestCount uint8
	listener     net.Listener
	server       *http.Server
}

func (tt *testTarget) Close() {
	_ = tt.listener.Close()
	_ = tt.server.Shutdown(context.Background())
}

// startTestTarget starts a new HTTP server that serves as some test target during the unit tests.
func startTestTarget(t *testing.T, host, name string, checkedHost bool) *testTarget {
	t.Helper()

	l, err := net.Listen("tcp", host)
	if err != nil {
		t.Fatalf("cannot start fake IDE host: %q", err)
		return nil
	}

	tt := &testTarget{
		Target:   &Target{Status: http.StatusOK},
		listener: l,
	}
	srv := &http.Server{Addr: host, Handler: http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		defer func() {
			tt.RequestCount++
		}()

		if tt.Target.Handler != nil {
			tt.Target.Handler(w, r, tt.RequestCount)
			return
		}

		if tt.Target.Status == http.StatusOK {
			w.Header().Set("Content-Type", "text/plain; charset=utf-8")
			w.WriteHeader(http.StatusOK)
			format := "%s hit: %s\n"
			args := []interface{}{name, r.URL.String()}
			if checkedHost {
				format += "host: %s\n"
				args = append(args, r.Host)
			}
			inlineVars := r.Header.Get("X-BlobServe-InlineVars")
			if inlineVars != "" {
				format += "inlineVars: %s\n"
				args = append(args, inlineVars)
			}
			fmt.Fprintf(w, format, args...)
			return
		}

		if tt.Target.Status != 0 {
			w.WriteHeader(tt.Target.Status)
			return
		}
		w.WriteHeader(http.StatusOK)
	})}
	go func() { _ = srv.Serve(l) }()
	tt.server = srv

	return tt
}

type requestModifier func(r *http.Request)

func addHeader(name string, val string) requestModifier {
	return func(r *http.Request) {
		r.Header.Add(name, val)
	}
}

func addAuthHeader(r *http.Request) {
	r.AddCookie(&http.Cookie{
		Name:  "X-DEVOPS-BK-TICKET",
		Value: "123",
	})
}

func addHostHeader(r *http.Request) {
	r.Header.Add(hostBasedHeader, r.Host)
}

func addCookie(c http.Cookie) requestModifier {
	return func(r *http.Request) {
		r.AddCookie(&c)
	}
}

func modifyRequest(r *http.Request, mod ...requestModifier) *http.Request {
	for _, m := range mod {
		m(r)
	}
	return r
}

func TestRoutes(t *testing.T) {
	type RouterFactory func(cfg *config.ProxyConfig) WorkspaceRouter
	type Expectation struct {
		Status int
		Header http.Header
		Body   string
	}
	type Targets struct {
		IDE       *Target
		Blobserve *Target
		Workspace *Target
		Remoting  *Target
		Port      *Target
	}
	tests := []struct {
		Desc        string
		Config      *config.ProxyConfig
		Request     *http.Request
		Router      RouterFactory
		Targets     *Targets
		IgnoreBody  bool
		Expectation Expectation
	}{
		{
			Desc: "port GET 404",
			Request: modifyRequest(httptest.NewRequest("GET", workspacePortUrl+"this-does-not-exist", nil),
				addHostHeader,
				addAuthHeader,
			),
			Targets: &Targets{Port: &Target{
				Handler: func(w http.ResponseWriter, r *http.Request, _ uint8) {
					w.WriteHeader(http.StatusNotFound)
					fmt.Fprintf(w, "host: %s\n", r.Host)
				},
			}},
			Expectation: Expectation{
				Header: http.Header{"Content-Length": {"35"}, "Content-Type": {"text/plain; charset=utf-8"}},
				Status: http.StatusNotFound,
				Body:   "host: 3000-remoting-12138.test.com\n",
			},
		},
		{
			Desc: "port GET unexposed",
			Request: modifyRequest(httptest.NewRequest("GET", workspacePortUrl+"this-does-not-exist", nil),
				addHostHeader,
				addAuthHeader,
			),
			Targets:    &Targets{},
			IgnoreBody: true,
			Expectation: Expectation{
				Status: http.StatusNotFound,
				Body:   "",
			},
		},
		{
			Desc: "port cookies",
			Request: modifyRequest(httptest.NewRequest("GET", workspacePortUrl+"this-does-not-exist", nil),
				addHostHeader,
				addAuthHeader,
				addCookie(http.Cookie{Name: "foobar", Value: "baz"}),
				addCookie(http.Cookie{Name: "another", Value: "cookie"}),
			),
			Targets: &Targets{
				Port: &Target{
					Handler: func(w http.ResponseWriter, r *http.Request, _ uint8) {
						fmt.Fprintf(w, "host: %s\n", r.Host)
						fmt.Fprintf(w, "%+q\n", r.Header["Cookie"])
					},
				},
			},
			Expectation: Expectation{
				Status: http.StatusOK,
				Header: http.Header{"Content-Length": {"88"}, "Content-Type": {"text/plain; charset=utf-8"}},
				Body:   "host: 3000-remoting-12138.test.com\n[\"X-DEVOPS-BK-TICKET=123;foobar=baz;another=cookie\"]\n",
			},
		},
		{
			Desc: "port GET 200 w/o X-Frame-Options header",
			Request: modifyRequest(httptest.NewRequest("GET", workspacePortUrl+"returns-200-with-frame-options-header", nil),
				addHostHeader,
				addAuthHeader,
			),
			Targets: &Targets{
				Port: &Target{
					Handler: func(w http.ResponseWriter, r *http.Request, _ uint8) {
						w.Header().Add("X-Frame-Options", "sameorigin")
						fmt.Fprintf(w, "host: %s\n", r.Host)
						w.WriteHeader(http.StatusOK)
					},
				},
			},
			Expectation: Expectation{
				Header: http.Header{
					"Content-Length": {"35"},
					"Content-Type":   {"text/plain; charset=utf-8"},
				},
				Status: http.StatusOK,
				Body:   "host: 3000-remoting-12138.test.com\n",
			},
		},
	}

	logs.Init("ws-proxy-test", "", false, true)
	logs.Logs.Logger.SetLevel(logrus.ErrorLevel)

	defaultTargets := &Targets{
		IDE:       &Target{Status: http.StatusOK},
		Blobserve: &Target{Status: http.StatusOK},
		Port:      &Target{Status: http.StatusOK},
		Remoting:  &Target{Status: http.StatusOK},
		Workspace: &Target{Status: http.StatusOK},
	}
	targets := make(map[string]*testTarget)
	controlTarget := func(target *Target, name, host string, checkedHost bool) {
		_, runs := targets[name]
		if runs && target == nil {
			targets[name].Close()
			delete(targets, name)
			return
		}

		if !runs && target != nil {
			targets[name] = startTestTarget(t, host, name, checkedHost)
			runs = true
		}

		if runs {
			targets[name].Target = target
			targets[name].RequestCount = 0
		}
	}
	defer func() {
		for _, c := range targets {
			c.Close()
		}
	}()

	for _, test := range tests {
		if test.Targets == nil {
			test.Targets = defaultTargets
		}

		t.Run(test.Desc, func(t *testing.T) {
			controlTarget(test.Targets.Port, "port", portServeHost, true)

			cfg := proxyConfig
			if test.Config != nil {
				cfg = *test.Config
			}
			router := HostBasedRouter(hostBasedHeader, wsHostSuffix)
			if test.Router != nil {
				router = test.Router(&cfg)
			}

			ingress := config.HostBasedIngressConfig{
				HTTPAddress:  "8080",
				HTTPSAddress: "9090",
			}

			proxy := NewWorkspaceProxy(ingress, cfg, devRemotingBackend, router, &fakeWsInfoProvider{infos: workspaces}, nil)
			handler, err := proxy.Handler(func(ctx context.Context, host, wsid, ticket string) (bool, error) {
				return true, nil
			})
			if err != nil {
				t.Fatalf("cannot create proxy handler: %q", err)
			}

			rec := httptest.NewRecorder()
			handler.ServeHTTP(rec, test.Request)
			resp := rec.Result()

			body, _ := io.ReadAll(resp.Body)
			resp.Body.Close()
			act := Expectation{
				Status: resp.StatusCode,
				Body:   string(body),
				Header: resp.Header,
			}

			delete(act.Header, "Date")

			if len(act.Header) == 0 {
				act.Header = nil
			}
			if test.IgnoreBody == true {
				test.Expectation.Body = act.Body
			}
			if diff := cmp.Diff(test.Expectation, act); diff != "" {
				t.Errorf("Expectation mismatch (-want +got):\n%s", diff)
			}
		})
	}
}

type fakeWsInfoProvider struct {
	infos []WorkspaceInfo
}

// GetWsInfoByID returns the workspace for the given ID.
func (p *fakeWsInfoProvider) WorkspaceInfo(workspaceID string) *WorkspaceInfo {
	for _, nfo := range p.infos {
		if nfo.WorkspaceID == workspaceID {
			return &nfo
		}
	}

	return nil
}

func TestRemoveSensitiveCookies(t *testing.T) {
	var (
		domain            = "test.com"
		sessionCookie     = &http.Cookie{Domain: domain, Name: "_test_com_", Value: "fobar"}
		portAuthCookie    = &http.Cookie{Domain: domain, Name: "_test_com_ws_77f6b236_3456_4b88_8284_81ca543a9d65_port_auth_", Value: "some-token"}
		ownerCookie       = &http.Cookie{Domain: domain, Name: "_test_com_ws_77f6b236_3456_4b88_8284_81ca543a9d65_owner_", Value: "some-other-token"}
		miscCookie        = &http.Cookie{Domain: domain, Name: "some-other-cookie", Value: "I like cookies"}
		invalidCookieName = &http.Cookie{Domain: domain, Name: "foobar[0]", Value: "violates RFC6266"}
	)

	tests := []struct {
		Name     string
		Input    []*http.Cookie
		Expected []*http.Cookie
	}{
		{"no cookies", []*http.Cookie{}, []*http.Cookie{}},
		{"session cookie", []*http.Cookie{sessionCookie, miscCookie}, []*http.Cookie{miscCookie}},
		{"portAuth cookie", []*http.Cookie{portAuthCookie, miscCookie}, []*http.Cookie{miscCookie}},
		{"owner cookie", []*http.Cookie{ownerCookie, miscCookie}, []*http.Cookie{miscCookie}},
		{"misc cookie", []*http.Cookie{miscCookie}, []*http.Cookie{miscCookie}},
		{"invalid cookie name", []*http.Cookie{invalidCookieName}, []*http.Cookie{invalidCookieName}},
	}
	for _, test := range tests {
		t.Run(test.Name, func(t *testing.T) {
			res := removeSensitiveCookies(test.Input, domain)
			if diff := cmp.Diff(test.Expected, res); diff != "" {
				t.Errorf("unexpected result (-want +got):\n%s", diff)
			}
		})
	}
}

func TestSensitiveCookieHandler(t *testing.T) {
	var (
		domain     = "test.com"
		miscCookie = &http.Cookie{Domain: domain, Name: "some-other-cookie", Value: "I like cookies"}
	)
	tests := []struct {
		Name     string
		Input    string
		Expected string
	}{
		{"no cookies", "", ""},
		{"valid cookie", miscCookie.String(), `some-other-cookie="I like cookies";Domain=test.com`},
		{"invalid cookie", `foobar[0]="violates RFC6266"`, `foobar[0]="violates RFC6266"`},
	}
	for _, test := range tests {
		t.Run(test.Name, func(t *testing.T) {
			req := httptest.NewRequest("GET", "http://"+domain, nil)
			if test.Input != "" {
				req.Header.Set("cookie", test.Input)
			}
			rec := httptest.NewRecorder()

			var act string
			sensitiveCookieHandler(domain)(http.HandlerFunc(func(rw http.ResponseWriter, r *http.Request) {
				act = r.Header.Get("cookie")
				rw.WriteHeader(http.StatusOK)
			})).ServeHTTP(rec, req)

			if diff := cmp.Diff(test.Expected, act); diff != "" {
				t.Errorf("unexpected result (-want +got):\n%s", diff)
			}
		})
	}
}
