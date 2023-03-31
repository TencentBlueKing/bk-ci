package registry

import (
	"context"
	"net/http"
	"registry-facade/api"

	"github.com/containerd/containerd/errdefs"
	lru "github.com/hashicorp/golang-lru"
	ociv1 "github.com/opencontainers/image-spec/specs-go/v1"
	"golang.org/x/xerrors"
)

// ErrRefInvalid 由无法解释 ref 的规范提供者返回
var ErrRefInvalid = xerrors.Errorf("invalid ref")

// ImageSpecProvider 基于ref为image pull提供spec
type ImageSpecProvider interface {
	GetSpec(ctx context.Context, ref string) (*api.ImageSpec, error)
}

// FixedImageSpecProvider provides a single spec
type FixedImageSpecProvider map[string]*api.ImageSpec

func (p FixedImageSpecProvider) GetSpec(ctx context.Context, ref string) (*api.ImageSpec, error) {
	res, ok := p[ref]
	if !ok {
		return nil, xerrors.Errorf("%w: %s", ErrRefInvalid, errdefs.ErrNotFound)
	}
	return res, nil
}

// NewCompositeSpecProvider 聚合多个image spec
type CompositeSpecProvider struct {
	providers []ImageSpecProvider
}

// NewCompositeSpecProvider produces a new composite image spec provider
func NewCompositeSpecProvider(providers ...ImageSpecProvider) *CompositeSpecProvider {
	return &CompositeSpecProvider{
		providers: providers,
	}
}

func (csp *CompositeSpecProvider) GetSpec(ctx context.Context, ref string) (spec *api.ImageSpec, err error) {
	if len(csp.providers) == 0 {
		return nil, xerrors.Errorf("no image spec providers configured")
	}

	for _, p := range csp.providers {
		spec, err = p.GetSpec(ctx, ref)
		if err == nil {
			return spec, nil
		}
	}
	return
}

// RemoteSpecProvider queries a remote spec provider using gRPC
type RemoteSpecProvider struct {
	addr string
	conn *http.Client
}

// NewRemoteSpecProvider produces a new remote spec provider
func NewRemoteSpecProvider(addr string) *RemoteSpecProvider {
	return &RemoteSpecProvider{
		addr: addr,
	}
}

// GetSpec returns the spec for the image or a wrapped ErrRefInvalid
func (p *RemoteSpecProvider) GetSpec(ctx context.Context, ref string) (*api.ImageSpec, error) {
	// client, err := p.getClient(ctx)
	// if err != nil {
	// 	return nil, xerrors.Errorf("%w: %s", ErrRefInvalid, err.Error())
	// }

	// resp, err := client.GetImageSpec(ctx, &api.GetImageSpecRequest{Id: ref})
	// if err != nil {
	// 	return nil, xerrors.Errorf("%w: %s", ErrRefInvalid, err.Error())
	// }
	return nil, nil
}

// NewCachingSpecProvider creates a new LRU caching spec provider with a max number of specs it can cache.
func NewCachingSpecProvider(space int, delegate ImageSpecProvider) (*CachingSpecProvider, error) {
	cache, err := lru.New(space)
	if err != nil {
		return nil, err
	}
	return &CachingSpecProvider{
		Cache:    cache,
		Delegate: delegate,
	}, nil
}

// CachingSpecProvider caches an image spec in an LRU cache
type CachingSpecProvider struct {
	Cache    *lru.Cache
	Delegate ImageSpecProvider
}

// GetSpec returns the spec for the image or a wrapped ErrRefInvalid
func (p *CachingSpecProvider) GetSpec(ctx context.Context, ref string) (*api.ImageSpec, error) {
	res, ok := p.Cache.Get(ref)
	if ok {
		return res.(*api.ImageSpec), nil
	}
	spec, err := p.Delegate.GetSpec(ctx, ref)
	if err != nil {
		return nil, err
	}
	p.Cache.Add(ref, spec)
	return spec, nil
}

// ConfigModifier 修改镜像配置
type ConfigModifier func(ctx context.Context, spec *api.ImageSpec, cfg *ociv1.Image) (layer []ociv1.Descriptor, err error)

// NewConfigModifierFromLayerSource produces a config modifier from a layer source
func NewConfigModifierFromLayerSource(src LayerSource) ConfigModifier {
	return func(ctx context.Context, spec *api.ImageSpec, cfg *ociv1.Image) (layer []ociv1.Descriptor, err error) {
		addons, err := src.GetLayer(ctx, spec)
		if err != nil {
			return
		}
		envs, err := src.Envs(ctx, spec)
		if err != nil {
			return
		}

		for _, l := range addons {
			layer = append(layer, l.Descriptor)
			cfg.RootFS.DiffIDs = append(cfg.RootFS.DiffIDs, l.DiffID)
		}

		if len(envs) > 0 {
			parsed := parseEnvs(cfg.Config.Env)
			for _, modifyEnv := range envs {
				modifyEnv(parsed)
			}
			cfg.Config.Env = parsed.serialize()
		}

		return
	}
}
