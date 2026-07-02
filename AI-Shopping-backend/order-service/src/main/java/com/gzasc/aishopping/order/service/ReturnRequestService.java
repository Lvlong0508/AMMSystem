package com.gzasc.aishopping.order.service;

import com.gzasc.aishopping.order.dto.AfterSaleVO;
import com.gzasc.aishopping.order.dto.CreateReturnRequest;
import com.gzasc.aishopping.order.dto.ReturnRequestDTO;
import com.gzasc.aishopping.order.dto.ReviewReturnRequest;
import com.gzasc.aishopping.order.dto.SubmitReturnLogisticsRequest;

import java.util.List;

public interface ReturnRequestService {
    void createReturnRequest(Long userId, String orderId, CreateReturnRequest request);

    void reviewReturnRequest(String shopId, String orderId, ReviewReturnRequest request);

    void submitReturnLogistics(Long userId, String orderId, SubmitReturnLogisticsRequest request);

    void deleteReturnRequest(Long userId, String orderId);

    List<AfterSaleVO> getAfterSaleList(Long userId);

    ReturnRequestDTO getReturnRequestByOrderId(String orderId);

    List<ReturnRequestDTO> listByShop(String shopId, String status);
}
