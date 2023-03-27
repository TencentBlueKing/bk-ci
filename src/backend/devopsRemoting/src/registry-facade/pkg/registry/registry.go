package registry

import (
	"common/logs"
	"context"
	"fmt"
	"net"
	"net/http"
	"os"
	"path/filepath"
	"registry-facade/pkg/config"
	"registry/api"

	distv2 "github.com/docker/distribution/registry/api/v2"
	"github.com/opencontainers/go-digest"
	ociv1 "github.com/opencontainers/image-spec/specs-go/v1"
	"github.com/prometheus/client_golang/prometheus"
	"golang.org/x/xerrors"

	"github.com/containerd/containerd/content"
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
	Config         config.Config
	Resolver       ResolverProvider
	Store          BlobStore
	IPFS           *IPFSBlobCache
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
	if tproot := os.Getenv("TELEPRESENCE_ROOT"); tproot != "" {
		storePath = filepath.Join(tproot, storePath)
	}
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
			err = jsonpb.UnmarshalString(string(v), &spec)
			if err != nil {
				return nil, xerrors.Errorf("cannot unmarshal fixed spec: %w", err)
			}
			prov[k] = &spec
		}
		specProvider[api.ProviderPrefixFixed] = prov
	}

	var ipfs *IPFSBlobCache
	if cfg.IPFSCache != nil && cfg.IPFSCache.Enabled {
		addr := cfg.IPFSCache.IPFSAddr
		if ipfsHost := os.Getenv("IPFS_HOST"); ipfsHost != "" {
			addr = strings.ReplaceAll(addr, "$IPFS_HOST", ipfsHost)
		}

		maddr, err := ma.NewMultiaddr(strings.TrimSpace(addr))
		if err != nil {
			return nil, xerrors.Errorf("cannot connect to IPFS: %w", err)
		}

		core, err := httpapi.NewApiWithClient(maddr, NewRetryableHTTPClient())
		if err != nil {
			return nil, xerrors.Errorf("cannot connect to IPFS: %w", err)
		}
		rdc, err := getRedisClient(cfg.RedisCache)
		if err != nil {
			return nil, xerrors.Errorf("cannot connect to Redis: %w", err)
		}

		ipfs = &IPFSBlobCache{
			Redis: rdc,
			IPFS:  core,
		}
		log.WithField("config", cfg.IPFSCache).Info("enabling IPFS caching")
	}

	layerSource := CompositeLayerSource(layerSources)
	return &Registry{
		Config:            cfg,
		Resolver:          newResolver,
		Store:             mfStore,
		IPFS:              ipfs,
		SpecProvider:      specProvider,
		LayerSource:       layerSource,
		staticLayerSource: staticLayer,
		ConfigModifier:    NewConfigModifierFromLayerSource(layerSource),
		metrics:           metrics,
	}, nil
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
	if reg.Config.RequireAuth {
		handler = reg.requireAuthentication(routes)
	}
	mux := http.NewServeMux()
	mux.Handle("/", handler)

	if addr := os.Getenv("REGFAC_NO_TLS_DEBUG"); addr != "" {
		// 端口转发也做 SSL 终止。 如果我们只提供 HTTPS 服务
		// 当使用 telepresence 时，我们不能直接向 registry facade 发出任何请求，
		// 例如 使用 curl 或其他 Docker 守护进程。 使用 env var 我们可以启用一个额外的
		// HTTP 服务。
		//
		// 注意：这仅适用于远程呈现设置
		go func() {
			err := http.ListenAndServe(addr, mux)
			if err != nil {
				logs.WithError(err).Error("start of registry server failed")
			}
		}()
	}

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
		if tproot := os.Getenv("TELEPRESENCE_ROOT"); tproot != "" {
			cert = filepath.Join(tproot, cert)
			key = filepath.Join(tproot, key)
		}

		return reg.srv.ServeTLS(l, cert, key)
	}

	logs.WithField("addr", addr).Info("HTTP registry server listening")
	return reg.srv.Serve(l)
}
