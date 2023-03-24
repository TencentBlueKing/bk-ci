package registry

import (
	"common/logs"
	"compress/gzip"
	"context"
	"io"
	"io/fs"
	"os"
	"registry/api"
	"sync"

	"github.com/pkg/errors"

	"github.com/containerd/containerd/errdefs"
	"github.com/containerd/containerd/remotes"
	"github.com/opencontainers/go-digest"
	ociv1 "github.com/opencontainers/image-spec/specs-go/v1"
)

type LayerSource interface {
	BlobSource
	GetLayer(ctx context.Context, spec *api.ImageSpec) ([]AddonLayer, error)
	Envs(ctx context.Context, spec *api.ImageSpec) ([]EnvModifier, error)
}

// AddonLayer OCI层描述和层diffId
type AddonLayer struct {
	Descriptor ociv1.Descriptor
	DiffID     digest.Digest
}

type filebackedLayer struct {
	AddonLayer
	Filename string
}

type FileLayerSource []filebackedLayer

func (s FileLayerSource) Name() string {
	return "filelayer"
}

func (s FileLayerSource) Envs(ctx context.Context, spec *api.ImageSpec) ([]EnvModifier, error) {
	return nil, nil
}

func (s FileLayerSource) GetLayer(ctx context.Context, spec *api.ImageSpec) ([]AddonLayer, error) {
	res := make([]AddonLayer, len(s))
	for i := range s {
		res[i] = s[i].AddonLayer
	}
	return res, nil
}

func (s FileLayerSource) HasBlob(ctx context.Context, spec *api.ImageSpec, dgst digest.Digest) bool {
	for _, l := range s {
		if l.Descriptor.Digest == dgst {
			return true
		}
	}
	return false
}

func (s FileLayerSource) GetBlob(ctx context.Context, spec *api.ImageSpec, dgst digest.Digest) (dontCache bool, mediaType string, url string, data io.ReadCloser, err error) {
	var src filebackedLayer
	for _, l := range s {
		if l.Descriptor.Digest == dgst {
			src = l
			break
		}
	}
	if src.Filename == "" {
		err = errdefs.ErrNotFound
		return
	}

	f, err := os.OpenFile(src.Filename, os.O_RDONLY, 0)
	if errors.Is(err, fs.ErrNotExist) {
		err = errdefs.ErrNotFound
		return
	}
	if err != nil {
		return
	}

	return false, src.Descriptor.MediaType, "", f, nil
}

// NewFileLayerSource 生成一个静态层源，其中每个文件都应该是一个 gzip 层
func NewFileLayerSource(ctx context.Context, file ...string) (FileLayerSource, error) {
	var res FileLayerSource
	for _, fn := range file {
		fr, err := os.OpenFile(fn, os.O_RDONLY, 0)
		if err != nil {
			return nil, err
		}
		defer fr.Close()

		stat, err := fr.Stat()
		if err != nil {
			return nil, err
		}

		dgst, err := digest.FromReader(fr)
		if err != nil {
			return nil, err
		}

		// start again to read the diffID
		_, err = fr.Seek(0, 0)
		if err != nil {
			return nil, err
		}
		diffr, err := gzip.NewReader(fr)
		if err != nil {
			return nil, err
		}
		defer diffr.Close()
		diffID, err := digest.FromReader(diffr)
		if err != nil {
			return nil, err
		}

		desc := ociv1.Descriptor{
			MediaType: ociv1.MediaTypeImageLayer,
			Digest:    dgst,
			Size:      stat.Size(),
		}
		res = append(res, filebackedLayer{
			AddonLayer: AddonLayer{
				Descriptor: desc,
				DiffID:     diffID,
			},
			Filename: fn,
		})

		logs.WithField("diffID", diffID).WithField("fn", fn).Debug("loaded static layer")
	}

	return res, nil
}

// EnvModifier 修改镜像env配置
type EnvModifier func(*ParsedEnvs)

// ParsedEnvs 解析镜像env配置
type ParsedEnvs struct {
	keys   []string
	values map[string]string
}

type imagebackedLayer struct {
	AddonLayer
	Fetcher remotes.Fetcher
}

// ImageLayerSource provides additional layers from another image
type ImageLayerSource struct {
	envs   []EnvModifier
	layers []imagebackedLayer
}

func (s ImageLayerSource) Name() string {
	return "imagelayer"
}

func (s ImageLayerSource) Envs(ctx context.Context, spec *api.ImageSpec) ([]EnvModifier, error) {
	return s.envs, nil
}

func (s ImageLayerSource) GetLayer(ctx context.Context, spec *api.ImageSpec) ([]AddonLayer, error) {
	res := make([]AddonLayer, len(s.layers))
	for i := range s.layers {
		res[i] = s.layers[i].AddonLayer
	}
	return res, nil
}

func (s ImageLayerSource) HasBlob(ctx context.Context, spec *api.ImageSpec, dgst digest.Digest) bool {
	for _, l := range s.layers {
		if l.Descriptor.Digest == dgst {
			return true
		}
	}
	return false
}

func (s ImageLayerSource) GetBlob(ctx context.Context, spec *api.ImageSpec, dgst digest.Digest) (dontCache bool, mediaType string, url string, data io.ReadCloser, err error) {
	var src imagebackedLayer
	for _, l := range s.layers {
		if l.Descriptor.Digest == dgst {
			src = l
		}
	}
	if src.Fetcher == nil {
		err = errdefs.ErrNotFound
		return
	}

	rc, err := src.Fetcher.Fetch(ctx, src.Descriptor)
	if err != nil {
		return
	}

	return false, src.Descriptor.MediaType, "", rc, nil
}

const (
	envPrefixSet     = "DEVOPS_REMOTING_ENV_SET_"
	envPrefixAppend  = "DEVOPS_REMOTING_ENV_APPEND_"
	envPrefixPrepend = "DEVOPS_REMOTING_ENV_PREPEND_"
)

// NewStaticSourceFromImage 下载镜像层到缓存中并将其作为静态层
func NewStaticSourceFromImage(ctx context.Context, resolver remotes.Resolver, ref string) (*ImageLayerSource, error) {
	_, desc, err := resolver.Resolve(ctx, ref)
	if err != nil {
		return nil, err
	}
	fetcher, err := resolver.Fetcher(ctx, ref)
	if err != nil {
		return nil, err
	}

	manifest, _, err := DownloadManifest(ctx, AsFetcherFunc(fetcher), desc)
	if err != nil {
		return nil, err
	}

	cfg, err := DownloadConfig(ctx, AsFetcherFunc(fetcher), ref, manifest.Config)
	if err != nil {
		return nil, err
	}

	// 镜像可以将前 N 层标记为不相关。
	// 我们为此使用标签来将该信息与镜像一起发送。
	skipN, err := getSkipNLabelValue(&cfg.Config)
	if err != nil {
		return nil, err
	}

	res := make([]imagebackedLayer, 0, len(manifest.Layers))
	for i, ml := range manifest.Layers {
		if i < skipN {
			continue
		}

		l := imagebackedLayer{
			AddonLayer: AddonLayer{
				Descriptor: ml,
				DiffID:     cfg.RootFS.DiffIDs[i],
			},
			Fetcher: fetcher,
		}
		res = append(res, l)
	}

	var envs []EnvModifier
	parsedEnvs := parseEnvs(cfg.Config.Env)
	for _, name := range parsedEnvs.keys {
		value := parsedEnvs.values[name]
		if strings.HasPrefix(name, envPrefixAppend) {
			name = strings.TrimPrefix(name, envPrefixAppend)
			if name == "" || value == "" {
				continue
			}
			envs = append(envs, newAppendEnvModifier(name, value))
		} else if strings.HasPrefix(name, envPrefixPrepend) {
			name = strings.TrimPrefix(name, envPrefixPrepend)
			if name == "" || value == "" {
				continue
			}
			envs = append(envs, newPrependEnvModifier(name, value))
		} else if strings.HasPrefix(name, envPrefixSet) {
			name = strings.TrimPrefix(name, envPrefixSet)
			if name == "" {
				continue
			}
			envs = append(envs, newSetEnvModifier(name, value))
		}
	}

	return &ImageLayerSource{
		layers: res,
		envs:   envs,
	}, nil
}

// CompositeLayerSource 用来添加不同源的层
type CompositeLayerSource []LayerSource

func (cs CompositeLayerSource) Name() string {
	return "composite"
}

func (cs CompositeLayerSource) Envs(ctx context.Context, spec *api.ImageSpec) ([]EnvModifier, error) {
	var res []EnvModifier
	for _, s := range cs {
		envs, err := s.Envs(ctx, spec)
		if err != nil {
			return nil, err
		}
		res = append(res, envs...)
	}
	return res, nil
}

func (cs CompositeLayerSource) GetLayer(ctx context.Context, spec *api.ImageSpec) ([]AddonLayer, error) {
	var res []AddonLayer
	for _, s := range cs {
		ls, err := s.GetLayer(ctx, spec)
		if err != nil {
			return nil, err
		}
		res = append(res, ls...)
	}
	return res, nil
}

func (cs CompositeLayerSource) HasBlob(ctx context.Context, spec *api.ImageSpec, dgst digest.Digest) bool {
	for _, s := range cs {
		if s.HasBlob(ctx, spec, dgst) {
			return true
		}
	}
	return false
}

func (cs CompositeLayerSource) GetBlob(ctx context.Context, spec *api.ImageSpec, dgst digest.Digest) (dontCache bool, mediaType string, url string, data io.ReadCloser, err error) {
	for _, s := range cs {
		if s.HasBlob(ctx, spec, dgst) {
			return s.GetBlob(ctx, spec, dgst)
		}
	}

	err = errdefs.ErrNotFound
	return
}

type RevisioningLayerSource struct {
	mu     sync.RWMutex
	active LayerSource
	past   []LayerSource
}

// NewRevisioningLayerSource 产生一个新的修订层源
func NewRevisioningLayerSource(active LayerSource) *RevisioningLayerSource {
	return &RevisioningLayerSource{
		active: active,
	}
}

func (src *RevisioningLayerSource) Name() string {
	src.mu.RLock()
	defer src.mu.RUnlock()

	return src.active.Name()
}

func (src *RevisioningLayerSource) Update(s LayerSource) {
	src.mu.Lock()
	defer src.mu.Unlock()

	src.past = append(src.past, src.active)
	src.active = s
}

func (src *RevisioningLayerSource) GetLayer(ctx context.Context, spec *api.ImageSpec) ([]AddonLayer, error) {
	src.mu.RLock()
	defer src.mu.RUnlock()

	return src.active.GetLayer(ctx, spec)
}

func (src *RevisioningLayerSource) Envs(ctx context.Context, spec *api.ImageSpec) ([]EnvModifier, error) {
	src.mu.RLock()
	defer src.mu.RUnlock()

	return src.active.Envs(ctx, spec)
}

// HasBlob checks if a digest can be served by this blob source
func (src *RevisioningLayerSource) HasBlob(ctx context.Context, details *api.ImageSpec, dgst digest.Digest) bool {
	src.mu.RLock()
	defer src.mu.RUnlock()

	if src.active.HasBlob(ctx, details, dgst) {
		return true
	}
	for _, p := range src.past {
		if p.HasBlob(ctx, details, dgst) {
			return true
		}
	}
	return false
}

// GetBlob provides access to a blob. If a ReadCloser is returned the receiver is expected to
// call close on it eventually.
func (src *RevisioningLayerSource) GetBlob(ctx context.Context, details *api.ImageSpec, dgst digest.Digest) (dontCache bool, mediaType string, url string, data io.ReadCloser, err error) {
	src.mu.RLock()
	defer src.mu.RUnlock()

	if src.active.HasBlob(ctx, details, dgst) {
		return src.active.GetBlob(ctx, details, dgst)
	}
	for _, p := range src.past {
		if p.HasBlob(ctx, details, dgst) {
			return p.GetBlob(ctx, details, dgst)
		}
	}

	err = errdefs.ErrNotFound
	return
}
