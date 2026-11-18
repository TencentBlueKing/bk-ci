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
  watch: {
    html() {
      this.$nextTick(() => this.patchLinks())
    },
  },
  mounted() {
    this.patchLinks()
  },
  updated() {
    this.patchLinks()
  },
  methods: {
    patchLinks() {
      const root = this.$el
      if (!root || !root.querySelectorAll) return
      root.querySelectorAll('a[href]').forEach((link) => {
        link.setAttribute('target', '_blank')
        link.setAttribute('rel', 'noopener noreferrer')
      })
    },
  },
}
</script>

<style scoped>
.convention-html {
  color: #4d4f56;
  font-size: 14px;
  font-weight: 400;
  line-height: 22px;
}
.convention-html :deep(p) {
  margin: 0 0 20px;
}
.convention-html :deep(h3) {
  margin: 0 0 12px;
  color: #313238 !important;
  font-size: 16px !important;
  font-weight: 700 !important;
  line-height: 24px !important;
  background: transparent;
}
.convention-html :deep(h3 + p) {
  margin-bottom: 24px;
}
.convention-html :deep(p:last-child) {
  margin-bottom: 0;
}
.convention-html :deep(strong) {
  font-weight: 700;
}
.convention-html :deep(a) {
  color: #1768ef;
  text-decoration: underline;
  text-underline-offset: 2px;
  cursor: pointer;
}
</style>
