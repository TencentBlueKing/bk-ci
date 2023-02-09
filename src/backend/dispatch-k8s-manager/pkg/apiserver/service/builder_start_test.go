package service

import (
	"disaptch-k8s-manager/pkg/types"
	"github.com/stretchr/testify/assert"
	"testing"
)

func Test_calculateRealResource(t *testing.T) {
	res := []types.ContainerResourceUsage{
		{Cpu: "373m", Memory: "277Mi"},
		{Cpu: "848m", Memory: "390Mi"},
		{Cpu: "966m", Memory: "378Mi"},
		{Cpu: "0m", Memory: "0Mi"},
		{Cpu: "832m", Memory: "471Mi"},
		{Cpu: "738m", Memory: "466Mi"},
		{Cpu: "941m", Memory: "323Mi"},
	}
	act := calculateRealResource("", res)
	expect := []types.ContainerResourceUsage{
		{Cpu: "966m", Memory: "471Mi"},
		{Cpu: "941m", Memory: "466Mi"},
		{Cpu: "848m", Memory: "390Mi"},
		{Cpu: "832m", Memory: "378Mi"},
		{Cpu: "738m", Memory: "323Mi"},
	}
	assertTrue := assert.New(t)
	assertTrue.Equal(expect, act)
}
