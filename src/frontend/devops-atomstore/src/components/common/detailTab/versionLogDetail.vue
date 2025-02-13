<template>
    <section>
        <div class="version">{{ $t('store.总版本数：') }}<span class="version-total font-dark">{{ total }}</span></div>
        <ul
            class="version-content"
            @scroll="debouncedHandleScroll"
        >
            <li
                v-for="item in versionList"
                :key="item.version"
                class="version-list"
            >
                <div>
                    <span class="font-dark version-num">{{ item.version }}</span>
                    <span class="version-tag">{{ item.tag }}</span>
                </div>
                <div>
                    <p class="version-light">{{ $t('store.最近更新时间') }}</p>
                    <p>{{ item.lastUpdateTime }}</p>
                </div>
                <div v-if="item.updateLog">
                    <p class="version-light">{{ $t('store.更新日志') }}</p>
                    <p class="update-log">{{ item.updateLog }}</p>
                </div>
            </li>
        </ul>
        <div
            class="test-dom"
            v-bkloading="{ isLoading: isLoading }"
        ></div>
    </section>
</template>

<script>
    import api from '@/api'
    import { debounce } from '@/utils/index'

    export default {
        props: {
            name: String,
            currentTab: String
        },

        data () {
            return {
                isLoading: false,
                total: 0,
                versionList: [],
                page: 1,
                pageSize: 10
            }
        },

        computed: {
            type () {
                return this.$route.params.type
            },
            atomCode () {
                return this.$route.params.code
            },
            storeType () {
                const storeTypeMap = {
                    atom: 'ATOM',
                    template: 'TEMPLATE',
                    image: 'IMAGE'
                }
                return storeTypeMap[this.type]
            }
        },

        watch: {
            currentTab: {
                handler (currentVal) {
                    if (currentVal === this.name) {
                        this.initVersionLog()
                    }
                },
                immediate: true
            }
        },

        methods: {
            initVersionLog () {
                if (this.isLoading || (this.versionList.length >= this.total && this.total !== 0)) {
                    return
                }
                this.isLoading = true
                const params = {
                    page: this.page,
                    pageSize: this.pageSize
                }
                api.getVersionLogs(this.storeType, this.atomCode, params).then((res) => {
                    this.total = res.count
                    this.versionList = [...this.versionList, ...res.records]
                }).catch((err) => {
                    this.$bkMessage({ theme: 'error', message: err.message || err })
                }).finally(() => {
                    this.isLoading = false
                })
            },
            handleScroll (event) {
                const { scrollTop, clientHeight, scrollHeight } = event.target
                if (scrollTop + clientHeight >= scrollHeight - 5 && this.versionList.length < this.total) {
                    this.page++
                    this.initVersionLog()
                }
            },
            debouncedHandleScroll (event) {
                debounce(() => this.handleScroll(event))
            }
        }
    }
</script>

<style lang="scss" scoped>
  .font-dark {
      color: #313238;
  }

  .version {
      color: #979ba5;
      font-size: 18px;
      margin: 14px 0;
      .version-total {
          font-weight: 700;
          font-size: 18px;
      }
  }

  .version-content {
      max-height: 400px;
      overflow: auto;

      &::-webkit-scrollbar-thumb {
          background-color: #c4c6cc !important;
          border-radius: 5px !important;
          &:hover {
              background-color: #979ba5 !important;
          }
      }
      &::-webkit-scrollbar {
          width: 5px !important;
          height: 5px !important;
      }

      .version-list {
          margin-bottom: 20px;
    
          .version-num {
              font-weight: 700;
              font-size: 16px;
          }
    
          .version-tag {
              color: #313238;
              margin-left: 10px;
          }
    
          div {
            margin-top: 10px
          }
    
          .version-light {
            color: #979ba5;
          }

          .update-log {
            white-space: pre-line;
        }
      }
  }

</style>
