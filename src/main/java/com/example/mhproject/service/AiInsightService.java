package com.example.mhproject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * AI 运营数据洞察：管理员用自然语言提问经营数据，
 * 后端先从数据库拉取统计，再让大模型生成文字分析和建议。
 */
@Service
public class AiInsightService {

    @Autowired
    private AiService aiService;
    @Autowired
    private MallService mallService;

    /**
     * 自然语言问经营数据。
     * @param question 如"最近经营情况怎么样？""哪些商品该补货了？"
     */
    public String insight(String question) {
        Map<String, Object> stats = mallService.getDashboardStats();

        // 把核心数据拼成文本喂给大模型
        StringBuilder data = new StringBuilder();
        data.append("【今日经营数据】\n");
        data.append("总订单数：").append(stats.get("orderCount")).append("\n");
        data.append("待发货订单：").append(stats.get("pendingShipCount")).append("\n");
        data.append("商品总数：").append(stats.get("productCount")).append("\n");
        data.append("用户总数：").append(stats.get("userCount")).append("\n");
        data.append("累计销售额：").append(stats.get("totalSales")).append("元\n");
        data.append("待处理退款：").append(stats.get("refundPendingCount")).append("\n");
        data.append("热销商品TOP5：").append(stats.get("hotProducts")).append("\n");
        data.append("低库存商品：").append(stats.get("lowStock")).append("\n");

        String prompt = String.format(
                "你是电商运营分析师。请根据以下经营数据，回答管理员的问题，给出有价值的分析和可执行的建议，控制在200字以内。\n\n" +
                "%s\n\n管理员问题：%s",
                data,
                question == null || question.trim().isEmpty() ? "帮我分析一下当前经营状况，给出建议" : question.trim());
        return aiService.chat(prompt);
    }
}
