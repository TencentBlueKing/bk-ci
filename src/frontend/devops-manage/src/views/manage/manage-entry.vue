<script setup lang="ts">
import ManageHeader from '@/components/manage-header.vue';
import { ref } from 'vue';

const manageTabs = ref(['项目信息', '用户组']);
const activeTab = ref('项目信息');
</script>

<template>
  <manage-header
    name="项目名称"
  >
    <span class="manage-tabs">
      <span
        v-for="manageTab in manageTabs"
        :key="manageTab"
        :class="{
          'manage-tab': true,
          active: activeTab === manageTab
        }"
        @click="activeTab = manageTab"
      >
        {{ manageTab }}
      </span>
    </span>
  </manage-header>
  <router-view></router-view>
</template>

<style lang="postcss" scoped>
.manage-tabs {
  margin-right: 50%;
  font-size: 14px;
}
.manage-tab {
  display: inline-block;
  position: relative;
  width: 116px;
  height: 60px;
  line-height: 60px;
  text-align: center;
  cursor: pointer;
  &.active {
    opacity: 0.5;
    background-color: #E1ECFF;
    color: #3A84FF;
    &:before {
      position: absolute;
      top: 0;
      left: 0;
      content: '';
      width: 116px;
      height: 4px;
      background-color: #3A84FF;
    }
  }
}
</style>
