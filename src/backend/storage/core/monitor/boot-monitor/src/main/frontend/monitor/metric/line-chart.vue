<template>
  <div :id="id" class="chart"></div>
</template>

<script>
import echarts from "echarts";

export default {
  props: {
    seriesData: {},
    
  },
  data() {
    return {
      id: Math.random()
        .toString(36)
        .substring(2),
      chart: undefined,
      chartOption: {
        tooltip: {
          trigger: "axis"
        },
        toolbox: {},
        legend: {},
        xAxis: {
          type: "time",
          minInterval: 1000 * 10,
          splitNumber: 10,
          splitLine: { show: false }
        },
        yAxis: {
          boundaryGap: [0, "50%"],
          splitLine: { show: true }
        },
        series: []
      }
    };
  },
  mounted() {
    this.chart = echarts.init(document.getElementById(this.id), "dark");
  },

  computed: {},

  watch: {
    seriesData: {
      handler(newSeriesData) {
        this.chartOption.series = Object.keys(newSeriesData).map(key =>
          this.generateSeries(key, newSeriesData[key])
        );
        this.chart.hideLoading();
        this.chart.setOption(this.chartOption, true);
      },
      deep: true
    }
  },

  methods: {
    generateSeries(instance, data) {
      return {
        name: instance,
        type: "line",
        data: data,
        markPoint: {
          data: [{ type: "max", name: "最大值" }]
        },
        markLine: {
          data: [{ type: "average", name: "平均值" }]
        }
      };
    }
  }
};
</script>

<style scoped>
.chart {
  height: 100%;
}
</style>