import { request } from './request'

/**
 * AI 聊天相关接口
 *
 * 端口: 8085
 * 用户标识由 Gateway 自动注入 `X-User-Id`
 * 响应为结构化 multi-turn 回复，通过 data.data.type 判别器区分类型:
 *   - "product" → 商品数据
 *   - "order"   → 订单数据
 *   - null      → 纯文本回复
 */

/**
 * AI 对话
 *
 * @param {string} message - 用户消息（必填，不能为空）
 * @returns {Promise<{message: string, reason: string, data: object|null}>}
 *
 * @example
 * // 请求
 * sendMessage('推荐一些商品')
 * // 响应（商品）
 * // {
 * //   message: "为您找到以下商品：",
 * //   reason: "用户请求推荐商品，调用 getAllProducts 获取列表后返回",
 * //   data: {
 * //     type: "product",
 * //     products: [
 * //       { id: 1, name: "商品名称", price: 99.99, tags: "标签1,标签2",
 * //         description: "商品描述", stock: 100, imageUrl: "...", shopName: "店铺名称" }
 * //     ]
 * //   }
 * // }
 *
 * @example
 * // 响应（纯文本）
 * // { message: "您好！有什么可以帮您的吗？", reason: "用户主动打招呼，无具体业务请求", data: null }
 */
export const sendMessage = (message) =>
  request.post('/api/user/chat/chat', { message })
