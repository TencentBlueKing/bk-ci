<template>
  <router-view></router-view>
</template>

<script lang="ts">
const devopsApp = window.top.document.getElementsByClassName('devops-app')[0];
devopsApp.setAttribute('class', 'devops-app permission-model');
</script>
