<template>
    <ul class="manage-code-repo">
        <li
            v-for="(code, index) in list"
            :key="index"
            class="code-item"
        >
            <span class="item-name">{{ code.label }}</span>
            <span
                :class="[{ 'item-link': code.link }, 'item-value']"
                :title="code.value"
                @click="goToLink(code.link)"
            >{{ code.value }}</span>
            <span
                v-if="code.tool && code.tool.show"
                class="item-tool"
            >
                <span
                    v-bk-tooltips="{ content: code.tool.info, placements: ['top'] }"
                    v-if="code.tool.info"
                >
                    <i class="bk-icon icon-info-circle"></i>
                </span>
                <span
                    @click="code.tool.click"
                    class="item-tool-label item-link"
                >{{ code.tool.label }}</span>
            </span>
        </li>
    </ul>
</template>

<script>
    export default {
        props: {
            detail: Object,
            type: String
        },

        data () {
            return {
                list: []
            }
        },

        watch: {
            detail: {
                handler () {
                    this.initData()
                },
                immediate: true
            }
        },

        methods: {
            initData () {
                const methodGenerator = {
                    atom: this.getAtomData
                }

                if (!Object.prototype.hasOwnProperty.call(methodGenerator, this.type) || typeof methodGenerator[this.type] !== 'function') {
                    this.$bkMessage({ message: this.$t('store.typeError'), theme: 'error' })
                    return
                }

                const currentMethod = methodGenerator[this.type]
                currentMethod()
            },

            getAtomData () {
                this.list = [
                    { label: this.$t('store.开发语言：'), value: this.detail.language }
                ]
            },

            goToLink (link) {
                if (link) window.open(link, '_blank')
            }
        }
    }
</script>

<style lang="scss" scoped>
    .manage-code-repo {
        margin-top: 8px;
        font-size: 14px;
        line-height: 20px;
        .code-item {
            padding: 8px 0;
            display: flex;
            align-items: center;
            .item-name {
                display: inline-block;
                color: #999999;
                width: 77px;
                text-align: right;
            }
            .item-value {
                max-width: calc(3.81rem - 215px);
                display: inline-block;
                white-space: nowrap;
                overflow: hidden;
                text-overflow: ellipsis;
            }
            .item-link {
                color: #1592ff;
                cursor: pointer;
            }
            .icon-info-circle {
                margin-left: 5px;
            }
        }
    }
</style>
