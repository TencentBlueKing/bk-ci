<template>
    <article class="market-home" v-bkloading="{ isLoading }">
        <swiper class="home-swiper" :pics="pics"></swiper>
        <section class="home-main">
            <hgroup v-for="cardGroup in cardGroups" :key="cardGroup.key" class="main-group">
                <h3 class="main-title">
                    <span>{{cardGroup.label}}</span>
                    <span v-if="cardGroup.records.length >= 8" class="title-route" @click="showMore(cardGroup.key)">显示全部</span>
                </h3>
                <card v-for="(card, index) in cardGroup.records" :key="index" :atom="card" class="main-card"></card>
                <empty v-if="cardGroup.records <= 0"></empty>
            </hgroup>
        </section>
    </article>
</template>

<script>
    import swiper from '@/components/common/swiper'
    import card from '@/components/common/card'
    import empty from '@/components/common/card/empty'

    export default {
        components: {
            swiper,
            card,
            empty
        },

        data () {
            return {
                isLoading: true,
                pics: [
                    { class: 'first-pic', color: '' }
                ],
                cardGroups: []
            }
        },

        watch: {
            '$route.query.pipeType' () {
                this.getHomeCards()
            }
        },

        created () {
            this.getHomeCards()
        },

        methods: {
            getHomeCards () {
                this.isLoading = true
                const urls = { atom: 'store/requestAtomHome', template: 'store/requestTemplateHome' }
                const type = this.$route.query.pipeType || 'atom'
                const url = urls[type]

                this.$store.dispatch(url)
                    .then((res) => {
                        const data = res || []
                        data.forEach(item => item.records.splice(8))
                        this.cardGroups = data
                    })
                    .catch(err => this.$bkMessage({ message: (err.message || err), theme: 'error' }))
                    .finally(() => (this.isLoading = false))
            },

            showMore (sortType) {
                switch (sortType) {
                    case 'hottest':
                        this.$parent.filterData.sortType = 'DOWNLOAD_COUNT'
                        break
                    case 'latest':
                        this.$parent.filterData.sortType = 'UPDATE_TIME'
                        break
                    default:
                        this.$parent.setClassifyValue(sortType)
                        break
                }
            }
        }
    }
</script>

<style lang="scss" scoped>
    @import '@/assets/scss/conf.scss';

    .market-home {
        width: 940px;
        .home-swiper {
            width: 100%;
            height: 240px;
        }
        .home-main {
            padding: 6px 0 20px;
            .main-group {
                margin-top: 18px;
                .main-title {
                    font-weight: normal;
                    span:nth-child(1) {
                        height: 19px;
                        line-height: 19px;
                        font-size: 15px;
                        color: $fontLightBlack;
                    }
                    .title-route {
                        cursor: pointer;
                        height: 16px;
                        line-height: 16px;
                        font-size: 12px;
                        color: $primaryColor;
                        float: right;
                    }
                }
                .main-card {
                    float: left;
                    margin: 12px 12px 0 0;
                    &:nth-child(4n+1) {
                        margin-right: 0;
                    }
                }
                &:after {
                    content: '';
                    display: table;
                    clear: both;
                }
            }
        }
    }
</style>
