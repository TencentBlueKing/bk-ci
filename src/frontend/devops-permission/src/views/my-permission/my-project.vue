<script lang="ts" setup>
import { useRoute } from 'vue-router';
import tools from '@/utils/tools';
const urlPerfix = location.origin
const baseUrl = `${urlPerfix}/manage/userManage`
const route = useRoute();
const projectCode = route?.params.projectCode || route?.query.projectCode || route?.query.project_code || tools.getCookie('X-DEVOPS-PROJECT-ID') || '';
const iframeUrl = `${baseUrl}?projectCode=${projectCode}&disableSyncUrl=true&showSelectProject=true`
</script>

<template>
  <iframe
    :src="iframeUrl"
    frameborder="0"
  />
</template>
