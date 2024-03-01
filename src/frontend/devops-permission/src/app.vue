<template>
  <router-view></router-view>
</template>

<script lang="ts">
try {
  const devopsApp = window.top?.document.querySelector('.devops-app');
  devopsApp?.setAttribute('class', 'devops-app permission-model');
} catch (error) {
  console.log(error);
}
</script>
