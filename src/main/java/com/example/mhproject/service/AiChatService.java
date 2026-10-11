package com.example.mhproject.service;

import com.example.mhproject.entity.Product;
import com.example.mhproject.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 智能客服导购（RAG 检索增强）。
 *
 * 思路：把当前在售商品列表作为"知识库"塞进 system prompt，
 * 大模型基于真实商品信息回答用户问题、做推荐，避免幻觉。
 */
@Service
public class AiChatService {

    @Autowired
    private AiService aiService;
    @Autowired
    private ProductRepository productRepo;

    /** 构建商品知识库文本（取前 50 条，控制 token 长度） */
    private String buildProductContext() {
        List<Product> products = productRepo.findAll();
        return products.stream()
                .limit(50)
                .map(p -> String.format(
                        "商品ID:%d, 名称:%s, 分类:%s, 价格:%s元, 库存:%d, 描述:%s",
                        p.getId(), p.getName(), p.getCategory(),
                        p.getPrice(), p.getStock() == null ? 0 : p.getStock(),
                        p.getDescription() == null ? "" : p.getDescription()))
                .collect(Collectors.joining("\n"));
    }

    /**
     * 智能客服对话：用户问什么就基于商品库回答。
     */
    public String chat(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "请问您想了解哪款商品？我可以帮您推荐。";
        }
        String prompt = String.format(
                "你是一个热情专业的电商商城导购客服。请严格基于下面的商品信息回答用户问题，" +
                "不要编造不存在的商品。如果用户在问推荐，根据需求给出最合适的2-3款并说明理由。" +
                "回答简洁友好，控制在150字以内。\n\n" +
                "【当前在售商品列表】\n%s\n\n" +
                "【用户问题】%s",
                buildProductContext(), userMessage.trim());
        return aiService.chat(prompt);
    }
}
