<template>
  <article class="no-enable-permission">
    <div class="content-wrapper">
      <bk-exception
        class="exception-wrap-item exception-part"
        type="403"
        scene="part"
        :class="{ 'exception-gray': isGray }"
      >
        <div>{{ title }}</div>
        <bk-button
          class="mt10"
          theme="primary"
          :disabled="!hasPermission"
          :loading="isOpenManageLoading"
          @click="openManage"
        >{{ '开启权限管理' }}</bk-button>
      </bk-exception>
    </div>
  </article>
</template>

<script>
import ajax from '../../../ajax/index';

export default {
  props: {
    hasPermission: {
      type: Boolean,
    },
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
    ajaxPrefix: {
      type: String,
      default: '',
    },
  },

  emits: ['open-manage'],

  data() {
    return {
      isOpenManageLoading: false,
    };
  },

  computed: {
    title() {
      const titleMap = {
        pipeline: this.$t('尚未开启此流水线权限管理功能'),
        project: this.$t('尚未开启此项目权限管理功能'),
        pipeline_group: this.$t('尚未开启此流水线组权限管理功能'),
      };
      return titleMap[this.resourceType];
    },
  },

  methods: {
    openManage() {
      this.isOpenManageLoading = true;
      return ajax
        .put(`${this.ajaxPrefix}/auth/api/user/auth/resource/${this.projectCode}/${this.resourceType}/${this.resourceCode}/enable`)
        .then(() => {
          this.$emit('open-manage');
        })
        .finally(() => {
          this.isOpenManageLoading = false;
        });
    },
  },
};
</script>

<style lang="scss" scoped>
.no-enable-permission {
    height: 100%;
}
.content-wrapper {
    display: flex;
    align-items: center;
    flex-direction: column;
    padding-top: 10%;
    width: 100%;
    height: 100%;
    background-color: #fff;
    box-shadow: 0 2px 2px 0 rgba(0,0,0,0.15);
    text-align: center;
    font-size: 14px;
}
.mt10 {
  margin-top: 10px;
}
</style>
