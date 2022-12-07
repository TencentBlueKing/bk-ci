<script setup lang="ts">
import {
  ref,
  onBeforeUnmount,
  computed,
  onMounted,
} from 'vue';
import {
  EditLine,
} from 'bkui-vue/lib/icon';
import IAMIframe from './IAM-Iframe';
import { useI18n } from 'vue-i18n';
import { Message } from 'bkui-vue';
import http from '@/http/api';

const {
  t,
} = useI18n();

const emits = defineEmits(['change']);

const props = defineProps({
  data: Object,
  type: String,
  isChange: Boolean,
});

const logoFiles = computed(() => {
  const { logoAddr } = projectData.value;
  const files = [];
  if (logoAddr) {
    files.push({
      url: logoAddr,
    });
  }
  return files;
});

const projectData = ref<any>(props.data);

const deptLoading = ref({
  bg: false,
  dept: false,
  center: false,
});

const curDepartmentInfo = ref({
  bg: [],
  dept: [],
  center: [],
});

const showDialog = ref(false);

const getDepartment = async (type: string, id: any) => {
  deptLoading.value[type] = true;
  try {
    const res = await http.getOrganizations({
      type,
      id,
    });
    curDepartmentInfo.value[type] = [...res];
  } catch (err: any) {
    Message({
      message: err.message || err,
      theme: 'danger',
    });
    curDepartmentInfo.value[type] = [];
  } finally {
    deptLoading.value[type] = false;
  }
};

/**
 * 根据 type id 设置组织名称
 * @param {*} type 类型
 * @param {*} id 类型ID
 */
const setOrgName = (type: string, id: any) => {
  const item = curDepartmentInfo.value[type].find((item: { id: any; }) => item.id === id);
  if (item) {
    projectData.value[`${type}Name`] = item.name;
  }
};
const handleChangeBg = (type: string, id: any) => {
  handleChangeForm();
  projectData.value.deptId = '';
  projectData.value.centerId = '';
  curDepartmentInfo.value.dept = [];
  curDepartmentInfo.value.center = [];
  if (id) {
    setOrgName(type, id);
    getDepartment('dept', id);
  }
};
const handleChangeDept = (type: string, id: any) => {
  handleChangeForm();
  projectData.value.centerId = '';
  curDepartmentInfo.value.center = [];
  if (id) {
    setOrgName(type, id);
    getDepartment('center', id);
  }
};

const handleChangeCenter = (type: string, id: any) => {
  handleChangeForm();
  if (id) {
    setOrgName(type, id);
  };
};

const fetchDepartmentList = () => {
  const { bgId, deptId } = projectData.value;
  getDepartment('bg', 0);
  if (bgId) {
    getDepartment('dept', bgId);
  }
  if (deptId) {
    getDepartment('center', deptId);
  }
};

const handleChangeForm = () => {
  emits('change', true);
};

const handleUploadLogo = async (res: any) => {
  handleChangeForm();
  const { file } = res;
  if (file) {
    if (!(file.type === 'image/jpeg' || file.type === 'image/png')) {
      Message({
        theme: 'danger',
        message: t('只允许上传png、jpg'),
      });
    } else if (file.size > (2 * 1024 * 1024)) {
      Message({
        theme: 'danger',
        message: t('大小不超过2M'),
      });
    } else {
      const reader = new FileReader();
      reader.readAsDataURL(file);
      reader.onload = () => {
        projectData.value.logoAddr = reader.result;
        const formData = new FormData();
        formData.append('file', file);
        projectData.value.logo = formData;
        // const config = {
        //   headers: {
        //     'Content-Type': 'multipart/form-data',
        //   },
        // };
        // http.changeProjectLogo({
        //   englishName: projectData.value.englishName,
        //   formData,
        //   config,
        // });
      };
    }
  }
};

const handleMessage = (event: any) => {
  const { data } = event;
  if (data.type === 'IAM') {
    switch (data.code) {
      case 'success':
        projectData.value.subjectScopes = [
          ...data.data.departments,
          ...data.data.users,
        ].map(item => ({
          id: item.id,
          type: item.type,
          name: item.name,
        }));
        break;
      case 'cancel':
        showDialog.value = false;
        break;
    }
  }
};

onMounted(() => {
  fetchDepartmentList();
  window.addEventListener('message', handleMessage);
});

onBeforeUnmount(() => {
  window.removeEventListener('message', handleMessage);
});
</script>

<template>
  <bk-form :label-width="160">
    <bk-form-item :label="t('项目名称')" :required="true">
      <bk-input
        v-model="projectData.projectName"
        :placeholder="t('请输入1-32字符的项目名称')"
        @change="handleChangeForm"
      ></bk-input>
    </bk-form-item>
    <bk-form-item :label="t('项目ID')" :required="true">
      <bk-input
        v-model="projectData.englishName"
        :disabled="type === 'edit'"
        :placeholder="t('请输入2-32 字符的项目ID，由小写字母、数字、中划线组成，以小写字母开头。提交后不可修改。')"
      ></bk-input>
    </bk-form-item>
    <bk-form-item :label="t('项目描述')" :required="true">
      <bk-input
        v-model="projectData.description"
        class="textarea"
        type="textarea"
        :rows="3"
        :maxlength="255"
        :placeholder="t('请输入项目描述')"
        @change="handleChangeForm"
      ></bk-input>
    </bk-form-item>
    <bk-form-item :label="t('项目LOGO')">
      <bk-upload
        theme="picture"
        :files="logoFiles"
        with-credentials
        :multiple="false"
        :custom-request="handleUploadLogo"
      />
      <span class="logo-upload-tip">{{ t('只允许上传png、jpg，大小不超过 2M')}}</span>
    </bk-form-item>
    <bk-form-item :label="t('项目所属组织')" :required="true">
      <div class="bk-dropdown-box">
        <bk-select
          v-model="projectData.bgId"
          placeholder="BG"
          name="bg"
          :loading="deptLoading.bg"
          filterable
          @change="id => handleChangeBg('bg', id)"
        >
          <bk-option
            v-for="bg in curDepartmentInfo.bg"
            :value="bg.id"
            :key="bg.id"
            :label="bg.name"
          />
        </bk-select>
      </div>
      <div class="bk-dropdown-box">
        <bk-select
          v-model="projectData.deptId"
          :placeholder="t('部门')"
          name="dept"
          :loading="deptLoading.dept"
          filterable
          @change="id => handleChangeDept('dept', id)"
        >
          <bk-option
            v-for="bg in curDepartmentInfo.dept"
            :value="bg.id"
            :key="bg.id"
            :label="bg.name"
          />
        </bk-select>
      </div>
      <div class="bk-dropdown-box">
        <bk-select
          v-model="projectData.centerId"
          :placeholder="t('中心')"
          name="center"
          :loading="deptLoading.center"
          filterable
          @change="id => handleChangeCenter('center', id)"
        >
          <bk-option
            v-for="center in curDepartmentInfo.center"
            :value="center.id"
            :key="center.id"
            :label="center.name"
          />
        </bk-select>
      </div>
    </bk-form-item>
    <bk-form-item :label="t('项目性质')" :required="true">
      <bk-radio-group
        v-model="projectData.authSecrecy"
        @change="handleChangeForm"
      >
        <bk-radio :label="false">{{ t('私有项目') }}</bk-radio>
        <bk-radio :label="true">{{ t('保密项目') }}</bk-radio>
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
  .bk-dropdown-box {
    width: 200px;
    margin-right: 12px;
    display: inline-block;
    vertical-align: middle;
  }
</style>
