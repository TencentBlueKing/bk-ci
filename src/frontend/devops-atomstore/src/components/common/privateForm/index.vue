<template>
    <section class="private-add" @click.self="cancleOperate">
        <div class="add-main">
            <ul class="add-inputs">
                <li :class="{ 'control-active': isNameFocus, 'input-error': isNameError }">
                    <span class="must-input">字段名：</span>
                    <input type="text" v-model="privateObj.fieldName" class="g-input-border" @focus="isNameFocus = true" @blur="(startNameVerify = true,isNameFocus = false)">
                    <i class="bk-icon icon-close-circle-shape clear-icon" v-if="privateObj.fieldName" @click="privateObj.fieldName = ''"></i>
                    <i class="bk-icon icon-info-circle icon-right" v-bktooltips.top="'以英文字母开头，由英文字母、数字、连接符(-)或下划线(_)组成，长度大于3小于30个字符'"></i>
                    <span v-if="isNameError" class="err-message">以英文字母开头，由英文字母、数字、连接符(-)或下划线(_)组成，长度大于3小于30个字符</span>
                </li>
                <li :class="{ 'control-active': isValueFocus, 'input-error': isValueError }">
                    <span class="must-input">字段值：</span>
                    <input :type="showPassWord ? 'text' : 'password'" v-model="privateObj.fieldValue" class="g-input-border" @focus="valueFocus" @blur="(startValueVerify = true,isValueFocus = false)">
                    <i class="bk-icon icon-close-circle-shape clear-icon" v-if="privateObj.fieldValue" @click="privateObj.fieldValue = ''"></i>
                    <template v-if="showEye">
                        <i class="bk-icon icon-eye icon-right" v-if="showPassWord" @click="showPassWord = false"></i>
                        <i class="bk-icon icon-hide icon-right" v-else @click="showPassWord = true"></i>
                    </template>
                    <span v-if="isValueError" class="err-message">字段值不能为空</span>
                </li>
                <li :class="{ 'control-active': isDesFocus }">
                    <span class="not-must">描述：</span>
                    <textarea v-model="privateObj.fieldDesc" class="g-input-border"></textarea>
                </li>
            </ul>
            <p class="add-buttons">
                <button class="add-button confirm" @click="confirm">确认</button>
                <button class="add-button cancel" @click="cancleOperate">取消</button>
            </p>
        </div>
    </section>
</template>

<script>
    import { mapActions } from 'vuex'

    export default {
        props: {
            sensitiveConfId: String,
            privateObj: Object
        },

        data () {
            return {
                isValueFocus: false,
                startValueVerify: false,
                isNameFocus: false,
                startNameVerify: false,
                showPassWord: false,
                showEye: true
            }
        },

        computed: {
            isNameError () {
                if (!this.startNameVerify) return false

                const isNameVerify = /^[a-zA-Z][a-zA-Z0-9-_]{2,29}$/.test(this.privateObj.fieldName)
                return !isNameVerify
            },

            isValueError () {
                if (!this.startValueVerify) return false

                const isValueVerify = /\S/.test(this.privateObj.fieldValue)
                return !isValueVerify
            }
        },

        created () {
            this.initEyeStatus()
        },

        methods: {
            ...mapActions('store', ['addSensitiveConf', 'modifySensitiveConf']),

            initEyeStatus () {
                if (this.sensitiveConfId) this.showEye = false
            },

            valueFocus () {
                this.isValueFocus = true
                const isOrignPassWord = /^\*+$/.test(this.privateObj.fieldValue)
                if (this.sensitiveConfId && isOrignPassWord) {
                    this.privateObj.fieldValue = ''
                    this.showEye = true
                }
            },

            confirm () {
                this.startValueVerify = true
                this.startNameVerify = true

                const name = this.privateObj.fieldName
                const isNameVerify = /^[a-zA-Z][a-zA-Z0-9-_]{2,29}$/.test(name)
                if (!isNameVerify) {
                    const message = '字段名校验失败，需要以英文字母开头，由英文字母、数字、连接符(-)或下划线(_)组成，长度大于3小于30个字符'
                    this.$bkMessage({ message, theme: 'warning' })
                    return
                }

                if (!this.privateObj.fieldValue) {
                    const message = '字段值校验失败，不能为空'
                    this.$bkMessage({ message, theme: 'warning' })
                    return
                }

                const data = {
                    atomCode: this.$route.params.atomCode,
                    id: this.sensitiveConfId,
                    postData: this.privateObj
                }

                const confirmMethod = this.sensitiveConfId ? this.modifySensitiveConf : this.addSensitiveConf
                confirmMethod(data).then(() => {
                    this.cancleOperate()
                    this.$emit('refresh')
                    this.$bkMessage({ message: '操作成功', theme: 'success' })
                }).catch((err) => {
                    this.$bkMessage({ message: (err.message || err), theme: 'error' })
                })
            },

            cancleOperate () {
                this.$emit('cancle')
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '../../../assets/scss/conf';

    .private-add {
        position: fixed;
        left: 0;
        right: 0;
        top: 0;
        bottom: 0;
        background: rgba(0,0,0,0.6);
        box-shadow: 0px 3px 7px 0px rgba(0,0,0,0.1);
        border-radius: 2px;
        z-index: 900;
        .add-main {
            width: 580px;
            height: 380px;
            margin: calc(50vh - 190px) auto 0;
            background: $bgHoverColor;
            .add-inputs {
                height: 320px;
                padding: 50px 20px;
                .input-error {
                    input {
                        border-color: $failColor;
                        outline-color: $failColor;
                    }
                    i.bk-icon {
                        color: $failColor;
                    }
                }
                li {
                    height: 70px;
                    width: 100%;
                    position: relative;
                    display: flex;
                    justify-content: center;
                    span {
                        text-align: right;
                        display: inline-block;
                        width: 70px;
                        height: 30px;
                    }
                    input {
                        width: 410px;
                        height: 30px;
                        padding: 0 20px 0 5px;
                        margin-right: 30px;
                    }
                    textarea {
                        width: 410px;
                        height: 100px;
                        padding: 5px;
                        margin-right: 30px;
                        resize: none;
                    }
                    .not-must {
                        padding-right: 14px;
                    }
                    .err-message {
                        position: absolute;
                        display: inline-block;
                        width: 410px;
                        bottom: 4px;
                        text-align: left;
                        line-height: 14px;
                        font-size: 12px;
                        right: 43px;
                        color: $failColor;
                    }
                    i {
                        position: absolute;
                        cursor: pointer;
                        height: 16px;
                        width: 16px;
                        right: 48px;
                        top: 9px;
                    }
                    &.control-active i{
                        color: $primaryColor;
                    }
                }
                .must-input {
                    display: flex;
                    align-items: center;
                    &:after {
                        content: '*';
                        color: $failColor;
                        display: inline-block;
                        height: 14px;
                        width: 6px;
                    }
                }
                .icon-right {
                    position: absolute;
                    right: 23px;
                    top: 9px;
                }
            }
            .add-buttons {
                height: 60px;
                display: flex;
                align-items: center;
                border-top: 1px solid $borderWeightColor;
                justify-content: flex-end;
            }
            .add-button {
                width: 70px;
                height: 36px;
                font-size: 14px;
                line-height: 36px;
                text-align: center;
                border: none;
                border-radius: 2px;
                font-weight: normal;
                &.confirm {
                    background: $primaryColor;
                    color: $white;
                    margin-right: 15px;
                }
                &.cancel {
                    color: $fontWeightColor;
                    background: $white;
                    border: 1px solid $lightGray;
                    line-height: 34px;
                    margin-right: 15px;
                }
            }
        }
    }
</style>
