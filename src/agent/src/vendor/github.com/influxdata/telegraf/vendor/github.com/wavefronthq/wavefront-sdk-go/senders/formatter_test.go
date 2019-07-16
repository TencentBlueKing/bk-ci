package senders

import (
	"testing"

	"github.com/wavefronthq/wavefront-sdk-go/histogram"
)

func TestMetricLine(t *testing.T) {
	line, err := MetricLine("foo.metric", 1.2, 1533529977, "test_source",
		map[string]string{"env": "test"}, "")
	expected := "\"foo.metric\" 1.2 1533529977 source=\"test_source\" \"env\"=\"test\"\n"
	assertEquals(expected, line, err, t)

	line, err = MetricLine("foo.metric", 1.2, 1533529977, "",
		map[string]string{"env": "test"}, "default")
	expected = "\"foo.metric\" 1.2 1533529977 source=\"default\" \"env\"=\"test\"\n"
	assertEquals(expected, line, err, t)
}

func TestHistoLine(t *testing.T) {
	centroids := makeCentroids()

	line, err := HistoLine("request.latency", centroids, map[histogram.Granularity]bool{histogram.MINUTE: true},
		1533529977, "test_source", map[string]string{"env": "test"}, "")
	expected := "!M 1533529977 #20 30 #10 5.1 \"request.latency\" source=\"test_source\" \"env\"=\"test\"\n"
	assertEquals(expected, line, err, t)

	line, err = HistoLine("request.latency", centroids, map[histogram.Granularity]bool{histogram.MINUTE: true},
		1533529977, "", map[string]string{"env": "test"}, "default")
	expected = "!M 1533529977 #20 30 #10 5.1 \"request.latency\" source=\"default\" \"env\"=\"test\"\n"
	assertEquals(expected, line, err, t)

	line, err = HistoLine("request.latency", centroids, map[histogram.Granularity]bool{histogram.HOUR: true},
		1533529977, "", map[string]string{"env": "test"}, "default")
	expected = "!H 1533529977 #20 30 #10 5.1 \"request.latency\" source=\"default\" \"env\"=\"test\"\n"
	assertEquals(expected, line, err, t)

	line, err = HistoLine("request.latency", centroids, map[histogram.Granularity]bool{histogram.DAY: true},
		1533529977, "", map[string]string{"env": "test"}, "default")
	expected = "!D 1533529977 #20 30 #10 5.1 \"request.latency\" source=\"default\" \"env\"=\"test\"\n"
	assertEquals(expected, line, err, t)

	line, err = HistoLine("request.latency", centroids, map[histogram.Granularity]bool{histogram.MINUTE: true, histogram.HOUR: true},
		1533529977, "test_source", map[string]string{"env": "test"}, "")
	expected = "!M 1533529977 #20 30 #10 5.1 \"request.latency\" source=\"test_source\" \"env\"=\"test\"\n" +
		"!H 1533529977 #20 30 #10 5.1 \"request.latency\" source=\"test_source\" \"env\"=\"test\"\n"
	if len(line) != len(expected) {
		t.Errorf("lines don't match. expected: %s, actual: %s", expected, line)
	}
}

func TestSpanLine(t *testing.T) {
	line, err := SpanLine("order.shirts", 1533531013, 343500, "test_source",
		"7b3bf470-9456-11e8-9eb6-529269fb1459", "7b3bf470-9456-11e8-9eb6-529269fb1459",
		[]string{"7b3bf470-9456-11e8-9eb6-529269fb1458"}, nil, nil, nil, "")
	expected := "\"order.shirts\" source=\"test_source\" traceId=7b3bf470-9456-11e8-9eb6-529269fb1459" +
		" spanId=7b3bf470-9456-11e8-9eb6-529269fb1459 parent=7b3bf470-9456-11e8-9eb6-529269fb1458 1533531013 343500\n"
	assertEquals(expected, line, err, t)

	line, err = SpanLine("order.shirts", 1533531013, 343500, "test_source",
		"7b3bf470-9456-11e8-9eb6-529269fb1459", "7b3bf470-9456-11e8-9eb6-529269fb1459", nil,
		[]string{"7b3bf470-9456-11e8-9eb6-529269fb1458"}, []SpanTag{{Key: "env", Value: "test"}}, nil, "")
	expected = "\"order.shirts\" source=\"test_source\" traceId=7b3bf470-9456-11e8-9eb6-529269fb1459" +
		" spanId=7b3bf470-9456-11e8-9eb6-529269fb1459 followsFrom=7b3bf470-9456-11e8-9eb6-529269fb1458 \"env\"=\"test\" 1533531013 343500\n"
	assertEquals(expected, line, err, t)

	line, err = SpanLine("order.shirts", 1533531013, 343500, "test_source",
		"7b3bf470-9456-11e8-9eb6-529269fb1459", "7b3bf470-9456-11e8-9eb6-529269fb1459", nil,
		[]string{"7b3bf470-9456-11e8-9eb6-529269fb1458"},
		[]SpanTag{{Key: "env", Value: "test"}, {Key: "env", Value: "dev"}}, nil, "")
	expected = "\"order.shirts\" source=\"test_source\" traceId=7b3bf470-9456-11e8-9eb6-529269fb1459" +
		" spanId=7b3bf470-9456-11e8-9eb6-529269fb1459 followsFrom=7b3bf470-9456-11e8-9eb6-529269fb1458 \"env\"=\"test\" \"env\"=\"dev\" 1533531013 343500\n"
	assertEquals(expected, line, err, t)
}

func assertEquals(expected, actual string, err error, t *testing.T) {
	if err != nil {
		t.Error(err)
	}
	if actual != expected {
		t.Errorf("lines don't match.\n expected: %s\n actual: %s", expected, actual)
	}
}

func makeCentroids() []histogram.Centroid {
	centroids := []histogram.Centroid{
		{
			Value: 30.0,
			Count: 20,
		},
		{
			Value: 5.1,
			Count: 10,
		},
	}
	return centroids
}
