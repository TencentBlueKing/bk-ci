<template>
  <div class="metric-panel-wrapper vld-parent">
    <loading :active.sync="loading" :can-cancel="false" :is-full-page="false" color="#fff" loader="bars"></loading>
    <p class="metric-panel-title">{{title}}</p>
    <div>
      <span class="metric-panel-tip">选择服务</span>
      <select class="metric-selector" v-model="selectedApplication">
        <option v-for="application in Object.keys(metrics)" :key="application">{{application}}</option>
      </select>
    </div>

    <gauge-chart class="gauge-chart" :seriesData="gaugeData"></gauge-chart>
    <line-chart class="liene-chart" :seriesData="lineData"></line-chart>
  </div>
</template>

<script>
import { getMetricsEventSource } from "../../service/stream"
import LineChart from "./line-chart";
import GaugeChart from "./gauge-chart";
import Loading from "vue-loading-overlay";
import "vue-loading-overlay/dist/vue-loading.css";

export default {
  components: { LineChart, GaugeChart, Loading },
  props: {
    metric: String,
    interval: {
      type: Number,
      required: false,
      default: 10
    }
  },
  data() {
    return {
      loading: true,
      title: "---",
      selectedApplication: undefined,
      metrics: {},
      lineData: {},
      gaugeData: 0,
      currentNormalizedTimestamp: 0,
      maxCount: 360
    };
  },
  mounted() {
  },
  created() {
    this.currentNormalizedTimestamp = this.computeCurrentNormalizedTimestamp();
    getMetricsEventSource().addEventListener(this.metric, message => {
      this.loading = false;
      let data = JSON.parse(message.data);
      this.processData(data);
    });
  },
  watch: {
    selectedApplication(newValue, oldValue) {
      if(oldValue) {
        this.updateLineData();
      }
    }
  },
  methods: {
    processData(data) {
      let application, instance;
      for (let tags of data.availableTags) {
        if (tags.tag == "service") application = tags.values[0];
        if (tags.tag == "instance") instance = tags.values[0];
      }
      let value = data.measurements[0].value || 0;
      this.metrics[application] = this.metrics[application] || {};
      this.metrics[application][instance] =
        this.metrics[application][instance] || [];
      let normalizedTimestamp = this.computeCurrentNormalizedTimestamp();
      
      if(this.metrics[application][instance].length >= this.maxCount) {
        this.metrics[application][instance].shift()
      }
      this.metrics[application][instance].push([normalizedTimestamp, value]);

      this.title = data.description;
      this.selectedApplication = this.selectedApplication || application;
      if (this.selectedApplication == application) {
        this.updateLineData();
        this.updateGaugeData(normalizedTimestamp, value);
      }
    },

    updateLineData() {
      this.lineData = this.metrics[this.selectedApplication];
    },

    updateGaugeData(normalizedTimestamp, value) {
      if (this.currentNormalizedTimestamp < normalizedTimestamp) {
        this.currentNormalizedTimestamp = normalizedTimestamp;
        this.gaugeData = value;
      } else {
        this.gaugeData += value;
      }
    },

    computeCurrentNormalizedTimestamp() {
      let currentTimestamp = new Date().getTime();
      return currentTimestamp - (currentTimestamp % (this.interval * 1000));
    }
  }
};
</script>

<style scoped>
.metric-panel-wrapper {
  width: 100%;
  height: 600px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.metric-panel-title {
  color: #eeeeee;
  font-size: 24px;
  margin: 10px 0;
  margin-bottom: 10px;
  font-weight: bold;
}
.metric-panel-tip {
  color: #dddddd;
  font-size: 16px;
  margin-right: 10px;
}
.metric-selector {
  background: #444444 0px 0px;
  height: 28px;
  width: 140px;
  line-height: 28px;
  font-size: 14px;
  color: #ffffff;
  border: 1px solid #cccccc;
  -moz-border-radius: 2px;
  -webkit-border-radius: 2px;
  border-radius: 2px;
}
.gauge-chart {
  height: 60%;
}
.line-chart {
  height: 40%;
}
</style>
