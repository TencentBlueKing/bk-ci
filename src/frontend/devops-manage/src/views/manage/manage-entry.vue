<script setup lang="ts">
import ManageHeader from '@/components/manage-header.vue';
import { onMounted, ref } from 'vue';
import { useI18n } from 'vue-i18n';
import {
  useRouter,
  useRoute,
} from 'vue-router';
const router = useRouter();
const route = useRoute();
const { t } = useI18n();
const props = defineProps({
  projectCode: String,
});


const manageTabs = ref([
  {
    title: t('项目信息'),
    name: 'show',
  },
  {
    title: t('用户管理'),
    name: 'group',
  },
  {
    title: t('微扩展管理'),
    name: 'expand',
  },
]);

const routeName = route.name;
const { projectCode } = route.params;
const activeTab = ref(t('项目信息'));
const handleChangeTab = (manageTab: any) => {
  activeTab.value = manageTab.title;
  router.push({
    name: manageTab.name,
  });
};
const initActiveTab = () => {
  const tab = manageTabs.value.find(tab => tab.name === routeName);
  activeTab.value = tab?.title || t('项目信息');
};
onMounted(() => {
  initActiveTab();
});
</script>

<template>
  <manage-header
    :name="projectCode"
  >
    <span class="manage-tabs">
      <span
        v-for="manageTab in manageTabs"
        :key="manageTab.name"
        :class="{
          'manage-tab': true,
          active: activeTab === manageTab.title
        }"
        @click="handleChangeTab(manageTab)"
      >
        {{ manageTab.title }}
      </span>
    </span>
  </manage-header>
  <router-view class="manage-main"></router-view>
</template>

<style lang="postcss" scoped>
.manage-tabs {
  font-size: 14px;
  flex: 1;
  text-align: center;
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
.manage-main {
  /* margin: 16px 24px 24px; */
  width: calc(100% - 48px);
  height: 100%;
  /* background-color: #fff; */
  /* box-shadow: 0 2px 2px 0 rgb(0 0 0 / 15%); */
}
</style>
