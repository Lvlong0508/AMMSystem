package com.gzasc.aishopping.order.service.impl;

import com.gzasc.aishopping.common.dto.logistics.LogisticsRequest;
import com.gzasc.aishopping.common.feign.logistics.LogisticsFeignClient;
import com.gzasc.aishopping.common.response.ApiResponse;
import com.gzasc.aishopping.order.dto.AfterSaleVO;
import com.gzasc.aishopping.order.dto.CreateReturnRequest;
import com.gzasc.aishopping.order.dto.OrderDetailDTO;
import com.gzasc.aishopping.order.dto.ReturnRequestDTO;
import com.gzasc.aishopping.order.dto.ReviewReturnRequest;
import com.gzasc.aishopping.order.dto.SubmitReturnLogisticsRequest;
import com.gzasc.aishopping.order.converter.OrderConverter;

import com.gzasc.aishopping.order.exception.OrderException;
import com.gzasc.aishopping.order.mapper.ReturnRequestMapper;
import com.gzasc.aishopping.order.model.Order;
import com.gzasc.aishopping.order.model.ReturnRequest;
import com.gzasc.aishopping.order.service.OrderService;
import com.gzasc.aishopping.order.service.ReturnRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReturnRequestServiceImpl implements ReturnRequestService {
    private final ReturnRequestMapper returnRequestMapper;
    private final OrderService orderService;
    private final LogisticsFeignClient logisticsFeignClient;
    private final OrderConverter orderConverter;

    @Override
    @Transactional
    public void createReturnRequest(Long userId, String orderId, CreateReturnRequest request) {
        OrderDetailDTO order = orderService.getOrderDetailByUser(userId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在或无权限操作");
        }
        if (!Order.SHIPPED.equals(order.getOrderStatus()) && !Order.DELIVERED.equals(order.getOrderStatus())) {
            throw new OrderException("当前订单状态不允许申请退货");
        }
        if (returnRequestMapper.selectByOrderId(orderId) != null) {
            throw new OrderException("该订单已存在退货申请");
        }

        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setOrderId(orderId);
        returnRequest.setUserId(userId);
        returnRequest.setShopId(order.getShopId());
        returnRequest.setReturnReason(request.getReturnReason());
        returnRequest.setStatus(ReturnRequest.APPLYING);
        returnRequest.setCreatedDate(new Timestamp(System.currentTimeMillis()));

        int inserted;
        try {
            inserted = returnRequestMapper.insert(returnRequest);
        } catch (RuntimeException e) {
            throw new OrderException("创建退货申请失败");
        }
        if (inserted <= 0) {
            throw new OrderException("创建退货申请失败");
        }
        log.info("退货申请已提交, orderId={}", orderId);
    }

    @Override
    @Transactional
    public void reviewReturnRequest(String shopId, String orderId, ReviewReturnRequest request) {
        if (!ReturnRequest.AGREED.equals(request.getStatus()) && !ReturnRequest.REJECTED.equals(request.getStatus())) {
            throw new OrderException("审核状态无效");
        }

        ReturnRequest returnRequest = returnRequestMapper.selectByOrderIdAndShop(orderId, shopId);
        if (returnRequest == null || !returnRequest.isApplying()) {
            throw new OrderException("退货申请不存在或已被处理");
        }

        int updated = returnRequestMapper.updateStatus(orderId, request.getStatus());
        if (updated <= 0) {
            throw new OrderException("审核失败");
        }

        if (ReturnRequest.AGREED.equals(request.getStatus())) {
            orderService.agreeReturnRequest(shopId, orderId);
            log.info("退货审核通过, orderId={}", orderId);
        } else {
            log.info("退货申请已拒绝, orderId={}", orderId);
        }
    }

    @Override
    @Transactional
    public void submitReturnLogistics(Long userId, String orderId, SubmitReturnLogisticsRequest request) {
        ReturnRequest returnRequest = returnRequestMapper.selectByOrderIdAndUser(orderId, userId);
        if (returnRequest == null) {
            throw new OrderException("退货申请不存在");
        }
        if (!returnRequest.isAgreed()) {
            throw new OrderException("退货申请未通过审核");
        }
        if (returnRequest.getLogisticsId() != null) {
            throw new OrderException("已提交过退货物流信息");
        }

        OrderDetailDTO order = orderService.getOrderDetailByUser(userId, orderId);
        if (order == null) {
            throw new OrderException("订单不存在");
        }
        if (!Order.RETURN_PENDING.equals(order.getOrderStatus())) {
            throw new OrderException("订单状态不允许提交退货物流");
        }

        LogisticsRequest logisticsRequest = new LogisticsRequest();
        logisticsRequest.setOrderId(orderId);
        logisticsRequest.setType("RETURN");
        logisticsRequest.setContactId(request.getContactId());
        logisticsRequest.setTrackingNumber(request.getTrackingNumber());

        ApiResponse<Map<String, Object>> response;
        try {
            response = logisticsFeignClient.createLogistics(logisticsRequest);
        } catch (RuntimeException e) {
            throw new OrderException("创建退货物流失败");
        }
        if (response == null || response.getCode() != 200 || response.getData() == null) {
            throw new OrderException("创建退货物流失败");
        }
        Object id = response.getData().get("id");
        int logisticsId = parseLogisticsId(id);

        int updated = returnRequestMapper.updateLogisticsId(orderId, logisticsId);
        if (updated <= 0) {
            throw new OrderException("更新退货物流信息失败");
        }

        orderService.submitReturnLogisticsStatus(userId, orderId);
        log.info("退货物流已提交, orderId={}, logisticsId={}", orderId, logisticsId);
    }

    @Override
    @Transactional
    public void deleteReturnRequest(Long userId, String orderId) {
        ReturnRequest returnRequest = returnRequestMapper.selectByOrderIdAndUser(orderId, userId);
        if (returnRequest == null) {
            throw new OrderException("退货申请不存在");
        }

        if (ReturnRequest.REJECTED.equals(returnRequest.getStatus())) {
            int deleted = returnRequestMapper.deleteByOrderIdAndUser(orderId, userId);
            if (deleted <= 0) {
                throw new OrderException("删除退货申请失败");
            }
            log.info("退货申请已删除, orderId={}", orderId);
            return;
        }

        if (ReturnRequest.AGREED.equals(returnRequest.getStatus())) {
            OrderDetailDTO order = orderService.getOrderDetailByUser(userId, orderId);
            if (order != null && Order.RETURNED.equals(order.getOrderStatus())) {
                int deleted = returnRequestMapper.deleteByOrderIdAndUser(orderId, userId);
                if (deleted <= 0) {
                    throw new OrderException("删除退货申请失败");
                }
                log.info("退货申请已删除, orderId={}", orderId);
                return;
            }
        }

        throw new OrderException("当前退货状态不允许删除");
    }

    private int parseLogisticsId(Object id) {
        if (id == null) {
            throw new OrderException("获取物流ID失败");
        }
        try {
            long value = id instanceof Number ? ((Number) id).longValue() : Long.parseLong(id.toString());
            if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
                throw new OrderException("获取物流ID失败");
            }
            return (int) value;
        } catch (NumberFormatException e) {
            throw new OrderException("获取物流ID失败");
        }
    }

    @Override
    public List<AfterSaleVO> getAfterSaleList(Long userId) {
        List<ReturnRequest> returnRequests = returnRequestMapper.selectByUserId(userId);
        if (returnRequests.isEmpty()) return List.of();

        return returnRequests.stream()
                .map(r -> buildAfterSaleVO(userId, r))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private AfterSaleVO buildAfterSaleVO(Long userId, ReturnRequest r) {
        OrderDetailDTO order;
        try {
            order = orderService.getOrderDetailByUser(userId, r.getOrderId());
        } catch (Exception e) {
            log.warn("获取订单详情失败, orderId={}, {}", r.getOrderId(), e.getMessage());
            return null;
        }
        if (order == null) return null;

        AfterSaleVO vo = orderConverter.toAfterSaleVO(order, r);

        if (r.getLogisticsId() != null) {
            try {
                ApiResponse<Map<String, Object>> logisticsResp =
                        logisticsFeignClient.getLatestLogistics(r.getOrderId(), "RETURN");
                if (logisticsResp != null && logisticsResp.getData() != null) {
                    vo.setReturnTrackingNumber((String) logisticsResp.getData().get("trackingNumber"));
                }
            } catch (Exception e) {
                log.warn("获取退货物流信息失败, orderId={}", r.getOrderId(), e);
            }
        }

        return vo;
    }

    @Override
    public ReturnRequestDTO getReturnRequestByOrderId(String orderId) {
        ReturnRequest returnRequest = returnRequestMapper.selectByOrderId(orderId);
        if (returnRequest == null) {
            throw new OrderException("退货申请不存在");
        }
        return toDTO(returnRequest);
    }

    @Override
    public List<ReturnRequestDTO> listByShop(String shopId, String status) {
        return returnRequestMapper.selectByShopAndStatus(shopId, status).stream().map(this::toDTO).toList();
    }

    private ReturnRequestDTO toDTO(ReturnRequest returnRequest) {
        ReturnRequestDTO dto = new ReturnRequestDTO();
        dto.setOrderId(returnRequest.getOrderId());
        dto.setUserId(returnRequest.getUserId());
        dto.setShopId(returnRequest.getShopId());
        dto.setReturnReason(returnRequest.getReturnReason());
        dto.setStatus(returnRequest.getStatus());
        dto.setLogisticsId(returnRequest.getLogisticsId());
        dto.setCreatedDate(returnRequest.getCreatedDate());
        dto.setUpdatedDate(returnRequest.getUpdatedDate());
        return dto;
    }
}
