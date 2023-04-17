<template>
  <permission-main
    class="permission-main"
    :resource-type="resourceType"
    :resource-code="projectCode"
    :project-code="projectCode"
    :rename-group-name="handelRenameGroupName"
  />
</template>

<script lang="ts">
import http from '@/http/api';
import PermissionMain from '@/components/user-group/components/permission-main.vue';
import { Message } from 'bkui-vue';

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
      return this.$route.params.projectCode;
    },
  },

  methods: {
    handelRenameGroupName(params: any) {
      const {
        resourceType,
        projectCode,
      } = this;

      const {
        groupName,
        groupId,
      } = params; 
      return http
        .renameGroupName({
          resourceType,
          projectCode,
          groupName,
          groupId,
        })
        .then(() => {
          Message({
            theme: 'success',
            message: this.$t('修改成功'),
          })
        })
    }
  },
};
</script>

<style lang="postcss" scoped>
  .permission-main {
    padding: 24px;
  }
</style>
