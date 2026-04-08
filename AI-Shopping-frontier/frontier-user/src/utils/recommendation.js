// 商品推荐算法
// 基于随机打乱和加权排序的推荐策略

/**
 * Fisher-Yates 洗牌算法 - 随机打乱数组
 * @param {Array} array - 要打乱的数组
 * @returns {Array} - 打乱后的新数组
 */
function shuffleArray(array) {
  const shuffled = [...array]
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]]
  }
  return shuffled
}

/**
 * 根据商品热度计算权重分数
 * @param {Object} product - 商品对象
 * @returns {number} - 权重分数
 */
function calculateWeight(product) {
  let weight = 1
  
  // 价格因素：中等价位商品权重更高
  if (product.price) {
    const price = parseFloat(product.price)
    if (price >= 100 && price <= 1000) {
      weight += 2
    } else if (price > 1000 && price <= 5000) {
      weight += 1
    }
  }
  
  // 库存因素：库存充足的商品权重更高
  if (product.stock) {
    const stock = parseInt(product.stock)
    if (stock > 100) {
      weight += 2
    } else if (stock > 50) {
      weight += 1
    }
  }
  
  // 名称长度因素：名称适中的商品权重稍高
  if (product.name) {
    const nameLength = product.name.length
    if (nameLength >= 8 && nameLength <= 20) {
      weight += 1
    }
  }
  
  return weight
}

/**
 * 加权随机选择算法
 * @param {Array} products - 商品列表
 * @param {number} count - 需要选择的数量
 * @returns {Array} - 选中的商品列表
 */
function weightedRandomSelect(products, count) {
  if (products.length <= count) {
    return shuffleArray(products)
  }
  
  // 计算每个商品的权重
  const weightedProducts = products.map(p => ({
    ...p,
    weight: calculateWeight(p),
    randomValue: Math.random()
  }))
  
  // 按权重*随机值排序，权重高的更有可能被选中
  weightedProducts.sort((a, b) => {
    const scoreA = a.weight * a.randomValue
    const scoreB = b.weight * b.randomValue
    return scoreB - scoreA
  })
  
  // 返回前count个商品
  return weightedProducts.slice(0, count).map(({ weight, randomValue, ...product }) => product)
}

/**
 * 推荐商品主函数
 * @param {Array} allProducts - 所有商品列表
 * @param {number} recommendCount - 推荐数量（默认5）
 * @param {Array} excludeIds - 需要排除的商品ID列表
 * @returns {Array} - 推荐的商品列表
 */
export function getRecommendedProducts(allProducts, recommendCount = 5, excludeIds = []) {
  if (!allProducts || allProducts.length === 0) {
    return []
  }
  
  // 过滤掉需要排除的商品
  const availableProducts = allProducts.filter(
    p => !excludeIds.includes(p.id) && !excludeIds.includes(p.productId)
  )
  
  if (availableProducts.length === 0) {
    return []
  }
  
  // 使用加权随机选择算法
  return weightedRandomSelect(availableProducts, recommendCount)
}

/**
 * 获取新的一批推荐商品（换一换功能）
 * @param {Array} allProducts - 所有商品列表
 * @param {Array} currentRecommendedIds - 当前已推荐的商品ID列表
 * @param {number} recommendCount - 推荐数量
 * @returns {Array} - 新的推荐商品列表
 */
export function refreshRecommendations(allProducts, currentRecommendedIds, recommendCount = 5) {
  return getRecommendedProducts(allProducts, recommendCount, currentRecommendedIds)
}

/**
 * 初始化推荐（首次加载）
 * @param {Array} allProducts - 所有商品列表
 * @param {number} recommendCount - 推荐数量
 * @returns {Array} - 初始推荐商品列表
 */
export function initRecommendations(allProducts, recommendCount = 5) {
  return getRecommendedProducts(allProducts, recommendCount, [])
}
