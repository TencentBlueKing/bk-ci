package registry

import (
	"bytes"
	"common/logs"
	"context"
	"encoding/json"
	"io"
	"net/http"
	"registry-facade/api"
	"sync"
	"syscall"
	"time"

	"github.com/containerd/containerd/content"
	"github.com/containerd/containerd/errdefs"
	"github.com/containerd/containerd/remotes"
	distv2 "github.com/docker/distribution/registry/api/v2"
	"github.com/gorilla/handlers"
	"github.com/opencontainers/go-digest"
	ociv1 "github.com/opencontainers/image-spec/specs-go/v1"
	"github.com/pkg/errors"
	"golang.org/x/xerrors"
	"k8s.io/apimachinery/pkg/util/wait"
)

var backoffParams = wait.Backoff{
	Duration: 100 * time.Millisecond,
	Factor:   1.5,
	Jitter:   0.2,
	Steps:    4,
}

func (reg *Registry) handleBlob(ctx context.Context, r *http.Request) http.Handler {
	spname, name := getSpecProviderName(ctx)
	sp, ok := reg.SpecProvider[spname]
	if !ok {
		logs.WithField("specProvName", spname).Error("unknown spec provider")
		return http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
			respondWithError(w, distv2.ErrorCodeManifestUnknown)
		})
	}
	spec, err := sp.GetSpec(ctx, name)
	if err != nil {
		logs.WithError(err).WithField("specProvName", spname).WithField("name", name).Error("cannot get spec")
		return http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
			respondWithError(w, distv2.ErrorCodeManifestUnknown)
		})
	}

	dgst, err := digest.Parse(getDigest(ctx))
	if err != nil {
		logs.WithError(err).WithField("instanceId", name).Error("cannot get workspace details")
	}

	blobHandler := &blobHandler{
		Context: ctx,
		Digest:  dgst,
		Name:    name,

		Spec:     spec,
		Resolver: reg.Resolver(),
		Store:    reg.Store,
		// IPFS:     reg.IPFS,
		AdditionalSources: []BlobSource{
			reg.LayerSource,
		},
		ConfigModifier: reg.ConfigModifier,

		Metrics: reg.metrics,
	}

	mhandler := handlers.MethodHandler{
		"GET":  http.HandlerFunc(blobHandler.getBlob),
		"HEAD": http.HandlerFunc(blobHandler.getBlob),
	}
	res := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		reg.metrics.BlobCounter.Inc()
		mhandler.ServeHTTP(w, r)
	})

	return res
}

type blobHandler struct {
	Context context.Context
	Digest  digest.Digest
	Name    string

	Spec              *api.ImageSpec
	Resolver          remotes.Resolver
	Store             BlobStore
	// IPFS              *IPFSBlobCache
	AdditionalSources []BlobSource
	ConfigModifier    ConfigModifier

	Metrics *metrics
}

var bufPool = sync.Pool{
	New: func() interface{} {
		// setting to 4096 to align with PIPE_BUF
		// http://man7.org/linux/man-pages/man7/pipe.7.html
		buffer := make([]byte, 4096)
		return &buffer
	},
}

func (bh *blobHandler) getBlob(w http.ResponseWriter, r *http.Request) {
	// v2.ErrorCodeBlobUnknown.WithDetail(bh.Digest)
	//nolint:staticcheck,ineffassign
	// span, ctx := opentracing.StartSpanFromContext(r.Context(), "getBlob")

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	err := func() error {
		// TODO: 不应该一遍又一遍地下载相同的清单，
		// 我们应该将它添加到store并尝试从那里获取它。
		// 只有当store获取失败时，我们才应该尝试下载它。
		manifest, fetcher, err := bh.downloadManifest(ctx, bh.Spec.BaseRef)
		if err != nil {
			return xerrors.Errorf("cannnot fetch the manifest: %w", err)
		}

		var srcs []BlobSource

		// 1. local store (faster)
		srcs = append(srcs, storeBlobSource{Store: bh.Store})

		// // 2. IPFS (if configured)
		// if bh.IPFS != nil {
		// 	ipfsSrc := ipfsBlobSource{source: bh.IPFS}
		// 	srcs = append(srcs, ipfsSrc)
		// }

		// 3. upstream registry
		srcs = append(srcs, proxyingBlobSource{Fetcher: fetcher, Blobs: manifest.Layers})

		srcs = append(srcs, &configBlobSource{Fetcher: fetcher, Spec: bh.Spec, Manifest: manifest, ConfigModifier: bh.ConfigModifier})
		srcs = append(srcs, bh.AdditionalSources...)

		w.Header().Set("Etag", bh.Digest.String())

		var retrieved bool
		var src BlobSource
		var dontCache bool
		for _, s := range srcs {
			if !s.HasBlob(ctx, bh.Spec, bh.Digest) {
				continue
			}

			retrieved, dontCache, err = bh.retrieveFromSource(ctx, s, w, r)
			if err != nil {
				logs.WithField("src", src.Name()).WithError(err).Error("unable to retrieve blob")
			}

			if retrieved {
				src = s
				break
			}
		}

		if !retrieved {
			logs.WithField("baseRef", bh.Spec.BaseRef).WithError(err).Error("unable to return blob")
			return xerrors.Errorf("unable to return blob: %w", err)
		}

		if dontCache {
			return nil
		}

		go func() {
			// we can do this only after the io.Copy above. Otherwise we might expect the blob
			// to be in the blobstore when in reality it isn't.
			_, _, _, rc, err := src.GetBlob(context.Background(), bh.Spec, bh.Digest)
			if err != nil {
				logs.WithError(err).WithField("digest", bh.Digest).Warn("cannot push to IPFS - unable to get blob")
				return
			}
			if rc == nil {
				logs.WithField("digest", bh.Digest).Warn("cannot push to IPFS - blob is nil")
				return
			}

			defer rc.Close()

			// err = bh.IPFS.Store(context.Background(), bh.Digest, rc, mediaType)
			if err != nil {
				logs.WithError(err).WithField("digest", bh.Digest).Warn("cannot push to IPFS")
			}
		}()

		return nil
	}()

	if err != nil {
		logs.WithError(err).Error("cannot get blob")
		respondWithError(w, err)
	}
	// tracing.FinishSpan(span, &err)
}

func (bh *blobHandler) retrieveFromSource(ctx context.Context, src BlobSource, w http.ResponseWriter, r *http.Request) (handled, dontCache bool, err error) {
	logs.Debugf("retrieving blob %s from %s", bh.Digest, src.Name())
	dontCache, mediaType, url, rc, err := src.GetBlob(ctx, bh.Spec, bh.Digest)
	if err != nil {
		return false, true, xerrors.Errorf("cannnot fetch the blob from source %s: %v", src.Name(), err)
	}
	if rc != nil {
		defer rc.Close()
	}

	if url != "" {
		http.Redirect(w, r, url, http.StatusPermanentRedirect)
		return true, true, nil
	}

	w.Header().Set("Content-Type", mediaType)

	bp := bufPool.Get().(*[]byte)
	defer bufPool.Put(bp)

	var n int64
	t0 := time.Now()
	err = wait.ExponentialBackoffWithContext(ctx, backoffParams, func() (done bool, err error) {
		n, err = io.CopyBuffer(w, rc, *bp)
		if err == nil {
			return true, nil
		}
		if errors.Is(err, syscall.ECONNRESET) || errors.Is(err, syscall.EPIPE) {
			logs.WithField("blobSource", src.Name()).WithField("baseRef", bh.Spec.BaseRef).WithError(err).Warn("retry get blob because of error")
			return false, nil
		}
		return true, err
	})

	if err != nil {
		if bh.Metrics != nil {
			bh.Metrics.BlobDownloadCounter.WithLabelValues(src.Name(), "false").Inc()
		}
		return false, true, err
	}

	if bh.Metrics != nil {
		bh.Metrics.BlobDownloadCounter.WithLabelValues(src.Name(), "true").Inc()
		bh.Metrics.BlobDownloadSpeedHist.WithLabelValues(src.Name()).Observe(float64(n) / time.Since(t0).Seconds())
		bh.Metrics.BlobDownloadSizeCounter.WithLabelValues(src.Name()).Add(float64(n))
	}

	return true, dontCache, nil
}

func (bh *blobHandler) downloadManifest(ctx context.Context, ref string) (res *ociv1.Manifest, fetcher remotes.Fetcher, err error) {
	_, desc, err := bh.Resolver.Resolve(ctx, ref)
	if err != nil {
		// ErrInvalidAuthorization
		return nil, nil, err
	}

	fetcher, err = bh.Resolver.Fetcher(ctx, ref)
	if err != nil {
		logs.WithError(err).WithField("ref", ref).WithField("instanceId", bh.Name).Error("cannot get fetcher")
		return nil, nil, err
	}
	res, _, err = DownloadManifest(ctx, AsFetcherFunc(fetcher), desc, WithStore(bh.Store))
	return
}

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

type storeBlobSource struct {
	Store BlobStore
}

func (sbs storeBlobSource) Name() string {
	return "blobstore"
}

func (sbs storeBlobSource) HasBlob(ctx context.Context, spec *api.ImageSpec, dgst digest.Digest) bool {
	_, err := sbs.Store.Info(ctx, dgst)
	return err == nil
}

func (sbs storeBlobSource) GetBlob(ctx context.Context, spec *api.ImageSpec, dgst digest.Digest) (dontCache bool, mediaType string, url string, data io.ReadCloser, err error) {
	info, err := sbs.Store.Info(ctx, dgst)
	if err != nil {
		return
	}

	r, err := sbs.Store.ReaderAt(ctx, ociv1.Descriptor{Digest: dgst})
	if err != nil {
		return
	}

	return false, info.Labels["Content-Type"], "", &reader{ReaderAt: r}, nil
}

type proxyingBlobSource struct {
	Fetcher remotes.Fetcher
	Blobs   []ociv1.Descriptor
}

func (sbs proxyingBlobSource) Name() string {
	return "proxy"
}

func (pbs proxyingBlobSource) HasBlob(ctx context.Context, spec *api.ImageSpec, dgst digest.Digest) bool {
	for _, b := range pbs.Blobs {
		if b.Digest == dgst {
			return true
		}
	}
	return false
}

func (pbs proxyingBlobSource) GetBlob(ctx context.Context, spec *api.ImageSpec, dgst digest.Digest) (dontCache bool, mediaType string, url string, data io.ReadCloser, err error) {
	var src ociv1.Descriptor
	for _, b := range pbs.Blobs {
		if b.Digest == dgst {
			src = b
			break
		}
	}
	if src.Digest == "" {
		err = errdefs.ErrNotFound
		return
	}

	r, err := pbs.Fetcher.Fetch(ctx, src)
	if err != nil {
		return
	}
	return false, src.MediaType, "", r, nil
}

type configBlobSource struct {
	Fetcher        remotes.Fetcher
	Spec           *api.ImageSpec
	Manifest       *ociv1.Manifest
	ConfigModifier ConfigModifier
}

func (sbs configBlobSource) Name() string {
	return "config"
}

func (pbs *configBlobSource) HasBlob(ctx context.Context, spec *api.ImageSpec, dgst digest.Digest) bool {
	cfg, err := pbs.getConfig(ctx)
	if err != nil {
		logs.WithError(err).Error("cannot (re-)produce image config")
		return false
	}

	cfgDgst := digest.FromBytes(cfg)
	return cfgDgst == dgst
}

func (pbs *configBlobSource) GetBlob(ctx context.Context, spec *api.ImageSpec, dgst digest.Digest) (dontCache bool, mediaType string, url string, data io.ReadCloser, err error) {
	if !pbs.HasBlob(ctx, spec, dgst) {
		err = distv2.ErrorCodeBlobUnknown
		return
	}

	cfg, err := pbs.getConfig(ctx)
	if err != nil {
		return
	}
	mediaType = pbs.Manifest.Config.MediaType
	data = io.NopCloser(bytes.NewReader(cfg))
	return
}

func (pbs *configBlobSource) getConfig(ctx context.Context) (rawCfg []byte, err error) {
	manifest := *pbs.Manifest
	cfg, err := DownloadConfig(ctx, AsFetcherFunc(pbs.Fetcher), "", manifest.Config)
	if err != nil {
		return
	}

	_, err = pbs.ConfigModifier(ctx, pbs.Spec, cfg)
	if err != nil {
		return
	}

	rawCfg, err = json.Marshal(cfg)
	return
}
