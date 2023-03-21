package proxy

import (
	"context"
	"net/http"
	"net/url"

	"github.com/gorilla/mux"
)

type checkAuth func(ctx context.Context, host, wsid, ticket string) (bool, error)

// 通过拿到的token调用后台的鉴权接口
// TODO: 未来等后台一起联调
func WorkspaceAuthHandler(hostname, redirUrl string, checkAuth checkAuth) mux.MiddlewareFunc {
	return func(h http.Handler) http.Handler {
		return http.HandlerFunc(func(resp http.ResponseWriter, req *http.Request) {
			var (
				log  = getLog(req.Context())
				vars = mux.Vars(req)
				wsID = vars[workspaceIDIdentifier]
				// TODO: 看未来是否有port公开的功能
				// port = vars[workspacePortIdentifier]
			)

			var ticketV string
			ticket, err := req.Cookie("X-DEVOPS-BK-TICKET")
			if err != nil {
				// 从蓝盾的cookie中拿，拿不到重定向到蓝盾
				lTicket, err := req.Cookie("bk_ticket")
				if err != nil {
					target := "http://" + req.Host + req.URL.Path
					if len(req.URL.RawQuery) > 0 {
						target += "?" + req.URL.RawQuery
					}
					target = url.QueryEscape(target)
					target = redirUrl + target

					http.Redirect(resp, req, target, http.StatusFound)
					return
				} else {
					ticketV = lTicket.Value
				}
			} else {
				if ticket.Value == "" {
					log.Warn("workspace request without ticket")
					resp.WriteHeader(http.StatusForbidden)
					return
				}
				ticketV = ticket.Value
			}

			if wsID == "" {
				log.Warn("workspace request without workspace ID")
				resp.WriteHeader(http.StatusForbidden)
				return
			}

			ok, err := checkAuth(context.Background(), hostname, wsID, ticketV)
			if err != nil {
				log.Errorf("checkauth err %s", err)
				resp.WriteHeader(http.StatusForbidden)
				return
			}
			if !ok {
				log.Warn("checkauth false")
				resp.WriteHeader(http.StatusForbidden)
				return
			}

			h.ServeHTTP(resp, req)
		})
	}
}
