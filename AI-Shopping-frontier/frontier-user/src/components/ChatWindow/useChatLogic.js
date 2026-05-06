import { ref, nextTick, onMounted, watch } from 'vue'
import MessageBubble from '../MessageBubble/MessageBubble.vue'
import OrderDialog from '../Order/OrderDialog.vue'
import { sendMessage } from '../../api/chat.js'
import { getAllProducts } from '../../api/product.js'
import { buildProductMap } from '../../utils/formatter.js'
import { initRecommendations, refreshRecommendations } from '../../utils/recommendation.js'
import Swal from 'sweetalert2'
import 'sweetalert2/dist/sweetalert2.min.css'

export function useChatLogic() {
  // 从 localStorage 加载历史对话
  const loadChatHistory = () => {
    const saved = localStorage.getItem('chat_history')
    if (saved) {
      try {
        return JSON.parse(saved)
      } catch (e) {
        console.error('Failed to parse chat history:', e)
      }
    }
    return []
  }

  // 保存对话到 localStorage
  const saveChatHistory = (msgs) => {
    localStorage.setItem('chat_history', JSON.stringify(msgs))
  }

  const messages = ref(loadChatHistory())
  const inputMessage = ref('')
  const loading = ref(false)
  const messagesContainer = ref(null)
  const orderDialogVisible = ref(false)
  const selectedProduct = ref(null)
  const paymentDialogVisible = ref(false)
  const paymentOrderInfo = ref(null)
  
  // 推荐商品相关
  const allProducts = ref([])
  const recommendedProducts = ref([])
  const refreshing = ref(false)
  let productMap = {} // 动态商品映射表
  let currentPage = 0 // 当前商品页码
  let refreshCount = 0 // 换一换点击计数
  const REFRESH_THRESHOLD = 3 // 换页阈值（点击3次后加载下一页）

  // 加载推荐商品
  const loadRecommendations = () => {
    recommendedProducts.value = initRecommendations(allProducts.value, 5)
  }

  // 换一换功能
  const handleRefreshProducts = async () => {
    if (refreshing.value || allProducts.value.length === 0) return
    
    refreshing.value = true
    refreshCount++
    
    // 检查是否需要加载更多页的商品
    if (refreshCount >= REFRESH_THRESHOLD) {
      try {
        currentPage++
        const response = await getAllProducts(currentPage, 50)
        const newProducts = response?.data || []
        
        if (newProducts.length > 0) {
          // 追加新商品到列表并去重
          const existingIds = new Set(allProducts.value.map(p => p.id))
          const uniqueNew = newProducts.filter(p => !existingIds.has(p.id))
          allProducts.value = [...allProducts.value, ...uniqueNew]
          productMap = buildProductMap(allProducts.value)
          console.log(`已加载第 ${currentPage} 页商品，当前共 ${allProducts.value.length} 个商品`)
        } else {
          // 没有更多商品了，重置计数
          currentPage = 0
          refreshCount = 0
        }
      } catch (error) {
        console.error('加载下一页商品失败:', error)
      }
      // 重置计数
      refreshCount = 0
    }
    
    // 模拟加载延迟，提升用户体验
    await new Promise(resolve => setTimeout(resolve, 300))
    
    const currentIds = recommendedProducts.value.map(p => p.id || p.productId)
    const newRecommendations = refreshRecommendations(allProducts.value, currentIds, 5)
    
    // 如果无法获取全新的推荐（商品数量不足），则重新初始化
    if (newRecommendations.length === 0) {
      recommendedProducts.value = initRecommendations(allProducts.value, 5)
    } else {
      recommendedProducts.value = newRecommendations
    }
    
    refreshing.value = false
  }

  // 监听消息变化并保存
  watch(messages, (newMsgs) => {
    saveChatHistory(newMsgs)
  }, { deep: true })
  onMounted(async () => {
    try {
      currentPage = 0
      refreshCount = 0
      const response = await getAllProducts()
      allProducts.value = response?.data || []
      productMap = buildProductMap(allProducts.value)
      // 初始化推荐商品
      loadRecommendations()
    } catch (error) {
      console.error('Failed to load products:', error)
    }
  })

  const scrollToBottom = async () => {
    await nextTick()
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  }

  const handleShowOrderDialog = (product) => {
    selectedProduct.value = product
    orderDialogVisible.value = true
  }

  // 订单创建成功，打开支付弹窗
  const handleOrderCreated = (orderInfo) => {
    orderDialogVisible.value = false
    paymentOrderInfo.value = orderInfo
    paymentDialogVisible.value = true
  }

  // 支付成功
  const handlePaymentSuccess = (paymentResult) => {
    paymentDialogVisible.value = false
    Swal.fire({
      icon: 'success',
      title: '支付成功！',
      html: `
        <div style="text-align: left;">
          <p><strong>订单编号：</strong>${paymentResult.orderId}</p>
          <p><strong>支付金额：</strong>¥${paymentResult.totalPrice.toFixed(2)}</p>
          <p style="color: #10b981; margin-top: 10px;">✓ 订单已确认，商家将尽快发货</p>
        </div>
      `,
      confirmButtonText: '确定',
      confirmButtonColor: '#3b82f6'
    })
  }

  // 支付取消
  const handlePaymentCancel = () => {
    paymentDialogVisible.value = false
    Swal.fire({
      icon: 'info',
      title: '支付已取消',
      text: '订单已创建，您可以在订单管理中继续支付',
      confirmButtonText: '确定',
      confirmButtonColor: '#3b82f6'
    })
  }

  const handleSendMessage = async () => {
    if (!inputMessage.value.trim() || loading.value) return

    const userMsg = inputMessage.value.trim()
    inputMessage.value = ''

    // Add user message
    messages.value.push({
      role: 'user',
      text: userMsg,
      products: []
    })

    await scrollToBottom()

    loading.value = true

    try {
      const response = await sendMessage(userMsg)
      const aiReply = response.data.reply

      const { pureText, jsonProducts } = parseAIResponse(aiReply)

      let finalProducts = []

      if (jsonProducts.length > 0) {
        finalProducts = jsonProducts
      } else {
        console.log('没有找到相关商品')
      }

      // Add AI message - 使用 pureText 显示 message 字段内容，不显示 JSON 代码块
      messages.value.push({
        role: 'ai',
        text: pureText || '抱歉，我遇到了一些问题。请稍后重试。',
        products: finalProducts
      })

      await scrollToBottom()
    } catch (error) {
      Swal.fire({
        icon: 'error',
        title: '请求失败',
        text: '抱歉，我遇到了一些问题。请稍后重试。',
        confirmButtonText: '确定'
      })
    } finally {
      loading.value = false
    }
  }

  const startNewChat = () => {
    messages.value = []
    localStorage.removeItem('chat_history')
    currentPage = 0
    refreshCount = 0
  }

  const parseAIResponse = (content) => {
    const result = {
      pureText: '',
      jsonProducts: []
    }

    const jsonRegex = /```json\s?([\s\S]*?)\s?```/
    const match = content.match(jsonRegex)

    if (match && match[1]) {
      try {
        const data = JSON.parse(match[1])
        // 从 data.message 获取文字内容，data.products 获取商品列表
        result.pureText = data.message || ''
        result.jsonProducts = data.products || []
      } catch (e) {
        console.log('JSON 解析失败')
      }
    } else {
      result.pureText = content
    }
    return result
  }

  return {
    messages,
    inputMessage,
    loading,
    messagesContainer,
    orderDialogVisible,
    selectedProduct,
    recommendedProducts,
    refreshing,
    paymentDialogVisible,
    paymentOrderInfo,
    handleShowOrderDialog,
    handleOrderCreated,
    handlePaymentSuccess,
    handlePaymentCancel,
    handleSendMessage,
    handleRefreshProducts,
    startNewChat
  }
}
