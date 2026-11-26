<template>
  <div class="admin-page">
    <header class="page-title">
      <h1>{{ t('蓝盾公约') }}</h1>
    </header>
    <nav class="page-tabs">
      <button
        type="button"
        :class="{ 'is-on': activeTab === 'body' }"
        @click="onTabChange('body')"
      >
        {{ t('公约正文') }}
      </button>
      <button
        type="button"
        :class="{ 'is-on': activeTab === 'records' }"
        @click="onTabChange('records')"
      >
        {{ t('签署记录') }}
      </button>
    </nav>

    <div
      v-if="activeTab === 'body'"
      class="workspace"
    >
      <bk-loading
        class="panel-loading"
        :loading="bodyLoading"
      >
        <div class="tool-row">
          <div
            class="lang-switch"
            role="group"
            :aria-label="t('中文')"
          >
            <button
              type="button"
              :class="{ 'is-on': contentLang === 'zh' }"
              @click="contentLang = 'zh'"
            >
              {{ t('中文') }}
            </button>
            <button
              type="button"
              :class="{ 'is-on': contentLang === 'en' }"
              @click="contentLang = 'en'"
            >
              English
            </button>
          </div>
          <div
            v-if="editing"
            class="tool-actions"
          >
            <span
              class="save-wrap"
              v-bk-tooltips="saveTip ? { content: saveTip } : { disabled: true }"
            >
              <bk-button
                theme="primary"
                class="tool-btn"
                :disabled="!!saveTip || saving"
                :loading="saving"
                @click="save"
              >
                {{ t('保存') }}
              </bk-button>
            </span>
            <bk-button
              class="tool-btn"
              :disabled="saving"
              @click="stopEdit"
            >
              {{ t('取消') }}
            </bk-button>
          </div>
          <bk-button
            v-else
            theme="primary"
            class="tool-btn"
            :disabled="bodyLoading"
            @click="startEdit"
          >
            {{ t('编辑') }}
          </bk-button>
        </div>
        <div
          v-if="editing"
          class="edit-split"
        >
          <section class="editor">
            <div class="editor-head">{{ t('HTML 正文') }}</div>
            <textarea
              v-model="draft"
              class="editor-input"
              spellcheck="false"
            />
          </section>
          <section class="edit-preview">
            <div class="editor-head">{{ t('预览') }}</div>
            <div class="preview-body">
              <ConventionHtml :html="draft" />
            </div>
          </section>
        </div>
        <section
          v-else
          class="content-card"
        >
          <ConventionHtml :html="savedHtml" />
        </section>
      </bk-loading>
    </div>

    <div
      v-else
      class="workspace records-workspace"
    >
      <bk-loading
        class="panel-loading"
        :loading="recordsLoading"
      >
        <div class="tool-row records-tools">
          <bk-input
            v-model="keyword"
            class="record-search"
            clearable
            :disabled="recordsLoading"
            :placeholder="t('搜索用户')"
            @clear="onSearch"
            @enter="onSearch"
            @input="onKeywordChange"
          />
          <span
            class="export-wrap"
            v-bk-tooltips="recordCount ? { disabled: true } : { content: t('没有可导出的签署记录') }"
          >
            <bk-button
              class="tool-btn export-btn"
              :disabled="!recordCount || exporting || recordsLoading"
              :loading="exporting"
              @click="exportRows"
            >
              {{ t('导出') }}
            </bk-button>
          </span>
        </div>
        <bk-table
          class="records-table"
          height="100%"
          remote-pagination
          :data="records"
          :pagination="pagination"
          :empty-text="tableEmpty"
          @page-value-change="onPage"
          @page-limit-change="onLimit"
        >
          <bk-table-column
            :label="t('用户')"
            prop="userId"
            :width="240"
            show-overflow-tooltip
          />
          <bk-table-column
            :label="t('签署时间')"
            prop="agreeTime"
            :width="180"
          />
        </bk-table>
      </bk-loading>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import { Message } from 'bkui-vue'
import { debounce } from 'lodash'
import http from '@/http/api'
import ConventionHtml from './ConventionHtml.vue'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const bodyLoading = ref(true)
const recordsLoading = ref(false)
const saving = ref(false)
const exporting = ref(false)
const activeTab = ref('body')
const contentLang = ref('zh')
const editing = ref(false)
const draft = ref('')
const savedHtml = ref('')
const keyword = ref('')
const appliedKeyword = ref('')
const records = ref([])
const recordCount = ref(0)
const page = ref(1)
const pageSize = ref(20)
let bodySeq = 0
let recordsSeq = 0

const saveTip = computed(() => {
  if (!draft.value.trim()) return t('请录入公约正文')
  if (draft.value === savedHtml.value) return t('没有要保存的修改')
  return ''
})

const pagination = computed(() => ({
  current: page.value,
  limit: pageSize.value,
  count: recordCount.value,
  align: 'right',
}))

const tableEmpty = computed(() => (
  appliedKeyword.value.trim() ? t('没有匹配的签署记录') : t('还没有签署记录')
))

async function loadBody() {
  const seq = ++bodySeq
  const lang = contentLang.value
  bodyLoading.value = true
  try {
    const data = await http.getBody(lang)
    if (seq !== bodySeq || contentLang.value !== lang) return
    savedHtml.value = data?.content || ''
    if (!editing.value) {
      draft.value = savedHtml.value
    }
  } catch (e) {
    if (seq !== bodySeq || contentLang.value !== lang) return
    Message({ theme: 'error', message: e.message || t('加载失败') })
  } finally {
    if (seq === bodySeq) {
      bodyLoading.value = false
    }
  }
}

async function loadRecords() {
  const seq = ++recordsSeq
  const query = {
    user: appliedKeyword.value.trim() || undefined,
    page: page.value,
    pageSize: pageSize.value,
  }
  recordsLoading.value = true
  try {
    const data = await http.getAgreements(query)
    if (
      seq !== recordsSeq
      || page.value !== query.page
      || pageSize.value !== query.pageSize
      || appliedKeyword.value.trim() !== (query.user || '')
    ) {
      return
    }
    records.value = data?.records || []
    recordCount.value = data?.count || 0
  } catch (e) {
    if (seq !== recordsSeq) return
    Message({ theme: 'error', message: e.message || t('加载失败') })
  } finally {
    if (seq === recordsSeq) {
      recordsLoading.value = false
    }
  }
}

function stopEdit() {
  editing.value = false
  draft.value = savedHtml.value
}

function startEdit() {
  draft.value = savedHtml.value
  editing.value = true
}

async function save() {
  if (saveTip.value) return
  saving.value = true
  try {
    await http.saveBody({
      lang: contentLang.value,
      content: draft.value,
    })
    savedHtml.value = draft.value
    editing.value = false
    Message({ theme: 'success', message: t('已保存') })
  } catch (e) {
    Message({ theme: 'error', message: e.message || t('保存失败') })
  } finally {
    saving.value = false
  }
}

function onTabChange(name) {
  const tab = name === 'records' ? 'records' : 'body'
  if ((route.query.tab || 'body') === tab) return
  router.replace({ query: { ...route.query, tab } })
}

function onSearch() {
  appliedKeyword.value = keyword.value
  page.value = 1
  loadRecords()
}

const onKeywordChange = debounce(() => {
  onSearch()
}, 300)

function onPage(nextPage) {
  page.value = nextPage
  loadRecords()
}

function onLimit(limit) {
  pageSize.value = limit
  page.value = 1
  loadRecords()
}

async function exportRows() {
  if (!recordCount.value) return
  exporting.value = true
  try {
    await http.exportAgreements(appliedKeyword.value.trim() || undefined)
  } catch (e) {
    Message({ theme: 'error', message: e.message || t('导出失败') })
  } finally {
    exporting.value = false
  }
}

watch(() => route.query.tab, (tab) => {
  const next = tab === 'records' ? 'records' : 'body'
  if (next !== 'body') stopEdit()
  activeTab.value = next
  if (next === 'records') {
    loadRecords()
  } else {
    loadBody()
  }
}, { immediate: true })

watch(contentLang, () => {
  stopEdit()
  if (activeTab.value === 'body') {
    loadBody()
  }
})
</script>

<style scoped>
.admin-page {
  flex: 1;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}
.page-title {
  display: flex;
  align-items: center;
  height: 52px;
  margin: 0;
  padding: 0 24px;
  background: #fff;
}
.page-title h1 {
  margin: 0;
  color: #313238;
  font-size: 16px;
  font-weight: 700;
  line-height: 24px;
}
.page-tabs {
  display: flex;
  align-items: flex-end;
  gap: 32px;
  height: 42px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #dcdee5;
}
.page-tabs button {
  height: 32px;
  padding: 0;
  border: 0;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: #4d4f56;
  font-size: 14px;
  line-height: 22px;
  cursor: pointer;
}
.page-tabs button.is-on {
  border-bottom-color: #1768ef;
  color: #1768ef;
}
.workspace {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 0;
  padding: 24px;
  overflow: auto;
  background: #f5f7fa;
}
.records-workspace {
  overflow: hidden;
}
.panel-loading {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.records-workspace .panel-loading {
  overflow: hidden;
}
.tool-row {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 16px;
  height: 32px;
}
.records-tools {
  gap: 8px;
}
.tool-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}
.tool-row > .tool-btn {
  margin-left: auto;
}
.tool-btn {
  width: 64px;
}
.export-btn {
  width: auto;
  min-width: 64px;
}
.tool-btn :deep(.bk-button),
.tool-actions :deep(.bk-button) {
  width: 64px;
  height: 32px;
  padding: 0;
  border-radius: 2px;
}
:deep(.tool-btn.bk-button-primary),
.tool-actions :deep(.bk-button.bk-button-primary) {
  background-color: #1768ef;
  border-color: #1768ef;
  color: #fff;
}
:deep(.tool-btn.bk-button-primary:hover),
:deep(.tool-btn.bk-button-primary:active),
.tool-actions :deep(.bk-button.bk-button-primary:hover),
.tool-actions :deep(.bk-button.bk-button-primary:active) {
  background-color: #1768ef;
  border-color: #1768ef;
  color: #fff;
}
.lang-switch {
  display: flex;
  align-items: center;
  box-sizing: border-box;
  width: 122px;
  height: 32px;
  padding: 4px;
  border: 0;
  border-radius: 16px;
  background: #eaebf0;
}
.lang-switch button {
  flex: 1 1 0;
  height: 24px;
  margin: 0;
  padding: 0;
  border: 0;
  outline: none;
  border-radius: 12px;
  background: transparent;
  color: #4d4f56;
  font-size: 12px;
  line-height: 24px;
  cursor: pointer;
}
.lang-switch button.is-on {
  background: #fff;
  color: #3a84ff;
}
.content-card {
  padding: 24px;
  border-radius: 2px;
  background: #fff;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.2);
}
.edit-split {
  display: flex;
  gap: 0;
  align-items: stretch;
}
.edit-preview {
  display: flex;
  flex-direction: column;
  min-width: 0;
  margin-left: -1px;
}
.edit-split > .editor .editor-head {
  border-radius: 2px 0 0 0;
}
.edit-split > .edit-preview .editor-head {
  border-radius: 0 2px 0 0;
}
.edit-split > .editor .editor-input {
  border-radius: 0 0 0 2px;
}
.preview-body {
  flex: 1;
  min-height: 0;
  box-sizing: border-box;
  margin-top: -1px;
  padding: 24px;
  overflow: auto;
  border: 1px solid #dcdee5;
  border-radius: 0 0 2px 0;
  background: #fff;
}
.edit-split > .editor,
.edit-split > .edit-preview {
  flex: 1 1 0;
  width: 0;
  min-width: 0;
  height: 650px;
}
.editor {
  display: flex;
  flex-direction: column;
}
.editor-head {
  display: flex;
  align-items: center;
  height: 36px;
  padding: 0 9px;
  border: 1px solid #dcdee5;
  border-bottom: 0;
  border-radius: 2px 2px 0 0;
  background: #f5f7fa;
  color: #313238;
  font-size: 14px;
  line-height: 22px;
}
.editor-input {
  flex: 1;
  box-sizing: border-box;
  height: 615px;
  margin-top: -1px;
  padding: 4px 8px;
  border: 1px solid #dcdee5;
  border-radius: 0 0 2px 2px;
  background: #fff;
  color: #4d4f56;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 20px;
  resize: none;
}
.edit-split > .editor .editor-input:focus {
  position: relative;
  z-index: 1;
  border: 1px solid #3a84ff;
  outline: 0;
  box-shadow: 0 0 3px 0 #a3c5fd;
}
.save-wrap,
.export-wrap {
  display: inline-block;
}
.export-wrap {
  margin-left: auto;
}
.save-wrap :deep(button[disabled]),
.export-wrap :deep(button[disabled]) {
  pointer-events: none;
}
.record-search {
  width: 240px;
  height: 32px;
}
.records-table {
  flex: 1;
  min-height: 0;
  height: 100%;
  border-radius: 2px;
  background: #fff;
  overflow: hidden;
}
.records-table :deep(.bk-table-head table),
.records-table :deep(.bk-table-body table) {
  width: 420px !important;
}
.records-table :deep(th),
.records-table :deep(td) {
  height: 42px;
  box-sizing: border-box;
}
</style>
