<template>
  <div>
    <bk-form-item
      :label="t('项目最大可授权人员范围')"
      :description="t('该设置表示可以加入项目的成员的最大范围，范围内的用户才可以成功加入项目下的任意用户组')"
      property="subjectScopes"
      :required="true">
      <bk-tag
        v-for="(subjectScope, index) in projectData.subjectScopes"
        :key="index"
      >
        {{ subjectScope.id === '*' ? t('全员') : subjectScope.name }}
      </bk-tag>
      <EditLine
        class="edit-line ml5"
        @click="showMemberDialog"
      />
    </bk-form-item>

    <bk-dialog
      :title="t('设置项目最大可授权人员范围')"
      width="900"
      size="large"
      dialog-type="show"
      :is-show="showDialog"
      @closed="() => showDialog = false"
    >
      <IAMIframe
        ref="iframeRef"
        class="member-iframe"
        path="add-member-boundary"
        :query="{
          search_sence: 'add'
        }"
    />
  </bk-dialog>
  </div>
</template>

<script setup name="PermissionContent">
import { useI18n } from 'vue-i18n';
import { EditLine } from 'bkui-vue/lib/icon';
import IAMIframe from '@/components/IAM-Iframe';
import { ref, getCurrentInstance, onMounted, onBeforeUnmount } from 'vue';

const { t } = useI18n();
const props = defineProps({
  data: {
    type: Object,
    required: true
  }
});
const emits = defineEmits(['handleChangeForm']);

const iframeRef = ref(null);
const showDialog = ref(false);
const projectData = ref(props.data);
const vm = getCurrentInstance();

function showMemberDialog (){
  showDialog.value = true;
}
function handleMessage (event) {
  const { data } = event;
  if (data.type === 'IAM') {
    switch (data.code) {
      case 'success':
        emits('handleChangeForm')
        projectData.value.subjectScopes = data.data.subject_scopes;
        showDialog.value = false;
        break;
      case 'cancel':
        showDialog.value = false;
        break;
      case 'load':
        setTimeout(() => {
          // 回显数据
          vm?.refs?.iframeRef?.$el?.firstElementChild?.contentWindow?.postMessage?.(
            JSON.parse(JSON.stringify({
              subject_scopes: projectData.value.subjectScopes,
            })),
            window.BK_IAM_URL_PREFIX,
          );
        }, 0);
        break;
    }
  }
};

onMounted(async () => {
  window.addEventListener('message', handleMessage);
});

onBeforeUnmount(() => {
  window.removeEventListener('message', handleMessage);
});

</script>

<style lang="scss" scoped>
.member-iframe {
  height: 600px;
}
.edit-line {
  cursor: pointer;
}
</style>