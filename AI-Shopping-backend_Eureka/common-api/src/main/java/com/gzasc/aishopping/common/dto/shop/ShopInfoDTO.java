package com.gzasc.aishopping.common.dto.shop;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShopInfoDTO implements Serializable {
    private Long id;
    private String name;
    private String description;

    @JsonProperty("logourl")
    private String logoUrl;
}
