<template>
  <div class="container-wrapper">
    <slot>Container</slot>
  </div>
</template>

<script>
export default {};
</script>

<style>
.container-wrapper {
  width: 100%;
  display: flex;
  display: -webkit-flex; /* Safari */
  flex-direction: row;
  justify-content: center;
  flex-wrap: wrap;
  margin: 10px;
}
</style>