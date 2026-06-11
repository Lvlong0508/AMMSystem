import { ref } from "vue"
import { getLogisticsByOrderId } from "@/api/logistics"
import { showError } from "@/utils/swal"

export function useLogisticsModal() {
  const loading = ref(false)
  const logisticsList = ref([])

  async function loadLogistics(orderId) {
    loading.value = true
    logisticsList.value = []
    try {
      const res = await getLogisticsByOrderId(orderId)
      logisticsList.value = res || []
    } catch (e) {
      console.error("获取物流信息失败", e)
      showError("获取物流信息失败")
      logisticsList.value = []
    } finally {
      loading.value = false
    }
  }

  return { loading, logisticsList, loadLogistics }
}