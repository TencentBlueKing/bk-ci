package v1

import "github.com/ericchiang/k8s"

func init() {
	k8s.Register("apiregistration.k8s.io", "v1", "apiservices", false, &APIService{})

	k8s.RegisterList("apiregistration.k8s.io", "v1", "apiservices", false, &APIServiceList{})
}
