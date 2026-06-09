import { useShopStore } from '@/store/shop'
import * as T from './Text.js'

export function useDashboard() {
  const shop = useShopStore()
  const shopName = shop.shop?.name || ''
  return { T, shopName }
}
