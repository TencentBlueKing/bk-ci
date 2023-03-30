<template>
    <div class="console" id="terminal" style="height: 100%; width: 100%; margin: 0; padding: 0;"></div>
</template>

<script>
    import { Base64 } from 'js-base64'
    import { Terminal } from 'xterm'
    import { FitAddon } from 'xterm-addon-fit'
    import { AttachAddon } from 'xterm-addon-attach'
    import 'xterm/css/xterm.css'
    export default {
        name: 'Console',
        props: {
            consoleType: {
                type: String,
                default: ''
            },
            url: {
                type: String,
                default: ''
            },
            resizeUrl: {
                type: String,
                default: ''
            },
            execId: {
                type: String,
                default: ''
            }
        },
        data () {
            return {
                term: null,
                fitAddon: null,
                terminalSocket: null,
                wsUrl: '',
                heartTimer: null
            }
        },
        mounted () {
            window.onresize = this.triggerTermResize
            this.term = new Terminal()
            this.fitAddon = new FitAddon()
            const terminalContainer = document.getElementById('terminal')
            this.term.open(terminalContainer)
            this.term.loadAddon(this.fitAddon)

            if (this.url) {
                this.wsUrl = this.url
                this.terminalSocket = new WebSocket(this.wsUrl)
                this.terminalSocket.onopen = this.runTerminal
                this.terminalSocket.onmessage = this.receiveFromTerminal
                this.terminalSocket.onclose = this.closeTerminal
                this.terminalSocket.onerror = this.errorTerminal
                if (this.consoleType === 'PUBLIC_BCS') {
                    this.term.onData((data) => {
                        this.sendEncodeCmd(data)
                    })
                } else {
                    const attachAddon = new AttachAddon(this.terminalSocket)
                    // Attach the socket to term
                    this.term.loadAddon(attachAddon)
                }
                this.term.onResize(this.handleResize)

                setTimeout(() => {
                    this.fitAddon.fit()
                    this.term.focus()
                    this.term._initialized = true
                }, 1000)

                this.term.write('#######################################################################\r\n#                    Welcome To BKDevOps Console                      #\r\n#######################################################################\r\n')
            } else {
                this.term.write('url is null')
            }
        },
        beforeDestroy () {
            this.terminalSocket.close()
            this.term.dispose()
        },
        methods: {
            // BCS类型，消息需要加密后发送
            sendEncodeCmd (cmd, channel = '0') {
                this.terminalSocket.send(channel + Base64.encode(cmd))
            },
            runTerminal (e) {
                const etcFileCmd = 'source /etc/profile\n'
                if (this.consoleType === 'PUBLIC_BCS') {
                    this.sendEncodeCmd('source /etc/profile\n')
                } else {
                    this.terminalSocket.send(etcFileCmd)
                }
            },
            // BCS类型，消息需要解密后才能显示
            receiveFromTerminal (e) {
                if (this.consoleType === 'PUBLIC_BCS') {
                    this.term.write(Base64.decode(e.data))
                }
                // console.log(e.data, 'receive msg')
            },
            errorTerminal (e) {
                console.log(e, 'error')
            // this.term.write('发生异常 ')
            },
            closeTerminal (e) {
                console.log(e, 'close')
                // this.term.write('连接中断')
            },
            triggerTermResize (e) {
                this.fitAddon.fit()
            },
            handleResize (size) {
                if (this.consoleType === 'PUBLIC_BCS') {
                    const data = JSON.stringify({ cols: size.cols, rows: size.rows })
                    this.sendEncodeCmd(data, '4')
                }
                // this.$nextTick(() => {
                //     this.resizeUrl && this.$store.dispatch('common/resizeTerm', {
                //         resizeUrl: this.resizeUrl,
                //         params: {
                //             exec_id: this.execId,
                //             height: size.rows,
                //             width: size.cols
                //         }
                //     })
                //     !this.resizeUrl && this.terminalSocket.send(`__resize__:${size.rows},${size.cols}\n`)
                // })
            }
        }
    }
</script>

<style lang="scss">
    .terminal {
        height: 100%;
        background: black;
    }
    .xterm .xterm-viewport::-webkit-scrollbar {
        background: transparent;
    }
</style>
