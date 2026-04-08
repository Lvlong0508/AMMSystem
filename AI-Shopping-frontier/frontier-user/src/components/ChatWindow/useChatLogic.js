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
  
  // 推荐商品相关
  const allProducts = ref([])
  const recommendedProducts = ref([])
  const refreshing = ref(false)
  let productMap = {} // 动态商品映射表

  // 加载推荐商品
  const loadRecommendations = () => {
    recommendedProducts.value = initRecommendations(allProducts.value, 5)
  }

  // 换一换功能
  const handleRefreshProducts = async () => {
    if (refreshing.value || allProducts.value.length === 0) return
    
    refreshing.value = true
    
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
      const response = await getAllProducts()
      allProducts.value = response.data || []
      productMap = buildProductMap(response.data)
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

  const handleOrderSuccess = (orderInfo) => {
    orderDialogVisible.value = false
    Swal.fire({
      icon: 'success',
      title: '下单成功！',
      html: `
        <div style="text-align: left;">
          <p><strong>订单编号：</strong>${orderInfo.orderId}</p>
          <p><strong>商品：</strong>${orderInfo.product.name}</p>
          <p><strong>数量：</strong>${orderInfo.quantity}</p>
          <p><strong>总额：</strong>¥${orderInfo.totalPrice.toFixed(2)}</p>
        </div>
      `,
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
    handleShowOrderDialog,
    handleOrderSuccess,
    handleSendMessage,
    handleRefreshProducts,
    startNewChat
  }
}
