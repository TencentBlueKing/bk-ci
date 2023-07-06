<template>
    <div class="build-num-switcher">
        <span>#{{ currentBuildNum }}</span>
        <p>
            <i
                class="devops-icon icon-angle-up"
                :disabled="isLatestBuild || isLoading"
                @click="switchBuildNum(1)"
            />
            <i
                class="devops-icon icon-angle-down"
                :disabled="isFirstBuild || isLoading"
                @click="switchBuildNum(-1)"
            />
        </p>
    </div>
</template>

<script>
    import { mapActions } from 'vuex'
    export default {
        props: {
            latestBuildNum: {
                type: Object,
                required: true
            },
            currentBuildNum: {
                type: Object,
                required: true
            }
        },
        data () {
            return {
                isLoading: false
            }
        },
        computed: {
            isLatestBuild () {
                return this.latestBuildNum === this.currentBuildNum
            },
            isFirstBuild () {
                return this.currentBuildNum === 1
            }
        },
        methods: {
            ...mapActions('atom', ['requestPipelineExecDetailByBuildNum']),
            async switchBuildNum (int = 0) {
                const nextBuildNum = this.currentBuildNum + int
                if (this.isLoading || nextBuildNum > this.latestBuildNum || nextBuildNum < 1) {
                    return
                }
                try {
                    this.isLoading = true
                    const response = await this.requestPipelineExecDetailByBuildNum({
                        buildNum: nextBuildNum,
                        ...this.$route.params
                    })
                    this.$router.push({
                        name: 'pipelinesDetail',
                        params: {
                            type: 'executeDetail',
                            ...this.$route.params,
                            buildNo: response.data.id,
                            executeCount: undefined
                        }
                    })
                } catch (error) {
                    // TODO: //
                } finally {
                    this.isLoading = false
                }
            }
        }
    }
</script>

<style lang="scss">
@import "@/scss/conf";
.build-num-switcher {
  display: grid;
  align-items: center;
  grid-auto-flow: column;
  grid-gap: 6px;
  > p {
    display: flex;
    flex-direction: column;
    > i.devops-icon {
      color: $primaryColor;
      cursor: pointer;
      font-size: 10px;

      &[disabled] {
        color: $fontLighterColor;
        cursor: auto;
      }
    }
  }
}
</style>
