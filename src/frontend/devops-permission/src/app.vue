<template>
  <router-view></router-view>
</template>

<script lang="ts">
if (window.top.GLOBAL_PID && window.top.GLOBAL_PID === 'bkdevops') {
  const devopsApp = window.top.document.getElementsByClassName('devops-app')[0];
  devopsApp?.setAttribute('class', 'devops-app permission-model');
}
</script>
