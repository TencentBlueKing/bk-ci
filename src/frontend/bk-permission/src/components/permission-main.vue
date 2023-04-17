<template>
  <article
    class="permission-wrapper"
    v-bkloading="{ isLoading }"
  >
    <template v-if="!isLoading">
      <template v-if="isEnablePermission">
        <permission-manage
          v-if="hasPermission"
          :show-create-group="showCreateGroup"
          :resource-type="resourceType"
          :resource-code="resourceCode"
          :resource-name="resourceName"
          :project-code="projectCode"
          :ajax-prefix="ajaxPrefix"
          @close-manage="initStatus"
        />
        <no-permission
          v-else
          :resource-type="resourceType"
          :resource-code="resourceCode"
          :project-code="projectCode"
          :ajax-prefix="ajaxPrefix"
        />
      </template>
      <no-enable-permission
        v-else
        :resource-type="resourceType"
        :resource-code="resourceCode"
        :project-code="projectCode"
        :ajax-prefix="ajaxPrefix"
        :has-permission="hasPermission"
        @open-manage="initStatus"
      />
    </template>
  </article>
</template>

<script>
import NoEnablePermission from './children/no-enable-permission/no-enable-permission.vue';
import NoPermission from './children/no-permission/no-permission.vue';
import PermissionManage from './children/permission-manage/permission-manage.vue';
import ajax from '../ajax/index';

export default {
  components: {
    NoEnablePermission,
    NoPermission,
    PermissionManage,
  },

  props: {
    resourceType: {
      type: String,
      default: '',
    },
    resourceCode: {
      type: String,
      default: '',
    },
    projectCode: {
      type: String,
      default: '',
    },
    resourceName: {
      type: String,
      default: '',
    },
    showCreateGroup: {
      type: Boolean,
      default: true,
    },
    ajaxPrefix: {
      type: String,
      default: '',
    },
  },

  data() {
    return {
      isEnablePermission: false,
      hasPermission: false,
      isLoading: true,
    };
  },

  created() {
    this.initStatus();
  },

  methods: {
    initStatus() {
      const commonPerfix = `${this.ajaxPrefix}/auth/api/user/auth/resource`;
      Promise
        .all([
          ajax.get(`${commonPerfix}/${this.projectCode}/${this.resourceType}/${this.resourceCode}/hasManagerPermission`),
          ajax.get(`${commonPerfix}/${this.projectCode}/${this.resourceType}/${this.resourceCode}/isEnablePermission`),
        ])
        .then(([hasManagerData, isEnableData]) => {
          this.isEnablePermission = isEnableData?.data;
          this.hasPermission = hasManagerData?.data;
        })
        .finally(() => {
          this.isLoading = false;
        });
    },
  },
};
</script>

<style lang="scss" scoped>
.permission-wrapper {
    overflow: auto;
    width: 100%;
    height: 100%;
}
</style>
