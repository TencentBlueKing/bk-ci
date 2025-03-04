<template>
    <bk-dropdown-menu
        class="devops-button-dropdown"
        :disabled="!isListReady"
    >
        <bk-button
            theme="primary"
            slot="dropdown-trigger"
        >
            <i class="devops-icon icon-plus"></i>
            <span>{{ $t('codelib.linkCodelib') }}</span>
        </bk-button>
        <ul
            class="devops-button-dropdown-menu"
            slot="dropdown-content"
        >
            <li
                v-for="item in codelibTypes"
                :key="item.scmType"
                @click="createCodelib(item.scmType, item.scmCode)"
                :class="{
                    'config-item': true,
                    'disabled-codelib-type': item.status !== 'SUCCESS'
                }"
            >
                <img :src="item.logoUrl" />
                <span class="config-name">{{ item.name }}</span>
                <span class="config-hosts">{{ item.hosts }}</span>
            </li>
        </ul>
    </bk-dropdown-menu>
</template>

<script>
    import { mapState } from 'vuex'
    export default {
        name: 'link-code-lib',
        props: {
            createCodelib: {
                type: Function,
                required: true
            }
        },
        computed: {
            ...mapState('codelib', [
                'codelibTypes'
            ]),
            isListReady () {
                return this.codelibTypes?.length > 0 && !this.disabled
            }
        }
    }
</script>

<style lang="scss">
    @import '../../assets/scss/conf';
    .devops-button-dropdown {
        &.disabled {
            pointer-events: none;
        }
        &-menu {
            > li {
                display: flex;
                align-items: center;
                line-height: 40px;
                padding: 0 15px;
                color: $fontColor;
                font-size: 12px;
                text-decoration: none;
                white-space: nowrap;
                cursor: pointer;

                &:hover {
                    background: $bgColor;
                    color: $primaryColor;
                }
                &.disabled-codelib-type {
                    color: #c4c6cc;
                    cursor: not-allowed;
                }
                img {
                    width: 16px;
                    height: 16px;
                    margin-right: 4px;
                }
                .config-hosts {
                    color: #979BA5;
                    margin-left: 10px;
                }
            }
        }
    }
</style>
