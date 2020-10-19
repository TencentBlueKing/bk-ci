<template>
  <div class="panel-wrapper">
    <slot></slot>
  </div>
</template>

<script>

export default {
  components: {
  },
  data() {
      return {
      }
  }
};
</script>

<style>
.panel-wrapper {
  margin: 5px;
  background-color: rgb(51, 51, 51);
}
</style>