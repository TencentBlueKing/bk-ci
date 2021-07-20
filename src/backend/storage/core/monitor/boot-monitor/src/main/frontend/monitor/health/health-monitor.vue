<template>
  <div class="health-monitor-wrapper vld-parent">
    <loading :active.sync="loading" :can-cancel="false" :is-full-page="false" color="#fff" loader="bars"></loading>
    <p class="health-monitor-title">{{health}}</p>
    <p class="health-monitor-status success" v-if="downCount === 0">HEALTH</p>
    <p class="health-monitor-status danger" v-else>UNHEALTH</p>
    <div class="health-monitor-detail">
      <div class="health-detail-item">
        <p class="health-detail-heading">TOTAL</p>
        <p class="health-detail-value" v-text="totalCount" />
      </div>
      <div class="health-detail-item">
        <p class="health-detail-heading">UP</p>
        <p class="health-detail-value" v-text="upCount" />
      </div>
      <div class="health-detail-item">
        <p class="health-detail-heading">DOWN</p>
        <p v-bind:class="['health-detail-value', downCount > 0 ? 'danger' : '']" v-text="downCount" />
      </div>
    </div>
  </div>
</template>

<script>
import { getHealthEventSource } from "../../service/stream"
import Loading from "vue-loading-overlay";
import "vue-loading-overlay/dist/vue-loading.css";

export default {
  components: { Loading },
  props: {
    health: String
  },
  data() {
    return {
      loading: true,
      healthData: {},
      totalCount: 0,
      upCount: 0,
      downCount: 0,
      downInstance: [],
    };
  },
  mounted() {
  },
  created() {
    getHealthEventSource().addEventListener(this.health, message => {
      this.loading = false;
      let data = JSON.parse(message.data);
      this.processData(data);
    })
  },
  methods: {
    processData(data) {
      this.healthData[data.instance] = data.status.status;
      this.totalCount = Object.keys(this.healthData).length;
      this.upCount = Object.values(this.healthData).filter(
        status => status === "UP"
      ).length;
      this.downCount = Object.values(this.healthData).filter(
        status => status !== "UP"
      ).length;
      this.downInstance = Object.keys(this.healthData).filter(
        instance => this.healthData[instance] !== "UP"
      );
    }
  }
};
</script>

<style scoped>
.health-monitor-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.health-monitor-title {
  color: #eeeeee;
  font-size: 3rem;
  font-weight: 600;
  line-height: 1.125;
  margin: 10px 0;
}
.health-monitor-status {
  font-size: 1.8rem;
  font-weight: 500;
  line-height: 1.125;
}
.health-monitor-detail {
  width: 100%;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
}
.health-detail-item {
  margin: 10px 30px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.health-detail-heading {
  color: #999999;
}
.health-detail-value {
  color: #eeeeee;
  font-size: 1.5rem;
}
.success {
  color: #23d160 !important;
}
.danger {
  color: #f56c6c !important;
}
</style>
