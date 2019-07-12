package tdigest

import (
	"fmt"
	"math"
	"math/rand"
	"sort"
	"testing"

	"github.com/leesper/go_rng"
	"gonum.org/v1/gonum/stat"
)

func init() {
	rand.Seed(0xDEADBEE)
}

func uncheckedNew(options ...tdigestOption) *TDigest {
	t, _ := New(options...)
	return t
}

// Test of tdigest internals and accuracy. Note no t.Parallel():
// during tests the default random seed is consistent, but varying
// concurrency scheduling mixes up the random values used in each test.
// Since there's a random number call inside tdigest this breaks repeatability
// for all tests. So, no test concurrency here.

func TestTInternals(t *testing.T) {
	tdigest := uncheckedNew()

	if !math.IsNaN(tdigest.Quantile(0.1)) {
		t.Errorf("Quantile() on an empty digest should return NaN. Got: %.4f", tdigest.Quantile(0.1))
	}

	if !math.IsNaN(tdigest.CDF(1)) {
		t.Errorf("CDF() on an empty digest should return NaN. Got: %.4f", tdigest.CDF(1))
	}

	_ = tdigest.Add(0.4)

	if tdigest.Quantile(0.1) != 0.4 {
		t.Errorf("Quantile() on a single-sample digest should return the samples's mean. Got %.4f", tdigest.Quantile(0.1))
	}

	if tdigest.CDF(0.3) != 0 {
		t.Errorf("CDF(x) on digest with a single centroid should return 0 if x < mean")
	}

	if tdigest.CDF(0.5) != 1 {
		t.Errorf("CDF(x) on digest with a single centroid should return 1 if x >= mean")
	}

	_ = tdigest.Add(0.5)

	if tdigest.summary.Len() != 2 {
		t.Errorf("Expected size 2, got %d", tdigest.summary.Len())
	}

	err := tdigest.AddWeighted(0, 0)

	if err == nil {
		t.Errorf("Expected AddWeighted() to error out with input (0,0)")
	}
}

func closeEnough(a float64, b float64) bool {
	const EPS = 0.000001
	if (a-b < EPS) && (b-a < EPS) {
		return true
	}
	return false
}

func assertDifferenceSmallerThan(tdigest *TDigest, p float64, m float64, t *testing.T) {
	tp := tdigest.Quantile(p)
	if math.Abs(tp-p) >= m {
		t.Errorf("T-Digest.Quantile(%.4f) = %.4f. Diff (%.4f) >= %.4f", p, tp, math.Abs(tp-p), m)
	}
}

func TestUniformDistribution(t *testing.T) {
	tdigest := uncheckedNew()

	for i := 0; i < 100000; i++ {
		_ = tdigest.Add(rand.Float64())
	}

	assertDifferenceSmallerThan(tdigest, 0.5, 0.02, t)
	assertDifferenceSmallerThan(tdigest, 0.1, 0.01, t)
	assertDifferenceSmallerThan(tdigest, 0.9, 0.01, t)
	assertDifferenceSmallerThan(tdigest, 0.01, 0.005, t)
	assertDifferenceSmallerThan(tdigest, 0.99, 0.005, t)
	assertDifferenceSmallerThan(tdigest, 0.001, 0.001, t)
	assertDifferenceSmallerThan(tdigest, 0.999, 0.001, t)
}

// Asserts quantile p is no greater than absolute m off from "true"
// fractional quantile for supplied data. So m must be scaled
// appropriately for source data range.
func assertDifferenceFromQuantile(data []float64, tdigest *TDigest, p float64, m float64, t *testing.T) {
	q := quantile(p, data)
	tp := tdigest.Quantile(p)

	if math.Abs(tp-q) >= m {
		t.Fatalf("T-Digest.Quantile(%.4f) = %.4f vs actual %.4f. Diff (%.4f) >= %.4f", p, tp, q, math.Abs(tp-q), m)
	}
}

func TestSequentialInsertion(t *testing.T) {
	tdigest := uncheckedNew()

	data := make([]float64, 10000)
	for i := 0; i < len(data); i++ {
		data[i] = float64(i)
	}

	for i := 0; i < len(data); i++ {
		_ = tdigest.Add(data[i])

		assertDifferenceFromQuantile(data[:i+1], tdigest, 0.001, 1.0+0.001*float64(i), t)
		assertDifferenceFromQuantile(data[:i+1], tdigest, 0.01, 1.0+0.005*float64(i), t)
		assertDifferenceFromQuantile(data[:i+1], tdigest, 0.05, 1.0+0.01*float64(i), t)
		assertDifferenceFromQuantile(data[:i+1], tdigest, 0.25, 1.0+0.03*float64(i), t)
		assertDifferenceFromQuantile(data[:i+1], tdigest, 0.5, 1.0+0.03*float64(i), t)
		assertDifferenceFromQuantile(data[:i+1], tdigest, 0.75, 1.0+0.03*float64(i), t)
		assertDifferenceFromQuantile(data[:i+1], tdigest, 0.95, 1.0+0.01*float64(i), t)
		assertDifferenceFromQuantile(data[:i+1], tdigest, 0.99, 1.0+0.005*float64(i), t)
		assertDifferenceFromQuantile(data[:i+1], tdigest, 0.999, 1.0+0.001*float64(i), t)
	}
}

func TestNonSequentialInsertion(t *testing.T) {
	tdigest := uncheckedNew()

	// Not quite a uniform distribution, but close.
	data := make([]float64, 1000)
	for i := 0; i < len(data); i++ {
		tmp := (i * 1627) % len(data)
		data[i] = float64(tmp)
	}

	sorted := make([]float64, 0, len(data))

	for i := 0; i < len(data); i++ {
		_ = tdigest.Add(data[i])
		sorted = append(sorted, data[i])

		// Estimated quantiles are all over the place for low counts, which is
		// OK given that something like P99 is not very meaningful when there are
		// 25 samples. To account for this, increase the error tolerance for
		// smaller counts.
		if i == 0 {
			continue
		}

		max := float64(len(data))
		fac := 1.0 + max/float64(i)

		sort.Float64s(sorted)
		assertDifferenceFromQuantile(sorted, tdigest, 0.001, fac+0.001*max, t)
		assertDifferenceFromQuantile(sorted, tdigest, 0.01, fac+0.005*max, t)
		assertDifferenceFromQuantile(sorted, tdigest, 0.05, fac+0.01*max, t)
		assertDifferenceFromQuantile(sorted, tdigest, 0.25, fac+0.01*max, t)
		assertDifferenceFromQuantile(sorted, tdigest, 0.5, fac+0.02*max, t)
		assertDifferenceFromQuantile(sorted, tdigest, 0.75, fac+0.01*max, t)
		assertDifferenceFromQuantile(sorted, tdigest, 0.95, fac+0.01*max, t)
		assertDifferenceFromQuantile(sorted, tdigest, 0.99, fac+0.005*max, t)
		assertDifferenceFromQuantile(sorted, tdigest, 0.999, fac+0.001*max, t)
	}
}

func TestSingletonInACrowd(t *testing.T) {
	tdigest := uncheckedNew()
	for i := 0; i < 10000; i++ {
		_ = tdigest.Add(10)
	}
	_ = tdigest.Add(20)
	_ = tdigest.Compress()

	for _, q := range []float64{0, 0.5, 0.8, 0.9, 0.99, 0.999} {
		if q == 0.999 {
			// Test for 0.999 disabled since it doesn't
			// pass in the reference implementation
			continue
		}
		result := tdigest.Quantile(q)
		if !closeEnough(result, 10) {
			t.Errorf("Expected Quantile(%.3f) = 10, but got %.4f (size=%d)", q, result, tdigest.summary.Len())
		}
	}

	result := tdigest.Quantile(1)
	if result != 20 {
		t.Errorf("Expected Quantile(1) = 20, but got %.4f (size=%d)", result, tdigest.summary.Len())
	}
}

func TestRespectBounds(t *testing.T) {
	tdigest := uncheckedNew(Compression(10))

	data := []float64{0, 279, 2, 281}
	for _, f := range data {
		_ = tdigest.Add(f)
	}

	quantiles := []float64{0.01, 0.25, 0.5, 0.75, 0.999}
	for _, q := range quantiles {
		result := tdigest.Quantile(q)
		if result < 0 {
			t.Errorf("q(%.3f) = %.4f < 0", q, result)
		}
		if tdigest.Quantile(q) > 281 {
			t.Errorf("q(%.3f) = %.4f > 281", q, result)
		}
	}
}

func TestWeights(t *testing.T) {
	tdigest := uncheckedNew(Compression(10))

	// Create data slice with repeats matching weights we gave to tdigest
	data := []float64{}
	for i := 0; i < 100; i++ {
		_ = tdigest.AddWeighted(float64(i), uint32(i))

		for j := 0; j < i; j++ {
			data = append(data, float64(i))
		}
	}

	assertDifferenceFromQuantile(data, tdigest, 0.001, 1.0+0.001*100.0, t)
	assertDifferenceFromQuantile(data, tdigest, 0.01, 1.0+0.005*100.0, t)
	assertDifferenceFromQuantile(data, tdigest, 0.05, 1.0+0.01*100.0, t)
	assertDifferenceFromQuantile(data, tdigest, 0.25, 1.0+0.01*100.0, t)
	assertDifferenceFromQuantile(data, tdigest, 0.5, 1.0+0.02*100.0, t)
	assertDifferenceFromQuantile(data, tdigest, 0.75, 1.0+0.01*100.0, t)
	assertDifferenceFromQuantile(data, tdigest, 0.95, 1.0+0.01*100.0, t)
	assertDifferenceFromQuantile(data, tdigest, 0.99, 1.0+0.005*100.0, t)
	assertDifferenceFromQuantile(data, tdigest, 0.999, 1.0+0.001*100.0, t)
}

func TestIntegers(t *testing.T) {
	tdigest := uncheckedNew()

	_ = tdigest.Add(1)
	_ = tdigest.Add(2)
	_ = tdigest.Add(3)

	if tdigest.Quantile(0.5) != 2 {
		t.Errorf("Expected p(0.5) = 2, Got %.2f instead", tdigest.Quantile(0.5))
	}

	tdigest = uncheckedNew()

	for _, i := range []float64{1, 2, 2, 2, 2, 2, 2, 2, 3} {
		_ = tdigest.Add(i)
	}

	if tdigest.Quantile(0.5) != 2 {
		t.Errorf("Expected p(0.5) = 2, Got %.2f instead", tdigest.Quantile(0.5))
	}

	var tot uint32
	tdigest.ForEachCentroid(func(mean float64, count uint32) bool {
		tot += count
		return true
	})

	if tot != 9 {
		t.Errorf("Expected the centroid count to be 9, Got %d instead", tot)
	}
}

func cdf(x float64, data []float64) float64 {
	var n1, n2 int
	for i := 0; i < len(data); i++ {
		if data[i] < x {
			n1++
		}
		if data[i] <= x {
			n2++
		}
	}
	return float64(n1+n2) / 2.0 / float64(len(data))
}

func quantile(q float64, data []float64) float64 {
	if len(data) == 0 {
		return math.NaN()
	}

	if q == 1 || len(data) == 1 {
		return data[len(data)-1]
	}

	index := q * (float64(len(data)) - 1)
	return data[int(index)+1]*(index-float64(int(index))) + data[int(index)]*(float64(int(index)+1)-index)
}

func TestMergeNormal(t *testing.T) {
	testMerge(t, false)
}

func TestMergeDescructive(t *testing.T) {
	testMerge(t, true)
}

func testMerge(t *testing.T, destructive bool) {
	if testing.Short() {
		t.Skipf("Skipping merge test. Short flag is on")
	}

	const numItems = 100000

	for _, numSubs := range []int{2, 5, 10, 20, 50, 100} {
		data := make([]float64, numItems)

		subs := make([]*TDigest, numSubs)
		for i := 0; i < numSubs; i++ {
			subs[i] = uncheckedNew()
		}

		dist := uncheckedNew()
		for i := 0; i < numItems; i++ {
			num := rand.Float64()

			data[i] = num
			_ = dist.Add(num)
			_ = subs[i%numSubs].Add(num)
		}

		_ = dist.Compress()

		dist2 := uncheckedNew()
		for i := 0; i < numSubs; i++ {
			if destructive {
				_ = dist2.MergeDestructive(subs[i])
			} else {
				_ = dist2.Merge(subs[i])
			}

		}

		if dist.Count() != dist2.Count() {
			t.Errorf("Expected the number of centroids to be the same. %d != %d", dist.Count(), dist2.Count())
		}

		if dist2.Count() != numItems {
			t.Errorf("Items shouldn't have disappeared. %d != %d", dist2.Count(), numItems)
		}

		sort.Float64s(data)

		for _, q := range []float64{0.001, 0.01, 0.1, 0.2, 0.3, 0.5} {
			z := quantile(q, data)
			p1 := dist.Quantile(q)
			p2 := dist2.Quantile(q)

			e1 := p1 - z
			e2 := p2 - z

			if math.Abs(e2)/q >= 0.3 {
				t.Errorf("rel >= 0.3: parts=%3d q=%.3f e1=%.4f e2=%.4f rel=%.3f real=%.3f",
					numSubs, q, e1, e2, math.Abs(e2)/q, z-q)
			}
			if math.Abs(e2) >= 0.015 {
				t.Errorf("e2 >= 0.015: parts=%3d q=%.3f e1=%.4f e2=%.4f rel=%.3f real=%.3f",
					numSubs, q, e1, e2, math.Abs(e2)/q, z-q)
			}

			z = cdf(q, data)
			e1 = dist.CDF(q) - z
			e2 = dist2.CDF(q) - z

			if math.Abs(e2)/q > 0.3 {
				t.Errorf("CDF e2 < 0.015: parts=%3d q=%.3f e1=%.4f e2=%.4f rel=%.3f",
					numSubs, q, e1, e2, math.Abs(e2)/q)
			}

			if math.Abs(e2) >= 0.015 {
				t.Errorf("CDF e2 < 0.015: parts=%3d q=%.3f e1=%.4f e2=%.4f rel=%.3f",
					numSubs, q, e1, e2, math.Abs(e2)/q)
			}
		}
	}
}

func TestCompressDoesntChangeCount(t *testing.T) {
	tdigest := uncheckedNew()

	for i := 0; i < 1000; i++ {
		_ = tdigest.Add(rand.Float64())
	}

	initialCount := tdigest.Count()

	err := tdigest.Compress()
	if err != nil {
		t.Errorf("Compress() triggered an unexpected error: %s", err)
	}

	if tdigest.Count() != initialCount {
		t.Errorf("Compress() should not change count. Wanted %d, got %d", initialCount, tdigest.Count())
	}
}

func TestGammaDistribution(t *testing.T) {
	const numItems = 100000

	digest := uncheckedNew()
	gammaRNG := rng.NewGammaGenerator(0xDEADBEE)

	data := make([]float64, numItems)
	for i := 0; i < numItems; i++ {
		data[i] = gammaRNG.Gamma(0.1, 0.1)
		_ = digest.Add(data[i])
	}

	sort.Float64s(data)

	softErrors := 0
	for _, q := range []float64{0.001, 0.01, 0.1, 0.5, 0.9, 0.99, 0.999} {

		ix := float64(len(data))*q - 0.5
		index := int(math.Floor(ix))
		p := ix - float64(index)
		realQuantile := data[index]*(1-p) + data[index+1]*p

		// estimated cdf of real quantile(x)
		if math.Abs(digest.CDF(realQuantile)-q) > 0.005 {
			t.Errorf("Error in estimated CDF too high")
		}

		// real cdf of estimated quantile(x)
		error := math.Abs(q - cdf(digest.Quantile(q), data))
		if error > 0.005 {
			softErrors++
		}

		if error > 0.012 {
			t.Errorf("Error in estimated Quantile too high")
		}
	}

	if softErrors >= 3 {
		t.Errorf("Too many soft errors")
	}

	// Issue #17, verify that we are hitting the extreme CDF case
	// XXX Maybe test this properly instead of having a hardcoded value
	extreme := digest.CDF(0.71875)
	if !closeEnough(extreme, 1) {
		t.Errorf("Expected something close to 1 but got %.4f instead", extreme)
	}
}

func shouldPanic(f func(), t *testing.T, message string) {
	defer func() {
		tryRecover := recover()
		if tryRecover == nil {
			t.Errorf(message)
		}
	}()
	f()
}

func TestPanic(t *testing.T) {
	tdigest := uncheckedNew()

	shouldPanic(func() {
		tdigest.Quantile(-42)
	}, t, "Quantile < 0 should panic!")

	shouldPanic(func() {
		tdigest.Quantile(42)
	}, t, "Quantile > 1 should panic!")
}

func TestForEachCentroid(t *testing.T) {
	tdigest := uncheckedNew(Compression(10))

	for i := 0; i < 100; i++ {
		_ = tdigest.Add(float64(i))
	}

	// Iterate limited number.
	means := []float64{}
	tdigest.ForEachCentroid(func(mean float64, count uint32) bool {
		means = append(means, mean)
		return len(means) != 3
	})
	if len(means) != 3 {
		t.Errorf("ForEachCentroid handled incorrect number of data items")
	}

	// Iterate all datapoints.
	means = []float64{}
	tdigest.ForEachCentroid(func(mean float64, count uint32) bool {
		means = append(means, mean)
		return true
	})
	if len(means) != tdigest.summary.Len() {
		t.Errorf("ForEachCentroid did not handle all data")
	}
}

func TestCDFInsideLastCentroid(t *testing.T) {
	// values pulled from a live digest. sorry it's a lot!
	td := &TDigest{
		summary: &summary{
			means:  []float64{2120.75048828125, 2260.3844299316406, 3900.490264892578, 3937.495807647705, 5390.479816436768, 10450.335285186768, 14152.897296905518, 16442.676349639893, 24303.143146514893, 56961.87361526489, 63891.24959182739, 73982.55232620239, 86477.50447463989, 110746.62556838989, 175479.7388496399, 300492.3404121399, 440452.5279121399, 515611.7700996399, 535827.0025215149, 546241.6822090149, 556965.3648262024, 569791.2124824524, 587320.6870918274, 603969.4175605774, 613751.6177558899, 624708.7593574524, 635060.0718574524, 641924.2007637024, 650656.4302558899, 660653.1714668274, 671380.9009590149, 687094.3667793274, 716595.8824043274, 740870.9800605774, 760276.2437324524, 768857.5786933899, 775021.0025215149, 787686.0337715149, 801473.4624824524, 815225.1255683899, 832358.6997871399, 852438.4751777649, 866134.2935371399, 1.10661549666214e+06, 1.1212118980293274e+06, 1.2230108433418274e+06, 1.5446490620918274e+06, 4.306712312091827e+06, 5.487582562091827e+06, 6.306383562091827e+06, 7.089308312091827e+06, 7.520797593341827e+06},
			counts: []uint32{0x1, 0x1, 0x1, 0x1, 0x1, 0x2, 0x1, 0x4, 0x5, 0x6, 0x3, 0x3, 0x4, 0x11, 0x23, 0x2f, 0x1e, 0x1b, 0x36, 0x31, 0x33, 0x4e, 0x5f, 0x61, 0x48, 0x2e, 0x26, 0x28, 0x2a, 0x31, 0x39, 0x51, 0x32, 0x2b, 0x12, 0x8, 0xb, 0xa, 0x11, 0xa, 0x11, 0x9, 0x7, 0x1, 0x1, 0x1, 0x3, 0x2, 0x1, 0x1, 0x1, 0x1},
		},
		compression: 5,
		count:       1250,
		rng:         globalRNG{},
	}

	if cdf := td.CDF(7.144560976650238e+06); cdf > 1 {
		t.Fatalf("invalid: %v", cdf)
	}
}

func TestTrimmedMean(t *testing.T) {
	tests := []struct {
		p1, p2 float64
	}{
		{0, 1},
		{0.1, 0.9},
		{0.2, 0.8},
		{0.25, 0.75},
		{0, 0.5},
		{0.5, 1},
		{0.1, 0.7},
		{0.3, 0.9},
	}

	for _, size := range []int{100, 1000, 10000} {
		for _, test := range tests {
			td := uncheckedNew(Compression(100))

			data := make([]float64, 0, size)
			for i := 0; i < size; i++ {
				f := rand.Float64()
				data = append(data, f)
				err := td.Add(f)
				if err != nil {
					t.Fatal(err)
				}
			}

			got := td.TrimmedMean(test.p1, test.p2)
			wanted := trimmedMean(data, test.p1, test.p2)
			if math.Abs(got-wanted) > 0.01 {
				t.Fatalf("got %f, wanted %f (size=%d p1=%f p2=%f)",
					got, wanted, size, test.p1, test.p2)
			}

			for i := 0; i < 10; i++ {
				err := td.Add(float64(i * 100))
				if err != nil {
					t.Fatal(err)
				}
			}
			mean := td.TrimmedMean(0.1, 0.999)
			if mean < 0 {
				t.Fatalf("mean < 0")
			}
		}
	}
}

func TestTrimmedMeanCornerCases(t *testing.T) {
	td := uncheckedNew(Compression(100))

	mean := td.TrimmedMean(0, 1)
	if mean != 0 {
		t.Fatalf("got %f, wanted 0", mean)
	}

	x := 1.0
	err := td.Add(x)
	if err != nil {
		t.Fatal(err)
	}

	mean = td.TrimmedMean(0, 1)
	if mean != 1 {
		t.Fatalf("got %f, wanted %f", mean, x)
	}

	err = td.Add(1000)
	if err != nil {
		t.Fatal(err)
	}

	mean = td.TrimmedMean(0, 1)
	wanted := 500.5
	if !closeEnough(mean, wanted) {
		t.Fatalf("got %f, wanted %f", mean, wanted)
	}
}

func trimmedMean(ff []float64, p1, p2 float64) float64 {
	sort.Float64s(ff)
	x1 := stat.Quantile(p1, stat.Empirical, ff, nil)
	x2 := stat.Quantile(p2, stat.Empirical, ff, nil)

	var sum float64
	var count int
	for _, f := range ff {
		if f >= x1 && f <= x2 {
			sum += f
			count++
		}
	}
	return sum / float64(count)
}

func TestClone(t *testing.T) {
	seed := func(td *TDigest) {
		for i := 0; i < 100; i++ {
			err := td.Add(rand.Float64())
			if err != nil {
				t.Fatal(err)
			}
		}
	}

	td := uncheckedNew(Compression(42))
	seed(td)
	clone := td.Clone()

	// Clone behaves like td.

	if clone.Compression() != td.Compression() {
		t.Fatalf("got %f, wanted %f", clone.Compression(), td.Compression())
	}

	cloneCount := clone.Count()
	if cloneCount != td.Count() {
		t.Fatalf("got %d, wanted %d", cloneCount, td.Count())
	}

	cloneQuantile := clone.Quantile(1)
	if cloneQuantile != td.Quantile(1) {
		t.Fatalf("got %f, wanted %f", cloneQuantile, td.Quantile(1))
	}

	seed(td)
	if td.Count() == clone.Count() {
		t.Fatal("seed does not work")
	}

	// Clone is not changed after td is changed.

	if clone.Count() != cloneCount {
		t.Fatalf("got %d, wanted %d", clone.Count(), cloneCount)
	}

	if clone.Quantile(1) != cloneQuantile {
		t.Fatalf("got %f, wanted %f", clone.Quantile(1), cloneQuantile)
	}

	// Clone is fully functional.

	err := clone.Add(1)
	if err != nil {
		t.Fatal(err)
	}
}

var compressions = []uint32{1, 10, 20, 30, 50, 100}

func BenchmarkTDigestAddOnce(b *testing.B) {
	for _, compression := range compressions {
		compression := compression
		b.Run(fmt.Sprintf("compression=%d", compression), func(b *testing.B) {
			benchmarkAddOnce(b, compression)
		})
	}
}

func benchmarkAddOnce(b *testing.B, compression uint32) {
	t := uncheckedNew(Compression(compression))

	data := make([]float64, b.N)
	for n := 0; n < b.N; n++ {
		data[n] = rand.Float64()
	}

	b.ReportAllocs()
	b.ResetTimer()
	for n := 0; n < b.N; n++ {
		err := t.Add(data[n])
		if err != nil {
			b.Error(err)
		}
	}
	b.StopTimer()
}

func BenchmarkTDigestAddMulti(b *testing.B) {
	for _, compression := range compressions {
		compression := compression
		for _, n := range []int{10, 100, 1000, 10000} {
			n := n
			name := fmt.Sprintf("compression=%d n=%d", compression, n)
			b.Run(name, func(b *testing.B) {
				benchmarkAddMulti(b, compression, n)
			})
		}
	}
}

func benchmarkAddMulti(b *testing.B, compression uint32, times int) {
	data := make([]float64, times)
	for i := 0; i < times; i++ {
		data[i] = rand.Float64()
	}

	b.ReportAllocs()
	b.ResetTimer()
	for n := 0; n < b.N; n++ {
		t := uncheckedNew(Compression(compression))
		for i := 0; i < times; i++ {
			err := t.AddWeighted(data[i], 1)
			if err != nil {
				b.Error(err)
			}
		}
	}
	b.StopTimer()
}

func BenchmarkTDigestMerge(b *testing.B) {
	for _, compression := range compressions {
		compression := compression
		for _, n := range []int{1, 10, 100} {
			name := fmt.Sprintf("compression=%d n=%d", compression, n)
			b.Run(name, func(b *testing.B) {
				benchmarkMerge(b, compression, n)
			})
		}
	}
}

func benchmarkMerge(b *testing.B, compression uint32, times int) {
	ts := make([]*TDigest, times)
	for i := 0; i < times; i++ {
		ts[i] = randomTDigest(compression)
	}

	b.ReportAllocs()
	b.ResetTimer()
	for n := 0; n < b.N; n++ {
		dst := uncheckedNew(Compression(compression))

		for i := 0; i < times; i++ {
			err := dst.Merge(ts[i])
			if err != nil {
				b.Fatal(err)
			}
		}

		err := dst.Compress()
		if err != nil {
			b.Fatal(err)
		}
	}
}

func randomTDigest(compression uint32) *TDigest {
	t := uncheckedNew(Compression(compression))
	n := 20 * int(compression)
	for i := 0; i < n; i++ {
		err := t.Add(rand.Float64())
		if err != nil {
			panic(err)
		}
	}
	return t
}

var sumSizes = []int{10, 100, 1000, 10000}

func BenchmarkSumLoopSimple(b *testing.B) {
	for _, size := range sumSizes {
		size := size
		b.Run(fmt.Sprint(size), func(b *testing.B) {
			benchmarkSumLoopSimple(b, size)
		})
	}
}

func benchmarkSumLoopSimple(b *testing.B, size int) {
	counts := generateCounts(size)
	indexes := generateIndexes(size)

	b.ReportAllocs()
	b.ResetTimer()
	for n := 0; n < b.N; n++ {
		for _, idx := range indexes {
			_ = sumUntilIndexSimple(counts, idx)
		}
	}
}

func BenchmarkSumLoopUnrolled(b *testing.B) {
	for _, size := range sumSizes {
		size := size
		b.Run(fmt.Sprint(size), func(b *testing.B) {
			benchmarkSumLoopUnrolled(b, size)
		})
	}
}

func benchmarkSumLoopUnrolled(b *testing.B, size int) {
	counts := generateCounts(size)
	indexes := generateIndexes(size)

	b.ReportAllocs()
	b.ResetTimer()
	for n := 0; n < b.N; n++ {
		for _, idx := range indexes {
			_ = sumUntilIndex(counts, idx)
		}
	}
}

func generateCounts(size int) []uint32 {
	counts := make([]uint32, size)
	for i := 0; i < size; i++ {
		counts[i] = rand.Uint32()
	}
	return counts
}

func generateIndexes(size int) []int {
	const num = 100

	indexes := make([]int, num)
	for i := 0; i < num; i++ {
		indexes[i] = rand.Intn(size)
	}
	return indexes
}

func sumUntilIndexSimple(counts []uint32, idx int) uint64 {
	var sum uint64
	for _, c := range counts {
		sum += uint64(c)
	}
	return sum
}
