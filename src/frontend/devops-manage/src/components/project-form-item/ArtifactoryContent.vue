<template>
  <div class="artifactory">
    <bk-form-item
      :label="t('元数据')"
      property="metadatas"
      :label-width="90"
      :description="t('当制品标记了分组为质量的元数据时，在构建历史，构建详情界面，将展示对应的质量标签，便于用户快速了解流水线产出的制品质量。')"
    >
      <p
        v-if="type !== 'show'"
        class="metadata-add"
        @click="showMetadataAdd"
      >
        <i class="manage-icon manage-icon-add-fill"></i>
        <span>{{ t('添加') }}</span>
      </p>
      <bk-table
        :border="['row']"
        :max-height="500"
        :data="metadataList"
        empty-cell-text="--"
      >
        <bk-table-column
          :label="t('元数据键')"
          prop="labelKey"
          :showOverflowTooltip="true"
        >
          <template #default="{row}">
            <div class="key-layout">
              <span class="key">
                <bk-overflow-title type="tips">{{ row.labelKey }}</bk-overflow-title>
              </span>
              <span v-if="row.system" class="tag">{{ t('内置') }}</span>
            </div>
          </template>
        </bk-table-column>
        <bk-table-column
          :label="t('元数据值类型')"
          prop="enumType"
          :min-width="120"
        >
          <template #default="{row}">
            <span>{{ row.enumType ? t('枚举类型') : t('字符串')}}</span>
          </template>
        </bk-table-column>
        <bk-table-column
          :label="t('枚举值')"
          :width="180"
          prop="labelColorMap"
          showOverflowTooltip
        >
          <template #default="{row}">
            <div class="label-color-content" v-if="Object.keys(row.labelColorMap ?? {}).length">
              <div v-for="(color, key) in row.labelColorMap" :key="key">
                <div class="color-map" style="width: 170px;">
                  <bk-overflow-title type="tips" :popover-options="{ maxWidth: 500 }">
                    <span :style="{backgroundColor: color}" class="enums-icon"></span>
                    <span style="word-wrap: break-word" :title="key">{{key}}</span>
                  </bk-overflow-title>
                </div>
              </div>
            </div>
            <span v-else>--</span>
          </template>
        </bk-table-column>
        <bk-table-column
          :label="t('元数据分组')"
          prop="category"
          :min-width="100"
        >
          <template #default="{row}">
            <span v-if="row.category" class="tag">{{ row.category}}</span>
            <span v-else>--</span>
          </template>
        </bk-table-column>
        <bk-table-column
          :label="t('描述')"
          prop="description"
          :showOverflowTooltip="true"
          :width="180"
        />
        <!-- <bk-table-column
          :label="t('是否启用')"
          prop="display"
          :sort="true"
          :min-width="110"
        >
          <template #default="{row}">
            <bk-switcher
              v-model="row.display"
              theme="primary"
              :disabled="type === 'show'"
              @change="updateMetadata"
            />
          </template>
        </bk-table-column> -->
        <bk-table-column
          v-if="type !== 'show'"
          :width="120"
          :label="t('操作')"
        >
          <template #default="{row}">
            <div v-if="!row.system">
              <bk-button text theme="primary" @click="handleEdit(row)"> {{t('编辑')}} </bk-button>
              <bk-button class="ml10" text theme="primary" @click="handleDelete(row)"> {{t('删除')}} </bk-button>
            </div>
            <div v-else>--</div>
          </template>
        </bk-table-column>
      </bk-table>
    </bk-form-item>
  </div>

  <bk-sideslider
    v-model:isShow="isShowMetadataForm"
    :title="metadaSidesliderTitle"
    renderDirective="if"
    @closed="handleMetadataCancel"
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
        property="labelKey"
        :required="true"
      >
        <bk-input
          v-model.trim="metadataForm.labelKey"
          :disabled="!isAdd"
          :placeholder="t('由字母、数字和下划线组成，且以英文字母开头')"
          clearable
        />
      </bk-form-item>
      <bk-form-item
        :label="t('元数据分组')"
        property="category"
      >
        <bk-select v-model="metadataForm.category">
          <bk-option
            label="质量"
            value="质量"
          />
        </bk-select>
        <span class="tips">{{ t('属于制品质量分组的元数据，将在制品列表、制品详情等页面展示') }}</span>
      </bk-form-item>
      <bk-form-item
        :label="t('元数据类型')"
        property="enumType"
        required
      >
        <bk-radio-group size="small" v-model="metadataForm.enumType">
          <bk-radio :label="true">{{ t('枚举值') }}</bk-radio>
          <bk-radio :label="false" :disabled="metadataForm.category === '质量'">{{ t('字符串') }}</bk-radio>
        </bk-radio-group>
      </bk-form-item>
      <bk-form-item
        v-if="metadataForm.enumType"
        property="labelColorMap"
        required
        class="enums"
      >
        <template #label>
          <span>{{ t('枚举值（单选）') }}</span>
        </template>
        <div class="enums-enable">
          <bk-switcher
            v-model="metadataForm.enableColorConfig"
            size="small"
            theme="primary"
          />
          <span>{{ t('开启颜色配置') }}</span>
        </div>
        <div>
          <div
            v-for="(item, index) in metadataForm.labelColorMap"
            :key="index"
            class="enums-content"
          >
            <div class="enums-list">
              <bk-color-picker
                v-if="metadataForm.enableColorConfig"
                :recommend="false"
                v-model="item.color"
              >
                <template #trigger>
                  <div :style="{backgroundColor: item.color}" class="color-picker" ></div>
                </template>
              </bk-color-picker>
              <bk-input
                v-model.trim="item.value"
                :class="{ 'input-error': enumErrors[index] }"
                @blur="validateEnum(index)"
                @input="validateEnum(index)"
              />
              <Del width="15" height="15" fill="#979BA5" @click="removeEnumValue(index)" />
            </div>
            <span v-if="enumErrors[index]" class="error-message">
              {{ enumErrors[index] === 'required' ? t('枚举值不能为空') : t('枚举值不能重复') }}
            </span>
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
        property="description"
      >
        <bk-input
          v-model="metadataForm.description"
          :placeholder="t('请输入')"
          :rows="3"
          :maxlength="100"
          type="textarea"
        />
      </bk-form-item>
    </bk-form>
    <div class="btn-group">
      <bk-button theme="primary" size="small" @click="handleMetadataSubmit"> {{t('确定')}} </bk-button>
      <bk-button size="small" @click="handleMetadataCancel"> {{t('取消')}} </bk-button>
    </div>
  </template>
  </bk-sideslider>
</template>

<script setup name="MetadataFormItem">
import http from '@/http/api';
import { computed, ref, reactive, watch, nextTick } from 'vue';
import { useI18n } from 'vue-i18n';
import { Del } from 'bkui-vue/lib/icon';
import { useRoute } from 'vue-router';
import { InfoBox } from 'bkui-vue';

const { t } = useI18n();
const route = useRoute();
const { projectCode } = route.params;
const props = defineProps({
  data: {
    type: Object,
    required: true
  },
  type: String,
  isRbac: Boolean,
  initPipelineDialect: String
});
const emits = defineEmits(['handleChangeForm', 'beforeChange', 'updateMetadata']);

const projectData = ref(props.data);
const enumErrors = reactive({});
const isShowMetadataForm = ref(false);
const isAdd = ref(false);
const metadataFormRef = ref();
const metadataForm = ref(getFormData())
const metadataRules = {
  labelKey: [
    { required: true, message: t('元数据键不能为空'), trigger: 'blur' },
    {
      validator: (value) => /^[a-zA-Z][a-zA-Z0-9_]*$/.test(value),
      message: t('由字母、数字和下划线组成，且以英文字母开头'),
      trigger: 'blur'
    }
  ],
};
const metadaSidesliderTitle = computed(() => isAdd.value ? t('新建元数据') : t('编辑元数据'))

watch(() => metadataForm.value.enableColorConfig, (newValue) => {
  if (!newValue) {
    metadataForm.value.labelColorMap.forEach(item => {
      item.color = '#C4C6CC';
    });
  }
}, { immediate: true });

watch(()=> metadataForm.value.category, (newValue)=>{
  if (newValue === '质量') {
    metadataForm.value.enumType = true
  }
}, { immediate: true });

const initialData = ref([]);
const metadataList = ref([]);
const isLoading= ref(false);

watch(() => projectData.value.properties, (newValue, oldValue) => {
  if (newValue && !oldValue) {
    nextTick(()=>{
      fetchMetadataList();
    });
  }
}, { immediate: true });

defineExpose({
  resetData,
});

function updateMetadata() {
  emits('updateMetadata', metadataList.value)
  emits('handleChangeForm')
}

async function fetchMetadataList () {
  isLoading.value = true;
  try {
    const res = await http.getMetadataList(projectCode);
    metadataList.value= res;
    initialData.value = JSON.parse(JSON.stringify(res));
    emits('updateMetadata', metadataList.value)
  } catch (error) {
    console.log("error", error);
  } finally {
    isLoading.value = false;
  }
}
function resetData () {
  metadataList.value = JSON.parse(JSON.stringify(initialData.value));
};

function getFormData () {
  return {
    labelKey: '',
    category: '',
    enumType: true,
    enableColorConfig: true,
    labelColorMap: [
      { color: '#C4C6CC', value: '' },
    ],
    description: ''
  }
}

function addEnumValue () {
  metadataForm.value.labelColorMap.push({
    value: '',
    color: '#C4C6CC'
  });

  const newIndex = metadataForm.value.labelColorMap.length - 1;
  enumErrors[newIndex] = null;
};

function removeEnumValue (index) {
  metadataForm.value.labelColorMap.splice(index, 1);

  const value = metadataForm.value.labelColorMap[index]?.value;
  value && delete enumErrors[index];
  validateAllEnums()
};

function validateEnum (index) {
  const value = metadataForm.value.labelColorMap[index]?.value;
  enumErrors[index] = value ? null : 'required';
  validateAllEnumsForDuplicates();
};
/**
 * 检查重复值并更新错误状态
 */
function validateAllEnumsForDuplicates() {
  const valueIndexes = {};
  metadataForm.value.labelColorMap.forEach((item, index) => {
    if (item.value) {
      if (!valueIndexes[item.value]) {
        valueIndexes[item.value] = [];
      }
      valueIndexes[item.value].push(index);
    }
  });

  // 先重置所有重复错误
  for (const key in enumErrors) {
    if (enumErrors[key] === 'duplicate') {
      enumErrors[key] = null;
    }
  }

  // 标记新的重复错误
  for (const value in valueIndexes) {
    const indices = valueIndexes[value];
    if (indices.length > 1) {
      indices.forEach(index => {
        enumErrors[index] = 'duplicate';
      });
    }
  }
  
  return valueIndexes;
}
/**
 * 校验所有枚举值
 */
function validateAllEnums() {
  Object.keys(enumErrors).forEach(key => {
    enumErrors[key] = null;
  });
  
  let hasEmptyValue = false;
  metadataForm.value.labelColorMap.forEach((item, index) => {
    if (!item.value) {
      enumErrors[index] = 'required';
      hasEmptyValue = true;
    }
  });

  const valueToIndices = validateAllEnumsForDuplicates();
  return !hasEmptyValue && Object.keys(valueToIndices).every(value => valueToIndices[value].length === 1);
}

function showMetadataAdd () {
  isAdd.value = true;
  isShowMetadataForm.value = true;
}

function colorMapObject(form) {
  const labelColorMap = form.labelColorMap.reduce((accumulator, item) => {
    accumulator[item.value] = item.color;
    return accumulator;
  }, {});

  return {
    ...form,
    display: true,
    labelColorMap
  }
}

async function handleMetadataSubmit () {
  try {
    if (metadataForm.value.enumType && !validateAllEnums()) return;
    
    const valid = await metadataFormRef.value.validate();
    if (valid) {
      const data = colorMapObject(metadataForm.value)
      if (isAdd.value) {
        metadataList.value.push(data)
      } else {
        const index = metadataList.value.findIndex(i => i.labelKey === data.labelKey);
        if (index !== -1) {
          metadataList.value.splice(index, 1, data);
        }
      }
      updateMetadata();
      isShowMetadataForm.value = false;
      handleMetadataCancel();
    }
  } catch (err) {
    console.log('err', err);
  }
}
function handleMetadataCancel () {
  isShowMetadataForm.value = false;
  metadataForm.value = getFormData();
  Object.keys(enumErrors).forEach(key => {
    enumErrors[key] = false;
  });
}
function convertObjectToArray(labelColorMap) {
  if (labelColorMap && typeof labelColorMap === 'object' && Object.keys(labelColorMap).length) {
    return Object.entries(labelColorMap).map(([value, color]) => {
      return {
        value,
        color
      }
    });
  }
  return [{ color: '#C4C6CC', value: '' }];
}

function handleEdit (row) {
  isShowMetadataForm.value = true;
  isAdd.value = false;
  metadataForm.value = {
    ...row,
    labelColorMap: convertObjectToArray(row.labelColorMap)
  };
}

async function handleDelete (row) {
  InfoBox({
    infoType: 'warning',
    title: t('确定删除此元数据吗？'),
    contentAlign: 'center',
    headerAlign: 'center',
    footerAlign: 'center',
    onConfirm: () => {
      const index = metadataList.value.findIndex(item => item.id === row.id);
      if (index !== -1) {
        metadataList.value.splice(index, 1);
        updateMetadata();
      }
    },
    onClosed: () => true,
  });
}

</script>
<style lang="scss">
.artifactory {
  .bk-form-content {
    max-width: 1065px !important;
  }
}
</style>
<style lang="scss" scoped>
.metadata-add {
  width: 150px;
  font-size: 12px;
  color: #3A84FF;
  cursor: pointer;
}
.key-layout {
  display: flex;
  align-items: center;
  color: #4D4F56;
  font-size: 12px;

  .key {
    max-width: 200px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}
.tag {
  display: inline-block;
  height: 20px;
  line-height: 20px;
  text-align: center;
  padding: 0 4px;
  background: #F0F1F5;
  border-radius: 2px;
}
.label-color-content {
  padding: 10px 0;
  .color-map {
    width: 180px;
    height: 20px;
    line-height: 20px;
  }
  .enums-icon {
    display: inline-block;
    width: 8px;
    height: 8px;
    margin-right: 8px;
    border: 1px solid #C4C6CC;
    border-radius: 50%;
  }
}
.metadata-slider {
  margin: 16px 24px;
  height: calc(100% - 38px);
  ::v-deep .bk-form-label {
    font-size: 12px;
  }
  .tips {
    color: #979BA5;
  }
  .enums {
    position: relative;
  }
  .enums-enable {
    color: #4D4F56;
    font-size: 12px;
    position: absolute;
    top: -32px;
    right: 0;
    span {
      padding-bottom: 2px;
    }
  }
}
.btn-group {
  margin: 32px 24px 0;
  position: sticky;
  bottom: 0;
  background-color: #fff;
}
.enums-content {
  margin-bottom: 12px;
}
.enums-list {
  display: flex;
  grid-gap: 8px;

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
.input-error {
  border-color: #ff5656 !important;
}
.error-message {
  color: #ff5656;
  font-size: 12px;
  margin-top: 4px;
}
</style>