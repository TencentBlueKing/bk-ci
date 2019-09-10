<template>
    <div class="atom-list-wrapper">
        <h3 class="market-home-title banner-nav">
            <icon class="title-icon" name="color-logo-store" size="25" />
            <p class="title-name">
                <span class="back-home" @click="toAtomStore">研发商店</span>
                <i class="right-arrow banner-arrow"></i>
                <span class="banner-des">工作台</span>
            </p>
            <a class="title-work" target="_blank" :href="currentTab === 'atom' ? atomDevelopLink : templateLink">{{ currentTab === 'atom' ? '插件指引' : '模板指引' }}</a>
        </h3>
        <div class="atomstore-list-content"
            v-bkloading="{
                isLoading: loading.isLoading,
                title: loading.title
            }">
            <div class="atomstore-list-main" v-if="!loading.isLoading">
                <bk-tab :active.sync="currentTab" @tab-change="changeTab" type="unborder-card">
                    <bk-tab-panel name="atom">
                        <template slot="label">
                            <span class="bk-icon icon-tab icon-atom"></span>
                            流水线插件
                        </template>
                        <span class="bk-icon icon-tab icon-atom" slot="tag"></span>
                        <div class="content-header">
                            <div class="atom-total-row">
                                <button class="bk-button bk-primary" @click="createNewAtom()">
                                    <span style="margin-left: 0;">新增插件</span>
                                </button>
                            </div>
                            <section :class="[{ 'control-active': isInputFocus }, 'g-input-search', 'list-input']">
                                <input class="g-input-border" type="text" placeholder="请输入关键字搜索" v-model="searchName" @focus="isInputFocus = true" @blur="isInputFocus = false" @keyup.enter="search" />
                                <i class="bk-icon icon-search" v-if="!searchName"></i>
                                <i class="bk-icon icon-close-circle-shape clear-icon" v-else @click="clearSearch"></i>
                            </section>
                        </div>
                        <bk-table style="margin-top: 15px;"
                            empty-text="暂时没有插件"
                            :data="renderList"
                            :pagination="pagination"
                            @page-change="pageChanged"
                            @page-limit-change="pageCountChanged"
                        >
                            <bk-table-column label="插件名称">
                                <template slot-scope="props">
                                    <span class="atom-name" :title="props.row.name" @click="routerAtoms(props.row.atomCode)">{{ props.row.name }}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column label="调试项目" prop="projectName"></bk-table-column>
                            <bk-table-column label="开发语言" prop="language"></bk-table-column>
                            <bk-table-column label="版本" prop="version"></bk-table-column>
                            <bk-table-column label="状态">
                                <template slot-scope="props">
                                    <div class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary" v-if="props.row.atomStatus === 'COMMITTING' || props.row.atomStatus === 'BUILDING' || props.row.atomStatus === 'BUILD_FAIL' || props.row.atomStatus === 'TESTING' || props.row.atomStatus === 'AUDITING' || props.row.atomStatus === 'UNDERCARRIAGING'">
                                        <div class="rotate rotate1"></div>
                                        <div class="rotate rotate2"></div>
                                        <div class="rotate rotate3"></div>
                                        <div class="rotate rotate4"></div>
                                        <div class="rotate rotate5"></div>
                                        <div class="rotate rotate6"></div>
                                        <div class="rotate rotate7"></div>
                                        <div class="rotate rotate8"></div>
                                    </div>
                                    <span class="atom-status-icon success" v-if="props.row.atomStatus === 'RELEASED'"></span>
                                    <span class="atom-status-icon fail" v-if="props.row.atomStatus === 'GROUNDING_SUSPENSION'"></span>
                                    <span class="atom-status-icon obtained" v-if="props.row.atomStatus === 'AUDIT_REJECT' || props.row.atomStatus === 'UNDERCARRIAGED'"></span>
                                    <span class="atom-status-icon bk-icon icon-initialize" v-if="props.row.atomStatus === 'INIT'"></span>
                                    <span>{{ atomStatusList[props.row.atomStatus] }}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column label="修改人" prop="modifier"></bk-table-column>
                            <bk-table-column label="修改时间" prop="updateTime" width="150"></bk-table-column>
                            <bk-table-column label="操作" width="200" class-name="handler-btn">
                                <template slot-scope="props">
                                    <span class="upgrade-btn"
                                        v-if="props.row.atomStatus === 'GROUNDING_SUSPENSION' || props.row.atomStatus === 'AUDIT_REJECT' || props.row.atomStatus === 'RELEASED'"
                                        @click="editHandle('upgradeAtom', 'atom', props.row.atomId)">升级</span>
                                    <span class="install-btn"
                                        v-if="props.row.atomStatus === 'RELEASED'"
                                        @click="installAHandle('atom', props.row.atomCode)">安装</span>
                                    <span class="shelf-btn"
                                        v-if="props.row.atomStatus === 'INIT' || props.row.atomStatus === 'UNDERCARRIAGED'"
                                        @click="editHandle('shelfAtom', 'atom', props.row.atomId)">上架</span>
                                    <span class="obtained-btn"
                                        v-if="props.row.atomStatus === 'AUDIT_REJECT' || props.row.atomStatus === 'RELEASED' || (props.row.atomStatus === 'GROUNDING_SUSPENSION' && props.row.releaseFlag)"
                                        @click="offline('atom', props.row)">下架</span>
                                    <span class="schedule-btn"
                                        v-if="props.row.atomStatus === 'COMMITTING' || props.row.atomStatus === 'BUILDING' || props.row.atomStatus === 'BUILD_FAIL'
                                            || props.row.atomStatus === 'TESTING' || props.row.atomStatus === 'AUDITING'"
                                        @click="routerProgress(props.row.atomId)">进度</span>
                                </template>
                            </bk-table-column>
                        </bk-table>
                    </bk-tab-panel>
                    <bk-tab-panel name="template">
                        <template slot="label">
                            <span class="bk-icon icon-tab icon-template"></span>
                            流水线模板
                        </template>
                        <div class="content-header">
                            <div class="atom-total-row">
                                <button class="bk-button bk-primary" @click="relateTemplate()">
                                    <span style="margin-left: 0;">关联模板</span>
                                </button>
                            </div>
                            <section :class="[{ 'control-active': isInputFocus }, 'g-input-search', 'list-input']">
                                <input class="g-input-border" type="text" placeholder="请输入关键字搜索" v-model="searchName" @focus="isInputFocus = true" @blur="isInputFocus = false" @keyup.enter="search" />
                                <i class="bk-icon icon-search" v-if="!searchName"></i>
                                <i class="bk-icon icon-close-circle-shape clear-icon" v-else @click="clearSearch"></i>
                            </section>
                        </div>
                        <bk-table style="margin-top: 15px;"
                            empty-text="暂时没有模板"
                            :data="renderList"
                            :pagination="pagination"
                            @page-change="pageChanged"
                            @page-limit-change="pageCountChanged"
                        >
                            <bk-table-column label="模板名称">
                                <template slot-scope="props">
                                    <span class="atom-name" :title="props.row.templateName" @click="routerAtoms(props.row.templateCode)">{{ props.row.templateName }}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column label="所属项目" prop="projectName"></bk-table-column>
                            <bk-table-column label="状态">
                                <template slot-scope="props">
                                    <div class="bk-spin-loading bk-spin-loading-mini bk-spin-loading-primary"
                                        v-if="props.row.templateStatus === 'AUDITING'">
                                        <div class="rotate rotate1"></div>
                                        <div class="rotate rotate2"></div>
                                        <div class="rotate rotate3"></div>
                                        <div class="rotate rotate4"></div>
                                        <div class="rotate rotate5"></div>
                                        <div class="rotate rotate6"></div>
                                        <div class="rotate rotate7"></div>
                                        <div class="rotate rotate8"></div>
                                    </div>
                                    <span class="atom-status-icon success" v-if="props.row.templateStatus === 'RELEASED'"></span>
                                    <span class="atom-status-icon fail" v-if="props.row.templateStatus === 'GROUNDING_SUSPENSION'"></span>
                                    <span class="atom-status-icon obtained" v-if="props.row.templateStatus === 'AUDIT_REJECT' || props.row.templateStatus === 'UNDERCARRIAGED'"></span>
                                    <span class="atom-status-icon bk-icon icon-initialize" v-if="props.row.templateStatus === 'INIT'"></span>
                                    <span>{{ templateStatusMap[props.row.templateStatus] }}</span>
                                </template>
                            </bk-table-column>
                            <bk-table-column label="修改人" prop="modifier"></bk-table-column>
                            <bk-table-column label="修改时间" prop="updateTime" width="150" :formatter="timeFormatter"></bk-table-column>
                            <bk-table-column label="操作" width="250" class-name="handler-btn">
                                <template slot-scope="props">
                                    <span class="shelf-btn"
                                        v-if="props.row.templateStatus === 'INIT' || props.row.templateStatus === 'UNDERCARRIAGED'
                                            || props.row.templateStatus === 'GROUNDING_SUSPENSION' || props.row.templateStatus === 'AUDIT_REJECT'"
                                        @click="editHandle('editTemplate', 'template', props.row.templateId)">上架
                                    </span>
                                    <span class="shelf-btn"
                                        v-if="props.row.templateStatus === 'RELEASED'"
                                        @click="editHandle('editTemplate', 'template', props.row.templateId)">升级
                                    </span>
                                    <span class="shelf-btn"
                                        v-if="props.row.templateStatus === 'RELEASED'"
                                        @click="installAHandle('template', props.row.templateCode)">安装
                                    </span>
                                    <span class="schedule-btn"
                                        v-if="props.row.templateStatus === 'AUDITING'"
                                        @click="toTemplateProgress(props.row.templateId)">进度
                                    </span>
                                    <span class="obtained-btn"
                                        v-if="props.row.templateStatus === 'AUDIT_REJECT' || props.row.templateStatus === 'RELEASED' || (props.row.templateStatus === 'GROUNDING_SUSPENSION' && props.row.releaseFlag)"
                                        @click="offline('template', props.row)"
                                    >下架</span>
                                    <span @click="deleteTemplate(props.row)" v-if="['INIT', 'GROUNDING_SUSPENSION', 'UNDERCARRIAGED'].includes(props.row.templateStatus)">移除</span>
                                    <span style="margin-right:0">
                                        <a target="_blank"
                                            style="color:#3c96ff;"
                                            :href="`/console/pipeline/${props.row.projectCode}/template/${props.row.templateCode}/edit`"
                                        >源模板</a>
                                    </span>
                                </template>
                            </bk-table-column>
                        </bk-table>
                    </bk-tab-panel>
                </bk-tab>
            </div>
            <bk-sideslider v-if="createAtomsideConfig.show"
                class="create-atom-slider g-slide-radio"
                :is-show.sync="createAtomsideConfig.show"
                :title="createAtomsideConfig.title"
                :quick-close="createAtomsideConfig.quickClose"
                :width="createAtomsideConfig.width">
                <template slot="content">
                    <form class="bk-form create-atom-form"
                        v-bkloading="{
                            isLoading: createAtomsideConfig.isLoading
                        }">
                        <div class="bk-form-item is-required">
                            <label class="bk-label">名称</label>
                            <div class="bk-form-content atom-item-content">
                                <input type="text" class="bk-form-input atom-name-input" placeholder="请输入中英文名称，不超过20个字符"
                                    name="atomName"
                                    v-model="createAtomForm.name"
                                    v-validate="{
                                        required: true,
                                        max: 20
                                    }"
                                    :class="{ 'is-danger': errors.has('atomName') }">
                                <p :class="errors.has('atomName') ? 'error-tips' : 'normal-tips'">{{ errors.first("atomName") }}</p>
                            </div>
                        </div>
                        <div class="bk-form-item is-required">
                            <label class="bk-label">标识</label>
                            <div class="bk-form-content atom-item-content is-tooltips">
                                <div style="min-width: 100%;">
                                    <input type="text" class="bk-form-input atom-id-input" placeholder="请输入英文名称，不超过30个字符"
                                        name="atomId"
                                        v-model="createAtomForm.atomCode"
                                        v-validate="{
                                            required: true,
                                            max: 30,
                                            regex: '^[a-zA-Z]+$'
                                        }"
                                        :class="{ 'is-danger': errors.has('atomId') }">
                                    <p :class="errors.has('atomId') ? 'error-tips' : 'normal-tips'">
                                        {{ errors.first("atomId") && errors.first("atomId").indexOf('正则') > 0 ? '只能输入英文' : errors.first("atomId") }}
                                    </p>
                                </div>
                                <bk-popover placement="right">
                                    <i class="bk-icon icon-info-circle"></i>
                                    <template slot="content">
                                        <p>唯一标识，创建后不能修改。将作为插件代码库路径。</p>
                                    </template>
                                </bk-popover>
                            </div>
                        </div>
                        <div class="bk-form-item is-required">
                            <label class="bk-label">调试项目</label>
                            <div class="bk-form-content atom-item-content is-tooltips">
                                <div style="min-width: 100%">
                                    <bk-select v-model="createAtomForm.projectCode" searchable>
                                        <bk-option v-for="(option, index) in projectList"
                                            :key="index"
                                            :id="option.project_code"
                                            :name="option.project_name"
                                            @click.native="selectedProject"
                                            :placeholder="'请选择调试项目'"
                                        >
                                        </bk-option>
                                        <div slot="extension" style="cursor: pointer;">
                                            <a :href="itemUrl" target="_blank">
                                                <i class="bk-icon icon-plus-circle" />
                                                {{ itemText }}
                                            </a>
                                        </div>
                                    </bk-select>
                                    <div v-if="atomErrors.projectError" class="error-tips">项目不能为空</div>
                                </div>
                                <bk-popover placement="right" width="400">
                                    <i class="bk-icon icon-info-circle"></i>
                                    <template slot="content">
                                        <p>插件默认安装的项目，当新版本进入测试阶段后，该项目下使用当前插件且版本为[主版本号.latest]的流水线执行时，默认使用测试版本。开发者可以验证插件新版本。</p>
                                    </template>
                                </bk-popover>
                            </div>
                        </div>
                        <div class="bk-form-item is-required">
                            <label class="bk-label">开发语言</label>
                            <div class="bk-form-content atom-item-content">
                                <bk-select v-model="createAtomForm.language" searchable>
                                    <bk-option v-for="(option, index) in languageList"
                                        :key="index"
                                        :id="option.language"
                                        :name="option.name"
                                        @click.native="selectedLanguage"
                                        :placeholder="'请选择开发语言'"
                                    >
                                    </bk-option>
                                </bk-select>
                                <div v-if="atomErrors.languageError" class="error-tips">开发语言不能为空</div>
                            </div>
                        </div>
                        <div class="form-footer">
                            <button class="bk-button bk-primary" type="button" @click="submitCreateAtom()">提交</button>
                            <button class="bk-button bk-default" type="button" @click="cancelCreateAtom()">取消</button>
                        </div>
                    </form>
                </template>
            </bk-sideslider>
            <bk-sideslider v-if="offlinesideConfig.show"
                class="offline-atom-slider"
                :is-show.sync="offlinesideConfig.show"
                :title="offlinesideConfig.title"
                :quick-close="offlinesideConfig.quickClose"
                :width="offlinesideConfig.width">
                <template slot="content">
                    <form class="bk-form offline-atom-form" v-bkloading="{ isLoading: offlinesideConfig.isLoading }">
                        <div class="bk-form-item">
                            <label class="bk-label">名称</label>
                            <div class="bk-form-content">
                                <p class="content-value">{{ curHandlerAtom.name }}</p>
                            </div>
                        </div>
                        <div class="bk-form-item">
                            <label class="bk-label">标识</label>
                            <div class="bk-form-content">
                                <p class="content-value">{{ curHandlerAtom.atomCode }}</p>
                            </div>
                        </div>
                        <div class="bk-form-item is-required">
                            <label class="bk-label">缓冲期</label>
                            <div class="bk-form-content">
                                <bk-select v-model="buffer" searchable>
                                    <bk-option v-for="(option, index) in bufferLength"
                                        :key="index"
                                        :id="option.value"
                                        :name="option.label"
                                        @click.native="selectedBuffer"
                                        :placeholder="'请选择缓冲期'"
                                    >
                                    </bk-option>
                                </bk-select>
                                <div v-if="atomErrors.bufferError" class="error-tips">缓冲期不能为空</div>
                            </div>
                        </div>
                        <form-tips :tips-content="offlineTips" :prompt-list="promptList"></form-tips>
                        <div class="form-footer">
                            <button class="bk-button bk-primary" type="button" @click="submitofflineAtom()">提交</button>
                        </div>
                    </form>
                </template>
            </bk-sideslider>
            <bk-sideslider v-if="templatesideConfig.show"
                class="create-atom-slider"
                :is-show.sync="templatesideConfig.show"
                :title="templatesideConfig.title"
                :quick-close="templatesideConfig.quickClose"
                :width="templatesideConfig.width">
                <template slot="content">
                    <form class="bk-form relate-template-form"
                        v-bkloading="{
                            isLoading: templatesideConfig.isLoading
                        }">
                        <div class="bk-form-item is-required">
                            <label class="bk-label">所属项目</label>
                            <div class="bk-form-content atom-item-content is-tooltips">
                                <div style="min-width: 100%">
                                    <bk-select v-model="relateTemplateForm.projectCode" searchable>
                                        <bk-option v-for="(option, index) in projectList"
                                            :key="index"
                                            :id="option.project_code"
                                            :name="option.project_name"
                                            :placeholder="'请选择项目'"
                                        >
                                        </bk-option>
                                        <div slot="extension" style="cursor: pointer;">
                                            <a :href="itemUrl" target="_blank">
                                                <i class="bk-icon icon-plus-circle" />
                                                {{ itemText }}
                                            </a>
                                        </div>
                                    </bk-select>
                                    <div v-if="templateErrors.projectError" class="error-tips">项目不能为空</div>
                                </div>
                                <bk-popover placement="right">
                                    <i class="bk-icon icon-info-circle"></i>
                                    <template slot="content">
                                        <p>源模版所属项目</p>
                                    </template>
                                </bk-popover>
                            </div>
                        </div>
                        <div class="bk-form-item is-required">
                            <label class="bk-label">模板</label>
                            <div class="bk-form-content atom-item-content">
                                <bk-select v-model="relateTemplateForm.template" searchable>
                                    <bk-option v-for="(option, index) in templateList"
                                        :key="index"
                                        :id="option.templateId"
                                        :name="option.name"
                                        :placeholder="'请选择模板'"
                                        @click.native="selectedTemplate"
                                    >
                                    </bk-option>
                                </bk-select>
                                <div v-if="templateErrors.tplError" class="error-tips">模板不能为空</div>
                            </div>
                        </div>
                        <div class="bk-form-item is-required">
                            <label class="bk-label">名称</label>
                            <div class="bk-form-content atom-item-content">
                                <input type="text" class="bk-form-input atom-name-input" placeholder="请输入发布到市场后的模板名称"
                                    name="templateName"
                                    v-model="relateTemplateForm.name"
                                    v-validate="{
                                        required: true,
                                        max: 20
                                    }"
                                    :class="{ 'is-danger': errors.has('templateName') }">
                                <p :class="errors.has('templateName') ? 'error-tips' : 'normal-tips'">{{ errors.first("templateName") }}</p>
                            </div>
                        </div>
                        <div class="form-footer">
                            <button class="bk-button bk-primary" type="button" @click="submitRelateTemplate()">提交</button>
                            <button class="bk-button bk-default" type="button" @click="cancelRelateTemplate()">取消</button>
                        </div>
                    </form>
                </template>
            </bk-sideslider>
            <bk-sideslider v-if="offlineTempConfig.show"
                class="offline-atom-slider"
                :is-show.sync="offlineTempConfig.show"
                :title="offlineTempConfig.title"
                :quick-close="offlineTempConfig.quickClose"
                :width="offlineTempConfig.width">
                <template slot="content">
                    <form class="bk-form offline-atom-form"
                        v-bkloading="{
                            isLoading: offlineTempConfig.isLoading
                        }">
                        <div class="bk-form-item">
                            <label class="bk-label">名称</label>
                            <div class="bk-form-content">
                                <p class="content-value">{{ curHandlerTemp.templateName }}</p>
                            </div>
                        </div>
                        <div class="bk-form-item">
                            <label class="bk-label">源模板</label>
                            <div class="bk-form-content">
                                <a target="_blank"
                                    style="color:#3c96ff;margin-top:7px;display:block;"
                                    :href="`/console/pipeline/${curHandlerTemp.projectCode}/template/${curHandlerTemp.templateCode}/edit`"
                                >查看</a>
                            </div>
                        </div>
                        <form-tips :tips-content="offlineTips" :prompt-list="tempPromptList"></form-tips>
                        <div class="form-footer">
                            <button class="bk-button bk-primary" type="button" @click="submitofflineTemp">提交</button>
                        </div>
                    </form>
                </template>
            </bk-sideslider>
        </div>
    </div>
</template>

<script>
    import formTips from '@/components/common/formTips/index'
    import { atomStatusMap, templateStatusList } from '@/store/constants'
    import { convertTime, getQueryString } from '@/utils/index'

    export default {
        components: {
            formTips
        },
        data () {
            return {
                isInputFocus: false,
                showContent: false,
                bufferError: false,
                isSearch: false,
                isInit: false,
                buffer: '',
                currentTab: 'atom',
                searchName: '',
                gitOAuthUrl: '',
                itemUrl: '/console/pm',
                itemText: '新建项目',
                offlineTips: '下架后：',
                atomDevelopLink: `${DOCS_URL_PREFIX}/所有服务/流水线插件Store/快速入门.html`,
                templateLink: `${DOCS_URL_PREFIX}/所有服务/流水线模版/summary.html`,
                atomCounts: 0,
                renderList: [],
                templateList: [],
                languageList: [
                    { language: 'java', name: 'java' },
                    { language: 'python', name: 'python' }
                ],
                promptList: [
                    '1、插件市场不再展示插件',
                    '2、已安装插件的项目不能再添加插件到流水线',
                    '3、已使用插件的流水线可以继续使用，但有插件已下架标识'
                ],
                tempPromptList: [
                    '1、不再在模版市场中展示',
                    '2、已使用模版的流水线可以继续使用，但有模版已下架标识'
                ],
                curHandlerAtom: {},
                curHandlerTemp: {},
                bufferLength: [
                    { label: '0天', value: '0' },
                    { label: '7天', value: '7' },
                    { label: '15天', value: '15' }
                ],
                createAtomForm: {
                    projectCode: '',
                    atomCode: '',
                    name: '',
                    language: ''
                },
                relateTemplateForm: {
                    projectCode: '',
                    template: '',
                    name: ''
                },
                loading: {
                    isLoading: false,
                    title: ''
                },
                atomErrors: {
                    projectError: false,
                    languageError: false,
                    bufferError: false,
                    openSourceError: false
                },
                templateErrors: {
                    projectError: false,
                    tplError: false
                },
                createAtomsideConfig: {
                    show: false,
                    isLoading: false,
                    quickClose: false,
                    width: 565,
                    title: '新增插件'
                },
                offlinesideConfig: {
                    show: false,
                    isLoading: false,
                    title: '下架插件',
                    quickClose: false,
                    width: 565
                },
                offlineTempConfig: {
                    show: false,
                    isLoading: false,
                    title: '下架模板',
                    quickClose: false,
                    width: 565
                },
                templatesideConfig: {
                    show: false,
                    isLoading: false,
                    quickClose: false,
                    width: 565,
                    title: '关联模板到Store'
                },
                statusList: {
                    publish: '已发布',
                    commiting: '提交中',
                    fail: '上架失败',
                    testing: '测试中',
                    auditing: '审核中',
                    obtained: '已下架',
                    draft: '草稿'
                },
                pagination: {
                    current: 1,
                    count: 1,
                    limit: 10
                },
                emptyAtomTipsConfig: {
                    title: '暂时没有插件',
                    desc: '可以创建你的第一个插件',
                    btns: [
                        {
                            type: 'primary',
                            size: 'normal',
                            handler: () => this.createNewAtom(),
                            text: '新增插件'
                        }
                    ]
                },
                emptyTempTipsConfig: {
                    title: '暂时没有模板',
                    desc: '可以创建你的第一个模板',
                    btns: [
                        {
                            type: 'primary',
                            size: 'normal',
                            handler: () => this.relateTemplate(),
                            text: '关联模板'
                        }
                    ]
                }
            }
        },
        computed: {
            listType () {
                return this.$route.params.type
            },
            atomStatusList () {
                return atomStatusMap
            },
            templateStatusMap () {
                return templateStatusList
            },
            projectList () {
                return window.projectList
            },
            userInfo () {
                return window.userInfo
            },
            isAtomTab () {
                return this.currentTab === 'atom'
            }
        },
        watch: {
            'createAtomsideConfig.show' (val) {
                if (!val) {
                    this.atomErrors.projectError = false
                    this.atomErrors.languageError = false
                    this.createAtomForm = {
                        projectCode: '',
                        atomCode: '',
                        name: '',
                        language: ''
                    }
                }
            },
            'offlinesideConfig.show' (val) {
                if (!val) {
                    this.atomErrors.bufferError = false
                    this.buffer = ''
                }
            },
            'relateTemplateForm.projectCode' (newVal, oldVal) {
                if (newVal) {
                    this.selectedTplProject()
                }
            }
        },
        async mounted () {
            this.isInit = true
            this.currentTab = this.listType
            if (getQueryString('projectCode') && getQueryString('templateId')) {
                this.relateTemplateForm.projectCode = getQueryString('projectCode')
                this.relateTemplateForm.template = getQueryString('templateId')
                this.templatesideConfig.show = true
                this.currentTab = 'template'
            }
            this.requestList()
        },
        methods: {
            clearSearch () {
                this.searchName = ''
                this.requestList()
            },

            timeFormatter (row, column, cellValue, index) {
                const date = new Date(cellValue)
                const year = date.toISOString().slice(0, 10)
                const time = date.toTimeString().split(' ')[0]
                return `${year} ${time}`
            },

            deleteTemplate (row) {
                this.loading.isLoading = true
                let message = '移除成功'
                let theme = 'success'

                this.$store.dispatch('store/deleteTemplate', row.templateCode).then((res) => {
                    this.requestList()
                }).catch((err) => {
                    message = err.message || err
                    theme = 'error'
                }).finally(() => {
                    this.$bkMessage({ message, theme })
                    this.loading.isLoading = false
                })
            },

            async requestList () {
                this.showContent = false
                this.loading.isLoading = true
                this.loading.title = '数据加载中，请稍候'

                const page = this.pagination.current
                const pageSize = this.pagination.limit

                try {
                    let res
                    if (this.isAtomTab) {
                        res = await this.$store.dispatch('store/requestAtomList', {
                            atomName: this.searchName,
                            page,
                            pageSize
                        })
                    } else {
                        res = await this.$store.dispatch('store/requestTemplateList', {
                            templateName: this.searchName,
                            page,
                            pageSize
                        })
                    }

                    this.renderList.splice(0, this.renderList.length, ...(res.records || []))
                    if (this.renderList.length) {
                        this.pagination.count = res.count
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.showContent = true
                    this.loading.isLoading = false
                    this.isInit = false
                }
            },
            async pageCountChanged (currentLimit, prevLimit) {
                if (currentLimit === this.pagination.limit) return

                this.pagination.current = 1
                this.pagination.limit = currentLimit
                await this.requestList()
            },
            async pageChanged (page) {
                this.pagination.current = page
                await this.requestList()
            },
            search () {
                this.isSearch = true
                this.pagination.current = 1
                this.requestList()
            },
            checkValid () {
                let errorCount = 0
                if (!this.createAtomForm.projectCode) {
                    this.atomErrors.projectError = true
                    errorCount++
                }
                if (!this.createAtomForm.language) {
                    this.atomErrors.languageError = true
                    errorCount++
                }

                if (errorCount > 0) {
                    return false
                }

                return true
            },
            async submitCreateAtom () {
                const isCheckValid = this.checkValid()
                const valid = await this.$validator.validate()
                if (isCheckValid && valid) {
                    let message, theme
                    const params = Object.assign(this.createAtomForm, {})

                    this.createAtomsideConfig.isLoading = true
                    try {
                        await this.$store.dispatch('store/createNewAtom', {
                            params: params
                        })

                        message = '新增成功'
                        theme = 'success'
                        this.cancelCreateAtom()
                        this.routerAtoms(this.createAtomForm.atomCode)
                    } catch (err) {
                        message = err.message ? err.message : err
                        theme = 'error'
                    } finally {
                        this.$bkMessage({
                            message,
                            theme
                        })
                        this.createAtomsideConfig.isLoading = false
                        this.requestList()
                        if (theme === 'success') {
                            this.cancelCreateAtom()
                        }
                    }
                }
            },
            async submitofflineAtom () {
                if (this.buffer === '') {
                    this.atomErrors.bufferError = true
                } else {
                    let message, theme
                    const params = {
                        bufferDay: this.buffer
                    }
                    
                    this.offlinesideConfig.isLoading = true
                    try {
                        await this.$store.dispatch('store/offlineAtom', {
                            atomCode: this.curHandlerAtom.atomCode,
                            params: params
                        })

                        message = '提交成功'
                        theme = 'success'
                        this.offlinesideConfig.show = false
                        this.requestList()
                    } catch (err) {
                        message = err.message ? err.message : err
                        theme = 'error'
                    } finally {
                        this.$bkMessage({
                            message,
                            theme
                        })

                        this.offlinesideConfig.isLoading = false
                    }
                }
            },
            async submitofflineTemp () {
                let message, theme

                this.offlineTempConfig.isLoading = true
                try {
                    await this.$store.dispatch('store/offlineTemplate', {
                        templateCode: this.curHandlerTemp.templateCode
                    })

                    message = '下架成功'
                    theme = 'success'
                    this.offlineTempConfig.show = false
                    this.requestList()
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })

                    this.offlineTempConfig.isLoading = false
                }
            },
            checkTplValid () {
                let errorCount = 0
                if (!this.relateTemplateForm.projectCode) {
                    this.templateErrors.projectError = true
                    errorCount++
                }
                if (!this.relateTemplateForm.template) {
                    this.templateErrors.tplError = true
                    errorCount++
                }

                if (errorCount > 0) {
                    return false
                }

                return true
            },
            async submitRelateTemplate () {
                const isCheckValid = this.checkTplValid()
                const valid = await this.$validator.validate()
                if (isCheckValid && valid) {
                    let message, theme
                    const params = {
                        projectCode: this.relateTemplateForm.projectCode,
                        templateName: this.relateTemplateForm.name
                    }

                    this.templatesideConfig.isLoading = true
                    try {
                        await this.$store.dispatch('store/relateTemplate', {
                            templateCode: this.relateTemplateForm.template,
                            params
                        })

                        message = '关联成功'
                        theme = 'success'
                        this.cancelRelateTemplate()
                    } catch (err) {
                        message = err.message ? err.message : err
                        theme = 'error'
                    } finally {
                        this.$bkMessage({
                            message,
                            theme
                        })
                        this.templatesideConfig.isLoading = false
                        this.requestList()
                    }
                }
            },
            cancelRelateTemplate () {
                this.templatesideConfig.show = false
                this.templateErrors.projectError = false
                this.templateErrors.tplError = false
                this.relateTemplateForm = {
                    projectCode: '',
                    template: '',
                    name: ''
                }
            },
            changeTab (tab) {
                if (!this.isInit) {
                    this.searchName = ''
                    this.currentTab = tab
                    this.isSearch = false
                    this.pagination.current = 1
                    this.pagination.limit = 10
                    this.renderList.splice(0, this.renderList.length)
                    this.$router.push({ name: 'atomList', params: { type: tab } })
                    this.requestList()
                }
            },
            selectedProject () {
                this.atomErrors.projectError = false
            },
            async selectedTplProject () {
                this.templateErrors.projectError = false
                try {
                    const res = await this.$store.dispatch('store/requestPipelineTemplate', {
                        projectCode: this.relateTemplateForm.projectCode
                    })
                    this.templateList.splice(0, this.templateList.length, ...res.models || [])
                } catch (err) {
                    this.$bkMessage({
                        message: err.message ? err.message : err,
                        theme: 'error'
                    })
                }
            },
            selectedLanguage () {
                this.atomErrors.languageError = false
            },
            selectedTemplate () {
                this.templateErrors.tplError = false
            },
            selectedBuffer () {
                this.atomErrors.bufferError = false
            },
            cancelCreateAtom () {
                this.createAtomsideConfig.show = false
            },
            routerAtoms (code) {
                const params = {}
                if (this.isAtomTab) {
                    params.atomCode = code
                } else {
                    params.templateCode = code
                }
                this.$router.push({
                    name: this.isAtomTab ? 'overview' : 'tplOverview',
                    params
                })
            },
            routerProgress (id) {
                this.$router.push({
                    name: 'releaseProgress',
                    params: {
                        releaseType: 'shelf',
                        atomId: id
                    }
                })
            },
            toTemplateProgress (id) {
                this.$router.push({
                    name: 'upgradeTemplate',
                    params: {
                        templateId: id
                    }
                })
            },
            toAtomStore () {
                this.$router.push({
                    name: 'atomHome'
                })
            },
            openValidate () {
                window.open(this.gitOAuthUrl, '_self')
            },
            createNewAtom () {
                this.createAtomsideConfig.show = true
            },
            relateTemplate () {
                this.templatesideConfig.show = true
            },
            offline (type, form) {
                if (type === 'atom') {
                    this.offlinesideConfig.show = true
                    this.curHandlerAtom = form
                } else {
                    this.offlineTempConfig.show = true
                    this.curHandlerTemp = form
                }
            },
            offlineTemplate () {

            },
            installAHandle (type, code) {
                const params = {}
                const name = type === 'atom' ? 'installAtom' : 'installTemplate'
                type === 'atom' ? params.atomCode = code : params.templateCode = code
                this.$router.push({
                    name,
                    params,
                    hash: '#MYATOM'
                })
            },
            editHandle (routerName, type, id) {
                const params = {}
                type === 'atom' ? params.atomId = id : params.templateId = id
                this.$router.push({
                    name: routerName,
                    params
                })
            },
            localTime (time) {
                return convertTime(time)
            },
            async requestDeleteAtom (atomId) {
                let message, theme
                try {
                    await this.$store.dispatch('store/requestDeleteAtom', {
                        atomId: atomId
                    })

                    message = '删除成功'
                    theme = 'success'
                    this.requestList()
                } catch (err) {
                    message = message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            deleteAtom (atom) {
                const h = this.$createElement
                const content = h('p', {
                    style: {
                        textAlign: 'center'
                    }
                }, `确定删除插件(${atom.name})？`)

                this.$bkInfo({
                    title: `删除`,
                    content,
                    confirmFn: async () => {
                        // this.requestDeleteAtom(atom.atomId)
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
    @import '@/assets/scss/conf.scss';
    
    .atom-list-wrapper {
        height: 100%;
        .atomstore-list-content {
            padding: 8px 25px 25px;
            height: calc(100% - 50px);
            overflow: auto;
            .bk-page {
                padding: 0 20px;
            }
            .bk-tab-section {
                padding-bottom: 0px;
            }
            .bk-tab.bk-tab-unborder-card li.bk-tab-label-item {
                background-color: transparent;
            }
        }
        .atomstore-list-main {
            height: 100%;
        }
        .bk-tab {
            margin-bottom: 25px;
        }
        .bk-tab2 {
            border: none;
            background: transparent;
        }
        .bk-tab2-head {
            height: 42px;
        }
        .bk-tab2 .bk-tab2-nav .tab2-nav-item {
            height: 42px;
            padding: 0 16px;
            line-height: 42px;
            .icon-tab {
                position: relative;
                top: 3px;
                font-size: 18px;
            }
        }
        .content-header {
            display: flex;
            .list-input {
                margin: 0;
                width: 240px;
                height: 36px;
                > input {
                    width: 240px;
                    height: 36px;
                }
                i {
                    top: 11px;
                }
            }
            .bk-button {
                padding: 0 15px;
                margin-right: 20px;
            }
        }
        .bk-tab2-content {
            padding-top: 20px;
        }
        .render-table {
            min-width: 1180px;
            margin: 20px 0;
            border: 1px solid $borderWeightColor;
        }
        .create-atom-slider,
        .offline-atom-slider {
            .bk-sideslider-content {
                height: calc(100% - 90px);
                .bk-form-content .bk-tooltip {
                    color: #63656e;
                }
            }
            .create-atom-form,
            .offline-atom-form,
            .relate-template-form {
                margin: 30px 50px 20px 28px;
                height: 100%;
            }
            .bk-label {
                width: 97px;
                font-weight: normal;
            }
            .bk-form-content {
                margin-left: 97px;
            }
            .bk-selector {
                min-width: 100%;
            }
            .is-tooltips {
                display: flex;
            }
            .bk-tooltip {
                margin-top: 10px;
                margin-left: 10px;
                color: $fontLigtherColor;
                p {
                    max-width: 250px;
                    text-align: left;
                    white-space: normal;
                    word-break: break-all;
                    font-weight: 400;
                }
            }
            .is-open {
                margin-top: 15px;
            }
            .atom-tip {
                margin-top: 16px;
            }
            .prompt-oauth {
                margin-top: 16px;
                i {
                    position: relative;
                    top: 2px;
                    margin-right: 4px;
                    font-size: 16px;
                    color: #FCB728;
                }
                span {
                    color: $fontWeightColor;
                }
            }
            .form-footer {
                margin-top: 26px;
                margin-left: 97px;
                button {
                    height: 32px;
                    line-height: 30px;
                }
            }
            .form-tips {
                margin-left: 21px;
            }
        }
        .offline-atom-slider {
            .offline-atom-form {
                margin: 30px 46px 20px 28px;
            }
            .content-value {
                position: relative;
                top: 7px;
                color: #333C48;
            }
            .is-required {
                margin-top: 32px;
            }
            .prompt-offline {
                margin-left: 97px;
                margin-top: 20px;
                i {
                    position: relative;
                    top: 2px;
                    margin-right: 2px;
                    font-size: 16px;
                    color: #FCB728;
                }
                span {
                    color: $fontWeightColor;
                }
                .prompt-line {
                    margin-left: 22px;
                }
            }
            .form-tips {
                margin-top: 24px;
                margin-left: 34px;
            }
        }
    }
</style>
