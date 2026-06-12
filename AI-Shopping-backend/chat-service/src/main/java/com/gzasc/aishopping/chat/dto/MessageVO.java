package com.gzasc.aishopping.chat.dto;

import java.util.List;

public class MessageVO {
    private String role;
    private String text;
    private List<ProductItem> products;

    public MessageVO() {}

    public MessageVO(String role, String text, List<ProductItem> products) {
        this.role = role;
        this.text = text;
        this.products = products;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public List<ProductItem> getProducts() { return products; }
    public void setProducts(List<ProductItem> products) { this.products = products; }
}
