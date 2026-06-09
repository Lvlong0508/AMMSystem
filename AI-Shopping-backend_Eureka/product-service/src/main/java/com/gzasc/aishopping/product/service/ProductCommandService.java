package com.gzasc.aishopping.product.service;

import com.gzasc.aishopping.product.model.Product;
import org.springframework.web.multipart.MultipartFile;

public interface ProductCommandService {

    int createProductWithImage(Product product, MultipartFile imageFile);

    int updateProductWithImage(Product product, MultipartFile image);
}
