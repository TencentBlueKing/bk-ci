<template>
  <div>
    <bk-form-item
      :label="t('制品质量元数据')"
      property="metadata"
      :description="t('当制品标记了此配置中的元数据时，在构建历史，构建详情总结界面，将展示对应的质量标签，便于用户快速了解构建产出的制品质量。')"
    >
      <p
        class="metadata-add"
        @click="showMetadataAdd"
      >
        <i class="manage-icon manage-icon-add-fill"></i>
        <span>{{ t('添加') }}</span>
      </p>
      <bk-table
        :border="['outer']"
        :max-height="495"
        :data="metadataList"
        show-overflow-tooltip
        :pagination="pipelinePagination"
        remote-pagination
        empty-cell-text="--"
        showOverflowTooltip
        @page-limit-change="pageLimitChange"
        @page-value-change="pageValueChange"
      >
        <bk-table-column
          :label="t('元数据键')"
          prop="key"
          :width="200"
          showOverflowTooltip
        >
          <template #default="{row}">
            <div class="key-layout">
              <p>
                <span class="tag">{{ t('内置') }}</span>
              </p>
              <div class="key-group">
                <p text>{{ row.key }}</p>
                <p class="name">{{ row.name }}</p>
              </div>
            </div>
          </template>
        </bk-table-column>
        <bk-table-column
          :label="t('枚举值')"
          prop="enums"
        >
          <template #default="{row}">
            <div v-for="item in row.enums">
              <p>
                <span :style="{backgroundColor: item.color}" class="enums-icon"></span>
                <span>{{ item.value }}</span>
              </p>
            </div>
          </template>
        </bk-table-column>
        <bk-table-column
          :label="t('分组')"
          prop="group"
        >
          <template #default="{row}">
            <span v-if="row.group" class="tag">{{ row.group}}</span>
            <span v-else>--</span>
          </template>
        </bk-table-column>
        <bk-table-column
          :label="t('描述')"
          prop="describe"
        />
        <bk-table-column
          :label="t('启用')"
          prop="isEnable"
        >
          <template #default="{row}">
            <bk-switcher
              v-model="row.isEnable"
              theme="primary"
            />
          </template>
        </bk-table-column>
        <bk-table-column
          :width="120"
          :label="t('操作')"
        >
          <template #default="{row}">
            <bk-button class="ml10" text theme="primary" @click="handleEdit(row)"> {{t('编辑')}} </bk-button>
            <bk-button class="ml10" text theme="primary" @click="handleDelete(row)"> {{t('删除')}} </bk-button>
          </template>
        </bk-table-column>
      </bk-table>
    </bk-form-item>
  </div>

  <bk-sideslider
    v-model:isShow="isShowMetadataAdd"
    :title="t('添加元数据')"
    renderDirective="if"
    :width="640"
  >
  <template #default>
    <bk-form
      ref="metadataFormRef"
      :model="metadataForm"
      :rules="metadataRules"
      form-type="vertical"
      class="metadata-slider"
    >
      <bk-form-item
        :label="t('元数据键')"
        property="key"
        required
      >
        <bk-input
          v-model="metadataForm.key"
          :placeholder="t('由字母、数字和下划线组成，且以英文字母开头')"
          clearable
        />
      </bk-form-item>
      <bk-form-item
        :label="t('元数据别名')"
        property="name"
      >
        <bk-input
          v-model="metadataForm.name"
          placeholder="请输入"
          clearable
        />
      </bk-form-item>
      <bk-form-item
        :label="t('元数据分组')"
        property="group"
      >
        <bk-select v-model="metadataForm.group">
          <bk-option
            label="质量"
            value="1"
          />
        </bk-select>
      </bk-form-item>
      <bk-form-item
        :label="t('元数据类型')"
        property="type"
        required
      >
        <bk-radio-group v-model="metadataForm.type">
          <bk-radio label="枚举值" />
          <bk-radio label="字符串" :disabled="metadataForm.group === '1'" />
        </bk-radio-group>
      </bk-form-item>
      <bk-form-item
        v-if="metadataForm.type === '枚举值'"
        :label="t('枚举值（单选）')"
        property="enums"
        required
      >
        <div>
          <div
            v-for="(item, index) in metadataForm.enums"
            :key="index"
            class="enums-list"
          >
            <bk-color-picker
              :recommend="false"
              v-model="item.color"
            >
              <template #trigger>
                <div :style="{backgroundColor: item.color}" class="color-picker" ></div>
              </template>
            </bk-color-picker>
            <bk-input
              v-model="item.value"
            />
            <Del width="15" height="15" fill="#979BA5" @click="removeEnumValue(index)" />
          </div>
          <p
            @click="addEnumValue"
            class="metadata-add"
          >
            <i class="manage-icon manage-icon-add-fill"></i>
            <span>{{ t('添加枚举值') }}</span>
          </p>
        </div>
      </bk-form-item>
      <bk-form-item
        :label="t('描述')"
        property="describe"
      >
        <bk-input
          v-model="metadataForm.describe"
          placeholder="请输入"
          :rows="3"
          :maxlength="100"
          type="textarea"
        />
      </bk-form-item>

      
    </bk-form>
    <div class="btn-group">
      <bk-button theme="primary" @click="handleMetadataAdd"> {{t('添加')}} </bk-button>
      <bk-button> {{t('取消')}} </bk-button>
    </div>
  </template>
  </bk-sideslider>
</template>

<script setup name="MetadataFormItem">
import http from '@/http/api';
import { ref } from 'vue';
import { useI18n } from 'vue-i18n';
import { Del } from 'bkui-vue/lib/icon';

const { t } = useI18n();
const props = defineProps({
  data: {
    type: Object,
    required: true
  },
  type: String
});
const emits = defineEmits(['handleChangeForm', 'beforeChange']);

const projectData = ref(props.data);
const isShowMetadataAdd = ref(false);
const metadataList = ref([{
  key: '1FSDDDXVDFSVD_DJJ_K1',
  name: 'sdf',
  group: '质量',
  type: '枚举值',
  isEnable: true,
  enums:[
    { color: '#EAA7A7', value: 'Signed' },
    { color: '#FFFFFF', value: 'Medium' },
    { color: '#E61717', value: '123' }
  ],
  describe: '一段描述'
},{
  key: 'FSCEFGV',
  name: 'sdf',
  group: '',
  type: '枚举值',
  isEnable: true,
  enums:[
    { color: '#FFFFFF', value: 'Signed' },
    { color: '#E61717', value: 'gddfbcv' }
  ],
  describe: ''
}]);
const pipelinePagination = ref({ count: 0, limit: 20, current: 1 });
const metadataFormRef = ref();
const metadataForm = ref({
  key: '',
  name: '',
  group: '',
  type: '枚举值',
  enums: [
    { color: '#FFFFFF', value: '' },
  ],
  describe: ''
})
const metadataRules = {
  name: [
    {
      validator: (value) => /^[a-zA-Z][a-zA-Z0-9_]*$/.test(value),
      message: t('由字母、数字和下划线组成，且以英文字母开头'),
      trigger: 'blur'
    }
  ],
};

const addEnumValue = () => {
  metadataForm.value.enums.push({ color: '#FFFFFF', value: '' });
};

const removeEnumValue = (index) => {
  metadataForm.value.enums.splice(index, 1);
};

async function fetchListViewPipelines () {
  try {
    isLoading.value = true;
    const params = {
      page: pipelinePagination.value.current,
      pageSize: pipelinePagination.value.limit,
    };
    const res = await http.listInheritedDialectPipelines(projectId.value, params);
    pipelineList.value = res.records;
    pipelinePagination.value.count = res.count;
  } catch (error) {
    console.log(error);
  } finally {
    isLoading.value = false;
  }
}

function pageLimitChange (limit) {
  pipelinePagination.value.limit = limit;
  fetchListViewPipelines();
}

function pageValueChange (value) {
  pipelinePagination.value.current = value;
  fetchListViewPipelines();
}

function showMetadataAdd () {
  isShowMetadataAdd.value = true;
}

function handleMetadataAdd () {
  console.log(metadataForm.value, '表单数据');
  metadataList.value.push(metadataForm.value)
}

function handleEdit (row) {
  isShowMetadataAdd.value = true;
  metadataForm.value = row;
}

function handleDelete (row) {

}

</script>

<style lang="scss" scoped>
.metadata-add {
  font-size: 12px;
  color: #3A84FF;
  cursor: pointer;
}
.key-layout {
  display: flex;
  align-items: center;

  .key-group {
    flex: 1;
    margin-left: 6px;
    font-size: 12px;
    color: #4D4F56;
    font-size: 12px;

    p {
      line-height: 16px;
      max-width: 168px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .name {
      color: #979BA5;
    }
  }
}
.tag {
  padding: 5px 6px;
  background: #F0F1F5;
  border-radius: 2px;
}
.enums-icon {
  display: inline-block;
  width: 8px;
  height: 8px;
  margin-right: 8px;
  border: 1px solid #C4C6CC;
  border-radius: 50%;
}
.metadata-slider {
  margin: 16px 24px;
  height: calc(100% - 38px);
}
.btn-group {
  margin: 32px 24px 0;
  position: sticky;
  bottom: 0;
  background-color: #fff;
}
.enums-list {
  display: flex;
  grid-gap: 8px;
  margin-bottom: 12px;

  .bk-input {
    flex: 1;
  }
}
.color-picker {
  width: 32px;
  height: 32px;
  border: 1px solid #C4C6CC;
  border-radius: 2px;
}
.bk-color-picker.bk-color-picker-show-value {
  border: none;
  width: 32px;
}
</style>