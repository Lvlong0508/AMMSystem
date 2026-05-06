/**
 * 从 AI 回复中提取商品 ID
 * 支持格式：【商品ID: 1001】、[商品ID: 1001]
 */
export function extractProductIds(text) {
    // 匹配【商品ID: XXXX】或 [商品ID: XXXX]
    const regex = /[【\[]商品ID[：:]\s*([a-zA-Z0-9\-]+)[】\]]/g
    const ids = []
    let match
    while ((match = regex.exec(text)) !== null) {
        const id = match[1]
        if (id) ids.push(id)
    }
    return [...new Set(ids)]
}

/**
 * 将商品数组转换为 Map，便于快速查询
 */
export function buildProductMap(products) {
    const map = {}
    if (!products) return map
    products.forEach(product => {
        map[product.id] = product
    })
    return map
}

/**
 * 格式化日期
 */
export function formatDate(date) {
    if (!date) return '-'
    const d = new Date(date)
    return d.toLocaleDateString('zh-CN')
}

/**
 * 格式化价格
 */
export function formatPrice(price) {
    return `¥${price.toFixed(2)}`
}