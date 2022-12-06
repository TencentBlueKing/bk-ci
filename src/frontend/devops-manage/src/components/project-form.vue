<script setup lang="ts">
import {
  ref,
  onBeforeMount,
  onBeforeUnmount,
} from 'vue';
import {
  EditLine,
} from 'bkui-vue/lib/icon';
import IAMIframe from './IAM-Iframe';

import { useI18n } from 'vue-i18n';

defineProps<{
  data: object,
}>();

const {
  t,
} = useI18n();
const showDialog = ref(false);
const query = {
  role_id: 1,
};

const handleMessage = (event: any) => {
  const { data } = event;
  if (data.type === 'IAM') {
    switch (data.code) {
      case 'success':
        console.log([
          ...data.data.departments,
          ...data.data.users,
        ].map(item => ({
          id: item.id,
          type: item.type,
        })));
        break;
      case 'cancel':
        showDialog.value = false;
        break;
    }
  }
};

onBeforeMount(() => {
  window.addEventListener('message', handleMessage);
});

onBeforeUnmount(() => {
  window.removeEventListener('message', handleMessage);
});
</script>

<template>
  <bk-form :label-width="160">
    <bk-form-item :label="t('项目名称')" :required="true" :property="'name'">
      <bk-input :placeholder="t('请输入1-32字符的项目名称')"></bk-input>
    </bk-form-item>
    <bk-form-item :label="t('项目ID')" :required="true" :property="'name'">
      <bk-input :placeholder="t('请输入2-32 字符的项目ID，由小写字母、数字、中划线组成，以小写字母开头。提交后不可修改。')"></bk-input>
    </bk-form-item>
    <bk-form-item :label="t('项目描述')" :required="true" :property="'name'">
      <bk-input class="textarea" type="textarea" :rows="3" :maxlength="255" :placeholder="t('请输入项目描述')"></bk-input>
    </bk-form-item>
    <bk-form-item :label="t('项目LOGO')" :required="true" :property="'name'">
      <bk-upload
        :theme="'picture'"
        :multiple="false"
        :with-credentials="true"
        :handle-res-code="handleRes"
        :url="'https://jsonplaceholder.typicode.com/posts/'">
      </bk-upload>
      <span class="logo-upload-tip">{{ t('只允许上传png、jpg，大小不超过 2M')}}</span>
    </bk-form-item>
    <bk-form-item :label="t('项目所属组织')" :required="true" :property="'name'">
      <bk-select
        :disabled="false"
        searchable>
        <bk-option
          v-for="option in []"
          :key="option.id"
          :id="option.id"
          :name="option.name">
        </bk-option>
      </bk-select>
    </bk-form-item>
    <bk-form-item :label="t('项目性质')" :required="true" :property="'name'">
      <bk-radio-group v-model="aa">
        <bk-radio value="1">{{ t('私有项目') }}</bk-radio>
        <bk-radio value="2">{{ t('保密项目') }}</bk-radio>
      </bk-radio-group>
    </bk-form-item>
    <bk-form-item :label="t('项目最大可授权人员范围')" :required="true" :property="'name'">
      <edit-line
        class="edit-line"
        @click="(showDialog = true)"
      />
    </bk-form-item>
    <div>
      <slot></slot>
    </div>
  </bk-form>

  <bk-dialog
    title="设置项目最大可授权人员范围"
    width="1328"
    size="large"
    dialog-type="show"
    :is-show="showDialog"
    @closed="() => showDialog = false"
  >
    <IAMIframe
      class="member-iframe"
      path="add-member-boundary"
      :query="query"
    />
  </bk-dialog>
</template>

<style lang="postcss" scoped>
  .textarea {
    :deep(textarea) {
        width: auto;
    }
  }
  :deep(.bk-form-label) {
    font-size: 12px;
  }
  .logo-upload-tip {
    font-size: 12px;
    color: #979BA5;
  }
  .edit-line {
    cursor: pointer;
  }
  .member-iframe {
    height: 100%;
  }
</style>
