import { request } from './request'

// 发送聊天消息
export const sendMessage = (message) =>
    request.post('/api/user/chat/chat', { message })
