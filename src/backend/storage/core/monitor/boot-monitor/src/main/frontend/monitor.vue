<template>
  <div class="main-wrapper">
    <monitor-container>
      <monitor-panel v-for="health in healthList" :key="health" class="health">
        <health-monitor :health="health" />
      </monitor-panel>
    </monitor-container>
    <monitor-container>
      <monitor-panel v-for="metric in metricList" :key="metric" class="metric">
        <metric-monitor :metric="metric" />
      </monitor-panel>
    </monitor-container>
  </div>
</template>

<script>
import {healthEventSource} from './service/stream';
import MonitorContainer from "./components/monitor-container";
import MonitorPanel from "./components/monitor-panel";
import MetricMonitor from "./monitor/metric/metric-monitor";
import HealthMonitor from "./monitor/health/health-monitor";

export default {
  components: {
    MonitorContainer,
    MonitorPanel,
    MetricMonitor,
    HealthMonitor
  },

  props: {
    applications: {
      type: Array,
      required: true
    },
    instance: { 
      type: Object,
      required: true
    }
  },

  data() {
    return {
      metricList: [],
      healthList: []
    };
  },
  mounted() {
    window.onbeforeunload = () => healthEventSource.close();
  },

  created() {
    console.log(this.applications[0].instances[0].id)
    console.log("id: ", this.applications[0].instances[0].id)
    console.log(this.applications[0].instances[0].id.value)
    this.getConfig().then(response => {
      this.metricList = Object.keys(response.data.metrics)
      this.healthList = Object.keys(response.data.health)
    })
  },

  methods: {
    async getConfig() {
      return await this.$axios.get("monitor/config")
    }
  }
  
};
</script>

<style scoped>
.main-wrapper {
  width: 100%;
  height: 100%;
  background-color: rgb(36, 36, 36);
}
.metric {
  flex: 0 0 48%;
}
.health {
  flex: 0 0 32%;
}
</style>