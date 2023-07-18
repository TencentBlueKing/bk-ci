<template>
    <section class="startup-parameter-box" v-bkloading="{ isLoading }">
        <div class="startup-parameter-wrapper">
            <div ref="parent" class="build-param-row" v-for="(param, index) in params" :key="index">
                <span class="build-param-span">
                    <span class="build-param-key-span" :title="param.key">
                        {{ param.key }}
                    </span>
                    <i
                        v-if="param.desc"
                        v-bk-tooltips="param.desc"
                        class="devops-icon icon-question-circle"
                    />
                </span>
                <span class="build-param-span">
                    <template v-if="typeof param.value !== 'undefined'">
                        <span
                            ref="valueSpan"
                            :class="{
                                'build-param-value-span': true,
                                'diff-param-value': param.isDiff
                            }"
                        >
                            {{ param.value }}
                        </span>
                        <bk-button
                            v-if="overflowSpan[index]"
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
                <div v-if="activeParam" slot="content" class="startup-param-detail-wrapper">
                    <p>{{ activeParam.key }}</p>
                    <pre>{{ activeParam.value }}</pre>
                </div>
            </bk-sideslider>
        </div>
    </section>
</template>

<script>
    import { mapActions } from 'vuex'
    export default {
        data () {
            return {
                isLoading: false,
                params: [],
                defaultParamMap: {},
                activeParam: null,
                isDetailShow: false,
                overflowSpan: []
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
            ...mapActions('atom', ['requestBuildParams']),
            showDetail (param) {
                this.isDetailShow = true
                this.activeParam = param
            },
            hideDetail () {
                this.activeParam = null
            },
            async init () {
                try {
                    this.isLoading = true
                    const { projectId, pipelineId, buildNo: buildId } = this.$route.params
                    const res = await this.requestBuildParams({
                        projectId,
                        pipelineId,
                        buildId
                    })

                    this.defaultParamMap = res.reduce((acc, item) => {
                        acc[item.key] = item.defaultValue
                        return acc
                    }, {})
                    this.params = res.map((item) => ({
                        ...item,
                        isDiff: this.isDefaultDiff(item)
                    }))
                    this.$nextTick(() => {
                        this.overflowSpan = this.isOverflow()
                    })
                } catch (e) {
                    console.error(e)
                } finally {
                    this.isLoading = false
                }
            },
            isDefaultDiff ({ key, value }) {
                const defaultValue = this.defaultParamMap[key]
                if (typeof defaultValue === 'boolean') {
                    return defaultValue.toString() !== value.toString()
                }
                return defaultValue !== value
            },
            isOverflow () {
                try {
                    return this.$refs.valueSpan?.map(span => span.scrollWidth > span.clientWidth) ?? []
                } catch (e) {
                    return []
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
}
.startup-parameter-wrapper {
  border-radius: 2px;
  width: fit-content;
  width: 100%;
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
