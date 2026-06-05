export function useActionSheet(props, emit) {
  function handleConfirm() {
    emit('confirm')
  }

  function handleCancel() {
    emit('cancel')
  }

  return {
    handleConfirm,
    handleCancel,
  }
}
