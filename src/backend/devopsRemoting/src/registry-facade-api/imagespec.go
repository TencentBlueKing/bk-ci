package api

type ImageSpec struct {
	// BaseRef 指向另一个registry中的镜像
	BaseRef string `json:"baseRef,omitempty"`
	// IdeRef ide进程使用的镜像
	IdeRef string `json:"ideRef,omitempty"`
	// ContentLayer 描述提供工作区内容的最后几层
	ContentLayer []*ContentLayer `json:"contentLayer,omitempty"`
	// RemotingRef remoting进程使用的镜像
	RemotingRef string `json:"remotingRef,omitempty"`
	// IdeLayerRef 除了web-ide和remoting之外的所有层
	IdeLayerRef []string `json:"ideLayerRef,omitempty"`
}

type ContentLayer struct {
	// Remote 远程拉取的层
	Remote *RemoteContentLayer `json:"remote"`
	// Direct 直接拉取的层
	Direct *DirectContentLayer `json:"direct"`
}

// RemoteContentLayer 是一个可以从远程 URL 下载的图层。
// 如果 diffId 为空或等于摘要，则该层应被解压缩。
type RemoteContentLayer struct {
	// Url 具体层内容，这必须是一个有效的 HTTPS URL 指向到 tar.gz 文件
	Url string `json:"url,omitempty"`
	// Digest URL指向的文件的摘要（内容哈希）
	Digest string `json:"digest,omitempty"`
	// DiffId 是 URL 指向的未压缩数据的摘要（内容哈希）
	// URL 指向一个压缩文件。 如果文件未压缩以该字段开头
	// 可以为空或与摘要相同。
	DiffId string `json:"diffId,omitempty"`
	// MediaType 是图层的内容类型，应该是以下之一：
	//	application/vnd.oci.image.layer.v1.tar
	//	application/vnd.oci.image.layer.v1.tar+gzip
	//	application/vnd.oci.image.layer.v1.tar+zstd
	MediaType string `json:"mediaType,omitempty"`
	// size 是层下载的大小（以字节为单位）
	Size int64 `json:"size,omitempty"`
}

// DirectContentLayer 一个未压缩的tar文件，直接添加为层
type DirectContentLayer struct {
	// 用作层的未压缩tar文件的字节数
	Content []byte `json:"content,omitempty"`
}
