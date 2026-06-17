package com.gzasc.aishopping.order.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc_v6x.callback.BlockExceptionHandler;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.alibaba.fastjson.JSON;
import com.gzasc.aishopping.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class CustomBlockExceptionHandler implements BlockExceptionHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            String resourceName,
            BlockException e) throws IOException {

        log.warn("Sentinel 拦截，资源: {}, 异常类型: {}", resourceName, e.getClass().getSimpleName());

        // 自定义路径报错信息处理
        String msg = "服务繁忙，请稍后重试";
        if("/api/user/order/place".equals(resourceName)){
            if(e instanceof DegradeException){
                msg = "创建订单发生错误，请稍后重试";
            }else {
                msg = "当前下单流量过大，请稍后重试";
            }
        }else{
            // 全局统一处理信息
            if (e instanceof DegradeException) {
                msg = "服务熔断，请稍后重试";
            } else if (e instanceof FlowException) {
                msg = "订单流量过大，请稍后重试";
            }
        }

        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        ApiResponse<Void> resp = ApiResponse.error(429, msg);
        response.getWriter().write(JSON.toJSONString(resp));
    }
}