package com.gzasc.aishopping.product.service;

import com.gzasc.aishopping.product.model.Product;
import com.gzasc.aishopping.product.model.ProductImageInfo;

import java.math.BigDecimal;
import java.util.List;

public interface ProductService {

    /**
     * 根据商品ID查询商品详情
     * @param productId 商品ID
     * @return 商品信息，null表示不存在
     */
    Product getProductById(String productId);

    /**
     * 根据商品名称模糊搜索
     * @param name 商品名称关键字
     * @return 符合条件的商品列表
     */
    List<Product> getProductsByName(String name);

    /**
     * 分页查询所有商品
     * @param page 页码，从0开始
     * @return 当前页的商品列表
     */
    List<Product> getAllProducts(int page);

    /**
     * 创建新商品
     * @param product 商品信息
     * @return 影响的行数
     */
    int createProduct(Product product);

    /**
     * 删除商品
     * @param productId 商品ID
     * @return 影响的行数
     */
    int deleteProduct(String productId);

    /**
     * 更新商品信息
     * @param product 商品信息
     * @return 影响的行数
     */
    int updateProduct(Product product);

    /**
     * 扣减商品库存（用于下单）
     * @param productId 商品ID
     * @param quantity 扣减数量
     * @return 是否扣减成功
     */
    boolean deductStock(String productId, int quantity);

    /**
     * 恢复商品库存（用于取消订单、退货）
     * @param productId 商品ID
     * @param quantity 恢复数量
     * @return 是否恢复成功
     */
    boolean restoreStock(String productId, int quantity);

    /**
     * 批量查询商品
     * @param ids 商品ID列表
     * @return 对应的商品列表
     */
    List<Product> getProductsByIds(List<String> ids);

    // ========== 商品图片相关 ==========

    /**
     * 添加商品图片
     * @param image 图片信息
     * @return 影响的行数
     */
    int addImage(ProductImageInfo image);

    /**
     * 删除商品图片
     * @param imageId 图片ID
     * @return 影响的行数
     */
    int removeImage(int imageId);

    /**
     * 根据图片ID查询图片
     * @param imageId 图片ID
     * @return 图片信息，null表示不存在
     */
    ProductImageInfo getImageById(int imageId);

    /**
     * 批量查询图片
     * @param ids 图片ID列表
     * @return 对应的图片列表
     */
    List<ProductImageInfo> getImagesByIds(List<Integer> ids);

    // ========== 商品上下架相关 ==========

    /**
     * 上架商品
     * @param productId 商品ID
     * @return 是否上架成功
     */
    boolean listProduct(String productId);

    /**
     * 下架商品
     * @param productId 商品ID
     * @return 是否下架成功
     */
    boolean unlistProduct(String productId);

    /**
     * 检查商品是否可售
     * @param productId 商品ID
     * @return 是否可售
     */
    boolean isProductSalable(String productId);

    /**
     * 获取所有可售商品ID列表
     * @return 可售商品ID列表
     */
    List<String> getAllSalableProductIds();

    // ========== 商品查询相关 ==========

    /**
     * 根据上下架状态查询商品
     * @param isSale true=在售，false=下架
     * @return 对应的商品列表
     */
    List<Product> getProductsBySaleStatus(boolean isSale);

    /**
     * 根据价格区间查询商品（无分页）
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @return 对应的商品列表
     */
    List<Product> getProductsByPriceRange(Double minPrice, Double maxPrice);

    /**
     * 分页查询可售商品
     * @param page 页码，从0开始
     * @return 当前页的可售商品列表
     */
    List<Product> getSalableProducts(int page);

    /**
     * 分页按价格区间查询商品
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param page 页码，从0开始
     * @return 当前页的商品列表
     */
    List<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page);
}