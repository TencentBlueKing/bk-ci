package registry

import (
	"context"
	"io"
	"registry/api"

	"github.com/containerd/containerd/content"
	"github.com/opencontainers/go-digest"
)

// BlobSource 提供blob以供下载
type BlobSource interface {
	// HasBlob 当前源是否提供blob
	HasBlob(ctx context.Context, details *api.ImageSpec, dgst digest.Digest) bool

	// GetBlob 提供对blob的访问，如果返回一个ReadCloser，接收方应该最终调用关闭它。
	GetBlob(ctx context.Context, details *api.ImageSpec, dgst digest.Digest) (dontCache bool, mediaType string, url string, data io.ReadCloser, err error)

	// Name 标识指标中的 blob 源
	Name() string
}

type reader struct {
	content.ReaderAt
	off int64
}

func (r *reader) Read(b []byte) (n int, err error) {
	n, err = r.ReadAt(b, r.off)
	r.off += int64(n)
	return
}
