package com.example.mhproject.service;

import com.example.mhproject.entity.Product;
import com.example.mhproject.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 智能搜索：把用户的自然语言描述（如"送女朋友的文创礼物，50元以内"）
 * 先让大模型提取关键词，再做数据库搜索，最后由大模型组织推荐话术。
 */
@Service
public class AiSearchService {

    @Autowired
    private AiService aiService;
    @Autowired
    private ProductRepository productRepo;

    /**
     * 自然语言搜索：返回推荐话术 + 匹配到的商品列表。
     */
    public Object smartSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new RuntimeException("请输入搜索内容");
        }

        // 第一步：直接用关键词模糊搜索（数据库兜底）
        List<Product> matches = productRepo.findByNameContaining(query.trim());
        // 如果直接搜不到，尝试用大模型提取关键词再搜
        if (matches.isEmpty()) {
            String kwPrompt = String.format(
                    "从下面的用户搜索描述中提取1-2个最核心的商品关键词，只输出关键词本身，不要解释。\n用户描述：%s",
                    query.trim());
            String keywords = aiService.chat(kwPrompt);
            keywords = keywords.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s]", "").trim();
            if (!keywords.isEmpty()) {
                String firstKw = keywords.split("\\s+")[0];
                matches = productRepo.findByNameContaining(firstKw);
            }
        }

        // 第二步：让大模型基于搜索结果组织推荐话术
        String productList = matches.stream()
                .limit(10)
                .map(p -> String.format("%s(%s元)", p.getName(), p.getPrice()))
                .collect(Collectors.joining("、"));
        String replyPrompt = String.format(
                "你是电商导购。根据用户需求：%s，从以下商品中挑最合适的推荐，话术简洁友好（100字以内）。\n商品列表：%s",
                query.trim(), productList.isEmpty() ? "暂无匹配商品" : productList);
        String reply = matches.isEmpty()
                ? "抱歉，暂时没有找到符合您需求的商品，换个关键词试试吧～"
                : aiService.chat(replyPrompt);

        return java.util.Map.of(
                "reply", reply,
                "products", matches.stream().limit(10).collect(Collectors.toList())
        );
    }
}
