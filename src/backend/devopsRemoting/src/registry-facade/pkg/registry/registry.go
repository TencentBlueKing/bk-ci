package registry

import (
	"common/logs"
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net"
	"net/http"
	"registry-facade/api"
	"registry-facade/pkg/config"
	"strings"

	"github.com/docker/distribution"
	"github.com/docker/distribution/reference"
	"github.com/docker/distribution/registry/api/errcode"
	distv2 "github.com/docker/distribution/registry/api/v2"
	"github.com/gorilla/mux"

	"github.com/prometheus/client_golang/prometheus"
	"golang.org/x/xerrors"

	"github.com/containerd/containerd/content/local"
	"github.com/containerd/containerd/remotes"
)

// BuildStaticLayer 通过静态层配置构造层对象
func buildStaticLayer(ctx context.Context, cfg []config.StaticLayerCfg, newResolver ResolverProvider) (CompositeLayerSource, error) {
	var l CompositeLayerSource
	for _, sl := range cfg {
		switch sl.Type {
		case "file":
			src, err := NewFileLayerSource(ctx, sl.Ref)
			if err != nil {
				return nil, xerrors.Errorf("cannot source layer from %s: %w", sl.Ref, err)
			}
			l = append(l, src)
		case "image":
			src, err := NewStaticSourceFromImage(ctx, newResolver(), sl.Ref)
			if err != nil {
				return nil, xerrors.Errorf("cannot source layer from %s: %w", sl.Ref, err)
			}
			l = append(l, src)
		default:
			return nil, xerrors.Errorf("unknown static layer type: %s", sl.Type)
		}
	}
	return l, nil
}

type ResolverProvider func() remotes.Resolver

type Registry struct {
	Config   config.Config
	Resolver ResolverProvider
	Store    BlobStore
	// IPFS           *IPFSBlobCache
	LayerSource    LayerSource
	ConfigModifier ConfigModifier
	SpecProvider   map[string]ImageSpecProvider

	staticLayerSource *RevisioningLayerSource
	metrics           *metrics
	srv               *http.Server
}

func NewRegistry(cfg config.Config, newResolver ResolverProvider, reg prometheus.Registerer) (*Registry, error) {
	var mfStore BlobStore

	storePath := cfg.Store
	var err error
	mfStore, err = local.NewStore(storePath)
	if err != nil {
		return nil, err
	}
	logs.WithField("storePath", storePath).Info("using local filesystem to cache manifests and config")
	// TODO: GC the store

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	metrics, err := newMetrics(reg, true)
	if err != nil {
		return nil, err
	}

	var layerSources []LayerSource

	// static layers
	logs.Info("preparing static layer")
	staticLayer := NewRevisioningLayerSource(CompositeLayerSource{})
	layerSources = append(layerSources, staticLayer)
	if len(cfg.StaticLayer) > 0 {
		l, err := buildStaticLayer(ctx, cfg.StaticLayer, newResolver)
		if err != nil {
			return nil, err
		}
		staticLayer.Update(l)
	}

	// ide layer
	ideRefSource := func(s *api.ImageSpec) (ref []string, err error) {
		ref = append(ref, s.IdeRef, s.RemotingRef)
		ref = append(ref, s.IdeLayerRef...)
		return ref, nil
	}
	ideLayerSource, err := NewSpecMappedImageSource(newResolver, ideRefSource)
	if err != nil {
		return nil, err
	}
	layerSources = append(layerSources, ideLayerSource)

	// content layer
	clsrc, err := NewContentLayerSource()
	if err != nil {
		return nil, xerrors.Errorf("cannot create content layer source: %w", err)
	}
	layerSources = append(layerSources, clsrc)

	specProvider := map[string]ImageSpecProvider{}
	if cfg.RemoteSpecProvider != nil {
		var providers []ImageSpecProvider
		for _, providerCfg := range cfg.RemoteSpecProvider {
			rsp, err := createRemoteSpecProvider(providerCfg)
			if err != nil {
				return nil, err
			}

			providers = append(providers, rsp)
		}

		specProvider[api.ProviderPrefixRemote] = NewCompositeSpecProvider(providers...)
	}

	if cfg.FixedSpecProvider != "" {
		fc, err := ioutil.ReadFile(cfg.FixedSpecProvider)
		if err != nil {
			return nil, xerrors.Errorf("cannot read fixed spec: %w", err)
		}

		f := make(map[string]json.RawMessage)
		err = json.Unmarshal(fc, &f)
		if err != nil {
			return nil, xerrors.Errorf("cannot unmarshal fixed spec: %w", err)
		}

		prov := make(FixedImageSpecProvider)
		for k, v := range f {
			var spec api.ImageSpec
			err = json.Unmarshal(v, &spec)
			if err != nil {
				return nil, xerrors.Errorf("cannot unmarshal fixed spec: %w", err)
			}
			prov[k] = &spec
		}
		specProvider[api.ProviderPrefixFixed] = prov
	}

	// var ipfs *IPFSBlobCache = nil
	// if cfg.IPFSCache != nil && cfg.IPFSCache.Enabled {
	// 	addr := cfg.IPFSCache.IPFSAddr
	// 	if ipfsHost := os.Getenv("IPFS_HOST"); ipfsHost != "" {
	// 		addr = strings.ReplaceAll(addr, "$IPFS_HOST", ipfsHost)
	// 	}

	// 	maddr, err := ma.NewMultiaddr(strings.TrimSpace(addr))
	// 	if err != nil {
	// 		return nil, xerrors.Errorf("cannot connect to IPFS: %w", err)
	// 	}

	// 	core, err := httpapi.NewApiWithClient(maddr, NewRetryableHTTPClient())
	// 	if err != nil {
	// 		return nil, xerrors.Errorf("cannot connect to IPFS: %w", err)
	// 	}
	// 	rdc, err := getRedisClient(cfg.RedisCache)
	// 	if err != nil {
	// 		return nil, xerrors.Errorf("cannot connect to Redis: %w", err)
	// 	}

	// 	ipfs = &IPFSBlobCache{
	// 		Redis: rdc,
	// 		IPFS:  core,
	// 	}
	// 	log.WithField("config", cfg.IPFSCache).Info("enabling IPFS caching")
	// }

	layerSource := CompositeLayerSource(layerSources)
	return &Registry{
		Config:   cfg,
		Resolver: newResolver,
		Store:    mfStore,
		// IPFS:              nil,
		SpecProvider:      specProvider,
		LayerSource:       layerSource,
		staticLayerSource: staticLayer,
		ConfigModifier:    NewConfigModifierFromLayerSource(layerSource),
		metrics:           metrics,
	}, nil
}

func createRemoteSpecProvider(cfg *config.RSProvider) (ImageSpecProvider, error) {
	// grpcOpts := common_grpc.DefaultClientOptions()
	// if cfg.TLS != nil {
	// 	tlsConfig, err := common_grpc.ClientAuthTLSConfig(
	// 		cfg.TLS.Authority, cfg.TLS.Certificate, cfg.TLS.PrivateKey,
	// 		common_grpc.WithSetRootCAs(true),
	// 		common_grpc.WithServerName("ws-manager"),
	// 	)
	// 	if err != nil {
	// 		logs.WithField("config", cfg.TLS).Error("Cannot load ws-manager certs - this is a configuration issue.")
	// 		return nil, xerrors.Errorf("cannot load ws-manager certs: %w", err)
	// 	}

	// 	grpcOpts = append(grpcOpts, grpc.WithTransportCredentials(credentials.NewTLS(tlsConfig)))
	// } else {
	// 	grpcOpts = append(grpcOpts, grpc.WithTransportCredentials(insecure.NewCredentials()))
	// }

	specprov, err := NewCachingSpecProvider(128, NewRemoteSpecProvider(cfg.Addr))
	if err != nil {
		return nil, xerrors.Errorf("cannot create caching spec provider: %w", err)
	}

	return specprov, nil
}

// UpdateStaticLayer updates the static layer a registry-facade adds
func (reg *Registry) UpdateStaticLayer(ctx context.Context, cfg []config.StaticLayerCfg) error {
	l, err := buildStaticLayer(ctx, cfg, reg.Resolver)
	if err != nil {
		return err
	}
	reg.staticLayerSource.Update(l)
	return nil
}

// MustServe 启动服务
func (reg *Registry) MustServe() {
	err := reg.Serve()
	if err != nil {
		logs.WithError(err).Fatal("cannot serve registry")
	}
}

// Serve serves the registry on the given port
func (reg *Registry) Serve() error {
	routes := distv2.RouterWithPrefix(reg.Config.Prefix)
	reg.registerHandler(routes)

	var handler http.Handler = routes
	// if reg.Config.RequireAuth {
	// 	handler = reg.requireAuthentication(routes)
	// }
	mux := http.NewServeMux()
	mux.Handle("/", handler)

	addr := fmt.Sprintf(":%d", reg.Config.Port)
	l, err := net.Listen("tcp", addr)
	if err != nil {
		return err
	}

	reg.srv = &http.Server{
		Addr:    addr,
		Handler: mux,
	}

	if reg.Config.TLS != nil {
		logs.WithField("addr", addr).Info("HTTPS registry server listening")

		cert, key := reg.Config.TLS.Certificate, reg.Config.TLS.PrivateKey

		return reg.srv.ServeTLS(l, cert, key)
	}

	logs.WithField("addr", addr).Info("HTTP registry server listening")
	return reg.srv.Serve(l)
}

func (reg *Registry) requireAuthentication(h http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		fail := func() {
			w.Header().Add("WWW-Authenticate", "Basic")
			w.WriteHeader(http.StatusUnauthorized)
		}

		_, _, ok := r.BasicAuth()
		if !ok {
			fail()
			return
		}

		// todo: implement auth

		h.ServeHTTP(w, r)
	})
}

// registerHandler registers the handle* functions with the corresponding routes
func (reg *Registry) registerHandler(routes *mux.Router) {
	routes.Get(distv2.RouteNameBase).HandlerFunc(reg.handleAPIBase)
	routes.Get(distv2.RouteNameManifest).Handler(dispatcher(reg.handleManifest))
	// routes.Get(v2.RouteNameCatalog).Handler(dispatcher(reg.handleCatalog))
	// routes.Get(v2.RouteNameTags).Handler(dispatcher(reg.handleTags))
	routes.Get(distv2.RouteNameBlob).Handler(dispatcher(reg.handleBlob))
	// routes.Get(v2.RouteNameBlobUpload).Handler(dispatcher(reg.handleBlobUpload))
	// routes.Get(v2.RouteNameBlobUploadChunk).Handler(dispatcher(reg.handleBlobUploadChunk))
	routes.NotFoundHandler = http.HandlerFunc(reg.handleAPIBase)
}

// handleApiBase 实现了一个简单的接口, 可以支持 auth 往返以支持 docker 登录。
func (reg *Registry) handleAPIBase(w http.ResponseWriter, r *http.Request) {
	const emptyJSON = "{}"
	// Provide a simple /v2/ 200 OK response with empty json response.
	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Content-Length", fmt.Sprint(len(emptyJSON)))

	fmt.Fprint(w, emptyJSON)
}

type dispatchFunc func(ctx context.Context, r *http.Request) http.Handler

// dispatcher 包装一个 dispatchFunc 并提供上下文
func dispatcher(d dispatchFunc) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		// 从请求中获取上下文，添加变量和其他信息并同步回来
		ctx := r.Context()
		ctx = &muxVarsContext{
			Context: ctx,
			vars:    mux.Vars(r),
		}
		r = r.WithContext(ctx)

		if nameRequired(r) {
			nameRef, err := reference.WithName(getName(ctx))
			if err != nil {
				logs.WithError(err).WithField("nameRef", nameRef).Errorf("error parsing reference from context")
				respondWithError(w, distribution.ErrRepositoryNameInvalid{
					Name:   nameRef.Name(),
					Reason: err,
				})
				return
			}
		}

		d(ctx, r).ServeHTTP(w, r)
	})
}

// nameRequired 如果路由需要名称，则返回 true
func nameRequired(r *http.Request) bool {
	route := mux.CurrentRoute(r)
	if route == nil {
		return true
	}
	routeName := route.GetName()
	return routeName != distv2.RouteNameBase && routeName != distv2.RouteNameCatalog
}

func respondWithError(w http.ResponseWriter, terr error) {
	err := errcode.ServeJSON(w, terr)
	if err != nil {
		logs.WithError(err).WithField("orignalErr", terr).Errorf("error serving error json")
	}
}

type muxVarsContext struct {
	context.Context
	vars map[string]string
}

func (ctx *muxVarsContext) Value(key interface{}) interface{} {
	if keyStr, ok := key.(string); ok {
		if keyStr == "vars" {
			return ctx.vars
		}

		keyStr = strings.TrimPrefix(keyStr, "vars.")

		if v, ok := ctx.vars[keyStr]; ok {
			return v
		}
	}

	return ctx.Context.Value(key)
}

// getName 从通过 mux 路由传入的上下文中提取名称 var
func getName(ctx context.Context) string {
	val := ctx.Value("vars.name")
	sval, ok := val.(string)
	if !ok {
		return ""
	}
	return sval
}
func getSpecProviderName(ctx context.Context) (specProviderName string, remainder string) {
	name := getName(ctx)
	segs := strings.Split(name, "/")
	if len(segs) > 1 {
		specProviderName = segs[0]
		remainder = strings.Join(segs[1:], "/")
	}
	return
}

// getReference extracts the referece var from the context which was passed in through the mux route
func getReference(ctx context.Context) string {
	val := ctx.Value("vars.reference")
	sval, ok := val.(string)
	if !ok {
		return ""
	}
	return sval
}

// getDigest extracts the digest var from the context which was passed in through the mux route
func getDigest(ctx context.Context) string {
	val := ctx.Value("vars.digest")
	sval, ok := val.(string)
	if !ok {
		return ""
	}

	return sval
}
