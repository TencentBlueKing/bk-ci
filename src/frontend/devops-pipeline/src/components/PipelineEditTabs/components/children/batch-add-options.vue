<template>
    <section>
        <div class="batch-add">
            {{ $t('editPage.batchAddTit') }}
            <i
                v-bk-tooltips="{ content: '#demo-html1', width: 225 }"
                class="bk-icon icon-info-circle"
            >
            </i>
            <div id="demo-html1">
                <ul id="restore-dots">
                    <li
                        v-for="(item, i) in tooltipsText"
                        :key="i"
                    >
                        {{ item }}
                    </li>
                </ul>
            </div>
        </div>
        <form-field
            :hide-colon="true"
            :desc="$t('editPage.batchAddTips')"
            :label="$t('editPage.batchAdd')"
        >
            <bk-input
                class="key-val"
                :type="'textarea'"
                v-model="batchInput"
                :row="3"
                :placeholder="$t('editPage.optionTips')"
            />
        </form-field>
        <div class="batch-confirm-div">
            <span @click="handleBatchInput">{{ $t('editPage.batchAddBtn') }}</span>
        </div>
    </section>
</template>

<script>
    export default {
        props: {
            submitBatchAdd: {
                type: Function,
                required: true
            }
        },
        data () {
            return {
                batchInput: ''
            }
        },
        computed: {
            tooltipsText () {
                return [
                    this.$t('editPage.lineBreak'),
                    this.$t('editPage.noEnglishCommas'),
                    this.$t('editPage.formatSupport')
                ]
            }
        },
        methods: {
            handleBatchInput () {
                this.submitBatchAdd(this.batchInput)
                this.batchInput = ''
            }
        }
    }
</script>

<style lang="scss" scoped>
    .batch-confirm-div {
        width: 100%;
        padding: 8px 0 16px 0;
        border-bottom: 1px solid #DCDEE5;
        color: #3A84FF;
        cursor: pointer;
        font-size: 12px;
    }
    .batch-add {
        display: flex;
        align-items: center;
        margin-bottom: 8px;
        font-size: 14px;
        color: #63656E;

        .icon-info-circle {
            width: 14px;
            height: 14px;
            margin-left: 9px;
            color: #979BA5;
        }
    }
    #restore-dots, li{
        list-style: circle !important;
        list-style-type: disc !important;
    }
    #restore-dots {
        padding-left: 10px;

    }
</style>
