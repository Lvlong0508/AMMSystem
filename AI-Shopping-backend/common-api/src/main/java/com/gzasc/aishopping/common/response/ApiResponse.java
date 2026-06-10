package com.gzasc.aishopping.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> implements Serializable {
    private int code;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> resp = new ApiResponse<T>();
        resp.setCode(200);
        resp.setMessage("操作成功");
        resp.setData(data);
        return resp;
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        ApiResponse<T> resp = new ApiResponse<T>();
        resp.setCode(200);
        resp.setMessage(message);
        resp.setData(data);
        return resp;
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        ApiResponse<T> resp = new ApiResponse<T>();
        resp.setCode(code);
        resp.setMessage(message);
        return resp;
    }

    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> resp = new ApiResponse<T>();
        resp.setCode(500);
        resp.setMessage(message);
        return resp;
    }
}
