<template>
    <div class="node-model-wrapper"
        v-bkloading="{
            isLoading: loading.isLoading,
            title: loading.title
        }">
        <h3>{{ $t('environment.nodeInfo.selectNewNode') }}</h3>
        <ul class="model-type-group" v-show="showContent">
            <li class="item-tab"
                v-for="(entry, index) in modelList"
                :key="index"
                :class="{
                    'hover': !entry.isDisabled,
                    'active': currentType === entry.value,
                    'disabled': entry.isDisabled
                }"
                @click="changeTab(entry)">
                <icon :name="entry.icon" size="24" class="model-logo"></icon>
                {{ entry.label }}
            </li>
        </ul>
        <div class="selection-modelbox">
            <div class="model-option"
                v-for="(model, index) in modelOption"
                :key="index"
                :class="{ 'first-item': !index }">
                <icon name="linux" size="200" style="fill:#3b3b3b" class="option-logo" />
                <div class="option-details">
                    <h5>{{ model.moduleName }}</h5>
                    <ul class="modelspecs-item">
                        <li v-for="(item, mindex) in model.description" :key="mindex">{{ item }}</li>
                    </ul>
                    <bk-button theme="primary" class="select-btn" @click="selectModel(model)">
                        <span>{{ $t('environment.select') }}</span>
                    </bk-button>
                    <span class="model-desc">{{ model.produceTime }}</span>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    export default {
        data () {
            return {
                showContent: false,
                currentType: 'linux',
                modelList: [
                    { label: 'macOS', value: 'os', icon: 'macos', isDisabled: true },
                    { label: 'Windows 10', value: 'windows', icon: 'windows', isDisabled: true },
                    { label: 'Linux', value: 'linux', icon: 'linux-view', isDisabled: false }
                ],
                modelOption: [],
                loading: {
                    isLoading: false,
                    title: this.$t('environment.loadingTitle')
                }
            }
        },
        mounted () {
            this.getDevCloudModel()
        },
        methods: {
            async getDevCloudModel () {
                this.loading.isLoading = true
                try {
                    const res = await this.$store.dispatch('environment/requestDevCloudModel', {
                        projectId: this.projectId
                    })
                    this.modelOption.splice(0, this.modelOption.length, ...res || [])
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
                }
            },
            changeTab (val) {
                if (!val.isDisabled && this.currentType !== val.value) {
                    this.currentType = val.value
                    this.getDevCloudModel()
                }
            },
            selectModel (model) {
                this.$store.commit('environment/modifyProcessHead', {
                    process: 'affirmance',
                    current: 1
                })
                this.$store.commit('environment/selectModelApply', { obj: model })
            }
        }
    }
</script>

<style lang="scss">
    @import './../../scss/conf';
    .node-model-wrapper {
        height: 100%;
        padding-top: 30px;
        text-align: center;
        .model-type-group {
            margin-top: 40px;
            display: flex;
            justify-content: center;
            .item-tab {
                margin-right: -1px;
                padding: 10px 30px;
                line-height: 1.5;
                border: 1px solid #DDE4EB;
                color: #333C48;
                font-weight: bold;
                font-size: 20px;
                text-align: center;
                cursor: pointer;
            }
            .model-logo {
                position: relative;
                top: 4px;
            }
            .hover:hover {
                background-color: #FFF;
                color: $primaryColor;
                z-index: 1;
                border: 1px solid $primaryColor;
            }
            .disabled {
                color: #ccc;
                border-color: #e6e6e6;
                background-color: #fafafa;
                cursor: not-allowed;
            }
            .active {
                background-color: #FFF;
                color: $primaryColor;
                z-index: 1;
                border: 1px solid $primaryColor;
            }
        }
        .selection-modelbox {
            display: flex;
            justify-content: center;
            margin: 80px auto;
            h5 {
                margin-bottom: 30px;
                font-size: 24px;
                text-align: left;
                font-weight: 600;
                color: #111;
            }
            .option-details {
                width: 400px;
                padding: 0 60px;
                border-left: 1px solid #e0e0e0;
                margin-top: 48px;
                text-align: left;
            }
            .first-item .option-details {
                border: none;
            }
        }
        .modelspecs-item > li {
            margin-bottom: 10px;
            color: #333;
        }
        .select-btn {
            margin: 30px auto 8px;
            width: 100%;
        }
        .model-desc {
            font-size: 12px;
        }
    }
</style>
