<script lang="ts" setup>
import { useRoute } from 'vue-router';
const urlPerfix = location.origin
const baseUrl = `${urlPerfix}/manage/userManage`
const route = useRoute();
const projectCode = route?.params.projectCode || route?.query.projectCode || route?.query.project_code
const iframeUrl = `${baseUrl}?projectCode=${projectCode}&disableSyncUrl=true`
</script>

<template>
  <iframe
    :src="iframeUrl"
    frameborder="0"
  />
</template>
