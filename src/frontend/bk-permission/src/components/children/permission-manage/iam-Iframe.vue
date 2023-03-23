<template>
  <iframe
    width="100%"
    height="100%"
    :frameborder="0"
    :src="iframeUrl"
  ></iframe>
</template>

<script>
export default {
  props: {
    path: {
      type: String,
    },
    query: {
      type: Object,
    },
  },
  data() {
    return {
      iframeUrl: '',
      commonQuery: {
        system_id: 'bk_ci_rbac',
        source: 'externalApp',
      },
    };
  },
  watch: {
    path: {
      handler() {
        this.calcUrl();
      },
      immediate: true,
      deep: true,
    },
    query: {
      handler() {
        this.calcUrl();
      },
      immediate: true,
      deep: true,
    },
  },
  methods: {
    calcUrl() {
      // 构造 url
      const url = new URL(`${window.BK_IAM_URL_PREFIX}/${this.path}`);
      const query = {
        ...this.commonQuery,
        ...this.query,
      };
      Object.keys(query).forEach((key) => {
        url.searchParams.append(key, query[key]);
      });
      this.iframeUrl = url.href;
    },
  },
};
</script>
