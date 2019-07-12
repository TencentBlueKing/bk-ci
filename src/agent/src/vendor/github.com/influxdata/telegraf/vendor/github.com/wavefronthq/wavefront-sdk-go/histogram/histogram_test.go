package histogram

import (
	"math/rand"
	"testing"
	"time"
)

func TestHistogram(t *testing.T) {
	if testing.Short() {
		t.Skip("skipping Histogram tests in short mode")
	}

	h := New(MaxBins(3), GranularityOption(MINUTE))

	for i := 0; i < 5; i++ {
		for i := 0; i < 1000; i++ {
			h.Update(rand.Float64())
		}
		time.Sleep(time.Minute)
	}

	distributions := h.Distributions()
	assertEqual(t, len(distributions), 3, "Error on distributions number")

	for _, distribution := range distributions {
		count := 0
		for _, centroid := range distribution.Centroids {
			count += centroid.Count
		}
		assertEqual(t, count, 1000, "Error on centroids count")
	}

	distributions = h.Distributions()
	assertEqual(t, len(distributions), 0, "Error on distributions number")
}

func assertEqual(t *testing.T, a interface{}, b interface{}, e string) {
	if a != b {
		t.Fatalf("%s - %v != %v", e, a, b)
	}
}
