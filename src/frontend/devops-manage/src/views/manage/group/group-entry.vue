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
import tools from '@/utils/tools';
import { Message } from 'bkui-vue';
import { useI18n } from 'vue-i18n';

export default {
  components: {
    PermissionMain,
  },

  data() {
    const { t } = useI18n();
    return {
      resourceType: 'project',
      t,
    };
  },

  computed: {
    projectCode() {
      return this.$route.params.projectCode || this.$route.query.projectCode || tools.getCookie('X-DEVOPS-PROJECT-ID') || '';
      
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
            message: this.t('修改成功'),
          })
        })
    }
  },
};
</script>
