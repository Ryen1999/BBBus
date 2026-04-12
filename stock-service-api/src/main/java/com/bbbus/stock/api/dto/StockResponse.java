package com.bbbus.stock.api.dto;

public class StockResponse {

    private String skuCode;
    private Integer available;

    public StockResponse() {
    }

    public StockResponse(String skuCode, Integer available) {
        this.skuCode = skuCode;
        this.available = available;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public Integer getAvailable() {
        return available;
    }

    public void setAvailable(Integer available) {
        this.available = available;
    }
}
