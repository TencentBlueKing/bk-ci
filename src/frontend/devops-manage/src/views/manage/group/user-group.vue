<script setup lang="ts">
import groupAside from './group-aside.vue';

</script>

<template>
  <article class="group-manage">
    <group-aside></group-aside>
  </article>
</template>

<style lang="postcss" scoped>
  .group-manage {
    display: flex;
    flex: 1;
  }
</style>
