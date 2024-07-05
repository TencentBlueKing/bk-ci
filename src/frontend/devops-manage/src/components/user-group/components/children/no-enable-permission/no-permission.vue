<template>
  <bk-exception
    type="empty"
    scene="part"
    :description="t('该用户暂无项目权限')"
  >
    <p class="empty-text">
      <i18n-t keypath="由于该用户仍有部分授权未移交，未能自动移出项目；如有需要，可前往「授权管理」处理" tag="div">
        <bk-button text theme="primary" @click="goPermission">
          {{ t("授权管理") }}
        </bk-button>
      </i18n-t>
    </p>
  </bk-exception>
</template>

<script setup>
import { useRouter } from 'vue-router';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';

const { t } = useI18n();

const router = useRouter();

function goPermission() {
  router.push({
    path: 'permission',
    // params:
  });
}
</script>

<style lang="scss" scoped>
::v-deep .bk-exception-part .bk-exception-img {
  width: 220px;
}
.empty-text {
  color: #979ba5;
  font-size: 12px;
  line-height: 20px;
}
</style>
