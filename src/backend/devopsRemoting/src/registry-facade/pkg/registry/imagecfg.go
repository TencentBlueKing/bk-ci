package registry

import (
	"context"
	"registry-facade/api"

	ociv1 "github.com/opencontainers/image-spec/specs-go/v1"
)

// ImageSpecProvider 基于ref为image pull提供spec
type ImageSpecProvider interface {
	GetSpec(ctx context.Context, ref string) (*api.ImageSpec, error)
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
