<template>
    <section
        class="startup-parameter-box"
        v-bkloading="{ isLoading }"
    >
        <param-set
            only-save-as-set
            ref="paramSet"
            is-start-up
            :disabled="!isBuildParamReady"
            :is-visible-version="isVisibleVersion"
            :build-num="execDetail?.buildNum"
            :all-params="buildParamProperities"
            :resolve-params-for-save="resolveParamsForSave"
        />
        <div class="startup-parameter-wrapper">
            <div
                ref="parent"
                class="build-param-row"
                v-for="(param, index) in params"
                :key="param.key || index"
            >
                <span class="build-param-span">
                    <span
                        class="build-param-key-span"
                        :title="param.key"
                    >
                        {{ param.key }}
                    </span>
                    <i
                        v-if="param.desc"
                        v-bk-tooltips="{ content: param.desc, allowHTML: false }"
                        class="devops-icon icon-question-circle"
                    />
                </span>
                <span class="build-param-span">
                    <template v-if="param.isLongValue">
                        <span
                            :class="{
                                'build-param-long-value-placeholder': true,
                                'build-param-long-value-error': param.valueError
                            }"
                        >
                            {{ getLongParamPlaceholder(param) }}
                        </span>
                        <bk-button
                            text
                            class="view-param-value-detail"
                            size="small"
                            :loading="param.valueLoading"
                            :disabled="param.valueLoading"
                            @click="showLongParamDetail(param, index)"
                        >
                            {{ param.valueLoaded ? $t("detail") : $t('details.loadParamValue') }}
                        </bk-button>
                    </template>
                    <template v-else-if="typeof param.value !== 'undefined'">
                        <span
                            ref="valueSpan"
                            :data-param-key="param.key"
                            :class="{
                                'build-param-value-span': true,
                                'diff-param-value': param.isDiff
                            }"
                        >
                            {{ param.value }}
                        </span>
                        <bk-button
                            v-if="overflowSpanMap[param.key]"
                            text
                            class="view-param-value-detail"
                            size="small"
                            @click="showDetail(param)"
                        >
                            {{ $t("detail") }}
                        </bk-button>
                    </template>
                    <span v-else>--</span>
                </span>
            </div>
            
            <bk-sideslider
                quick-close
                :width="640"
                :title="$t('details.paramDetail')"
                :is-show.sync="isDetailShow"
                @hidden="hideDetail"
            >
                <div
                    v-if="activeParam"
                    slot="content"
                    class="startup-param-detail-wrapper"
                >
                    <p>{{ activeParam.key }}</p>
                    <pre>{{ detailRenderValue }}</pre>
                </div>
            </bk-sideslider>
        </div>
    </section>
</template>

<script>
    import ParamSet from '@/components/ParamSet.vue'
    import {
        formatBuildParamsForDisplay,
        getDetailRenderValue,
        isLongInputValue,
        isOverflowReference,
        mergeBuildParamValue,
        toStartupParamMeta
    } from '@/utils/buildParamLongValue'
    import { allVersionKeyList } from '@/utils/pipelineConst'
    import { mapActions, mapGetters } from 'vuex'
    export default {
        components: {
            ParamSet
        },
        data () {
            return {
                isLoading: false,
                isBuildParamReady: false,
                params: [],
                defaultParamMap: {},
                activeParam: null,
                isDetailShow: false,
                overflowSpanMap: {},
                buildParamProperities: [],
                isVisibleVersion: false
            }
        },
        computed: {
            ...mapGetters('atom', {
                execDetail: 'getExecDetail'
            }),
            archiveFlag () {
                return this.$route.query.archiveFlag
            },
            detailRenderValue () {
                return getDetailRenderValue(this.activeParam?.value)
            }
        },
        watch: {
            '$route.params.buildNo': function () {
                this.$nextTick(this.init)
            }
        },
        created () {
            this.init()
        },
        methods: {
            ...mapActions('atom', [
                'requestBuildParams',
                'requestBuildParameterValue'
            ]),
            showDetail (param) {
                this.isDetailShow = true
                this.activeParam = param
            },
            getLongParamPlaceholder (param) {
                if (param.valueError) {
                    return this.$t('details.longParamValueLoadFailed')
                }
                if (param.valueLoaded) {
                    return this.$t('details.longParamValueLoaded')
                }
                if (param.overflowLength != null) {
                    return this.$t('details.longParamValueHiddenWithLength', [param.overflowLength])
                }
                return this.$t('details.longParamValueHidden')
            },
            async showLongParamDetail (param, index) {
                if (param.valueLoaded && !isOverflowReference(param.value)) {
                    this.showDetail(param)
                    return
                }

                const { projectId, pipelineId, buildNo: buildId } = this.$route.params
                this.$set(this.params, index, {
                    ...param,
                    valueLoading: true,
                    valueError: false
                })
                try {
                    const result = await this.requestBuildParameterValue({
                        projectId,
                        pipelineId,
                        buildId,
                        key: param.key,
                        ...(this.archiveFlag ? { archiveFlag: this.archiveFlag } : {})
                    })
                    const value = result?.value ?? ''
                    // 接口若仍返回引用串，视为加载失败，避免把 __BK_OVF__ 当真实值展示
                    if (isOverflowReference(value)) {
                        this.$set(this.params, index, {
                            ...this.params[index],
                            valueLoading: false,
                            valueLoaded: false,
                            valueError: true
                        })
                        this.$bkMessage({
                            theme: 'error',
                            message: this.$t('details.longParamValueLoadFailed')
                        })
                        return
                    }
                    this.params = mergeBuildParamValue(this.params, param.key, value).map(item => {
                        if (item.key !== param.key) return item
                        return {
                            ...item,
                            isDiff: this.isDefaultDiff(item)
                        }
                    })
                    const activeParam = this.params.find(item => item.key === param.key)
                    if (activeParam) {
                        this.showDetail(activeParam)
                    }
                } catch (e) {
                    this.$set(this.params, index, {
                        ...this.params[index],
                        valueLoading: false,
                        valueError: true
                    })
                    console.error(e)
                }
            },
            hideDetail () {
                // 关闭详情后释放已加载的大文本，避免多次查看后内存堆积
                if (this.activeParam?.isLongValue && this.activeParam?.key) {
                    const key = this.activeParam.key
                    this.params = this.params.map(item => {
                        if (item.key !== key || !item.isLongValue) return item
                        return {
                            ...item,
                            value: undefined,
                            valueLoaded: false,
                            valueLoading: false,
                            isDiff: false
                        }
                    })
                }
                this.activeParam = null
            },
            /**
             * 保存参数组合时才按需 resolve 大变量，避免进入 Tab 时批量加载导致 OOM
             */
            async resolveParamsForSave (params = []) {
                const { projectId, pipelineId, buildNo: buildId } = this.$route.params
                const resolved = []
                for (const param of params) {
                    const key = param.id ?? param.key
                    let value = param.value
                    if (isOverflowReference(value)) {
                        const result = await this.requestBuildParameterValue({
                            projectId,
                            pipelineId,
                            buildId,
                            key,
                            ...(this.archiveFlag ? { archiveFlag: this.archiveFlag } : {})
                        })
                        value = result?.value ?? ''
                        if (isOverflowReference(value)) {
                            throw new Error(this.$t('details.longParamValueLoadFailed'))
                        }
                    }
                    resolved.push({
                        ...param,
                        id: key,
                        value
                    })
                }
                return resolved
            },
            async init () {
                try {
                    this.isLoading = true
                    this.isBuildParamReady = false
                    this.overflowSpanMap = {}
                    const { projectId, pipelineId, buildNo: buildId } = this.$route.params
                    const urlParams = {
                        projectId,
                        pipelineId,
                        buildId,
                        ...(this.archiveFlag ? { archiveFlag: this.archiveFlag } : {})
                    }
                    // 列表接口（action 内已 sanitize）；切勿调用 getCombinationFromBuild（会批量 resolve 大值 OOM）
                    const res = await this.requestBuildParams(urlParams) || []
                    this.defaultParamMap = res.reduce((acc, item) => {
                        if (!isLongInputValue(item.defaultValue) && typeof item.defaultValue !== 'undefined') {
                            acc[item.key] = item.defaultValue
                        }
                        return acc
                    }, {})
                    // 引用串无法与默认值比较，等按需加载后再算 isDiff
                    this.params = formatBuildParamsForDisplay(res.map((item) => ({
                        ...item,
                        isDiff: isLongInputValue(item.value) ? false : this.isDefaultDiff(item)
                    })))
                    // ParamSet 仅保留短元数据，供「保存为参数组合」时再按需 resolve
                    this.buildParamProperities = res.map(toStartupParamMeta)
                    this.isVisibleVersion = this.buildParamProperities.some(item => allVersionKeyList.includes(item.id))
                    this.isBuildParamReady = true
                    this.$nextTick(() => {
                        this.overflowSpanMap = this.computeOverflowSpanMap()
                    })
                } catch (e) {
                    console.error(e)
                } finally {
                    this.isLoading = false
                }
            },
            isDefaultDiff ({ key, value }) {
                if (!Object.prototype.hasOwnProperty.call(this.defaultParamMap, key)) {
                    return false
                }
                const defaultValue = this.defaultParamMap[key]
                if (typeof defaultValue === 'boolean') {
                    return defaultValue.toString() !== value.toString()
                }
                return defaultValue !== value
            },
            /**
             * 用 data-param-key 按 key 判断截断，避免 valueSpan 数组下标与列表 index 错位（1847）
             * 不要用函数 ref + $set 写响应式对象，否则会触发无限更新导致崩溃
             */
            computeOverflowSpanMap () {
                try {
                    const spans = this.$refs.valueSpan
                    if (!spans) return {}
                    const list = Array.isArray(spans) ? spans : [spans]
                    return list.reduce((acc, span) => {
                        const key = span?.dataset?.paramKey
                        if (key) {
                            acc[key] = span.scrollWidth > span.clientWidth
                        }
                        return acc
                    }, {})
                } catch (e) {
                    return {}
                }
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/mixins/ellipsis";
.startup-parameter-box {
    width: 100%;
    height: 100%;
    padding: 24px;
    overflow: scroll;
}
.startup-parameter-wrapper {
  border-radius: 2px;
  width: fit-content;
  width: 100%;
  margin-top: 12px;
  .build-param-row {
    display: flex;
    align-items: center;
    border: 1px solid #dcdee5;
    border-bottom: 0;
    border-collapse: collapse;
    &:last-child {
        border-bottom: 1px solid #dcdee5;
    }
    .build-param-span {
        @include ellipsis();
        display: flex;
        align-items: center;
        font-size: 12px;
        height: 42px;
        line-height: 42px;
        flex: 1;
        padding: 0 16px;
        
        &:first-child {
          color: #313238;
          background-color: #fafbfd;
          max-width: 382px;
          border-right: 1px solid #dcdee5;
        }
        > .icon-question-circle {
          margin-left: auto;
          justify-items: flex-end;
          flex-shrink: 0;
          color: #979ba5;
        }
        .build-param-value-span {
            @include ellipsis();
            flex: 1;
        }
        .build-param-long-value-placeholder {
            @include ellipsis();
            flex: 1;
            color: #979ba5;
        }
        .build-param-long-value-error {
            color: #ea3636;
        }
        .view-param-value-detail {
          display: flex;
          align-items: center;
          justify-items: flex-end;
          margin-left: auto;
          flex-shrink: 0;
        }
        .diff-param-value {
          color: #4cbd20;
        }
      }
  }
}
.startup-param-detail-wrapper {
  display: flex;
  flex-direction: column;
  padding: 24px;
  font-size: 12px;
  height: 100%;
  > p {
    flex-shrink: 0;
  }
  > pre {
    flex: 1;
    margin: 6px 0;
    padding: 6px 10px;
    background: #fafbfd;
    border: 1px solid #dcdee5;
    border-radius: 2px;
    overflow-y: auto;
    white-space: pre-wrap;
    word-wrap: break-word;
  }
}
</style>
