<script setup lang="ts">
import {
  ref,
  watch,
} from 'vue';
import { Error } from 'bkui-vue/lib/icon'
const emits = defineEmits(['hidden-detail']);

const props = defineProps({
  groupInfo: Object,
  isShow: Boolean,
});
const showDetail = ref(false);

watch(() => props.isShow, () => {
  showDetail.value = props.isShow;
}, { 
  immediate: true,
})

const handleHidden = () => {
  emits('hidden-detail', false);
}

</script>

<template>
  <bk-sideslider
    v-model:isShow="showDetail"
    :width="1250"
    quick-close
    @hidden="handleHidden"
  >
    <template #header>
      <div>
        <p class="group-name">用户组【流水线-后台发布-拥有者】的详情</p>
        <p class="group-id">ID: 112</p>
      </div>
    </template>
    <template #default>
      <div class="detail-content">
        todo..
        {{ props.isShowed }}
      </div>
    </template>
  </bk-sideslider>
</template>

<style lang="postcss" scoped>
  :deep(.bk-sideslider-title) {
    line-height: normal !important;
  }
  .group-name {
    padding-top: 8px;
    font-size: 16px;
    color: #313238;
  }
  .group-id {
    padding-top: 3px;
    font-size: 12px;
    color: #979BA5;
  }
  .detail-content {
    padding: 20px;
  }
</style>
