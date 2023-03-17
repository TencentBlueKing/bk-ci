package proxy

import (
	"devopsRemoting/common/logs"
	"net/http"
	"path/filepath"
	"strconv"
	"strings"

	"github.com/gorilla/mux"
)

const (
	// 用作在请求 mux.Vars() 映射中存储工作区端口的键。
	workspacePortIdentifier = "workspacePort"

	// 用作在请求 mux.Vars() 映射中存储工作区 ID 的键。
	workspaceIDIdentifier = "workspaceID"
)

type WorkspaceRouter func(r *mux.Router, wsInfoProvider WorkspaceInfoProvider) (ideRouter *mux.Router, portRouter *mux.Router)

type WorkspaceCoords struct {
	// The workspace ID
	ID string
	// The workspace port
	Port string
}

func getWorkspaceCoords(req *http.Request) WorkspaceCoords {
	vars := mux.Vars(req)
	return WorkspaceCoords{
		ID:   vars[workspaceIDIdentifier],
		Port: vars[workspacePortIdentifier],
	}
}

func HostBasedRouter(header, wsHostSuffix string) WorkspaceRouter {
	return func(r *mux.Router, _ WorkspaceInfoProvider) (*mux.Router, *mux.Router) {
		// 确保 acme 路由器是第一个处理程序设置，以确保它有机会接受 acme 挑战
		setupAcmeRouter(r)

		var (
			getHostHeader = func(req *http.Request) string {
				host := req.Header.Get(header)
				// 如果我们没有从特殊标头中获取主机，则回退到使用 req.Host
				if header == "Host" || host == "" {
					parts := strings.Split(req.Host, ":")
					return parts[0]
				}
				return host
			}
			portRouter = r.MatcherFunc(matchWorkspaceHostHeader(wsHostSuffix, getHostHeader, true)).Subrouter()
			ideRouter  = r.MatcherFunc(matchWorkspaceHostHeader(wsHostSuffix, getHostHeader, false)).Subrouter()
		)

		r.NotFoundHandler = http.HandlerFunc(func(w http.ResponseWriter, req *http.Request) {
			hostname := getHostHeader(req)
			logs.Debugf("no match for path %s, host: %s", req.URL.Path, hostname)
			w.WriteHeader(http.StatusNotFound)
		})
		return ideRouter, portRouter
	}
}

type hostHeaderProvider func(req *http.Request) string

func matchWorkspaceHostHeader(wsHostSuffix string, headerProvider hostHeaderProvider, matchPort bool) mux.MatcherFunc {
	return func(req *http.Request, m *mux.RouteMatch) bool {
		hostname := headerProvider(req)

		workspaceID, workspacePort, ok := parseWsHostName(wsHostSuffix, hostname, matchPort)
		if !ok {
			return false
		}

		if m.Vars == nil {
			m.Vars = make(map[string]string)
		}
		m.Vars[workspaceIDIdentifier] = workspaceID
		if workspacePort != "" {
			m.Vars[workspacePortIdentifier] = workspacePort
		}

		return true
	}
}

func parseWsHostName(wsHostSuffix, hostname string, matchPort bool) (workspaceID, workspacePort string, ok bool) {
	if hostname == "" {
		return "", "", false
	}

	// hostname可能情况
	// 1、userid-string.hostsuffix 				  2
	// 2、port-userid-string.hostsuffix 		  3
	// 3、userid(v-xxx)-string.hostsuffix  	  3
	// 4、port-userid(v-xxx)-string.hostsuffix  4
	hostname = strings.TrimSuffix(hostname, wsHostSuffix)
	matches := strings.Split(hostname, "-")

	if len(matches) < 2 || len(matches) > 4 {
		return "", "", false
	}

	if len(matches) == 4 {
		workspaceID = strings.TrimPrefix(hostname, matches[0]+"-")
		workspacePort = matches[0]
		// port一定是数字否则false
		if _, err := strconv.Atoi(matches[0]); err != nil {
			return "", "", false
		}
	} else if len(matches) == 3 {
		// 当第一位是数字是情况2，否则3
		if _, err := strconv.Atoi(matches[0]); err == nil {
			workspaceID = strings.TrimPrefix(hostname, matches[0]+"-")
			workspacePort = matches[0]
		} else {
			workspaceID = hostname
			workspacePort = ""
		}
	} else {
		workspaceID = hostname
		workspacePort = ""
	}

	if workspaceID == "" {
		return "", "", false
	}

	if matchPort && workspacePort == "" {
		return "", "", false
	}

	if !matchPort && workspacePort != "" {
		return "", "", false
	}

	return workspaceID, workspacePort, true
}

func setupAcmeRouter(router *mux.Router) {
	router.MatcherFunc(matchAcmeChallenge()).HandlerFunc(func(w http.ResponseWriter, req *http.Request) {
		logs.Debugf("ACME challenge found for path %s, host: %s", req.URL.Path, req.Host)
		w.WriteHeader(http.StatusForbidden)
		w.Header().Set("Content-Type", "text/plain; charset=utf-8")
	})
}

func matchAcmeChallenge() mux.MatcherFunc {
	return func(req *http.Request, m *mux.RouteMatch) bool {
		return isAcmeChallenge(req.URL.Path)
	}
}

func isAcmeChallenge(path string) bool {
	return strings.HasPrefix(filepath.Clean(path), "/.well-known/acme-challenge/")
}
