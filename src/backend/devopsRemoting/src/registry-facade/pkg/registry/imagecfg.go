package registry

import (
	"context"
	ociv1 "github.com/opencontainers/image-spec/specs-go/v1"
	"registry/api"
)

// ImageSpecProvider 基于ref为image pull提供spec
type ImageSpecProvider interface {
	GetSpec(ctx context.Context, ref string) (*api.ImageSpec, error)
}

// ConfigModifier 修改镜像配置
type ConfigModifier func(ctx context.Context, spec *api.ImageSpec, cfg *ociv1.Image) (layer []ociv1.Descriptor, err error)
