<template>
  <div>
    <bk-form-item :label="t('项目名称')" property="projectName" :required="true">
      <bk-input
        v-model="projectData.projectName"
        :placeholder="t('请输入1-32字符的项目名称')"
        :maxlength="32"
        @change="handleChangeForm"
      ></bk-input>
      <div class="error-tips" v-if="validateProjectNameTips">
        {{ validateProjectNameTips }}
      </div>
    </bk-form-item>
    <bk-form-item :label="t('项目ID')" property="englishName" :required="true">
      <bk-input
        v-model="projectData.englishName"
        :disabled="type === 'edit'"
        :maxlength="32"
        :placeholder="t('请输入2-32 字符的项目ID，由小写字母、数字、中划线组成，以小写字母开头。提交后不可修改。')"
      ></bk-input>
      <div class="error-tips" v-if="validateEnglishNameTips">
        {{ validateEnglishNameTips }}
      </div>
    </bk-form-item>
    <bk-form-item :label="t('项目描述')" property="description" :required="true">
      <bk-input
        v-model="projectData.description"
        class="textarea"
        :rows="3"
        :maxlength="255"
        :placeholder="t('请输入项目描述')"
        @change="handleChangeForm"
        type="textarea"
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
    <bk-form-item :label="t('项目类型')" property="projectType" required>
      <bk-select
        v-model="projectData.projectType"
        :placeholder="t('选择项目类型')"
        name="center"
        searchable
        @change="handleChangeForm"
      >
        <bk-option
          v-for="type in projectTypeList"
          :value="type.id"
          :key="type.id"
          :label="type.name"
        />
      </bk-select>
    </bk-form-item>
    <bk-form-item
      v-if="isRbac"
      :label="t('项目性质')"
      property="authSecrecy"
      :required="true"
    >
      <bk-radio-group
        v-model="projectData.authSecrecy"
        @change="handleChangeForm"
      >
        <bk-radio class="mr10" :label="0">
          <Popover :content="t('`项目最大可授权人员范围`内的用户可以主动申请加入项目')">
            <span class="authSecrecy-item">{{ t('私有项目') }}</span>
          </Popover>
        </bk-radio>
        <bk-radio :label="1">
          <Popover :content="t('拥有项目/资源管理权限的成员才可以主动添加用户')">
            <span class="authSecrecy-item">{{ t('保密项目') }}</span>
          </Popover>
        </bk-radio>
      </bk-radio-group>
    </bk-form-item>
  </div>
</template>

<script setup lang="ts" name="BaseInfoContent">
import http from '@/http/api';
import { ref, computed, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { Message, Popover } from 'bkui-vue';

const { t } = useI18n();

const props = defineProps({
  data: {
    type: Object,
    required: true
  },
  type: String,
  isRbac: Boolean,
  initPipelineDialect: String
});
const emits = defineEmits(['handleChangeForm', 'clearValidate']);

const projectData = ref(props.data);
const projectTypeList = [
  {
    id: 1,
    name: t('手游'),
  },
  {
    id: 2,
    name: t('端游'),
  },
  {
    id: 3,
    name: t('页游'),
  },
  {
    id: 4,
    name: t('平台产品'),
  },
  {
    id: 5,
    name: t('支撑产品'),
  },
];
const validateProjectNameTips = ref('');
const validateEnglishNameTips = ref('');

const logoFiles = computed(() => {
  const { logoAddr } = projectData.value;
  const files: any = [];
  if (logoAddr) {
    files.push({
      url: logoAddr,
    });
  }
  return files;
});

watch(() => projectData.value.projectName, (val) => {
  if (props.type === 'apply' && val) {
    http.validateProjectName(val)
      .then(() => {
        validateProjectNameTips.value = '';
      })
      .catch(() => {
        emits('clearValidate');
        validateProjectNameTips.value = t('项目名称已存在');
      });
  } else if (!val) {
    validateProjectNameTips.value = '';
  }
}, {
  deep: true,
});

watch(() => projectData.value.englishName, (val) => {
  if (props.type === 'apply' && val && /^[a-z][a-z0-9\-]{1,32}$/.test(val)) {
    http.validateEnglishName(val)
      .then(() => {
        validateEnglishNameTips.value = '';
      })
      .catch(() => {
        emits('clearValidate');
        validateEnglishNameTips.value = t('项目ID已存在');
      });
  } else if (!val || !/^[a-z][a-z0-9\-]{1,32}$/.test(val)) {
    validateEnglishNameTips.value = '';
  }
}, {
  deep: true,
});

function handleChangeForm() {
  emits('handleChangeForm')
}

async function handleUploadLogo (res: any) {
  emits('handleChangeForm');
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
      };
    }
    const formData = new FormData();
    formData.append('logo', file);
    await http.uploadProjectLogo({
      formData,
    }).then((res) => {
      projectData.value.logoAddress = res;
    });
  }
};
</script>

<style lang="scss" scoped>
.error-tips {
  color: #ff5656;
  font-size: 12px;
  position: absolute;
  top: 26px;
}
.bk-dropdown-box {
  width: 200px;
  margin-right: 12px;
  display: inline-block;
  vertical-align: middle;
}
.logo-upload-tip {
  font-size: 12px;
  color: #979BA5;
}
  .authSecrecy-item {
    border-bottom: 1px dashed #979ba5;
  }
</style>