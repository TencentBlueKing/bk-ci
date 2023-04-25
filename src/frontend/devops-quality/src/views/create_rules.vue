<template>
    <div class="create-rule-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
        <div class="info-header">
            <div class="title">
                <!-- <i class="devops-icon icon-arrows-left" @click="toRuleList"></i> -->
                <span class="header-text">{{title}}</span>
            </div>
        </div>
        <div class="create-rule-content" v-if="hasPermission && showContent">
            <div class="fast-create">
                <div class="info-title">{{$t('quality.快捷创建规则')}}</div>
                <ul class="rule-types-container">
                    <li
                        :class="{ 'type-item-card': true, 'active-item': curFastType === entry.hashId }"
                        :title="entry.name"
                        v-for="(entry, index) in fastTypeRuleList"
                        :key="index"
                        @click="changeFastType(entry)"
                    >
                        <div class="template-icon" v-if="templateName.includes(entry.name)">
                            <icon v-if="entry.name === '日常构建'" name="daily-build" size="14" />
                            <icon v-if="entry.name === '版本转测'" name="version-test" size="14" />
                            <icon v-if="entry.name === '发布上线'" name="release-online" size="14" />
                        </div>
                        <i :class="{ 'devops-icon': true, 'label-icon': true, 'icon-placeholder': true }" v-else></i>
                        <span class="card-label">{{entry.name}}</span>
                        <i class="devops-icon icon-check-circle-shape" v-if="curFastType === entry.hashId"></i>
                    </li>
                </ul>
            </div>
            <div class="create-steps-rule">
                <div class="create-rule-form">
                    <p class="info-title">{{$t('quality.基本信息')}}</p>
                    <hr>
                    <bk-form :label-width="100" :model="createRuleForm">
                        <devops-form-item :label="$t('quality.名称')" :required="true" :property="'name'" :is-error="errors.has('ruleName')" :error-msg="errors.first('ruleName')">
                            <bk-input
                                class="rule-name-input"
                                :placeholder="$t('quality.请输入规则名称')"
                                name="ruleName"
                                v-model="createRuleForm.name"
                                v-validate="'required'">
                            </bk-input>
                        </devops-form-item>
                        <bk-form-item :label="$t('quality.描述')" :property="'desc'">
                            <bk-input
                                type="text"
                                class="rule-desc-input"
                                :placeholder="$t('quality.请输入描述')"
                                name="ruleDesc"
                                v-model="createRuleForm.desc">
                            </bk-input>
                        </bk-form-item>
                    </bk-form>
                    <p class="info-title" style="margin-top: 28px;">{{$t('quality.指标')}}
                        <bk-popover placement="right">
                            <i class="devops-icon icon-info-circle"></i>
                            <template slot="content">
                                <p style="width: 200px; text-align: left; white-space: normal;word-break: break-all;font-weight: 400;">{{$t('quality.选择关注的指标并设置范围，不满足的流水线将会在控制点位置停住')}}</p>
                            </template>
                        </bk-popover>
                        <a class="add-indicator" target="_blank" :href="`/console/quality/${projectId}/metadataList`">{{$t('quality.缺少需要的指标？')}}</a>
                    </p>
                    <hr>
                    <table class="rule-metadata-table" v-if="createRuleForm.indicators.length"
                        style="border-collapse:separate; border-spacing:0px 10px;">
                        <tr>
                            <th width="323">{{$t('quality.指标名称')}}</th>
                            <th width="118">{{$t('quality.操作')}}</th>
                            <th width="118">{{$t('quality.阈值')}}</th>
                            <th width="100"></th>
                        </tr>
                        <tr v-for="(row, index) of createRuleForm.indicators"
                            :key="index">
                            <td class="indicator-item">
                                <div class="metadata-name" :title="getIndicatorName(row)">{{row.cnName}}
                                    <span v-if="row.type === 'CUSTOM'">({{row.enName}})</span>
                                </div>
                            </td>
                            <td class="handler-item" style="border-left: none;">
                                <bk-select
                                    v-model="row.operation"
                                    :disabled="row.readOnly">
                                    <bk-option v-for="(option, optionIndex) in row.operationList"
                                        :key="optionIndex"
                                        :id="option.value"
                                        :name="option.label">
                                    </bk-option>
                                </bk-select>
                            </td>
                            <td class="threshold-item" style="border-left: none;">
                                <bk-select
                                    v-model="row.threshold"
                                    :disabled="row.readOnly"
                                    v-if="row.thresholdType === 'BOOLEAN'">
                                    <bk-option v-for="(option, optionIndex) in optionBoolean"
                                        :key="optionIndex"
                                        :id="option.value"
                                        :name="option.label">
                                    </bk-option>
                                </bk-select>
                                <input
                                    v-if="row.thresholdType === 'INT'"
                                    type="number"
                                    class="bk-form-input"
                                    :disabled="row.readOnly"
                                    v-model="row.threshold"
                                    onkeypress="return(/[\d]/.test(String.fromCharCode(event.keyCode)))"
                                />
                                <input
                                    v-if="row.thresholdType === 'FLOAT'"
                                    type="number"
                                    class="bk-form-input"
                                    :disabled="row.readOnly"
                                    v-model="row.threshold"
                                    onkeypress="return(/[\d\.]/.test(String.fromCharCode(event.keyCode)))"
                                />
                                <input
                                    v-if="row.thresholdType === 'STRING'"
                                    type="text"
                                    class="bk-form-input"
                                    :disabled="row.readOnly"
                                    v-model="row.threshold"
                                />
                            </td>
                            <td>
                                <i class="devops-icon icon-plus-circle-shape"
                                    @click="selectMetadata(index)"></i>
                                <i class="devops-icon icon-minus-circle-shape"
                                    @click="reduceMetadata(row, index)"></i>
                            </td>
                        </tr>
                    </table>
                    <p class="error-tips open-tips" v-if="includeOpenINdicator">{{$t('quality.腾讯开源规范指标不支持修改')}}</p>
                    <div class="no-metadata-row" v-if="!createRuleForm.indicators.length">
                        {{$t('quality.还未添加指标，')}}
                        <span @click="selectMetadata(0)">{{$t('quality.立即添加')}}</span>
                    </div>
                    <p class="info-title" style="margin-top: 28px;">{{$t('quality.控制点')}}
                        <bk-popover placement="right">
                            <i class="devops-icon icon-info-circle"></i>
                            <template slot="content">
                                <p style="width: 200px; text-align: left; white-space: normal;word-break: break-all;font-weight: 400;">{{$t('quality.控制点插件需要满足全部指标条件才能顺利执行')}}</p>
                            </template>
                        </bk-popover>
                    </p>
                    <hr>

                    <bk-form :label-width="100" :model="createRuleForm">
                        <devops-form-item :label="$t('quality.控制点名称')" :required="true"
                            :property="'controlPointName'"
                            :is-error="errors.has('controlPoint')"
                            :error-msg="errors.first('controlPoint')">
                            <bk-input
                                class="control-point-name"
                                placeholder=""
                                disabled
                                name="controlPoint"
                                v-model="createRuleForm.controlPointName"
                                v-validate="'required'">
                            </bk-input>
                            <span class="select-control-point" @click="selectControlPoint()">{{$t('quality.选择控制点')}}</span>
                        </devops-form-item>
                        <bk-form-item :label="$t('quality.红线位置')" :required="true"
                            :property="'controlPointPosition'"
                            :is-error="errors.has('controlPointPosition')"
                            :error-msg="errors.first('controlPointPosition')">
                            <bk-select v-model="createRuleForm.controlPointPosition" style="width:467px;">
                                <bk-option v-for="(option, index) in createRuleForm.availablePosition"
                                    :key="index"
                                    :id="option.name"
                                    :name="option.cnName">
                                </bk-option>
                            </bk-select>
                        </bk-form-item>
                        <devops-form-item :label="$t('quality.控制点前缀')" :property="'id'" :is-error="errors.has('gatewayId')" :error-msg="errors.first('gatewayId')">
                            <bk-input
                                class="rule-name-input"
                                :placeholder="$t('quality.默认可不填，不填则对所有控制点生效。仅支持英文和数字，例如gate1')"
                                name="gatewayId"
                                v-model="createRuleForm.gatewayId"
                                v-validate="{
                                    max: 10,
                                    customRuleId: true
                                }">
                            </bk-input>
                        </devops-form-item>
                        <p class="gateway-id-tips">
                            <i class="devops-icon icon-info-circle-shape"></i>
                            <span>{{$t('quality.若输入了前缀（例如gate1），红线将只对名称以前缀加下划线开头的控制点生效（例如gate1_XX）')}}</span>
                        </p>
                        <bk-form-item :label="$t('quality.生效范围')" class="blod-label"
                            :property="'controlPointPosition'">
                            <bk-dropdown-menu @show="isDropdownShow = true" @hide="isDropdownShow = false" ref="dropdown">
                                <bk-button type="primary" slot="dropdown-trigger">
                                    <i class="devops-icon icon-plus select-effect-btn"></i><span>{{$t('quality.选择流水线')}}</span>
                                    <i :class="['devops-icon icon-angle-down',{ 'icon-flip': isDropdownShow }]"></i>
                                </bk-button>
                                <ul class="bk-dropdown-list" slot="dropdown-content">
                                    <li><a href="javascript:;" @click="triggerHandler('pipeline')">{{$t('quality.单流水线')}}</a></li>
                                    <li><a href="javascript:;" @click="triggerHandler('template')">{{$t('quality.模板')}}</a></li>
                                </ul>
                            </bk-dropdown-menu>
                            <div class="pipeline-table-container">
                                <bk-table
                                    size="small"
                                    class="effect-pipeline-table"
                                    :outer-border="false"
                                    :data="createRuleForm.pipelineList"
                                    v-bkloading="{
                                        isLoading: tableLoading
                                    }">
                                    <bk-table-column :label="$t('quality.名称')" width="200">
                                        <template slot-scope="props">
                                            <span>{{props.row.type === 'pipeline' ? props.row.pipelineName : props.row.templateName}}</span>
                                        </template>
                                    </bk-table-column>
                                    <bk-table-column :label="$t('quality.类型')" prop="elementCount">
                                        <template slot-scope="props">
                                            <span>{{props.row.type === 'pipeline' ? $t('quality.单流水线') : $t('quality.模板')}}</span>
                                        </template>
                                    </bk-table-column>
                                    <bk-table-column :label="$t('quality.相关插件')" min-width="260">
                                        <template slot-scope="props">
                                            <p v-if="props.row.lackPointElement.length" class="atom-tips" :title="$t('quality.缺少指标所需的{0}插件', [getPipelineStatus(props.row.lackPointElement)])">
                                                <span class="mark-circle"></span>
                                                {{$t('quality.缺少指标所需的{0}插件', [getPipelineStatus(props.row.lackPointElement)])}}
                                            </p>
                                            <p
                                                v-if="checkAtomAsync(props.row.existElement)"
                                                class="atom-tips"
                                                :title="$t('quality.{0}插件不是同步，无法及时获取产出结果', [getAsyncAtom(props.row.existElement)])"
                                            >
                                                <span class="mark-circle"></span>
                                                {{$t('quality.{0}插件不是同步，无法及时获取产出结果', [getAsyncAtom(props.row.existElement)])}}
                                            </p><p style="color: #00C873"
                                                v-if="!props.row.lackPointElement.length && !checkAtomAsync(props.row.existElement) && !props.row.isSetPipeline">
                                                {{$t('quality.指标所需插件完整')}}
                                            </p>
                                            <p class="atom-tips" v-if="checkAtomCount(props.row.existElement)">
                                                <span class="mark-circle"></span>{{$t('quality.有多个控制点插件。请将需配置红线的控制点插件别名开头加上控制点前缀+下划线')}}
                                            </p>
                                            <p v-if="props.row.isSetPipeline">-</p>
                                        </template>
                                    </bk-table-column>
                                    <bk-table-column :label="$t('quality.操作建议')">
                                        <template slot-scope="props">
                                            <a class="add-btn"
                                                v-if="props.row.type === 'pipeline' && (props.row.lackPointElement.length || checkAtomAsync(props.row.existElement) || checkAtomCount(props.row.existElement)) && !props.row.isRefresh"
                                                target="_blank"
                                                :href="`/console/pipeline/${projectId}/${props.row.pipelineId}/edit`"
                                                @click="updatePipelineStatus(props.row.pipelineId)"
                                            >{{$t('quality.去修改')}}</a>
                                            <a class="add-btn"
                                                v-else-if="props.row.type === 'template' && (props.row.lackPointElement.length || checkAtomAsync(props.row.existElement) || checkAtomCount(props.row.existElement)) && !props.row.isRefresh"
                                                target="_blank"
                                                :href="`/console/pipeline/${projectId}/template/${props.row.templateId}/edit`"
                                                @click="updateTemplateStatus(props.row.templateId)"
                                            >{{$t('quality.去修改')}}</a>
                                            <span v-else-if="props.row.isRefresh" class="add-btn" @click="handleRefresh">{{$t('quality.刷新')}}</span>
                                            <span v-else>-</span>
                                        </template>
                                    </bk-table-column>
                                    <template slot="empty">
                                        <div class="no-data">{{$t('quality.未选择流水线')}}</div>
                                    </template>
                                </bk-table>
                            </div>
                        </bk-form-item>
                        <bk-form-item :label="$t('quality.操作')" class="blod-label">
                            <div class="rule-item-content notice-type-content">
                                <bk-radio-group v-model="createRuleForm.operation">
                                    <bk-radio :value="'END'">{{$t('quality.终止后通知')}}</bk-radio>
                                    <bk-radio :value="'AUDIT'">{{$t('quality.人工审核')}}</bk-radio>
                                </bk-radio-group>
                                <div class="selected-item-tooltips notice-item-tooltips"
                                    :class="{ 'system-active': createRuleForm.operation === 'END' }">
                                    <bk-form :label-width="120" :model="createRuleForm" v-if="createRuleForm.operation === 'END'">
                                        <bk-form-item :label="$t('quality.通知方式')" :required="true" class="notice-type-item">
                                            <bk-checkbox-group v-model="createRuleForm.notifyTypeList">
                                                <bk-checkbox :value="entry.value" v-for="(entry, index) in noticeTypeList" :key="index">
                                                    <logo :name="entry.name" size="30" class="nav-icon" />
                                                </bk-checkbox>
                                            </bk-checkbox-group>
                                        </bk-form-item>
                                        <bk-form-item :label="$t('quality.发送通知到')" :property="'desc'" class="notice-group-item">
                                            <bk-checkbox-group v-model="createRuleForm.notifyGroupList">
                                                <bk-checkbox :value="col.groupHashId" v-for="(col, index) in groupList" :key="index">
                                                    <span class="notice-name" :title="col.name">{{col.name}}</span>
                                                    <bk-popover :delay="500" placement="top">
                                                        <i class="devops-icon icon-member-list"></i>
                                                        <template slot="content">
                                                            <p style="max-width: 300px; text-align: left; white-space: normal;word-break: break-all;font-weight: 400;">
                                                                <span v-for="(entry, opIndex) in col.innerUsers" :key="opIndex">{{entry.replace('"', '')}}<span v-if="opIndex !== (col.innerUsers.length - 1)">,</span></span>
                                                            </p>
                                                        </template>
                                                    </bk-popover>
                                                </bk-checkbox>
                                                <bk-checkbox class="add-group" :disabled="true" @click.native="toCreateGroup">
                                                    <i class="devops-icon icon-plus-circle"></i>
                                                    <span class="bk-checkbox-text create-group">{{$t('quality.新增通知组')}}</span>
                                                </bk-checkbox>
                                            </bk-checkbox-group>
                                        </bk-form-item>
                                        <bk-form-item :label="$t('quality.附加通知人员')" :desc="$t('quality.请输入通知人员，支持输入流水线变量，默认发给流水线触发人')">
                                            <user-input :handle-change="handleChange" name="attacher" :value="createRuleForm.notifyUserList" :placeholder="$t('quality.请输入通知人员，支持输入流水线变量，默认发给流水线触发人')"></user-input>
                                        </bk-form-item>
                                    </bk-form>

                                    <bk-form v-else :label-width="120" :model="createRuleForm" class="user-audit-form">
                                        <bk-form-item :label="$t('quality.审核人')" :desc="$t('quality.请输入通知人员，支持输入流水线变量，默认发给流水线触发人')" :required="true">
                                            <user-input :handle-change="handleChange" name="reviewer" :value="createRuleForm.auditUserList" :placeholder="$t('quality.请输入通知人员，支持输入流水线变量，默认发给流水线触发人')"></user-input>
                                        </bk-form-item>
                                        <bk-form-item :label="$t('quality.审核超时时间')">
                                            <bk-input type="number"
                                                :placeholder="$t('quality.可设置60分钟以内的超时时间，以分钟为单位')"
                                                v-model="createRuleForm.auditTimeoutMinutes">
                                            </bk-input>
                                            <span class="time-unit">{{$t('quality.分钟')}}</span>
                                            <p class="prompt-tips">{{$t('quality.默认为15分钟，最长不超过60分钟')}}</p>
                                        </bk-form-item>
                                    </bk-form>
                                </div>
                            </div>
                        </bk-form-item>
                    </bk-form>
                    <div>
                        <bk-button theme="primary" class="submit-handle" @click="submit()">{{$t('quality.完成')}}</bk-button>
                        <bk-button theme="default" class="submit-handle" @click="toRuleList()">{{$t('quality.取消')}}</bk-button>
                    </div>
                </div>
                <div class="rule-preview">
                    <p class="info-title">{{$t('quality.红线预览')}}</p>
                    <hr>
                    <p class="priview-tips" v-if="createRuleForm.controlPointPosition === 'AFTER'">
                        {{$t('quality.流水线在执行控制点')}} <i>{{createRuleForm.controlPointName || '-'}}</i> {{$t('quality.之后需满足')}} <i>{{currentINdicators || '-'}}</i> {{$t('quality.的阈值条件，否则将不会执行后续插件。')}}
                    </p>
                    <p class="priview-tips" v-else>
                        {{$t('quality.流水线在执行控制点')}} <i>{{createRuleForm.controlPointName || '-'}}</i> {{$t('quality.之前需满足')}} <i>{{currentINdicators || '-'}}</i> {{$t('quality.的阈值条件，否则将会停在红线位置。')}}
                    </p>
                    <div class="preview-image">
                        <img v-if="createRuleForm.controlPointPosition"
                            :src="createRuleForm.controlPointPosition === 'BEFORE' ? beforeSiteImg : afterSiteImg">
                    </div>
                </div>
            </div>
        </div>

        <bk-sideslider
            class="metadata-side-slider"
            :is-show.sync="sideSliderConfig.show"
            :quick-close="sideSliderConfig.quickClose"
            :width="sideSliderConfig.width">
            <header class="metadata-panel-header" slot="header">
                <span>{{sideSliderConfig.title}}</span>
                <div class="search-input-row" :class="{ 'crtl-point-panel': isCtrPointPanel }">
                    <input class="bk-form-input" type="text"
                        v-model="searchKey"
                        @keyup.enter="handleSearch()">
                    <i class="bk-icon icon-search" @click="handleSearch()"></i>
                </div>
            </header>
            <template slot="content">
                <div style="width: 100%; height: 100%"
                    v-bkloading="{
                        isLoading: sideSliderConfig.loading.isLoading,
                        title: sideSliderConfig.loading.title
                    }">
                    <metadata-panel
                        ref="metadataPanel"
                        :is-panel-show="sideSliderConfig.show"
                        :panel-type="panelType"
                        :search-key="searchKey"
                        :selected-atom="createRuleForm.controlPoint"
                        :selected-meta="createRuleForm.indicators"
                        @comfireHandle="handleMetadata"
                    >
                    </metadata-panel>
                </div>

            </template>
        </bk-sideslider>

        <empty-tips
            v-if="!hasPermission && showContent"
            :title="emptyTipsConfig.title"
            :desc="emptyTipsConfig.desc"
            :btns="emptyTipsConfig.btns"
        >
        </empty-tips>

        <pipeline-list
            :is-show="showPipelineList"
            :selected-pielines="selectedPipelines"
            @comfire="comfirePipelineList"
            @close="closePipelineList"
        >
        </pipeline-list>

        <TemplateList
            :is-show="showTemplateList"
            :selected-templates="selectedTemplates"
            @comfire="comfireTemplateList"
            @close="closeTemplateList"
        >
        </TemplateList>

        <createGroup
            :node-select-conf="nodeSelectConf"
            :create-group-form="createGroupForm"
            :loading="dialogLoading"
            :on-change="handleChange"
            :error-handler="errorHandler"
            @confirmFn="confirmFn"
            :cancel-fn="cancelFn"
        >
        </createGroup>
    </div>
</template>

<script>
    import UserInput from '@/components/devops/UserInput/index.vue'
    import createGroup from '@/components/devops/create_group'
    import emptyTips from '@/components/devops/emptyTips'
    import metadataPanel from '@/components/devops/metadata-panel'
    import pipelineList from '@/components/devops/pipeline-list'
    import TemplateList from '@/components/devops/template-list'
    import i18nImages from '@/utils/i18nImages'
    import { getQueryString } from '@/utils/util'
    import { mapGetters } from 'vuex'

    export default {
        components: {
            createGroup,
            pipelineList,
            TemplateList,
            metadataPanel,
            UserInput,
            emptyTips
        },
        data () {
            return {
                hasPermission: true,
                showContent: false,
                showPipelineList: false,
                showTemplateList: false,
                tableLoading: false,
                isDropdownShow: false,
                isInit: false,
                searchKey: '',
                panelType: '',
                curFastType: '',
                lastClickCount: '',
                title: this.$t('quality.创建红线规则'),
                beforeSiteImg: i18nImages.beforeSiteImage[this.$i18n.locale],
                afterSiteImg: i18nImages.afterSiteImage[this.$i18n.locale],
                templateName: [this.$t('quality.日常构建'), this.$t('quality.版本转测'), this.$t('quality.发布上线')],
                optionBoolean: [
                    { label: this.$t('quality.是'), value: 'true' },
                    { label: this.$t('quality.否'), value: 'false' }
                ],
                groupList: [],
                fastTypeRuleList: [],
                selectedPipelines: [],
                selectedTemplates: [],
                pipelineSetting: {},
                nodeSelectConf: {
                    title: '',
                    isShow: false,
                    hasFooter: false
                },
                createGroupForm: {
                    idEdit: false,
                    name: '',
                    internal_list: [],
                    external_list: '',
                    desc: ''
                },
                dialogLoading: {
                    isLoading: false,
                    title: ''
                },
                errorHandler: {
                    nameError: false
                },
                formErrors: {
                    gateSiteError: false
                },
                pipelineSelectConf: {
                    selected: [],
                    searchable: true,
                    searchKey: 'alias'
                },
                loading: {
                    isLoading: false,
                    title: ''
                },
                sideSliderConfig: {
                    show: false,
                    title: this.$t('quality.指标选择'),
                    quickClose: true,
                    width: 640,
                    value: '',
                    loading: {
                        isLoading: false,
                        title: ''
                    }
                },
                handlerList: {
                    LT: '<',
                    LE: '<=',
                    EQ: '=',
                    GT: '>',
                    GE: '>='
                },
                localCreateForm: {},
                baseForm: {
                    name: '',
                    gatewayId: '',
                    desc: '',
                    indicators: [],
                    controlPoint: '',
                    controlPointName: '',
                    controlPointPosition: '',
                    availablePosition: [],
                    pipelineList: [],
                    operation: 'END',
                    notifyTypeList: [],
                    notifyGroupList: [],
                    notifyUserList: [],
                    auditUserList: [],
                    auditTimeoutMinutes: ''
                },
                createRuleForm: {},
                // 权限配置
                emptyTipsConfig: {
                    title: this.$t('quality.没有权限'),
                    desc: this.$t('quality.你在该项目下没有【创建】拦截规则权限，请切换项目访问或申请'),
                    btns: [
                        {
                            type: 'primary',
                            size: 'normal',
                            handler: this.changeProject,
                            text: this.$t('quality.切换项目')
                        },
                        {
                            type: 'success',
                            size: 'normal',
                            handler: this.goToApplyPerm,
                            text: this.$t('quality.去申请权限')
                        }
                    ]
                },
                customRuleId: {
                    getMessage: field => this.$t('quality.开头必须是英文字母，中间可以包含英文，数字，中划线'),
                    validate: value => /^[a-zA-Z]([a-z|A-Z|0-9|-]+)*$/.test(value)
                }
            }
        },
        computed: {
            ...mapGetters('quality', [
                'getUserGroup'
            ]),
            projectId () {
                return this.$route.params.projectId
            },
            ruleId () {
                return this.$route.params.ruleId
            },
            canRemove () {
                return this.createRuleForm.indicators.length === 1
            },
            includeOpenINdicator () {
                return this.createRuleForm.indicators.length && this.createRuleForm.indicators.some(item => item.tag === 'TENCENTOPEN')
            },
            currentINdicators () {
                const target = this.createRuleForm.indicators.map(item => item.cnName)
                return target.join('、')
            },
            noticeTypeList () {
                const list = [
                    { name: 'email', value: 'EMAIL', isChecked: false }
                ]
                return list
            }
        },
        watch: {
            projectId (val) {
                this.$router.push({
                    name: 'qualityOverview',
                    params: {
                        projectId: this.projectId
                    }
                })
            },
            getUserGroup (val) {
                val.forEach(item => {
                    if (this.createGroupForm.internal_list.indexOf(item) === -1) {
                        this.createGroupForm.internal_list.push(item)
                    }
                })
            },
            'createRuleForm.indicators.length' (val) {
                if (val && !this.isInit && this.selectedPipelines.length) {
                    this.checkPipelineAtom(this.selectedPipelines)
                }
                if (val && !this.isInit && this.selectedTemplates.length) {
                    this.checkTemplateAtom(this.selectedTemplates)
                }
            },
            'createRuleForm.controlPoint' (newVal, oldVal) {
                if (newVal && !this.isInit && this.selectedPipelines.length) {
                    this.checkPipelineAtom(this.selectedPipelines)
                }
                if (newVal && !this.isInit && this.selectedTemplates.length) {
                    this.checkTemplateAtom(this.selectedTemplates)
                }
            }
        },
        async created () {
            this.addLeaveListenr()
            if (this.ruleId) {
                this.title = this.$t('quality.编辑红线规则')
                this.initData()
                await this.requestRuleDetail()
            } else {
                this.requestHasPermission()
                this.createRuleForm = JSON.parse(JSON.stringify(this.baseForm))
                if (getQueryString('indicator')) {
                    this.requestIndicatorDetail(getQueryString('indicator'))
                } else if (getQueryString('pipelineId') && getQueryString('element')) {
                    this.getPipelineDetail(getQueryString('pipelineId'))
                    this.getPipelineControlPoint(getQueryString('element'))
                } else if (getQueryString('templateId') && getQueryString('element')) {
                    this.getTemplateDetail(getQueryString('templateId'))
                    this.getPipelineControlPoint(getQueryString('element'))
                }
            }
        },
        beforeDestroy () {
            this.removeLeaveListenr()
        },
        mounted () {
            this.$nextTick(() => {
                this.$validator.extend('customRuleId', this.customRuleId)
            })
        },
        methods: {
            changeProject () {
                this.iframeUtil.toggleProjectMenu(true)
            },
            goToApplyPerm () {
                const url = PERM_URL_PREFIX
                window.open(url, '_blank')
            },
            addLeaveListenr () {
                window.addEventListener('beforeunload', this.leaveSure)
            },
            removeLeaveListenr () {
                window.removeEventListener('beforeunload', this.leaveSure)
            },
            leaveSure (e) {
                e.returnValue = this.$t('quality.离开后，新编辑的数据将丢失')
                return this.$t('quality.离开后，新编辑的数据将丢失')
            },
            triggerHandler (type) {
                this.$refs.dropdown.hide()
                if (!this.createRuleForm.indicators.length) {
                    this.$bkMessage({
                        message: this.$t('quality.请先选择指标'),
                        theme: 'error'
                    })
                } else if (type === 'pipeline') {
                    this.showPipelineList = true
                } else if (type === 'template') {
                    this.showTemplateList = true
                }
            },
            changeFastType (data) {
                const target = JSON.parse(JSON.stringify(data))
                if (this.curFastType === target.hashId) {
                    this.comfireInfo(target, 'cancel')
                } else if (JSON.stringify(this.createRuleForm) !== JSON.stringify(this.baseForm)) {
                    this.comfireInfo(target, 'comfire')
                } else {
                    this.curFastType = target.hashId
                    Object.assign(this.createRuleForm, target, {})
                    this.createRuleForm.indicators.forEach(item => {
                        item.operationList = item.operationList.map(operation => {
                            return {
                                label: this.handlerList[operation],
                                value: operation
                            }
                        })
                    })
                    this.createRuleForm.controlPointPosition = this.createRuleForm.controlPointPosition.name
                    this.createRuleForm.pipelineList = []
                    this.selectedPipelines = []
                    this.selectedTemplates = []
                }
            },
            comfireInfo (tpl, type) {
                const h = this.$createElement
                const isComfire = type === 'comfire'
                const title = isComfire ? this.$t('quality.使用{0}红线模板', [tpl.name]) : this.$t('quality.取消使用{0}红线模板', [tpl.name])
                const msg = isComfire ? this.$t('quality.确认使用后，之前填写的信息将会被覆盖。') : this.$t('quality.取消使用后，之前填写的信息将会被清空。')
                const content = h('p', {
                    style: {
                        textAlign: 'center'
                    }
                }, msg)

                this.$bkInfo({
                    title: title,
                    subHeader: content,
                    confirmFn: async () => {
                        if (isComfire) {
                            this.curFastType = tpl.hashId
                            Object.assign(this.createRuleForm, tpl, {})
                            this.createRuleForm.controlPointPosition = this.createRuleForm.controlPointPosition.name
                            this.createRuleForm.pipelineList = []
                            this.selectedPipelines = []
                            this.selectedTemplates = []
                            this.createRuleForm.indicators.forEach(item => {
                                item.operationList = item.operationList.map(operation => {
                                    return {
                                        label: this.handlerList[operation],
                                        value: operation
                                    }
                                })
                            })
                        } else {
                            this.curFastType = ''
                            this.createRuleForm = JSON.parse(JSON.stringify(this.baseForm))
                        }
                    }
                })
            },
            getIndicatorName (indicator) {
                const enName = indicator.type === 'CUSTOM' ? `(${indicator.enName})` : ''
                return `${indicator.cnName}${enName}`
            },
            selectMetadata (index) {
                this.lastClickCount = index
                this.sideSliderConfig.show = true
                this.searchKey = ''
                this.sideSliderConfig.title = this.$t('quality.指标选择')
                this.panelType = 'index'
            },
            reduceMetadata (indicator, index) {
                if (indicator.tag === 'TENCENTOPEN') {
                    const h = this.$createElement
                    const msg = this.$t('quality.该指标为腾讯开源指标，删除后若代码对外开源，将会有不合规的风险。')
                    const content = h('p', {
                        style: {
                            textAlign: 'center'
                        }
                    }, msg)

                    this.$bkInfo({
                        title: this.$t('quality.删除{0}指标', [indicator.cnName]),
                        subHeader: content,
                        confirmFn: async () => {
                            this.createRuleForm.indicators.splice(index, 1)
                        }
                    })
                } else {
                    this.createRuleForm.indicators.splice(index, 1)
                }
            },
            selectControlPoint () {
                this.sideSliderConfig.show = true
                this.searchKey = ''
                this.sideSliderConfig.title = this.$t('quality.控制点选择')
                this.panelType = 'controlPoint'
            },
            async getPipelineDetail (pipelineId) {
                this.loading.isLoading = true

                try {
                    const res = await this.$store.dispatch('quality/getPipelineDetail', {
                        projectId: this.projectId,
                        pipelineId
                    })

                    this.pipelineSetting = res
                    this.createRuleForm.pipelineList.push({
                        pipelineId: res.pipelineId,
                        pipelineName: res.pipelineName,
                        elementCount: res.taskCount,
                        lackPointElement: [],
                        existElement: [],
                        isSetPipeline: true,
                        type: 'pipeline'
                    })
                    this.selectedPipelines.push(res.pipelineId)
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.loading.isLoading = false
                }
            },
            async getTemplateDetail (templateId) {
                this.loading.isLoading = true

                try {
                    const res = await this.$store.dispatch('quality/getTemplateDetail', {
                        projectId: this.projectId,
                        templateId
                    })

                    this.pipelineSetting = {
                        ...res,
                        templateId: templateId
                    }
                    this.createRuleForm.pipelineList.push({
                        templateId: templateId,
                        templateName: res.templateName,
                        lackPointElement: [],
                        existElement: [],
                        isSetPipeline: true,
                        type: 'template'
                    })
                    this.selectedTemplates.push(templateId)
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.loading.isLoading = false
                }
            },
            async getPipelineControlPoint (element) {
                this.loading.isLoading = true

                try {
                    const res = await this.$store.dispatch('quality/getControlPoint', { element, projectId: this.projectId })

                    // 控制点处理
                    this.createRuleForm.controlPointName = res.name
                    this.createRuleForm.availablePosition = res.availablePos
                    this.createRuleForm.controlPointPosition = res.defaultPos.name
                    this.createRuleForm.controlPoint = res.type
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    this.loading.isLoading = false
                }
            },
            async requestIndicatorDetail (indicator) {
                try {
                    const res = await this.$store.dispatch('quality/requestIndicatorDetail', {
                        projectId: this.projectId,
                        indicatorId: indicator
                    })

                    res.operationList = res.operationList.map(operation => {
                        return {
                            label: this.handlerList[operation],
                            value: operation
                        }
                    })
                    this.createRuleForm.indicators.push(res)
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async requestHasPermission () {
                this.loading.isLoading = true

                try {
                    const res = await this.$store.dispatch('quality/requestPermission', {
                        projectId: this.projectId
                    })

                    this.hasPermission = res
                    if (this.hasPermission) {
                        this.initData()
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                    }, 1000)
                    this.showContent = true
                }
            },
            initData () {
                this.requestGroupList()
                this.requestRuleTemplate()
            },
            async requestGroupList () {
                try {
                    const res = await this.$store.dispatch('quality/requestGroupList', {
                        projectId: this.projectId
                    })

                    res.records.forEach(item => {
                        if (!this.groupList.some(group => group.groupHashId === item.groupHashId)) {
                            this.groupList.push(item)
                        }
                    })
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async requestRuleTemplate () {
                try {
                    const res = await this.$store.dispatch('quality/requestRuleTemplate', {
                        projectId: this.projectId
                    })

                    this.fastTypeRuleList.splice(0, this.fastTypeRuleList.length)
                    if (res.length) {
                        res.forEach(item => {
                            this.fastTypeRuleList.push(item)
                        })
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async requestRuleDetail () {
                this.loading.isLoading = true
                this.isInit = true
                try {
                    const res = await this.$store.dispatch('quality/requestRuleDetail', {
                        projectId: this.projectId,
                        ruleHashId: this.ruleId
                    })

                    this.createRuleForm = JSON.parse(JSON.stringify(this.baseForm))
                    Object.assign(this.createRuleForm, res)

                    // 指标处理
                    this.createRuleForm.indicators.forEach(item => {
                        item.operationList = item.operationList.map(operation => {
                            return {
                                label: this.handlerList[operation],
                                value: operation
                            }
                        })
                    })

                    // 控制点处理
                    this.createRuleForm.controlPointName = this.createRuleForm.controlPoint.cnName
                    this.createRuleForm.availablePosition = this.createRuleForm.controlPoint.availablePosition
                    this.createRuleForm.controlPointPosition = this.createRuleForm.controlPoint.position.name
                    this.createRuleForm.controlPoint = this.createRuleForm.controlPoint.name

                    // 生效流水线处理
                    if (this.createRuleForm.range.length) {
                        const pipelineIds = this.createRuleForm.range.map(pipeline => pipeline.id)
                        this.checkPipelineAtom(pipelineIds)
                    }
                    if (this.createRuleForm.templateRange.length) {
                        const templateIds = this.createRuleForm.templateRange.map(template => template.id)
                        this.checkTemplateAtom(templateIds)
                    }

                    // 操作处理
                    if (this.createRuleForm.operation === 'END') {
                        this.createRuleForm.auditTimeoutMinutes = ''
                        this.createRuleForm.auditUserList = []
                    }
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.$bkMessage({
                        message,
                        theme
                    })
                } finally {
                    setTimeout(() => {
                        this.loading.isLoading = false
                        this.isInit = false
                    }, 1000)
                    this.showContent = true
                }
            },
            toRuleList () {
                this.$router.push({
                    name: 'ruleList',
                    params: {
                        projectId: this.projectId
                    }
                })
            },
            async comfirePipelineList (data) {
                if (this.selectedPipelines.sort().toString() !== data.sort().toString()) {
                    this.checkPipelineAtom(data)
                } else {
                    this.closePipelineList()
                }
            },
            comfireTemplateList (data) {
                if (this.selectedTemplates.sort().toString() !== data.sort().toString()) {
                    this.checkTemplateAtom(data)
                } else {
                    this.closePipelineList()
                }
            },
            closeTemplateList () {
                this.showTemplateList = false
            },
            async checkPipelineAtom (data) {
                this.selectedPipelines = JSON.parse(JSON.stringify(data))

                const params = {
                    projectId: this.projectId,
                    gatewayId: this.createRuleForm.gatewayId,
                    pipelineIds: data,
                    controlPointType: this.createRuleForm.controlPoint || '',
                    indicatorIds: []
                }
                this.createRuleForm.indicators.forEach(item => {
                    params.indicatorIds.push(item.hashId)
                })
                this.showPipelineList = false
                this.tableLoading = true

                try {
                    const res = await this.$store.dispatch('quality/requestRangePipeline', { params })

                    this.createRuleForm.pipelineList = this.createRuleForm.pipelineList.filter(item => item.type !== 'pipeline')

                    res.forEach(item => {
                        item.isRefresh = false
                        item.type = 'pipeline'
                        if (this.pipelineSetting && item.pipelineId === this.pipelineSetting.pipelineId) {
                            item.pipelineId = this.pipelineSetting.pipelineId
                            item.pipelineName = this.pipelineSetting.pipelineName
                            item.lackPointElement = []
                            item.existElement = []
                            item.isSetPipeline = true
                        }
                        this.createRuleForm.pipelineList.push(item)
                    })
                    this.tableLoading = false
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.tableLoading = false
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            async checkTemplateAtom (template) {
                this.selectedTemplates = JSON.parse(JSON.stringify(template))

                const params = {
                    projectId: this.projectId,
                    gatewayId: this.createRuleForm.gatewayId,
                    templateIds: template,
                    controlPointType: this.createRuleForm.controlPoint || '',
                    indicatorIds: []
                }
                this.createRuleForm.indicators.forEach(item => {
                    params.indicatorIds.push(item.hashId)
                })
                this.showTemplateList = false
                this.tableLoading = true

                try {
                    const res = await this.$store.dispatch('quality/requestRangeTemplate', { params })

                    this.createRuleForm.pipelineList = this.createRuleForm.pipelineList.filter(item => item.type !== 'template')

                    res.forEach(item => {
                        item.isRefresh = false
                        item.type = 'template'
                        if (this.pipelineSetting && item.templateId === this.pipelineSetting.templateId) {
                            item.templateId = this.pipelineSetting.templateId
                            item.templateName = this.pipelineSetting.templateName
                            item.lackPointElement = []
                            item.existElement = []
                            item.isSetPipeline = true
                        }
                        this.createRuleForm.pipelineList.push(item)
                    })

                    this.tableLoading = false
                } catch (err) {
                    const message = err.message ? err.message : err
                    const theme = 'error'

                    this.tableLoading = false
                    this.$bkMessage({
                        message,
                        theme
                    })
                }
            },
            closePipelineList () {
                this.showPipelineList = false
            },
            handleChange (name, value) {
                if (name === 'reviewer') {
                    this.createRuleForm.auditUserList = value
                } else if (name === 'attacher') {
                    this.createRuleForm.notifyUserList = value
                } else if (name === 'innerList') {
                    this.createGroupForm.internal_list = value
                }
            },
            toCreateGroup () {
                this.createGroupForm = {
                    isEdit: false,
                    groupHashId: '',
                    name: '',
                    internal_list: [],
                    external_list: '',
                    desc: ''
                }
                this.nodeSelectConf.title = this.$t('quality.新增通知组')
                this.nodeSelectConf.isShow = true
            },
            validate () {
                let errorCount = 0
                if (!this.createGroupForm.name) {
                    this.errorHandler.nameError = true
                    errorCount++
                }

                if (errorCount > 0) {
                    return false
                }

                return true
            },
            async confirmFn (params) {
                let message, theme
                this.dialogLoading.isLoading = true

                try {
                    await this.$store.dispatch('quality/createUserGroups', {
                        projectId: this.projectId,
                        params
                    })

                    message = this.$t('quality.保存成功')
                    theme = 'success'
                    this.requestGroupList()
                    this.nodeSelectConf.isShow = false
                } catch (err) {
                    message = err.message ? err.message : err
                    theme = 'error'
                } finally {
                    this.$bkMessage({
                        message,
                        theme
                    })

                    this.dialogLoading.isLoading = false
                }
            },
            cancelFn () {
                if (!this.dialogLoading.isLoading) {
                    this.nodeSelectConf.isShow = false
                    this.errorHandler.nameError = false
                }
            },
            getPipelineStatus (list) {
                return list.join('、')
            },
            checkAtomAsync (element) {
                return element.some(item => item.params.asynchronous)
            },
            checkAtomCount (element) {
                return element.some(item => item.count > 1)
            },
            updatePipelineStatus (pipelineId) {
                const target = this.createRuleForm.pipelineList.map(item => {
                    return {
                        ...item,
                        isRefresh: pipelineId === item.pipelineId ? true : item.isRefresh
                    }
                })
                this.createRuleForm.pipelineList.splice(0, this.createRuleForm.pipelineList.length, ...target)
            },
            updateTemplateStatus (templateId) {
                const target = this.createRuleForm.pipelineList.map(item => {
                    return {
                        ...item,
                        isRefresh: templateId === item.templateId ? true : item.isRefresh
                    }
                })
                this.createRuleForm.pipelineList.splice(0, this.createRuleForm.pipelineList.length, ...target)
            },
            handleRefresh () {
                this.checkPipelineAtom(this.selectedPipelines)
                this.checkTemplateAtom(this.selectedTemplates)
            },
            handleSearch () {
                this.$refs.metadataPanel.toSearch()
            },
            getAsyncAtom (element) {
                const target = []
                element.forEach(item => {
                    if (item.params.asynchronous) {
                        target.push(item.cnName)
                    }
                })
                return target.join('、')
            },
            handleMetadata (params) {
                if (params.type === 'controlPoint') {
                    this.createRuleForm.controlPoint = params.data.type
                    this.createRuleForm.controlPointName = params.data.name
                    this.createRuleForm.availablePosition = params.data.availablePos
                    this.createRuleForm.controlPointPosition = params.data.defaultPos.name
                    this.sideSliderConfig.show = false
                } else {
                    const tempArr = []
                    const tplData = JSON.parse(JSON.stringify(params.data))

                    tplData.forEach(item => {
                        const isExist = this.createRuleForm.indicators.some(val => val.hashId === item.hashId)
                        if (!isExist) {
                            item.operationList = item.operationList.map(operation => {
                                return {
                                    label: this.handlerList[operation],
                                    value: operation
                                }
                            })
                            tempArr.push(item)
                        }
                    })

                    const target = this.createRuleForm.indicators
                    this.createRuleForm.indicators = [...target.slice(0, this.lastClickCount + 1), ...tempArr, ...target.slice(this.lastClickCount + 1, target.length)]
                }
                // this.sideSliderConfig.show = false
            },
            submitValidate () {
                let errMsg = ''
                const IntReg = /^([0-9]|[1-9][0-9]+)$/ // 自然数
                const floatReg = /^\d+(\.\d+)?$/ // 正浮点数
                const timeout = /^([1-9]|[1-5]\d|60)$/ // 0-60整数
                const validThreshold = this.createRuleForm.indicators.some(item => {
                    return (!item.operation || !item.threshold)
                        || (item.thresholdType === 'INT' && !IntReg.test(item.threshold))
                        || (item.thresholdType === 'FLOAT' && !floatReg.test(item.threshold))
                })

                if (!this.createRuleForm.indicators.length) {
                    errMsg = this.$t('quality.请选择指标')
                } else if (this.createRuleForm.indicators.length && validThreshold) {
                    errMsg = this.$t('quality.请填写正确的阈值')
                } else if (!this.createRuleForm.controlPoint) {
                    errMsg = this.$t('quality.请选择控制点')
                } else if (!this.createRuleForm.pipelineList.length) {
                    errMsg = this.$t('quality.请选择生效的流水线')
                } else if (this.createRuleForm.operation === 'END' && !this.createRuleForm.notifyTypeList.length) {
                    errMsg = this.$t('quality.请选择通知方式')
                } else if (this.createRuleForm.operation === 'AUDIT' && !this.createRuleForm.auditUserList.length) {
                    errMsg = this.$t('quality.请填写审核人')
                } else if (this.createRuleForm.auditTimeoutMinutes && !timeout.test(this.createRuleForm.auditTimeoutMinutes)) {
                    errMsg = this.$t('quality.请填写60分钟以内的大于0的整数')
                }

                return errMsg
            },
            getParams () {
                const obj = {
                    name: this.createRuleForm.name,
                    gatewayId: this.createRuleForm.gatewayId,
                    desc: this.createRuleForm.desc,
                    indicatorIds: [],
                    controlPoint: this.createRuleForm.controlPoint,
                    controlPointPosition: this.createRuleForm.controlPointPosition,
                    range: [],
                    templateRange: [],
                    operation: this.createRuleForm.operation,
                    auditTimeoutMinutes: parseInt(this.createRuleForm.auditTimeoutMinutes) || undefined
                }

                this.createRuleForm.pipelineList.forEach(item => {
                    if (item.type === 'pipeline') {
                        obj.range.push(item.pipelineId)
                    } else {
                        obj.templateRange.push(item.templateId)
                    }
                })
                this.createRuleForm.indicators.forEach(item => {
                    obj.indicatorIds.push({
                        hashId: item.hashId,
                        operation: item.operation,
                        threshold: item.threshold
                    })
                })

                if (obj.operation === 'END') {
                    obj.notifyTypeList = this.createRuleForm.notifyTypeList
                    obj.notifyGroupList = this.createRuleForm.notifyGroupList
                    obj.notifyUserList = this.createRuleForm.notifyUserList
                    obj.auditTimeoutMinutes = null
                } else {
                    obj.auditUserList = this.createRuleForm.auditUserList
                }

                return obj
            },
            submit () {
                this.$validator.validateAll().then(async (result) => {
                    if (result) {
                        const isValid = this.submitValidate()

                        if (isValid) {
                            this.$bkMessage({
                                message: isValid,
                                theme: 'error'
                            })
                        } else {
                            let message, theme
                            const params = this.getParams()

                            this.loading.isLoading = true
                            try {
                                if (this.ruleId) {
                                    await this.$store.dispatch('quality/editRule', {
                                        projectId: this.projectId,
                                        ruleHashId: this.ruleId,
                                        params
                                    })

                                    message = this.$t('quality.编辑规则成功')
                                    theme = 'success'
                                } else {
                                    await this.$store.dispatch('quality/createRule', {
                                        projectId: this.projectId,
                                        params
                                    })

                                    message = this.$t('quality.创建规则成功')
                                    theme = 'success'
                                }
                            } catch (err) {
                                message = err.message ? err.message : err
                                theme = 'error'
                            } finally {
                                this.isEditing = false
                                this.loading.isLoading = false
                                this.$bkMessage({
                                    message,
                                    theme
                                })
                                if (theme === 'success') this.toRuleList()
                            }
                        }
                    }
                })
            }
        }
    }
</script>

<style lang="scss">
    @import '@/scss/conf.scss';

    .create-rule-wrapper {
        position: relative;
        .info-header {
            display: flex;
            padding: 18px 20px;
            width: 100%;
            height: 60px;
            border-bottom: 1px solid #DDE4EB;
            background-color: #fff;
            box-shadow:0px 2px 5px 0px rgba(51,60,72,0.03);
            .title {
                display: flex;
                align-items: center;
            }
            .header-text {
                font-size: 16px;
            }
            .icon-arrows-left {
                margin-right: 8px;
                cursor: pointer;
                color: $iconPrimaryColor;
                font-size: 16px;
                font-weight: 600;
            }
        }
        .create-rule-content {
            height: calc(100% - 60px);
            padding: 8px 20px 22px;
            overflow: auto;
        }
        .info-title {
            color: #737987;
            font-weight: bold;
            .icon-info-circle {
                position: relative;
                top: 2px;
                color: #C3CDD7;
            }
            .add-indicator {
                margin-top: 4px;
                float: right;
                color: $primaryColor;
                font-size: 12px;
                font-weight: normal;
            }
        }
        .title-tips {
            color: $primaryColor;
            font-weight: normal;
            cursor: pointer;
            a {
                color: $primaryColor;
            }
        }
        .open-tips {
            margin-bottom: 10px;
            color: #979BA5;
        }
        hr {
            margin-top: 8px;
            height: 1px;
            border: none;
            background-color: #DDE4EB;
        }
        .fast-create {
            width: 100%;
            height: 150px;
            padding: 18px 20px;
            border: 1px solid #DDE4EB;
            background-color: #fff;
            .rule-types-container {
                display: inline-block;
                margin-top: 16px;
            }
            .type-item-card {
                float: left;
                display: flex;
                align-items: center;
                position: relative;
                width: 130px;
                height: 76px;
                margin-right: 10px;
                border: 1px solid #DDE4EB;
                border-radius: 2px;
                box-shadow: 0px 2px 5px 0px rgba(51,60,72,0.05);
                cursor: pointer;
                .template-icon {
                    display: flex;
                    justify-content: center;
                    align-items: center;
                    width: 24px;
                    height: 24px;
                    margin-left: 16px;
                    margin-right: 8px;
                    border-radius: 50%;
                    background-color: #C5C7D1;
                }
                .label-icon {
                    margin-left: 16px;
                    margin-right: 8px;
                    font-size: 22px;
                    color: #C5C7D1;
                }
                .icon-check-circle-shape {
                    position: absolute;
                    right: 8px;
                    bottom: 8px;
                    font-size: 16px;
                    color: $primaryColor;
                }
                .card-label {
                    display: block;
                    white-space: nowrap;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    padding-right: 10px;
                }
                &:hover {
                    border-color: $primaryColor;
                    .label-icon {
                        color: #A3C5FD;
                    }
                    .template-icon {
                        background-color: #A3C5FD;
                    }
                }
            }
            .active-item {
                border-color: $primaryColor;
                .label-icon {
                    color: #A3C5FD;
                }
                .card-label {
                    color: #3C96FF;
                }
                .template-icon {
                    background-color: #A3C5FD;
                }
            }
        }
        .create-steps-rule {
            display: flex;
            justify-content: space-between;
            margin-top: 30px;
        }
        .create-rule-form {
            width: calc(100% - 376px);
            .bk-form-item {
                margin-top: 20px;
            }
            .blod-label .bk-label {
                font-weight: bold;
                text-align: left;
                color: #737987;
            }
            .blod-label .selected-item-tooltips .bk-label {
                font-weight: normal;
                text-align: right;
                line-height: 32px;
            }
            .notice-type-content {
                line-height: 24px;
            }
            .bk-form-content {
                margin-left: 93px;
            }
            .bk-form-input,
            .bk-tag-selector {
                width: 467px;
            }
            .select-control-point {
                position: absolute;
                left: 476px;
                top: 6px;
                color: $primaryColor;
                cursor: pointer;
            }
            .bk-form-radio {
                margin-right: 38px;
            }
            .gateway-id-tips {
                margin-top: 8px;
                margin-left: 100px;
                color:#313238;
                font-size: 12px;
                .icon-info-circle-shape {
                    font-size: 14px;
                    color: #ffb848;
                }
            }
        }
        .rule-metadata-table {
            width: auto;
            th {
                padding: 0;
                color: #979BA5;
                font-weight: bold;
                font-size: 12px;
            }
            th, td {
                text-align: left;
                &:first-child {
                    padding-left: 0;
                }
            }
            td {
                border: 1px solid #DDE4EB;
                color: #737987;
                &:last-child {
                    border: none;
                }
            }
                
        }
        .no-metadata-row {
            padding-top: 10px;
            text-align: center;
            span {
                color: $primaryColor;
                cursor: pointer;
            }
        }
        .metadata-name {
            padding-left: 8px;
            width: 323px;
            height: 36px;
            line-height: 36px;
            border-right: none;
            background-color: #FAFBFD;
            white-space: nowrap;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .handler-item,
        .threshold-item {
            width: 118px;
            margin-right: -1px;
            background-color: #FFF;
            .bk-select {
                width: 118px;
                margin-right: 0;
                height: 36px;
                border: 0;
                .tippy-active,
                .bk-select-name {
                    height: 36px;
                }
                .devops-icon,
                input {
                    border-radius: 0;
                    border-color: #DDE4EB;
                    color: #737987;
                    border: none;
                }
            }
            .selected-input {
                .bk-selector-input {
                    border-radius: 0;
                    border-color: #DDE4EB;
                    // color: $fontLigtherColor;
                    border: none;
                }
            }
        }
        .threshold-item {
            position: relative;
            .bk-form-input {
                width: 116px;
                border-radius: 0;
                border-left: none;
                border-color: #DDE4EB;
                color: $fontWeightColor;
                border: none;
            }
        }
        .add-group {
            cursor: pointer;
            .bk-checkbox {
                display: none;
            }
            .bk-checkbox-text {
                margin: 0;
            }
        }
        .icon-plus-circle-shape,
        .icon-minus-circle-shape {
            margin-left: 20px;
            font-size: 16px;
            cursor: pointer;
        }
        .icon-minus-circle-shape {
            margin-left: 4px;
        }
        .single-plus-circle {
            color: #C4C6CC;
            cursor: default;
        }
        .select-effect-btn {
            margin-right: 8px;
        }
        .pipeline-table-container {
            width: 624px;
            max-height: 295px;
            margin-top: 10px;
            padding-bottom: 10px;
            border: 1px solid #dfe0e5;
            overflow-x: hidden;
            overflow-y: auto;
            td .cell {
                padding: 10px 15px;
                display: block;
            }
        }
        .effect-pipeline-table {
            position: relative;
            top: -1px;
            .bk-table-empty-text {
                padding: 20px;
            }
            .add-btn {
                color: $primaryColor;
                cursor: pointer;
            }
            .no-data {
                color: #979BA5;
                text-align: center;
            }
            .atom-tips {
                text-overflow: ellipsis;
                overflow: hidden;
                color: #F5A623;
            }
            .mark-circle {
                display: inline-block;
                margin-right: 4px;
                position: relative;
                top: -2px;
                width: 4px;
                height: 4px;
                background-color: $fontWeightColor;
                border-radius: 50%;
            }
        }
        .notice-group-item {
            // margin-top: 0;
            .icon-plus-circle,
            .create-group {
                color: $iconPrimaryColor;
                cursor: pointer;
            }
            .icon-plus-circle {
                position: relative;
                top: 2px;
            }
            .icon-member-list {
                display: none;
                position: relative;
                top: -3px;
                cursor: pointer;
                &:hover {
                    color: $primaryColor;
                }
            }
            .bk-form-checkbox {
                margin-bottom: 4px;
                margin-right: 12px;
                width: 165px;
                line-height: 28px;
                display: inline-block;
                overflow: hidden;
                white-space: nowrap;
                text-overflow: ellipsis;
                &:hover {
                    .icon-member-list {
                        display: block;
                    }
                }
            }
            .notice-name {
                display: inline-block;
                position: relative;
                top: 5px;
                max-width: 126px;
                overflow: hidden;
                text-overflow: ellipsis;
            }
        }
        .selected-item-tooltips {
            position: relative;
            top: 14px;
            left: -100px;
            padding: 12px 20px 20px;
            width: 724px;
            min-height:64px;
            background:rgba(255,255,255,1);
            border-radius:2px;
            border: 1px solid $borderWeightColor;
            &:before {
                content: '';
                padding-top: 4px;
                position: absolute;
                top: -6px;
                left: 234px;
                width: 10px;
                height: 6px;
                border: 1px solid $borderWeightColor;
                border-right-color: transparent;
                border-bottom-color: transparent;
                background-color: #fff;
                transform: rotate(45deg);
            }
            .bk-selector .bk-selector-input {
                padding-right: 20px;
                text-overflow: ellipsis;
            }
            .selected-input .bk-selector-input {
                color: $fontWeightColor;
            }
            .bk-form-item {
                margin-top: 14px;
                &:first-child {
                    margin-top: 14px;
                }
            }
            .notice-type-item {
                .bk-form-checkbox {
                    margin-right: 36px;
                }
            }
            .bk-form-checkbox {
                margin-right: 36px;
            }
        }
        .system-active {
            &:before {
                left: 100px;
            }
        }
        .notice-item-tooltips .bk-label {
            width: 120px;
            line-height: 24px;
        }
        .notice-type-checkbox {
            margin-right: 12px;
            width: 80px;
            display: inline-block;
            overflow: hidden;
            white-space: nowrap;
            text-overflow: ellipsis;
            .type-item {
                position: relative;
                top: -10px;
            }
        }
        .user-audit-form {
            .prompt-tips {
                font-size: 12px;
            }
            .time-unit {
                position: absolute;
                left: 476px;
                top: 4px;
            }
        }
        .submit-handle {
            margin-top: 44px;
            padding: 0 30px;
        }
        .rule-preview {
            width: 346px;
            min-width: 346px;
            .preview-image {
                height: 188px;
                margin-top: 14px;
                background-color: #FFF;
                img {
                    width: 100%;
                    height: 100%;
                }
            }
        }
        .bk-sideslider-wrapper {
            top: 0;
            padding-bottom: 0;
             .bk-sideslider-content {
                height: calc(100% - 60px);
            }
        }
        .control-point-name {
            background-color: #FAFBFD;
            color: #737987;
        }
        .metadata-panel-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            .search-input-row {
                display: flex;
                align-items: center;
                margin-right: 20px;
                padding: 0 10px;
                width: 279px;
                height: 36px;
                border: 1px solid #dde4eb;
                background-color: #fff;
                .bk-form-input {
                    padding: 0;
                    border: 0;
                    -webkit-box-shadow: border-box;
                    box-shadow: border-box;
                    outline: none;
                    width: 239px;
                    height: 32px;
                    margin-left: 0;
                }
                .icon-search {
                    float: right;
                    color: #c3cdd7;
                    cursor: pointer;
                }
            }
        }
    }
</style>
