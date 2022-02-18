/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package httpserver

import (
	"fmt"
	"net"
	"net/http"
	"strconv"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/ssl"

	"github.com/emicklei/go-restful"
	"github.com/gorilla/mux"
)

// HTTPServer is data struct of http server
type HTTPServer struct {
	addr         string
	port         uint
	insecureAddr string
	insecurePort uint
	sock         string
	isSSL        bool
	caFile       string
	certFile     string
	keyFile      string
	certPasswd   string
	webContainer *restful.Container
	router       *mux.Router
}

// NewHTTPServer get a new http server.
func NewHTTPServer(port uint, addr, sock string) *HTTPServer {
	return &HTTPServer{
		addr:         addr,
		port:         port,
		sock:         sock,
		webContainer: restful.NewContainer(),
		router:       mux.NewRouter(),
		isSSL:        false,
	}
}

// SetInsecureServer set insecure(http) address and port into http server.
func (s *HTTPServer) SetInsecureServer(insecureAddr string, insecurePort uint) {
	s.insecureAddr = insecureAddr
	s.insecurePort = insecurePort
}

// GetWebContainer return the web container.
func (s *HTTPServer) GetWebContainer() *restful.Container {
	return s.webContainer
}

// GetRouter return the router.
func (s *HTTPServer) GetRouter() *mux.Router {
	return s.router
}

// SetSSL set certs files into http server.
func (s *HTTPServer) SetSSL(cafile, certfile, keyfile, certPasswd string) {
	s.caFile = cafile
	s.certFile = certfile
	s.keyFile = keyfile
	s.certPasswd = certPasswd
	s.isSSL = true
}

// RegisterWebServer register a new web server beyond rootPath and handle these actions.
func (s *HTTPServer) RegisterWebServer(rootPath string, filter restful.FilterFunction, actions []*Action) error {
	//new a web service
	ws := s.NewWebService(rootPath, filter)

	//register action
	s.RegisterActions(ws, actions)

	return nil
}

// NewWebService get a new web service.
func (s *HTTPServer) NewWebService(rootPath string, filter restful.FilterFunction) *restful.WebService {
	ws := new(restful.WebService)
	if "" != rootPath {
		ws.Path(rootPath)
	}

	ws.Produces(restful.MIME_JSON, restful.MIME_XML, restful.MIME_OCTET)

	if nil != filter {
		ws.Filter(filter)
	}

	s.webContainer.Add(ws)

	return ws
}

// RegisterActions register a list of actions into web service.
func (s *HTTPServer) RegisterActions(ws *restful.WebService, actions []*Action) {
	for _, action := range actions {
		switch action.Verb {
		case "POST":
			route := ws.POST(action.Path).To(action.Handler)
			ws.Route(route)
			blog.Infof("register post api, url(%s)", action.Path)
		case "GET":
			route := ws.GET(action.Path).To(action.Handler)
			ws.Route(route)
			blog.Infof("register get api, url(%s)", action.Path)
		case "PUT":
			route := ws.PUT(action.Path).To(action.Handler)
			ws.Route(route)
			blog.Infof("register put api, url(%s)", action.Path)
		case "DELETE":
			route := ws.DELETE(action.Path).To(action.Handler)
			ws.Route(route)
			blog.Infof("register delete api, url(%s)", action.Path)
		case "PATCH":
			route := ws.PATCH(action.Path).To(action.Handler)
			ws.Route(route)
			blog.Infof("register patch api, url(%s)", action.Path)
		default:
			blog.Error("unrecognized action verb: %s", action.Verb)
		}
	}
}

// ListenAndServe start a http server
func (s *HTTPServer) ListenAndServe() error {

	var chError = make(chan error)
	//list and serve by addrport
	go func() {
		addrport := net.JoinHostPort(s.addr, strconv.FormatUint(uint64(s.port), 10))
		httpserver := &http.Server{Addr: addrport, Handler: s.webContainer}
		if s.isSSL {
			tlsConf, err := ssl.ServerTSLConf(s.caFile, s.certFile, s.keyFile, s.certPasswd)
			if err != nil {
				blog.Error("fail to load certfile, err:%s", err.Error())
				chError <- fmt.Errorf("fail to load certfile")
				return
			}
			httpserver.TLSConfig = tlsConf
			blog.Info("Start https service on(%s:%d)", s.addr, s.port)
			chError <- httpserver.ListenAndServeTLS("", "")
		} else {
			blog.Info("Start http service on(%s:%d)", s.addr, s.port)
			chError <- httpserver.ListenAndServe()
		}
	}()

	return <-chError
}
