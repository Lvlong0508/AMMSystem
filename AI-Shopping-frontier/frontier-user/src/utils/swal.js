import Swal from 'sweetalert2'
import { APP_TITLE } from '../config/messages.js'

// 蓝白配色主题
const swalTheme = {
  confirmButtonColor: '#3b82f6',
  cancelButtonColor: '#6b7280',
  background: '#ffffff',
  color: '#1f2937',
  backdrop: false,
  allowOutsideClick: false,
  customClass: {
    popup: 'swal-blue-white',
    title: 'swal-title',
    confirmButton: 'swal-confirm-btn',
    cancelButton: 'swal-cancel-btn'
  }
}

// 成功提示
export const showSuccess = (message, buttonText = '确定') => {
  return Swal.fire({
    ...swalTheme,
    icon: 'success',
    title: APP_TITLE,
    text: message,
    confirmButtonText: buttonText,
    showCancelButton: false,
    timer: 2000,
    timerProgressBar: true
  })
}

// 错误提示
export const showError = (message, buttonText = '确定') => {
  return Swal.fire({
    ...swalTheme,
    icon: 'error',
    title: APP_TITLE,
    text: message,
    confirmButtonText: buttonText,
    showCancelButton: false
  })
}

// 确认对话框
export const showConfirm = (title, text, confirmText = '确定', cancelText = '取消') => {
  return Swal.fire({
    ...swalTheme,
    icon: 'warning',
    title: title || APP_TITLE,
    text: text,
    showCancelButton: true,
    confirmButtonText: confirmText,
    cancelButtonText: cancelText,
    reverseButtons: true
  })
}

// 警告提示
export const showWarning = (message, buttonText = '确定') => {
  return Swal.fire({
    ...swalTheme,
    icon: 'warning',
    title: APP_TITLE,
    text: message,
    confirmButtonText: buttonText,
    showCancelButton: false
  })
}

// 信息提示
export const showInfo = (message, buttonText = '确定') => {
  return Swal.fire({
    ...swalTheme,
    icon: 'info',
    title: APP_TITLE,
    text: message,
    confirmButtonText: buttonText,
    showCancelButton: false
  })
}
