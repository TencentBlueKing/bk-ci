package api

const (
	// ProviderPrefixBase32 是 base32 图像规范的图像存储库前缀
	// 与其他前缀不同，这个前缀非常短且缺乏描述性，无法将尽可能多的数据放入
	// Docker 存储库名称尽可能包含 255 个字符
	ProviderPrefixBase32 = "c"

	// ProviderPrefixRemote 是远程获取的镜像规范的镜像仓库前缀
	ProviderPrefixRemote = "remote"

	// ProviderPrefixFixed 是固定图像规范的图像存储库前缀。 这个有用
	// 仅用于调试
	ProviderPrefixFixed = "fixed"
)
