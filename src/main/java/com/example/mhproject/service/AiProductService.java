package com.example.mhproject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * AI 商品描述生成：管理员上架商品时，根据商品名称/分类自动生成详情文案。
 */
@Service
public class AiProductService {

    @Autowired
    private AiService aiService;

    /**
     * 根据商品名称和分类生成商品详情描述。
     * @param name 商品名称
     * @param category 商品分类
     * @param price 商品价格（字符串）
     * @return 生成的商品描述
     */
    public String generateDescription(String name, String category, String price) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("商品名称不能为空");
        }
        String prompt = String.format(
                "你是电商商品运营。请为以下商品生成一段吸引人的详情描述（80-120字），突出卖点和使用场景，适合文创/校园商城风格，不要用极限广告词。\n" +
                "商品名称：%s\n商品分类：%s\n商品价格：%s元",
                name.trim(),
                category == null ? "综合" : category.trim(),
                price == null ? "面议" : price);
        return aiService.chat(prompt);
    }
}
