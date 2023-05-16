<script setup lang="ts">
import {
  computed,
  onMounted,
  onUnmounted,
} from 'vue';
import {
  useRoute,
  useRouter,
} from 'vue-router';

const route = useRoute();
const router = useRouter();
const projectId = computed(() => route.params.projectId)
// 设置 rem
const calcRem = () => {
  const doc = window.document;
  const docEl = doc.documentElement;
  const designWidth = 1580; // 默认设计图宽度
  const maxRate = 2560 / designWidth;
  const minRate = 1280 / designWidth;
  const clientWidth = docEl.getBoundingClientRect().width || window.innerWidth;
  const flexibleRem = Math.max(Math.min(clientWidth / designWidth, maxRate), minRate) * 100;
  docEl.style.fontSize = `${flexibleRem}px`;
};

onMounted(() => {
  calcRem();
  window.addEventListener('resize', calcRem, false);
  window.globalVue.$on('change::$currentProjectId', data => { // 蓝盾选择项目时切换
    if (projectId.value !== data.currentProjectId) {
      const params = Object.assign({}, route.params, { projectId: data.currentProjectId })
      router.replace({
          name: route.name,
          params
      })
    }
  })
});

onUnmounted(() => {
  window.removeEventListener('resize', calcRem, false);
});
</script>

<template>
  <router-view></router-view>
</template>
