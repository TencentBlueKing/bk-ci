<template>
  <div>
    <bk-form-item
      property="pipelineDialect"
    >
      <template #label>
        <dialect-popover-table />
      </template>
      <bk-radio-group
        v-model="projectData.properties.pipelineDialect"
        @change="handleChangeForm"
        :beforeChange="beforeChange"
        class="dialect-radio"
      >
        <bk-radio label="CLASSIC">
          <span>{{ t('CLASSIC') }}</span>
        </bk-radio>
        <bk-radio label="CONSTRAINED">
          <span>{{ t('CONSTRAINED') }}</span>
        </bk-radio>
      </bk-radio-group>
      <div v-if="type==='apply'">
        <bk-alert theme="warning">
          <template #title>
            <div v-if="projectData.properties.pipelineDialect==='CONSTRAINED'">
              {{ t('CLASSICTIP') }}
            </div>
            <div v-else-if="projectData.properties.pipelineDialect==='CLASSIC'">
              <p>{{ t('CONSTRAINEDTIP') }}</p>
              <p>{{ t('示例，存在流水线变量 a=1，则如下脚本打印出的 a为1，而不是脚本中设置的 3') }}</p>
              <p class="tip-script">
                <p class="text-white">a=3</p>
                <p>
                  <span class="text-echo">echo</span><span class="text-a">$</span>{<span class="text-a">a</span>}
                </p>
              </p>
              <p>{{ t('请知悉，编排流水线时， Bash 脚本中的局部变量请勿与流水线变量重名，避免出现同名赋值问题。') }}</p>
            </div>
          </template>
        </bk-alert>
      </div>
    </bk-form-item>
    <bk-form-item
      :label="t('命名规范提示')"
      :description="t('开启后，需填写流水线命名规范提示说明。规范提示说明将展示在「创建流水线」页面进行提示。')"
    >
      <bk-switcher
        v-model="projectData.properties.enablePipelineNameTips"
        size="small"
        theme="primary"
        @change="handleChangeForm"
      />
      <bk-input
        class="textarea"
        v-show="projectData.properties.enablePipelineNameTips"
        v-model="projectData.properties.pipelineNameFormat"
        :placeholder="t('请输入流水线命名规范提示说明')"
        :rows="3"
        :maxlength="200"
        type="textarea"
        @change="handleChangeForm"
      >
      </bk-input>
    </bk-form-item>
    <bk-form-item
      :label="t('构建日志归档阈值')"
      property="loggingLineLimit"
      :description="t('单个步骤(Step)日志达到阈值时，将压缩并归档到日志仓库。可下载日志文件到本地查看。')"
    >
      <bk-input
        v-model="projectData.properties.loggingLineLimit"
        class="log-line-limit-input"
        type="number"
        :showControl="false"
        :min="1"
        :max="100"
        :suffix="t('万行')"
        :placeholder="t('缺省时默认为10')"
        @change="handleChangeForm"
      >
      </bk-input>
    </bk-form-item>
    
    <bk-sideslider
      v-model:isShow="pipelineSideslider"
      :title="`${t('切换变量语法风格影响的流水线列表')}(${t('共X个', [pipelinePagination.count])})`"
      :width="640"
      renderDirective="if"
      class="pipeline-sideslider"
    >
      <bk-loading :loading="isLoading">
        <bk-table
          :max-height="600"
          :data="pipelineList"
          show-overflow-tooltip
          :pagination="pipelinePagination"
          remote-pagination
          @page-limit-change="pageLimitChange"
          @page-value-change="pageValueChange"
          >
          <bk-table-column
            :label="t('流水线名称')"
            prop="pipelineName"
          >
            <template #default="{row}">
              <span class="text-link edit-line" @click="handleToPipeline(row)">{{ row.pipelineName }}</span>
            </template>
          </bk-table-column>
        </bk-table>
      </bk-loading>
    </bk-sideslider>
  </div>
</template>

<script setup name="PipelineContent">
import http from '@/http/api';
import { useI18n } from 'vue-i18n';
import { ref, computed, h } from 'vue';
import { copyToClipboard } from "@/utils/util.js"
import copyImg from "@/css/svg/copy.svg";
import { Message, Button, InfoBox, Alert } from 'bkui-vue';
import DialectPopoverTable from "@/components/dialectPopoverTable.vue";

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
const emits = defineEmits(['handleChangeForm', 'beforeChange']);

const projectData = ref(props.data);
const confirmSwitch = ref('');
const infoBoxRef = ref();
const pipelineList = ref([]);
const isLoading = ref(false);
const pipelineSideslider = ref(false);
const pipelinePagination = ref({ count: 0, limit: 20, current: 1 });
const projectId = computed(() => projectData.value.englishName);
const currentPipelineDialect = computed(() => projectData.value?.properties?.pipelineDialect);

function handleChangeForm() {
  emits('handleChangeForm')
}
function changeClassic () {
  return createAlertContent('危险操作告警：', [
    h('p', t('alarmTip1')),
    h('p', { class: 'tip-script', style: { 'font-size': '12px' } }, [
      h('p', { class: 'text-white' }, 'a=3'),
      h('p', [
        h('span', { class: 'text-echo' }, 'echo'),
        h('span', { class: 'text-a' }, '$'),
        h('span', '{'),
        h('span', { class: 'text-a' }, 'a'),
        h('span', '}'),
      ]),
    ]),
    h('p', [
      h('span', {
        class: 'tip-blue',
        onClick() {
          pipelineSideslider.value = true;
          fetchListViewPipelines()
        },
      }, t('X条', [pipelinePagination.value.count])),
      h('span', t('alarmTip2'))
    ]),
  ]);
};
function createAlertContent (titleText, paragraphs) {
  return h(Alert, { theme: "warning" }, {
    title: () => h('div', { style: { 'line-height': '20px' } }, [
      h('p', { class: 'alarm' }, t(titleText)),
      ...paragraphs
    ])
  });
};
function changeConstrained () {
  return createAlertContent(t('危险操作告警：'), [
    h('p', [
      h('span', t('alarmTip3')),
      h('span', {
        class: 'tip-blue',
        onClick() {
          pipelineSideslider.value = true;
          fetchListViewPipelines()
        },
      }, t('X条', [pipelinePagination.value.count])),
      h('span', t('alarmTip4'))
    ]),
  ]);
};
async function getCountPipelineByDialect () {
  try {
    const res = await http.countInheritedDialectPipeline(projectId.value);
    pipelinePagination.value.count = res;
  } catch (error) {
    console.log(error);
  }
}
function beforeChange(params) {
  return new Promise(async (resolve) => {
    if (props.type === 'edit' && props.initPipelineDialect === currentPipelineDialect.value) {
      confirmSwitch.value = '';
      await getCountPipelineByDialect();
      const copyText = t('我已明确变更风险且已确认变更无影响');
      const isClassic = currentPipelineDialect.value !== 'CLASSIC';
      const title = isClassic ? t('确认切换成“传统风格？') : t('确认切换成“制约风格？');
      const content = isClassic ? changeClassic() : changeConstrained();
      infoBoxRef.value = InfoBox({
        type: 'warning',
        width: 640,
        title: title,
        content: h('div', [
          content,
          h('div', { class: 'confirm-switch' }, [
            h('p', { class: 'confirm-tit' }, [
              h('span', t('请输入')),
              h('span', { style: { 'font-weight': 700 } }, ` "${copyText}" `),
              h('img', {
                src: copyImg,
                style: { width: '12px', 'vertical-align': 'middle', margin: '4px' },
                onClick() {
                  copyToClipboard(copyText);
                  Message({ theme: 'success', message: t('复制成功') });
                },
              }),
              h('span', t('以确认切换'))
            ]),
            h('input', {
              class: 'confirm-input',
              placeholder: t('请输入'),
              value: confirmSwitch.value,
              onInput: (event) => {
                confirmSwitch.value = event.target.value;
              }
            })
          ])
        ]),
        footer: () => h('div',{}, [
          h(Button, {
            disabled: !confirmSwitch.value,
            theme: 'danger' ,
            onClick() {
              if (confirmSwitch.value === copyText) {
                resolve(true);
              } else {
                resolve(false);
                Message({ theme: 'error', message: t('请输入：我已明确变更风险且已确认变更无影响') });
              }
              infoBoxRef.value.hide()
            },
          }, t('确认切换')),
          h(Button, {
            onClick() {
              resolve(false);
              infoBoxRef.value.hide()
            },
          }, t('取消'))
        ]),
        onCancel() {
          resolve(false);
          infoBoxRef.value.hide()
        },
      });
    } else {
      resolve(true);
    }
  });
}
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
function handleToPipeline (row) {
  window.open(`/console/pipeline/${projectId.value}/${row.pipelineId}/history/pipeline`, '__blank');
}

</script>
<style lang="scss">
.dialect-radio {
  .bk-radio-label {
    font-size: 12px;
  }
}
.pipeline-sideslider .bk-modal-content{
  padding: 16px 24px;
}
.confirm-input {
  width: 576px;
  height: 32px;
  color: #63656e;
  font-size: 14px;
  border: 1px solid #C4C6CC;
  outline: none;
  box-sizing: border-box;
  padding: 5px;
}
.confirm-input:focus {
  border-color: #3a84ff;
}
.confirm-input::placeholder {
  color: #C4C6CC;
}
.alarm {
  color: #4D4F56;
  font-size: 12px;
  font-weight: 700;
}
.tip-script {
  display: flex;
  flex-direction: column;
  justify-content: center;
  width: 200px;
  height: 60px;
  line-height: 22px;
  padding-left: 20px;
  margin: 10px 0;
  background: #313238;
  font-size: 14px;
  color: #F8B64F;
}
.text-white {
  color: #F5F7FA;
}
.text-echo {
  color: #699DF4;
  margin-right: 10px;
}
.text-a {
  color: #65B6C3;
}
.tip-blue {
  cursor: pointer;
  color: #3A84FF; 
  font-weight: 700;
}
.confirm-switch {
  display: flex;
  flex-direction: column;
  align-items: start;
  margin-top: 16px;
}
.confirm-tit {
  color: #333333;
  font-size: 14px;
  margin-bottom: 10px;
}
.log-line-limit-input {
  width: 150px;
}
.edit-line {
  cursor: pointer;
}
.text-link {
  font-size: 12px;
  color: #3c96ff;
}
</style>