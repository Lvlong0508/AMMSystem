package com.gzasc.aishopping.product.service;

import com.gzasc.aishopping.product.dto.ProductWithImageAbstractDTO;
import com.gzasc.aishopping.product.dto.ProductWithImageDetailDTO;
import com.gzasc.aishopping.product.model.Product;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品服务接口
 */
public interface ProductService {

    // ==================== CRUD 操作 ====================

    /**
     * 根据商品ID查询商品详情
     * @param productId 商品ID
     * @return 商品信息，null表示不存在
     */
    ProductWithImageDetailDTO getProductById(Long productId);

    /**
     * 根据商品名称模糊搜索
     * @param name 商品名称关键字
     * @return 符合条件的商品列表
     */
    List<ProductWithImageDetailDTO> getProductsByName(String name);

    /**
     * 用户端: 根据可售商品ID批量查询抽象信息
     * (id, name, price, tags, imageId)
     */
    List<ProductWithImageAbstractDTO> getAbstractProductsForBuyer(List<Long> ids);

    /**
     * 用户端: 分页查询可售商品抽象信息
     */
    List<ProductWithImageAbstractDTO> getSalableProductsAbstract(int page);

    /**
     * 删除商品
     * @param productId 商品ID
     * @return 影响的行数
     */
    int deleteProduct(Long productId);

    /**
     * 商家端: 根据商品ID批量查询抽象信息（包含isSale字段）
     * (id, name, price, tags, isSale, imageId)
     */
    List<ProductWithImageAbstractDTO> getAbstractProductsForMerchant(List<Long> ids);


    // ==================== 库存管理 ====================

    /**
     * 恢复商品库存（用于取消订单、退货）
     * @param productId 商品ID
     * @param quantity 恢复数量
     * @return 是否恢复成功
     */
    boolean restoreStock(Long productId, int quantity);


    // ==================== 上下架管理 ====================

    /**
     * 上架商品
     * @param productId 商品ID
     * @return 是否上架成功
     */
    boolean listProduct(Long productId);

    /**
     * 下架商品
     * @param productId 商品ID
     * @return 是否下架成功
     */
    boolean unlistProduct(Long productId);

    // ==================== 高级查询 ====================

    /**
     * 分页按价格区间查询商品
     * @param minPrice 最低价格
     * @param maxPrice 最高价格
     * @param page 页码，从0开始
     * @return 当前页的商品列表
     */
    List<ProductWithImageAbstractDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page);

    // ==================== 统一创建/更新（含图片） ====================

    /**
     * 创建商品并关联图片（同一事务：先写图片 → 获取ID → 再写商品）
     * @param product 商品信息（不含 imageId，由方法内部设置）
     * @param imageFile 图片文件
     * @return 影响的行数
     */
    int createProductWithImage(Product product, MultipartFile imageFile);

    /**
     * 更新商品并关联新图片（同一事务：先写图片 → 获取ID → 再更新商品）
     * @param product 商品基本信息（需包含ID）
     * @param image 图片文件，为空则不更新图片
     * @return 影响的行数
     */
    int updateProductWithImage(Product product, MultipartFile image);

}