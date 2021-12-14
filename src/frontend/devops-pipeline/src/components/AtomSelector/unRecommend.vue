<template>
    <section v-if="showUnrecommend">
        <h3 :class="[{ 'expand': expandObtained }, 'search-title', 'gap-border', 'uninstall']" @click="expandObtained = !expandObtained">
            {{ $t('editPage.notIntroduce') }}（{{unrecommendArr.length}}）
            <bk-popover placement="top">
                <i class="bk-icon icon-info-circle "></i>
                <div slot="content">
                    {{ $t('editPage.notIntroduceReason') }}
                </div>
            </bk-popover>
        </h3>
        <template v-if="expandObtained">
            <atom-card v-for="atom in unrecommendArr"
                :key="atom.atomCode"
                :disabled="atom.disabled"
                :atom="atom"
                :container="container"
                :element-index="elementIndex"
                :atom-code="atomCode"
                @close="$emit('close')"
                @click.native="$emit('choose', atom.atomCode)"
                :class="{
                    active: atom.atomCode === activeAtomCode,
                    selected: atom.atomCode === atomCode
                }"
            ></atom-card>
        </template>
    </section>
</template>

<script>
    import atomCard from './atomCard'

    export default {
        components: {
            atomCard
        },

        props: {
            showUnrecommend: {
                type: Boolean,
                default: false
            },
            unrecommendArr: {
                type: Array,
                required: true
            },
            activeAtomCode: {
                type: String
            },
            atomCode: {
                type: String
            },
            container: {
                type: Object
            },
            elementIndex: {
                type: Number
            }
        },

        data () {
            return {
                expandObtained: false
            }
        },

        created () {
            setTimeout(() => {
                const index = this.unrecommendArr.findIndex(item => item.atomCode === this.atomCode)
                this.expandObtained = index > -1
            }, 0)
        }
    }
</script>

<style lang="scss" scoped>
    @import '../../scss/conf';
    .search-title {
        line-height:16px;
        font-weight:bold;
        font-size: 12px;
        margin: 9px 0;
        &.gap-border {
            padding-top: 10px;
            border-top: 1px solid #ebf0f5;
        }
    }

    .uninstall{
        position: relative;
        cursor: pointer;
        ::v-deep .bk-tooltip {
            vertical-align: bottom;
        }
        &:after {
            content: '';
            position: absolute;
            right: 4px;
            top: 13px;
            border-right: 1px solid $fontWeightColor;
            border-bottom: 1px solid $fontWeightColor;
            display: inline-block;
            height: 7px;
            width: 7px;
            transform: rotate(-45deg);
            transition: transform 200ms;
            transform-origin: 5.5px 5.5px;
        }
        &.expand:after {
            transform: rotate(45deg);
        }
    }
</style>
