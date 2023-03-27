package registry

import (
	"bytes"
	"common/logs"
	"context"
	"encoding/json"
	"io"
	"strings"

	"github.com/containerd/containerd/content"
	"github.com/containerd/containerd/errdefs"
	"github.com/containerd/containerd/images"
	"github.com/containerd/containerd/remotes"
	"github.com/opencontainers/go-digest"
	ociv1 "github.com/opencontainers/image-spec/specs-go/v1"
	"github.com/pkg/errors"
	"golang.org/x/xerrors"
)

// DownloadConfig 下载desc指定的 OCIv2 镜像配置
func DownloadConfig(ctx context.Context, fetch FetcherFunc, ref string, desc ociv1.Descriptor, options ...ManifestDownloadOption) (cfg *ociv1.Image, err error) {
	if desc.MediaType != images.MediaTypeDockerSchema2Config &&
		desc.MediaType != ociv1.MediaTypeImageConfig {

		return nil, xerrors.Errorf("unsupported media type: %s", desc.MediaType)
	}

	var opts manifestDownloadOptions
	for _, o := range options {
		o(&opts)
	}

	var rc io.ReadCloser
	if opts.Store != nil {
		r, err := opts.Store.ReaderAt(ctx, desc)
		if errors.Is(err, errdefs.ErrNotFound) {
			// not cached yet
		} else if err != nil {
			logs.WithError(err).WithField("desc", desc).Warn("cannot read config from store - fetching again")
		} else {
			defer r.Close()
			rc = io.NopCloser(content.NewReader(r))
		}
	}
	if rc == nil {
		fetcher, err := fetch()
		if err != nil {
			return nil, err
		}
		rc, err = fetcher.Fetch(ctx, desc)
		if err != nil {
			return nil, xerrors.Errorf("cannot download config: %w", err)
		}
		defer rc.Close()
	}

	buf, err := io.ReadAll(rc)
	if err != nil {
		return nil, xerrors.Errorf("cannot read config: %w", err)
	}

	var res ociv1.Image
	err = json.Unmarshal(buf, &res)
	if err != nil {
		return nil, xerrors.Errorf("cannot decode config: %w", err)
	}

	if opts.Store != nil && ref != "" {
		// ref can be empty for some users of DownloadConfig. However, some store implementations
		// (e.g. the default containerd store) expect ref to be set. This would lead to stray errors.

		err := func() error {
			w, err := opts.Store.Writer(ctx, content.WithDescriptor(desc), content.WithRef(ref))
			if err != nil {
				return err
			}
			defer w.Close()

			n, err := w.Write(buf)
			if err != nil {
				return err
			}
			if n != len(buf) {
				return io.ErrShortWrite
			}

			return w.Commit(ctx, int64(len(buf)), digest.FromBytes(buf), content.WithLabels(contentTypeLabel(desc.MediaType)))
		}()
		if err != nil && !strings.Contains(err.Error(), "already exists") {
			logs.WithError(err).WithField("ref", ref).WithField("desc", desc).Warn("cannot cache config")
		}
	}

	return &res, nil
}

type FetcherFunc func() (remotes.Fetcher, error)

func AsFetcherFunc(f remotes.Fetcher) FetcherFunc {
	return func() (remotes.Fetcher, error) { return f, nil }
}

// DownloadManifest 下载desc指定的manifest，如果返回列表选择第一个
func DownloadManifest(ctx context.Context, fetch FetcherFunc, desc ociv1.Descriptor, options ...ManifestDownloadOption) (cfg *ociv1.Manifest, rdesc *ociv1.Descriptor, err error) {
	var opts manifestDownloadOptions
	for _, o := range options {
		o(&opts)
	}

	var (
		placeInStore bool
		rc           io.ReadCloser
		mediaType    = desc.MediaType
	)
	if opts.Store != nil {
		func() {
			nfo, err := opts.Store.Info(ctx, desc.Digest)
			if errors.Is(err, errdefs.ErrNotFound) {
				// not in store yet
				return
			}
			if err != nil {
				logs.WithError(err).WithField("desc", desc).Warn("cannot get manifest from store")
				return
			}
			if nfo.Labels["Content-Type"] == "" {
				// we have broken data in the store - ignore it and overwrite
				return
			}

			r, err := opts.Store.ReaderAt(ctx, desc)
			if errors.Is(err, errdefs.ErrNotFound) {
				// not in store yet
				return
			}
			if err != nil {
				logs.WithError(err).WithField("desc", desc).Warn("cannot get manifest from store")
				return
			}

			mediaType, rc = nfo.Labels["Content-Type"], &reader{ReaderAt: r}
		}()
	}
	if rc == nil {
		// 没有在缓存中或者缓存中读取失败
		placeInStore = true

		var fetcher remotes.Fetcher
		fetcher, err = fetch()
		if err != nil {
			return
		}

		rc, err = fetcher.Fetch(ctx, desc)
		if err != nil {
			err = xerrors.Errorf("cannot fetch manifest: %w", err)
			return
		}
		mediaType = desc.MediaType
	}

	inpt, err := io.ReadAll(rc)
	rc.Close()
	if err != nil {
		err = xerrors.Errorf("cannot download manifest: %w", err)
		return
	}

	rdesc = &desc
	rdesc.MediaType = mediaType

	switch rdesc.MediaType {
	case images.MediaTypeDockerSchema2ManifestList, ociv1.MediaTypeImageIndex:
		logs.WithField("desc", rdesc).Debug("resolving image index")

		var list ociv1.Index
		err = json.Unmarshal(inpt, &list)
		if err != nil {
			err = xerrors.Errorf("cannot unmarshal index: %w", err)
			return
		}
		if len(list.Manifests) == 0 {
			err = xerrors.Errorf("empty manifest")
			return
		}

		var fetcher remotes.Fetcher
		fetcher, err = fetch()
		if err != nil {
			return
		}

		// TODO: 根据平台选择而不是只选择第一个
		md := list.Manifests[0]
		rc, err = fetcher.Fetch(ctx, md)
		if err != nil {
			err = xerrors.Errorf("cannot download config: %w", err)
			return
		}
		rdesc = &md
		inpt, err = io.ReadAll(rc)
		rc.Close()
		if err != nil {
			err = xerrors.Errorf("cannot download manifest: %w", err)
			return
		}
	}

	switch rdesc.MediaType {
	case images.MediaTypeDockerSchema2Manifest, ociv1.MediaTypeImageManifest:
	default:
		err = xerrors.Errorf("unsupported media type: %s", rdesc.MediaType)
		return
	}

	var res ociv1.Manifest
	err = json.Unmarshal(inpt, &res)
	if err != nil {
		err = xerrors.Errorf("cannot decode config: %w", err)
		return
	}

	if opts.Store != nil && placeInStore {
		// 这里将manifest写到image desc，这样下一次获取镜像就不用去解析index
		w, err := opts.Store.Writer(ctx, content.WithDescriptor(desc), content.WithRef(desc.Digest.String()))
		if err != nil {
			if err != nil && !strings.Contains(err.Error(), "already exists") {
				logs.WithError(err).WithField("desc", *rdesc).Warn("cannot create store writer")
			}
		} else {
			_, err = io.Copy(w, bytes.NewReader(inpt))
			if err != nil {
				logs.WithError(err).WithField("desc", *rdesc).Warn("cannot copy manifest")
			}

			err = w.Commit(ctx, 0, digest.FromBytes(inpt), content.WithLabels(map[string]string{"Content-Type": rdesc.MediaType}))
			if err != nil {
				logs.WithError(err).WithField("desc", *rdesc).Warn("cannot store manifest")
			}
			w.Close()
		}
	}

	cfg = &res
	return
}

type BlobStore interface {
	ReaderAt(ctx context.Context, desc ociv1.Descriptor) (content.ReaderAt, error)

	Writer(ctx context.Context, opts ...content.WriterOpt) (content.Writer, error)

	Info(ctx context.Context, dgst digest.Digest) (content.Info, error)
}


func contentTypeLabel(mt string) map[string]string {
	return map[string]string{"Content-Type": mt}
}

type manifestDownloadOptions struct {
	Store BlobStore
}

// ManifestDownloadOption 改变默认的mainfest下载行为
type ManifestDownloadOption func(*manifestDownloadOptions)
