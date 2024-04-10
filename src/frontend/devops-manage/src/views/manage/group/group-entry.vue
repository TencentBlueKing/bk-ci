<template>
  <permission-main
    class="permission-main"
    :resource-type="resourceType"
    :resource-code="projectCode"
    :project-code="projectCode"
  />
</template>

<script lang="ts">
import PermissionMain from '@/components/user-group/components/permission-main.vue';

export default {
  components: {
    PermissionMain,
  },

  data() {
    return {
      resourceType: 'project'
    };
  },

  computed: {
    projectCode() {
      return this.$route.params.projectCode || this.$route.query.projectCode;
    },
  },
};
</script>

<style lang="postcss" scoped>
  .permission-main {
    padding: 24px;
  }
</style>
