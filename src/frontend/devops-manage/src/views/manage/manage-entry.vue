<script setup lang="ts">
import ManageHeader from '@/components/manage-header.vue';
import { onMounted, ref, watch, computed } from 'vue';
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
    title: t('项目设置'),
    name: 'show',
  },
  {
    title: t('用户管理'),
    name: 'group',
  },
  {
    title: t('授权管理'),
    name: 'permission',
  },
  // {
  //   title: t('微扩展管理'),
  //   name: 'expand',
  // },
]);

const routeName = computed(()=>route.name);
const { projectCode } = route.params;
const projectList = window.parent?.vuexStore?.state?.projectList;
const projectName = projectList?.find(project => project.projectCode === projectCode)?.projectName || projectCode;
const activeTab = ref(t('项目设置'));
const handleChangeTab = (manageTab: any) => {
  activeTab.value = manageTab.title;
  router.push({
    name: manageTab.name,
  });
};
const initActiveTab = () => {
  const tab = manageTabs.value.find(tab => tab.name === routeName.value);
  activeTab.value = tab?.title || t('项目设置');
};
onMounted(() => {
  initActiveTab();
});

watch(route, initActiveTab, { immediate: true });

</script>

<template>
  <manage-header
    :name="projectName"
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
  padding-right: 100px;
}
.manage-tab {
  display: inline-block;
  position: relative;
  min-width: 130px;
  height: 60px;
  line-height: 60px;
  text-align: center;
  padding: 0 10px;
  cursor: pointer;
  &.active {
    background-color: #E1ECFF;
    color: #3A84FF;
    &:before {
      position: absolute;
      top: 0;
      left: 0;
      content: '';
      width: 100%;
      height: 4px;
      background-color: #3A84FF;
    }
  }
}
.manage-main {
  /* margin: 16px 24px 24px; */
  height: 100%;
  /* background-color: #fff; */
  /* box-shadow: 0 2px 2px 0 rgb(0 0 0 / 15%); */
}
</style>
