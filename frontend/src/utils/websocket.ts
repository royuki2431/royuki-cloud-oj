import { Client, IMessage } from '@stomp/stompjs'

// 评测状态消息
export interface JudgeStatusMessage {
    submissionId: number
    status: string
    message: string
    timestamp: number
}

// 评测结果消息
export interface JudgeResultMessage {
    submissionId: number
    status: string
    statusDesc: string
    score: number
    timeUsed: number
    memoryUsed: number
    passRate: string
    errorMessage: string | null
    testCaseResults: Array<{
        testCaseId: number
        status: string
        timeUsed: number
        memoryUsed: number
        input: string
        expectedOutput: string
        actualOutput: string
        errorMessage: string | null
    }>
}

// WebSocket 配置
const WS_URL = import.meta.env.VITE_WS_URL || 'ws://localhost:8080/ws/judge'

class JudgeWebSocket {
    private client: Client | null = null
    private userId: number | null = null
    private statusCallbacks: Map<number, (msg: JudgeStatusMessage) => void> = new Map()
    private resultCallbacks: Map<number, (msg: JudgeResultMessage) => void> = new Map()
    private globalStatusCallback: ((msg: JudgeStatusMessage) => void) | null = null
    private globalResultCallback: ((msg: JudgeResultMessage) => void) | null = null
    private connected = false
    private reconnectAttempts = 0
    private maxReconnectAttempts = 5

    /**
     * 连接 WebSocket
     */
    connect(userId: number): Promise<void> {
        return new Promise((resolve, reject) => {
            if (this.connected && this.userId === userId) {
                resolve()
                return
            }

            this.userId = userId
            this.disconnect()

            this.client = new Client({
                brokerURL: WS_URL,
                reconnectDelay: 5000,
                heartbeatIncoming: 4000,
                heartbeatOutgoing: 4000,
                debug: (str) => {
                    if (import.meta.env.DEV) {
                        console.log('[WebSocket]', str)
                    }
                },
                onConnect: () => {
                    console.log('[WebSocket] 连接成功')
                    this.connected = true
                    this.reconnectAttempts = 0
                    this.subscribeToTopics()
                    resolve()
                },
                onDisconnect: () => {
                    console.log('[WebSocket] 连接断开')
                    this.connected = false
                },
                onStompError: (frame) => {
                    console.error('[WebSocket] STOMP 错误:', frame.headers['message'])
                    reject(new Error(frame.headers['message']))
                },
                onWebSocketError: (event) => {
                    console.error('[WebSocket] WebSocket 错误:', event)
                    this.reconnectAttempts++
                    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
                        reject(new Error('WebSocket 连接失败'))
                    }
                }
            })

            this.client.activate()
        })
    }

    /**
     * 订阅评测状态和结果 topic
     */
    private subscribeToTopics() {
        if (!this.client || !this.userId) return

        // 订阅评测状态
        this.client.subscribe(`/topic/judge-status/${this.userId}`, (message: IMessage) => {
            const data: JudgeStatusMessage = JSON.parse(message.body)
            console.log('[WebSocket] 收到评测状态:', data)

            // 调用特定提交的回调
            const callback = this.statusCallbacks.get(data.submissionId)
            if (callback) {
                callback(data)
            }

            // 调用全局回调
            if (this.globalStatusCallback) {
                this.globalStatusCallback(data)
            }
        })

        // 订阅评测结果
        this.client.subscribe(`/topic/judge-result/${this.userId}`, (message: IMessage) => {
            const data: JudgeResultMessage = JSON.parse(message.body)
            console.log('[WebSocket] 收到评测结果:', data)

            // 调用特定提交的回调
            const callback = this.resultCallbacks.get(data.submissionId)
            if (callback) {
                callback(data)
                // 收到结果后移除回调
                this.resultCallbacks.delete(data.submissionId)
                this.statusCallbacks.delete(data.submissionId)
            }

            // 调用全局回调
            if (this.globalResultCallback) {
                this.globalResultCallback(data)
            }
        })

        console.log(`[WebSocket] 已订阅用户 ${this.userId} 的评测通知`)
    }

    /**
     * 监听特定提交的评测结果
     */
    onSubmissionResult(
        submissionId: number,
        onStatus: (msg: JudgeStatusMessage) => void,
        onResult: (msg: JudgeResultMessage) => void
    ) {
        this.statusCallbacks.set(submissionId, onStatus)
        this.resultCallbacks.set(submissionId, onResult)
    }

    /**
     * 设置全局回调（用于提交历史页面等）
     */
    setGlobalCallbacks(
        onStatus: ((msg: JudgeStatusMessage) => void) | null,
        onResult: ((msg: JudgeResultMessage) => void) | null
    ) {
        this.globalStatusCallback = onStatus
        this.globalResultCallback = onResult
    }

    /**
     * 移除特定提交的监听
     */
    removeSubmissionListener(submissionId: number) {
        this.statusCallbacks.delete(submissionId)
        this.resultCallbacks.delete(submissionId)
    }

    /**
     * 断开连接
     */
    disconnect() {
        if (this.client) {
            this.client.deactivate()
            this.client = null
        }
        this.connected = false
        this.statusCallbacks.clear()
        this.resultCallbacks.clear()
    }

    /**
     * 是否已连接
     */
    isConnected(): boolean {
        return this.connected
    }
}

// 导出单例
export const judgeWebSocket = new JudgeWebSocket()
