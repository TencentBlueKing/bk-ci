package prometheus

import (
	"disaptch-k8s-manager/pkg/config"
	"disaptch-k8s-manager/pkg/logs"
	"disaptch-k8s-manager/pkg/types"
	"encoding/json"
	"fmt"
	"github.com/pkg/errors"
	"io/ioutil"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"
)

const (
	cpuCoreUsageQuery = "irate(container_cpu_usage_seconds_total{container=~\"%s\", image!=\"\", pod=~\"%s\", namespace=\"%s\"}[1m])"
	memoryUsageQuery  = "container_memory_working_set_bytes{container=~\"%s\", image!=\"\", pod=~\"%s\", namespace=\"%s\"}"
)

// QueryRangeContainerResourceUsage 计算资源使用量
func QueryRangeContainerResourceUsage(podName, containerName string, startTime, endTime time.Time) (*types.ContainerResourceUsage, error) {
	if !config.UseRealResourceUsage() {
		return nil, nil
	}
	queryFrom := map[string]string{
		"query": fmt.Sprintf(cpuCoreUsageQuery, containerName, podName, config.Config.Kubernetes.NameSpace),
		"start": strconv.FormatInt(startTime.Unix(), 10),
		"end":   strconv.FormatInt(endTime.Unix(), 10),
		"step":  "1m",
	}
	// 只认 resultType 是 matrix 的返回值
	// "values": [ [ <unix_time>, "<sample_value>" ], ... ]
	res := &types.ContainerResourceUsage{}
	cpuUsage, err := postFormPrometheus(queryFrom)
	if err != nil {
		return nil, err
	}
	if cpuUsage != nil &&
		cpuUsage.Data != nil &&
		cpuUsage.Data.ResultType == "matrix" &&
		cpuUsage.Data.Result != nil &&
		len(cpuUsage.Data.Result) > 0 &&
		len((cpuUsage.Data.Result)[0].Values) > 0 {
		var v float64
		v, err = calculateUsage((cpuUsage.Data.Result)[0].Values)
		if err != nil {
			logs.Error(fmt.Sprintf("%s|%s|calculateUsage cpu error ", podName, containerName), err)
		}

		res.Cpu = fmt.Sprintf("%.0fm", v*1000)
	}

	queryFrom["query"] = fmt.Sprintf(memoryUsageQuery, containerName, podName, config.Config.Kubernetes.NameSpace)
	memUsage, err := postFormPrometheus(queryFrom)
	if err != nil {
		return nil, err
	}
	if memUsage != nil &&
		memUsage.Data != nil &&
		memUsage.Data.ResultType == "matrix" &&
		memUsage.Data.Result != nil &&
		len(memUsage.Data.Result) > 0 &&
		len((memUsage.Data.Result)[0].Values) > 0 {
		v, err := calculateUsage((memUsage.Data.Result)[0].Values)
		if err != nil {
			logs.Error(fmt.Sprintf("%s|%s|calculateUsage memory error ", podName, containerName), err)
		}

		res.Memory = fmt.Sprintf("%.0fMi", v/1000000)
	}

	return res, nil
}

// calculateUsage 通过prometheus 的values计算使用量的最大值
// "values": [ [ <unix_time>, "<sample_value>" ], ... ]
// TODO: 目前采用平均值，后续看线上数据优化算法
func calculateUsage(values [][]interface{}) (float64, error) {
	var sum = 0.0
	for _, value := range values {
		if len(value) != 2 {
			return 0, fmt.Errorf("calculateUsage|values %v format error len not equal 2", values)
		}

		var v float64
		var err error
		switch value[1].(type) {
		case string:
			v, err = strconv.ParseFloat(value[1].(string), 64)
			if err != nil {
				return 0, errors.Wrap(err, "format value error")
			}
			break
		case int:
			v = float64(value[1].(int))
			break
		case float64:
			v = value[1].(float64)
			break
		}

		sum += v
	}
	return sum / float64(len(values)), nil
}

func postFormPrometheus(formParams map[string]string) (*prometheusApiResult, error) {
	if !config.UseRealResourceUsage() {
		return nil, nil
	}

	payload := url.Values{}
	for k, v := range formParams {
		payload.Set(k, v)
	}

	prometheusUrl := fmt.Sprintf("http://%s/api/v1/query_range", config.Config.Dispatch.Builder.RealResource.PrometheusUrl)
	req, err := http.NewRequest(http.MethodPost, prometheusUrl, strings.NewReader(payload.Encode()))
	if err != nil {
		return nil, err
	}

	req.Header.Add("Content-Type", "application/x-www-form-urlencoded; param=value")

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return nil, err
	}

	defer resp.Body.Close()

	data, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf(" %v req prometheus error not ok %s", formParams, data)
	}

	result := &prometheusApiResult{}
	err = json.Unmarshal(data, result)
	if err != nil {
		return nil, err
	}

	return result, nil
}

type prometheusApiResult struct {
	Status string                   `json:"status"`
	Data   *prometheusApiResultData `json:"data"`
}

type prometheusApiResultData struct {
	ResultType string                          `json:"resultType"`
	Result     []prometheusApiResultDataResult `json:"result"`
}

type prometheusApiResultDataResult struct {
	Metric interface{}     `json:"metric"`
	Values [][]interface{} `json:"values"`
}
