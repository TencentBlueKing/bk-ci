package registry

import (
	redis "github.com/go-redis/redis/v8"
	ipfs "github.com/ipfs/interface-go-ipfs-core"
)

type IPFSBlobCache struct {
	Redis *redis.Client
	IPFS  ipfs.CoreAPI
}