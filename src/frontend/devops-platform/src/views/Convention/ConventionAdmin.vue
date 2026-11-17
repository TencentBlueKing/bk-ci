<template>
  <div
    class="admin-page"
    v-bkloading="{ isLoading }"
  >
    <header class="admin-head">
      <h1>{{ t('蓝盾公约') }}</h1>
    </header>

    <bk-tab
      v-model:active="activeTab"
      class="admin-tabs"
      type="card-tab"
      @change="onTabChange"
    >
      <bk-tab-panel
        name="body"
        :label="t('公约正文')"
      >
        <div class="admin-stack">
          <section class="panel">
            <div class="panel-bar">
              <bk-radio-group v-model="contentLang">
                <bk-radio-button label="zh">{{ t('中文') }}</bk-radio-button>
                <bk-radio-button label="en">English</bk-radio-button>
              </bk-radio-group>
              <bk-button
                v-if="!editing"
                theme="primary"
                @click="startEdit"
              >
                {{ t('编辑') }}
              </bk-button>
            </div>
            <template v-if="editing">
              <bk-input
                v-model="draft"
                class="html-input"
                type="textarea"
                :rows="12"
              />
              <div class="save-row">
                <bk-button @click="stopEdit">
                  {{ t('取消') }}
                </bk-button>
                <span
                  class="save-wrap"
                  v-bk-tooltips="saveTip ? { content: saveTip } : { disabled: true }"
                >
                  <bk-button
                    theme="primary"
                    :disabled="!!saveTip || saving"
                    :loading="saving"
                    @click="save"
                  >
                    {{ t('保存') }}
                  </bk-button>
                </span>
              </div>
              <h2>{{ t('预览') }}</h2>
              <ConventionHtml :html="draft" />
            </template>
            <ConventionHtml
              v-else
              :html="savedHtml"
            />
          </section>
        </div>
      </bk-tab-panel>
      <bk-tab-panel
        name="records"
        :label="t('签署记录')"
      >
        <div class="admin-stack">
          <section class="panel records-panel">
            <div class="filters">
              <bk-input
                v-model="keyword"
                class="record-search"
                clearable
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
                  :disabled="!recordCount || exporting"
                  :loading="exporting"
                  @click="exportRows"
                >
                  {{ t('导出') }}
                </bk-button>
              </span>
            </div>
            <bk-table
              class="records-table"
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
          </section>
        </div>
      </bk-tab-panel>
    </bk-tab>
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

const isLoading = ref(false)
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
  isLoading.value = true
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
      isLoading.value = false
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
  isLoading.value = true
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
      isLoading.value = false
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
  router.replace({ name: 'Convention', query: { tab } })
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
  min-height: 100%;
}
.admin-head {
  display: flex;
  align-items: center;
  height: 52px;
  padding: 0 24px;
  background: #fff;
  border-bottom: 1px solid #dcdee5;
}
.admin-head h1 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #313238;
}
.admin-tabs {
  background: #fff;
}
.admin-stack {
  padding: 16px 24px 24px;
  background: #f5f7fa;
}
.panel {
  padding: 16px;
  background: #fff;
  border-radius: 2px;
}
.records-panel {
  padding-bottom: 8px;
}
.records-table :deep(.bk-table-head table),
.records-table :deep(.bk-table-body table) {
  width: 420px !important;
}
.filters {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.record-search {
  width: 240px;
}
.export-wrap,
.save-wrap {
  display: inline-block;
}
.export-wrap {
  margin-left: auto;
}
.export-wrap :deep(button[disabled]),
.save-wrap :deep(button[disabled]) {
  pointer-events: none;
}
.panel-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.html-input :deep(textarea) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  line-height: 20px;
}
.save-row {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}
h2 {
  margin: 24px 0 12px;
  font-size: 14px;
  font-weight: 600;
  line-height: 22px;
  color: #313238;
}
</style>
