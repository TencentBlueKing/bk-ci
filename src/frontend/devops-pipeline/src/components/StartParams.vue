<template>
    <div class="startup-parameter-wrapper" v-bkloading="{ isLoading }">
        <div class="build-param-row" v-for="(_chunk, index) in paramChunks" :key="index">
            <div class="build-param-column" v-for="param in _chunk" :key="param.key">
                <span class="build-param-span">
                    <span class="build-param-key-span">
                        {{ param.key }}
                    </span>
                    <i
                        v-if="param.desc"
                        v-bk-tooltips="param.desc"
                        class="devops-icon icon-question-circle"
                    />
                </span>
                <span class="build-param-span">
                    <template v-if="param.value">
                        <span
                            :class="{
                                'build-param-value-span': true,
                                'diff-param-value': param.isDiff
                            }"
                        >
                            {{ param.value }}
                        </span>
                        <bk-button
                            v-if="param.isOverflow"
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
                <pre>
                    {{ activeParam.value }}
                </pre
                >
            </div>
        </bk-sideslider>
    </div>
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
                isDetailShow: false
            }
        },
        computed: {
            paramChunks () {
                return this.chunk(this.params, 2)
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

                    this.defaultParamMap = this.params.reduce((acc, item) => {
                        acc[item.key] = item.defaultValue
                        return acc
                    }, {})
                    this.params = res.map((item) => ({
                        ...item,
                        isDiff: this.isDefaultDiff(item),
                        isOverflow: this.isOverflow(item.value ?? '')
                    }))
                } catch (e) {
                    console.error(e)
                } finally {
                    this.isLoading = false
                }
            },
            isDefaultDiff ({ key, value }) {
                let defaultValue = this.defaultParamMap[key]
                if (typeof defaultValue === 'boolean') {
                    defaultValue = defaultValue.toString()
                }
                return defaultValue !== value
            },
            chunk (list, size = 2) {
                if (!Array.isArray(list) || !Number.isInteger(size) || size < 0) {
                    return []
                }
                if (list.length < size) {
                    return [list]
                }
                let resultIndex = 0
                let index = 0
                const result = []
                while (index < list.length) {
                    result[resultIndex++] = list.slice(index, (index += size))
                }
                return result
            },
            isOverflow (text) {
                if (!text) return false
                const canvas = document.createElement('canvas')
                const context = canvas.getContext('2d')
                const { width } = context.measureText(text)
                return width > 183
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/mixins/ellipsis";
.startup-parameter-wrapper {
  margin: 24px;
  border: 1px solid #dcdee5;
  border-radius: 2px;
  width: fit-content;
  .build-param-row {
    display: flex;
    align-items: center;
    border-bottom: 1px solid #dcdee5;
    &:last-child {
      border-bottom: 0;
    }

    .build-param-column {
      display: flex;
      align-items: center;
      &:nth-child(2n) {
        .build-param-span:last-child {
          border-right: 0;
        }
      }
      .build-param-span {
        border-right: 1px solid #dcdee5;
        display: flex;
        align-items: center;
        font-size: 12px;
        height: 42px;
        line-height: 42px;
        width: 280px;
        padding: 0 16px;
        &:first-child {
          color: #313238;
          background-color: #fafbfd;
          width: 160px;
        }
        > .icon-question-circle {
          margin-left: auto;
          justify-items: flex-end;
          flex-shrink: 0;
          color: #979ba5;
        }
        .build-param-value-span,
        .build-param-key-span {
          @include ellipsis();
          margin-right: 4px;
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
