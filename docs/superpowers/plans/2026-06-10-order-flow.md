# 下单流程完善 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan.

**Goal:** 完善用户端下单流程
**Architecture:** 新增OrderModal和PaymentModal组件，修改ChatView/OrderList/OrderDetail/PaymentModal复用，OrderCard加倒计时。
**Tech Stack:** Vue 3 + Composition API, CSS Variables, SweetAlert2

---

## File Structure

### New files:
- frontier-user/src/components/OrderModal/OrderModal.vue
- frontier-user/src/components/OrderModal/useOrderModal.js
- frontier-user/src/components/OrderModal/OrderModal.css
- frontier-user/src/components/OrderModal/Text.js
- frontier-user/src/components/PaymentModal/PaymentModal.vue
- frontier-user/src/components/PaymentModal/usePaymentModal.js
- frontier-user/src/components/PaymentModal/PaymentModal.css
- frontier-user/src/components/PaymentModal/Text.js

### Modified files:
- frontier-user/src/views/Chat/ChatView/ChatView.vue
- frontier-user/src/views/Chat/ChatView/useChatView.js
- frontier-user/src/views/Order/OrderListView/useOrderList.js
- frontier-user/src/views/Order/OrderDetailView/useOrderDetail.js
- frontier-user/src/components/OrderCard/OrderCard.vue
- frontier-user/src/components/OrderCard/useOrderCard.js
- frontier-user/src/components/OrderCard/OrderCard.css

---

---
### Task 1: Create OrderModal – Text.js

**Files:**
- Create: `frontier-user/src/components/OrderModal/Text.js`

- [ ] **Step 1: Write Text.js**

```js
export const ORDER_MODAL_TEXT = {
  TITLE: "确认下单",
  QUANTITY: "数量",
  ADDRESS: "收货地址",
  NO_ADDRESS: "暂无收货地址，请先添加",
  MANAGE_ADDRESS: "管理地址",
  TOTAL: "合计",
  SUBMIT: "确认下单",
  SUBMITTING: "提交中...",
}
```

---

### Task 2: Create OrderModal – useOrderModal.js

**Files:**
- Create: `frontier-user/src/components/OrderModal/useOrderModal.js`

- [ ] **Step 1: Write useOrderModal.js**

```js
import { ref, computed, watch } from "vue"
import { getContactList } from "@/api/contact"
import { placeOrder } from "@/api/order"
import { showError } from "@/utils/swal"
import { requireLogin } from "@/stores/authStore"

export function useOrderModal(props, { emit }) {
  const quantity = ref(1)
  const contacts = ref([])
  const selectedContactId = ref(null)
  const submitting = ref(false)
  const loadingAddress = ref(false)
  const maxQuantity = computed(() => props.product?.stock ?? 1)
  const totalPrice = computed(() => (props.product?.price || 0) * quantity.value)
  const canSubmit = computed(() => quantity.value > 0 && quantity.value <= maxQuantity.value && selectedContactId.value !== null && !submitting.value)

  const decrement = () => { if (quantity.value > 1) quantity.value-- }
  const increment = () => { if (quantity.value < maxQuantity.value) quantity.value++ }

  const loadContacts = async () => {
    loadingAddress.value = true
    try {
      const res = await getContactList()
      contacts.value = res.contacts || []
      const def = contacts.value.find(c => c.isDefault === 1 || c.isDefault === true)
      if (def) selectedContactId.value = def.id
      else if (contacts.value.length) selectedContactId.value = contacts.value[0].id
    } catch { showError("加载地址失败") }
    finally { loadingAddress.value = false }
  }

  const handleSubmit = async () => {
    if (!requireLogin() || !canSubmit.value) return
    submitting.value = true
    try {
      const oid = await placeOrder({ productId: props.product.id, quantity: quantity.value, contactId: selectedContactId.value })
      emit("order-placed", oid)
    } catch (e) { showError(e.message || "下单失败") }
    finally { submitting.value = false }
  }

  watch(() => props.visible, (v) => { if (v) { quantity.value = 1; selectedContactId.value = null; loadContacts() } })

  return { quantity, contacts, selectedContactId, totalPrice, maxQuantity, canSubmit, submitting, loadingAddress, decrement, increment, handleSubmit }
}
```

---

### Task 3: Create OrderModal ? OrderModal.css

**Files:**
- Create: `frontier-user/src/components/OrderModal/OrderModal.css`

- [ ] **Step 1: Write OrderModal.css**

Key CSS classes:
- `.order-modal-overlay` - fixed fullscreen overlay, flex align-items: flex-end, z-index 1000
- `.order-modal` - 100% width max-480px, white bg, rounded top corners, slide-up animation
- `.order-modal__qty-selector` - inline-flex with border, +/- buttons on sides, input in middle
- `.order-modal__address-item` - flex card with radio-style selection, blue highlight when selected
- `.order-modal__footer` - flex with total amount + blue submit button
- `.order-modal__submit:disabled` - lighter blue, cursor not-allowed

Colors: #3b82f6 (primary blue), #ef4444 (price red), #64748b (label gray), #1e293b (text dark)

---

### Task 4: Create OrderModal ? OrderModal.vue

**Files:**
- Create: `frontier-user/src/components/OrderModal/OrderModal.vue`

- [ ] **Step 1: Write OrderModal.vue**

Structure:
1. Teleport to="body" overlay div
2. Header: "????" title + X close button
3. Body: Product card (image, name, price) + Quantity selector (-/input/+) + Address radio list (from getContactList)
4. Footer: Total price (red) + Submit button
5. Script: import Text.js, useOrderModal, useRouter; defineProps(visible, product); defineEmits(close, order-placed); onQtyInput validation; goManageAddress navigation

Use existing patterns from ContactView's modal (Teleport overlay, click.stop on inner, CSS animations)

---

### Task 5: Create PaymentModal ? Text.js

**Files:**
- Create: `frontier-user/src/components/PaymentModal/Text.js`

- [ ] **Step 1: Write Text.js**

```js
export const PAYMENT_MODAL_TEXT = {
  TITLE: "??????",
  ORDER_ID: "???",
  TOTAL: "??",
  PAY_METHOD: "????",
  WECHAT: "????",
  ALIPAY: "???",
  PAY_NOW: "????",
  PAY_LATER: "????",
  PAYING: "???...",
  PAY_SUCCESS: "????",
  TIMEOUT_WARNING: "???? {minutes} ???????",
}
```

---

### Task 6: Create PaymentModal ? usePaymentModal.js

**Files:**
- Create: `frontier-user/src/components/PaymentModal/usePaymentModal.js`

- [ ] **Step 1: Write usePaymentModal.js**

```js
import { ref, computed, watch } from "vue"
import { payOrder } from "@/api/order"
import { showSuccess, showError } from "@/utils/swal"

export function usePaymentModal(props, { emit }) {
  const selectedMethod = ref("wechat")
  const paying = ref(false)
  const paid = ref(false)
  const remainingMinutes = ref(30)
  let timer = null

  const methods = [
    { key: "wechat", label: "????", icon: "??" },
    { key: "alipay", label: "???", icon: "??" },
    { key: "unionpay", label: "????", icon: "??" },
  ]

  const calcRemaining = () => {
    if (!props.orderDate) return 30
    const elapsed = Math.floor((Date.now() - new Date(props.orderDate).getTime()) / 60000)
    return Math.max(0, 30 - elapsed)
  }

  const startTimer = () => {
    stopTimer()
    timer = setInterval(() => {
      remainingMinutes.value = calcRemaining()
      if (remainingMinutes.value <= 0) { stopTimer(); emit("timeout") }
    }, 10000)
  }

  const stopTimer = () => { if (timer) { clearInterval(timer); timer = null } }

  const handlePay = async () => {
    if (paying.value || paid.value) return
    paying.value = true
    try {
      await payOrder(props.orderId)
      paid.value = true; showSuccess("????"); stopTimer(); emit("pay-success")
    } catch { showError("????") }
    finally { paying.value = false }
  }

  const handlePayLater = () => { stopTimer(); emit("pay-later") }

  watch(() => props.visible, (v) => {
    if (v) { selectedMethod.value = "wechat"; paying.value = false; paid.value = false; remainingMinutes.value = calcRemaining(); startTimer() }
    else { stopTimer() }
  })

  return { selectedMethod, paying, paid, methods, remainingMinutes, handlePay, handlePayLater }
}
```

---

### Task 7: Create PaymentModal ? PaymentModal.css

**Files:**
- Create: `frontier-user/src/components/PaymentModal/PaymentModal.css`

- [ ] **Step 1: Write PaymentModal.css**

Key CSS classes:
- `.payment-modal-overlay` - centered overlay, z-index 1100
- `.payment-modal` - 90% max-400px, white bg, rounded card, scale-in animation
- `.payment-modal__order-summary` - light gray bg, order details rows
- `.payment-modal__method-item` - flex card with radio-style selection, blue highlight
- `.payment-modal__timeout` - amber/amber-light warning banner (shown when <=10min)
- `.payment-modal__pay-later-btn` - ghost button with gray border
- `.payment-modal__success` - centered success state with green checkmark

---

### Task 8: Create PaymentModal ? PaymentModal.vue

**Files:**
- Create: `frontier-user/src/components/PaymentModal/PaymentModal.vue`

- [ ] **Step 1: Write PaymentModal.vue**

Structure:
1. Teleport overlay with click-to-payLater handler
2. Header with title + close button (hidden when paid)
3. Body shows either:
   - Success state (paid=true): green checkmark + success text + ok button
   - Payment state: order summary (orderId, productName, qty, total), timeout warning bar (if <=10min), method list (3 options), pay now button + pay later button
4. Import Text.js, usePaymentModal, PaymentModal.css

---

### Task 9: Modify ChatView ? handle ProductCard events

**Files:**
- Modify: `frontier-user/src/views/Chat/ChatView/ChatView.vue`
- Modify: `frontier-user/src/views/Chat/ChatView/useChatView.js`

- [ ] **Step 1: Add `@buyNow` and `@viewDetail` to ProductCard in ChatView.vue**

In the ProductCard element, change:
```vue
<ProductCard ... @viewDetail="..." />
```
to:
```vue
<ProductCard ... @viewDetail="(prod) => handleViewDetail(prod)" @buyNow="(prod) => handleBuyNow(prod)" />
```

- [ ] **Step 2: Add OrderModal and PaymentModal to ChatView.vue template**

After the chat-view closing div, add:
```vue
<OrderModal :visible="showOrderModal" :product="selectedProduct" @close="showOrderModal = false" @order-placed="onOrderPlaced" />
<PaymentModal :visible="showPaymentModal" :orderId="placedOrderId" @close="showPaymentModal = false" @pay-success="onPaymentSuccess" @pay-later="onPayLater" />
```

Import components:
```js
import OrderModal from "@/components/OrderModal/OrderModal.vue"
import PaymentModal from "@/components/PaymentModal/PaymentModal.vue"
```

- [ ] **Step 3: Add handlers to useChatView.js**

Add refs: selectedProduct=ref(null), showOrderModal=ref(false), showPaymentModal=ref(false), placedOrderId=ref("")

Add handlers:
- handleBuyNow(product): sets selectedProduct, opens order modal
- handleViewDetail(product): showInfo swal with product details
- onOrderPlaced(orderId): closes order modal, opens payment modal
- onPaymentSuccess/onPayLater: closes payment modal, clears orderId

Return all new refs and handlers from the composable.

---

### Task 10: Update OrderListView ? use PaymentModal

**Files:**
- Modify: `frontier-user/src/views/Order/OrderListView/useOrderList.js`

- [ ] **Step 1: Replace direct pay with PaymentModal**

Add refs: payingOrder=ref(null), showPaymentModal=ref(false)

Change handlePay to open PaymentModal:
```js
const handlePay = async (order) => { payingOrder.value = order; showPaymentModal.value = true }
const onPaymentSuccess = async () => { showPaymentModal.value = false; payingOrder.value = null; await loadOrders() }
const onPayLater = () => { showPaymentModal.value = false; payingOrder.value = null }
```

- [ ] **Step 2: Add PaymentModal to OrderListView.vue template**

```vue
<PaymentModal :visible="showPaymentModal" :orderId="payingOrder?.orderId" :order="payingOrder" @close="showPaymentModal = false" @pay-success="onPaymentSuccess" @pay-later="onPayLater" />
```

---

### Task 11: Update OrderDetailView ? use PaymentModal

**Files:**
- Modify: `frontier-user/src/views/Order/OrderDetailView/useOrderDetail.js`

- [ ] **Step 1: Replace direct pay with PaymentModal**

Remove payOrder import. Add ref: showPaymentModal=ref(false)

Change handlePay:
```js
const handlePay = async () => { showPaymentModal.value = true }
const onPaymentSuccess = async () => { showPaymentModal.value = false; await loadOrder() }
const onPayLater = () => { showPaymentModal.value = false }
```

- [ ] **Step 2: Add PaymentModal to OrderDetailView.vue template**

```vue
<PaymentModal :visible="showPaymentModal" :orderId="order?.orderId" :order="order" :orderDate="order?.orderDate" @close="showPaymentModal = false" @pay-success="onPaymentSuccess" @pay-later="onPayLater" />
```

---

### Task 12: Update OrderCard ? add 30min timeout display

**Files:**
- Modify: `frontier-user/src/components/OrderCard/useOrderCard.js`
- Modify: `frontier-user/src/components/OrderCard/OrderCard.vue`
- Modify: `frontier-user/src/components/OrderCard/OrderCard.css`

- [ ] **Step 1: Add countdown logic to useOrderCard.js**

```js
import { ref, onMounted, onUnmounted } from "vue"
// ... existing code stays ...

// Add to the composable:
const remainingMinutes = ref(0)
let timer = null

const updateCountdown = () => {
  if (!props.order?.orderDate) return
  const elapsed = Math.floor((Date.now() - new Date(props.order.orderDate).getTime()) / 60000)
  remainingMinutes.value = Math.max(0, 30 - elapsed)
}

onMounted(() => {
  if (props.order?.orderStatus === "PENDING" && props.order?.orderDate) {
    updateCountdown()
    timer = setInterval(updateCountdown, 60000)
  }
})

onUnmounted(() => { if (timer) clearInterval(timer) })
```

Return remainingMinutes from the composable.

- [ ] **Step 2: Add timeout display to OrderCard.vue template**

In both abstract and detail variants, after the meta/info section, add:
```html
<div v-if="order.orderStatus === 'PENDING' && remainingMinutes > 0" class="order-card__timeout">
  ? ?? {{ remainingMinutes }} ??????
</div>
<div v-if="order.orderStatus === 'PENDING' && remainingMinutes <= 0" class="order-card__timeout order-card__timeout--expired">
  ? ????
</div>
```

- [ ] **Step 3: Add timeout styles to OrderCard.css**

```css
.order-card__timeout {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: 11px; color: #d97706; background: #fffbeb;
  padding: 2px 8px; border-radius: 4px; margin-top: 4px;
}
.order-card__timeout--expired { color: #dc2626; background: #fef2f2; }
```

---

## Self-Review

**1. Spec coverage:**
- [x] ??????????? ? Task 9 (ChatView??buyNow)
- [x] ?????? ? Task 9 (viewDetail?swal??)
- [x] ???? ? Tasks 1-4 (OrderModal????)
- [x] ???? ? Tasks 5-8 (PaymentModal????)
- [x] ?????? ? Task 10 (???PaymentModal)
- [x] ?????? ? Task 11 (???PaymentModal)
- [x] 30??????? ? Task 12 (OrderCard countdown)
- [x] ?????? ? PaymentModal?pay-later??
- [x] ??????? ? onOrderPlaced??????PaymentModal

**2. Placeholder scan:** ?????????????TBD/TODO????

**3. Type consistency:**
- OrderModal??product: { id, name, price, imageUrl, stock }
- PaymentModal??: orderId (string) + order: { orderId, productName, quantity, totalPrice } + orderDate
- OrderCard?remainingMinutes?useOrderCard/template/CSS???