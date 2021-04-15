<template>
  <div :id="id" class="chart"></div>
</template>

<script>
import echarts from "echarts";

export default {
  props: ["seriesData"],
  data() {
    return {
      id: Math.random()
        .toString(36)
        .substring(2),
      chart: undefined,
      chartOption: {
        series: [
          {
            name: "总数",
            type: "gauge",
            max: 10,
            data: [{ value: 0, name: "数量" }]
          }
        ]
      },
    };
  },
  mounted() {
    this.chart = echarts.init(document.getElementById(this.id), "dark");
    this.chart.setOption(this.chartOption);
  },

  computed: {},

  watch: {
    seriesData: {
      handler(newData) {
        if (newData > this.chartOption.series[0].max) {
          this.chartOption.series[0].max = newData;
        }
        this.chartOption.series[0].data[0].value = newData;
        this.chart.setOption(this.chartOption, true);
      }
    }
  },

  methods: {
  }
};
</script>

<style>
.chart {
  height: 100%;
  width: 100%;
}
</style>