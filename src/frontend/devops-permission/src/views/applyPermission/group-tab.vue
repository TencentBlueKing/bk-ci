<script setup lang="ts">
import {
  ref,
} from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const props = defineProps({
  active: String,
});
const emits = defineEmits(['change-tab']);
const tabList = [
  {
    displayName: t('项目级用户组'),
    id: 'PROJECT'
  },
  {
    displayName: t('资源级用户组'),
    id: 'OTHER'
  },
];

const handleChangeTab = (id) => {
  emits('change-tab', id)
}
</script>

<template>
  <div class="group-tab">
    <div
      v-for="tab in tabList"
      :key="tab.id"
      :class="{
        'tab-item': true,
        'active': active === tab.id
      }"
      @click="handleChangeTab(tab.id)">
      {{ tab.displayName }}
    </div>
  </div>
</template>

<style lang="postcss" scoped>
  .group-tab {
    display: flex;
    margin-top: 16px;
    width: 750px;
    height: 42px;
    line-height: 42px;
    border: 1px solid #dcdee5;
    border-bottom: none;
    background-color: #fafbfd;
  }
  .tab-item {
    padding: 0 20px;
    cursor: pointer;
    border-right: 1px solid #dcdee5;
    &:hover {
      color: #3a84ff;
      background-color: #fff;
    }
    &.active {
      color: #3a84ff;
      background-color: #fff;
    }
  }
</style>
