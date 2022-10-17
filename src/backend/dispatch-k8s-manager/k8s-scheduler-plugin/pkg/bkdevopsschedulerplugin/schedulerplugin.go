package bkdevopsschedulerplugin

import (
	"context"
	"encoding/json"
	"fmt"
	v1 "k8s.io/api/core/v1"
	"k8s.io/apimachinery/pkg/api/resource"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/klog/v2"
	"k8s.io/kubernetes/pkg/scheduler/framework"
	schedutil "k8s.io/kubernetes/pkg/scheduler/util"
)

const Name = "BkDevopsSchedulerPlugin"

func (s *SchedulerPlugin) Name() string {
	return Name
}

func New(_ runtime.Object, handle framework.Handle) (framework.Plugin, error) {
	return &SchedulerPlugin{handle}, nil
}

type SchedulerPlugin struct {
	handle framework.Handle
}

var _ framework.ScorePlugin = &SchedulerPlugin{}

// 一些配置信息，需要和kubeManager中的配置文件保持一致
const (
	nodesAnnotation        = "bkci.dispatch.kubenetes/builder-history-nodes"
	readResourceAnnotation = "bkci.dispatch.kubenetes/builder-real-resources"
)

// RealResourceUsage 容器资源使用量
// Cpu cpu核数，单位是毫核 100m = 0.1
// Memory 内存，单位是Mi
type realResourceUsage struct {
	Cpu    string `json:"cpu"`
	Memory string `json:"memory"`
}

func (s *SchedulerPlugin) Score(_ context.Context, _ *framework.CycleState, pod *v1.Pod, nodeName string) (int64, *framework.Status) {
	// 历史调度的node节点
	var nodeHis []string
	if nodesS, ok := pod.ObjectMeta.Annotations[nodesAnnotation]; ok {
		err := json.Unmarshal([]byte(nodesS), &nodeHis)
		if err != nil {
			klog.ErrorS(err, "BkDevopsSchedulerPlugin|Score|Unmarshal", nodesAnnotation, "|error")
		}
	}

	// 真实资源信息
	var realResources []realResourceUsage
	if realS, ok := pod.ObjectMeta.Annotations[readResourceAnnotation]; ok {
		err := json.Unmarshal([]byte(realS), &realResources)
		if err != nil {
			klog.ErrorS(err, "BkDevopsSchedulerPlugin|Score|Unmarshal|", readResourceAnnotation, "|error")
		}
	}

	if len(nodeHis) == 0 && len(realResources) == 0 {
		return framework.MinNodeScore, nil
	}

	nodeScore := calculateNodeHisScore(nodeHis, nodeName)

	nodeInfo, err := s.handle.SnapshotSharedLister().NodeInfos().Get(nodeName)
	if err != nil {
		return framework.MinNodeScore, framework.NewStatus(framework.Error, fmt.Sprintf("getting node %q from Snapshot: %v", nodeName, err))
	}
	realResourceScore := calculateRealResourceScore(realResources, pod, nodeInfo)

	klog.InfoS("BkDevopsSchedulerPlugin|Score",
		"podName", pod.Name,
		"node", nodeInfo.Node().Name,
		"score", nodeScore+realResourceScore,
		"nodeScore", nodeScore,
		"realResourceScore", realResourceScore,
	)

	return nodeScore + realResourceScore, nil
}

var nodeHisScores = map[int]int64{0: 30, 1: 20, 2: 10}

// calculateNodeHisScore 计算历史节点分数，将3个历史节点从最近到最远依次打分 30 - 10分
func calculateNodeHisScore(nodeHis []string, nodeName string) int64 {
	if len(nodeHis) == 0 {
		return framework.MinNodeScore
	}

	for index, name := range nodeHis {
		if name != nodeName {
			continue
		}

		score := framework.MinNodeScore
		if indexS, ok := nodeHisScores[index]; ok {
			score = indexS
		}

		return score
	}

	return framework.MinNodeScore
}

var realResourceIndexScores = map[int]int64{0: 60, 1: 50, 2: 40, 3: 30, 4: 20}

// calculateRealResourceScore 计算真实资源分数，将 limit real(5个) request 打分 70 - 10分
func calculateRealResourceScore(realResources []realResourceUsage, pod *v1.Pod, nodeInfo *framework.NodeInfo) int64 {
	if len(realResources) == 0 {
		return framework.MinNodeScore
	}

	var cpuScore int64 = 0
	var memScore int64 = 0

	nodeLastCpu, nodeLastMem := nodeInfo.Allocatable.MilliCPU-nodeInfo.Requested.MilliCPU, nodeInfo.Allocatable.Memory-nodeInfo.NonZeroRequested.Memory
	podRequestCpu, podRequestMem, podLimitCpu, podLimitMem := calculatePodResource(pod)

	// 对于满足limit的直接打分
	if nodeLastCpu > podLimitCpu {
		cpuScore = 70
	}
	if nodeLastMem > podLimitMem {
		memScore = 70
	}

	// 按照real排名打分
	for index, realR := range realResources {
		if cpuScore == 0 {
			realCpu, err := resource.ParseQuantity(realR.Cpu)
			if err != nil {
				klog.ErrorS(err, "parse quantity real cpu error", "nodeName", nodeInfo.Node().Name, "podName", pod.Name)
			}
			if err == nil && podRequestCpu < realCpu.MilliValue() && nodeLastCpu >= realCpu.MilliValue() {
				if realS, ok := realResourceIndexScores[index]; ok {
					cpuScore = realS
				}
			}
		}
		if memScore != 0 {
			continue
		}
		realMem, err := resource.ParseQuantity(realR.Memory)
		if err != nil {
			klog.ErrorS(err, "parse quantity real mem error", "nodeName", nodeInfo.Node().Name, "podName", pod.Name)
			continue
		}
		if podRequestMem < realMem.Value() && nodeLastMem >= realMem.Value() {
			if realS, ok := realResourceIndexScores[index]; ok {
				memScore = realS
			}
		}
	}

	// 都没有就按最小的request打分
	if cpuScore == 0 {
		cpuScore = 10
	}
	if memScore == 0 {
		memScore = 10
	}

	return (cpuScore + memScore) / 2
}

// calculatePodResource 计算pod请求和最大的cpu和memory
func calculatePodResource(pod *v1.Pod) (requestCpu, requestMem, limitCpu, limitMem int64) {
	for i := range pod.Spec.Containers {
		container := &pod.Spec.Containers[i]
		value := schedutil.GetNonzeroRequestForResource(v1.ResourceCPU, &container.Resources.Requests)
		requestCpu += value

		value = schedutil.GetNonzeroRequestForResource(v1.ResourceMemory, &container.Resources.Requests)
		requestMem += value

		if _, found := container.Resources.Limits[v1.ResourceCPU]; !found {
			limitCpu += 0
		} else {
			limitCpu += container.Resources.Limits.Cpu().MilliValue()
		}

		if _, found := container.Resources.Limits[v1.ResourceMemory]; !found {
			limitMem += 0
		} else {
			limitMem += container.Resources.Limits.Memory().Value()
		}
	}

	return requestCpu, requestMem, limitCpu, limitMem
}

func (s *SchedulerPlugin) ScoreExtensions() framework.ScoreExtensions {
	return nil
}
