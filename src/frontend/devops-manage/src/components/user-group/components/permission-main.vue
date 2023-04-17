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
      <template v-else>
        <no-permission
          v-if="isApprover"
          :resource-type="resourceType"
          :resource-code="resourceCode"
          :project-code="projectCode"
          :ajax-prefix="ajaxPrefix"
          :error-code="errorCode"
        />
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
      isApprover: false,
      errorCode: 0
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
        .catch(err => {
          if ([404, 403, 2119042].includes(err.code)) {
              this.isApprover = true;
              this.errorCode =err.code
          }
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
    width: 100%;
    height: 100%;
}
</style>
