<template>
  <article class="group-manage">
    <div class="content-wrapper">
      <bk-exception
        class="exception-wrap-item exception-part"
        type="403"
        scene="part"
        :title="title"
      >
        <bk-button
          class="apply-btn"
          theme="primary"
          @click="handleApply"
        >
          {{ $t('去申请') }}
        </bk-button>
      </bk-exception>
    </div>
  </article>
</template>

<script>
import {
  handleProjectManageNoPermission,
} from '@/utils/permission.js'
export default {
  props: {
    title: {
      type: String,
      default: '',
    },
    projectCode: {
      type: String,
      default: '',
    },
    resourceType: {
      type: String,
      default: '',
    },
    resourceAction: {
      type: String,
      default: '',
    }
  },
  methods: {
    handleApply () {
      const { resourceAction, projectCode } = this;
      handleProjectManageNoPermission({
        // action: resourceAction,
        projectId: projectCode,
        resourceCode: projectCode,
      });
    }
  }
};
</script>

<style lang="postcss" scoped>
.group-manage {
  flex: 1;
}
.content-wrapper {
  display: flex;
  align-items: center;
  flex-direction: column;
  width: 100%;
  height: 100%;
  background-color: #fff;
  box-shadow: 0 2px 2px 0 rgba(0,0,0,0.15);
  text-align: center;
  padding: 32px 48px;
}
.apply-btn {
  margin-top: 32px;
}
:deep(.bk-exception-img) {
  height: 240px;
}
:deep(.bk-exception-title) {
  font-size: 24px;
  color: #313238;
  margin-top: 18px;
}
</style>
