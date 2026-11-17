<template>
  <div
    class="convention-html"
    v-bk-xss-html="html"
  />
</template>

<script>
export default {
  name: 'ConventionHtml',
  props: {
    html: {
      type: String,
      default: '',
    },
  },
}
</script>

<style scoped>
.convention-html {
  color: #313238;
  font-size: 14px;
  line-height: 24px;
}
.convention-html :deep(p) {
  margin: 0 0 16px;
}
.convention-html :deep(p:last-child) {
  margin-bottom: 0;
}
.convention-html :deep(strong) {
  font-weight: 600;
}
.convention-html :deep(a) {
  color: #3a84ff;
  white-space: nowrap;
  text-decoration: none;
  cursor: pointer;
}
.convention-html :deep(a:hover) {
  color: #699df4;
}
</style>
