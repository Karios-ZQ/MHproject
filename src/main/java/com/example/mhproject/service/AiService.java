package com.example.mhproject.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AiService {
    @Value("${spring.ai.deepseek.api-key:}")
    private String apiKey;

    // 豆包文生图（火山方舟 Seedream）配置，通过环境变量注入
    @Value("${doubao.ark.api-key:}")
    private String doubaoApiKey;
    @Value("${doubao.ark.model:}")
    private String doubaoModel;

    // RestTemplate 加超时：连接 5 秒，读取 15 秒（熔断降级，避免挂死）
    private final RestTemplate restTemplate;
    {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(15000);
        restTemplate = new RestTemplate(factory);
    }
    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final String ARK_IMAGE_URL = "https://ark.cn-beijing.volces.com/api/v3/images/generations";

    // ================ 通用对话能力 ================
    public String chat(String prompt) {
        // API Key 未配置时直接走兜底
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return fallbackReply();
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            Map<String, Object> message = Map.of("role", "user", "content", prompt);
            Map<String, Object> requestBody = Map.of(
                    "model", "deepseek-chat", "messages", List.of(message), "temperature", 0.7, "stream", false);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List choices = (List) response.getBody().get("choices");
                Map firstChoice = (Map) choices.get(0);
                Map messageMap = (Map) firstChoice.get("message");
                return (String) messageMap.get("content");
            }
            return fallbackReply();
        } catch (Exception e) {
            // 熔断降级：AI 服务超时/异常时返回友好兜底话术，不拖垮电商主流程
            return fallbackReply();
        }
    }

    /** 降级兜底话术 */
    private String fallbackReply() {
        return "AI 助手暂时有点忙，您可以先浏览商品分类看看，或者稍后再试～😊";
    }

    // ================ 功能1：AI 生成广告文案 ================
    public String generateAdCopy(String theme, String style, String length) {
        String prompt = String.format(
                "你是电商广告文案助手，根据用户输入主题，生成3组适合商城首页轮播广告的文案。\n" +
                        "每组包含【广告标题】(10字以内)和【广告简短描述】(30字以内)。\n" +
                        "要求文案风格为%s，字数限制为%s，适合轮播图展示，不要违规内容。\n" +
                        "只返回json数组格式，不要任何多余解释或markdown标记，格式严格为：\n" +
                        "[{\"title\":\"...\",\"desc\":\"...\"},{\"title\":\"...\",\"desc\":\"...\"},{\"title\":\"...\",\"desc\":\"...\"}]\n" +
                        "主题：%s",
                style != null ? style : "简洁",
                length != null ? length : "标准",
                theme);
        return chat(prompt);
    }

    // ================ 功能2：AI 广告内容安全检测 ================
    public String checkAdSafety(String adText) {
        String prompt = String.format(
                "你是电商广告内容审核助手。检查下面广告文本，识别：极限广告词（如最、第一、全网最低）、导流微信/QQ、色情、政治、虚假宣传等违规内容。\n" +
                        "只返回json格式，不要任何多余解释或markdown标记，格式严格为：\n" +
                        "{\"isRisk\":true或false,\"riskType\":\"风险类型名称，无风险填无\",\"suggest\":\"修改建议，无风险填内容合规\"}\n" +
                        "广告文本：%s",
                adText);
        return chat(prompt);
    }

    // ================ 功能3：AI 广告文案润色 ================
    public String polishAdCopy(String adText) {
        String prompt = String.format(
                "你是电商广告文案优化助手。请对以下广告文案进行精简、润色，使其更简洁、更有吸引力，适合轮播图短文案展示。\n" +
                        "直接返回润色后的文案，不要任何多余解释，不要加引号。\n" +
                        "原广告文案：%s",
                adText);
        return chat(prompt);
    }

    // ================ 豆包文生图（火山方舟 Seedream） ================
    /**
     * 调用豆包 Seedream 生成商品图片，返回 base64 编码的图片数据（不含 data: 前缀）。
     * 需要环境变量：DOUBAO_ARK_API_KEY（火山方舟 API Key）、DOUBAO_ARK_MODEL（Seedream 模型 ID，如 doubao-seedream-5-0-260128）。
     * 未配置时返回友好提示，不影响其它功能。
     *
     * @param prompt 中文画面描述，如 "一个红色的中国龙手办"
     * @param size 图片尺寸，如 "1024x1024"；为空时默认 1024x1024
     * @return base64 图片数据，或错误提示
     */
    public String generateImage(String prompt, String size) {
        if (doubaoApiKey == null || doubaoApiKey.trim().isEmpty()
                || doubaoModel == null || doubaoModel.trim().isEmpty()) {
            return "AI 生图未配置：请先设置环境变量 DOUBAO_ARK_API_KEY 和 DOUBAO_ARK_MODEL（火山方舟豆包 Seedream 模型 ID）。";
        }
        if (prompt == null || prompt.trim().isEmpty()) return "请输入画面描述";
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(doubaoApiKey);
            String imgSize = (size == null || size.trim().isEmpty()) ? "1024x1024" : size.trim();
            Map<String, Object> requestBody = Map.of(
                    "model", doubaoModel,
                    "prompt", prompt,
                    "size", imgSize,
                    "response_format", "b64_json",
                    "n", 1);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(ARK_IMAGE_URL, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List data = (List) response.getBody().get("data");
                if (data != null && !data.isEmpty()) {
                    Map first = (Map) data.get(0);
                    Object b64 = first.get("b64_json");
                    if (b64 != null) return (String) b64;
                }
            }
            return "AI 生图失败: " + response.getStatusCode();
        } catch (Exception e) {
            e.printStackTrace();
            return "AI 生图异常: " + e.getMessage();
        }
    }

    // ================ 功能9：AI 地址智能识别（粘贴地址自动拆分） ================
    /**
     * 根据用户粘贴的一整段收货地址（如 "北京市海淀区中关村大街27号中关村大厦A座801室"），
     * 自动识别拆分为省 / 市 / 区 / 详细地址四个字段，供前端自动填入对应输入框。
     * <p>策略：优先调用 DeepSeek 语义识别；AI 未配置、超时或返回异常时，自动降级用本地规则兜底，
     * 保证功能在 AI 不可用时依然可用。
     *
     * @param raw 用户粘贴的原始完整地址
     * @return 含 province / city / district / detailAddress 四个键的 Map
     */
    public Map<String, String> parseAddress(String raw) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("province", "");
        result.put("city", "");
        result.put("district", "");
        result.put("detailAddress", "");
        if (raw == null) return result;

        String text = raw.trim();
        if (text.isEmpty()) return result;

        // 1) 优先 AI 语义识别
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            try {
                String prompt = "你是地址解析助手。请把下面这段中国收货地址拆分成语义字段，只返回严格 JSON，不要任何解释或 markdown 标记：\n" +
                        "{\"province\":\"省/直辖市\",\"city\":\"市\",\"district\":\"区/县\",\"detailAddress\":\"街道、门牌号、楼栋房间号等剩余部分\"}\n" +
                        "规则：直辖市（如北京、上海、天津、重庆）province 与 city 相同；行政区划缺失的字段填空字符串；detailAddress 必须包含除省市区外的全部剩余内容。\n" +
                        "待解析地址：" + text;
                String content = callDeepSeek(prompt);
                Map<String, String> aiResult = extractAddressFromJson(content);
                if (aiResult != null && !aiResult.isEmpty()) {
                    return fillResult(aiResult, text);
                }
            } catch (Exception ignored) {
                // 降级走本地规则
            }
        }

        // 2) 本地规则兜底
        return localParseAddress(text);
    }

    /** 调用 DeepSeek 返回原始内容（无 API Key 或异常时返回 null） */
    private String callDeepSeek(String prompt) {
        if (apiKey == null || apiKey.trim().isEmpty()) return null;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            Map<String, Object> message = Map.of("role", "user", "content", prompt);
            Map<String, Object> requestBody = Map.of(
                    "model", "deepseek-chat", "messages", List.of(message), "temperature", 0.2, "stream", false);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(API_URL, entity, Map.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List choices = (List) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map firstChoice = (Map) choices.get(0);
                    Map msg = (Map) firstChoice.get("message");
                    return (String) msg.get("content");
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** 从 AI 返回文本中提取地址 JSON 字段（兼容前后多出的文字 / 代码块标记） */
    private Map<String, String> extractAddressFromJson(String content) {
        if (content == null) return null;
        Map<String, String> map = new LinkedHashMap<>();
        for (String key : new String[]{"province", "city", "district", "detailAddress"}) {
            String v = extractJsonField(content, key);
            if (v == null) return null; // 关键字段缺失视为解析失败
            map.put(key, v.trim());
        }
        return map;
    }

    /** 从文本中用正则提取指定 JSON 字段值（去掉首尾引号与转义） */
    private String extractJsonField(String content, String key) {
        Matcher m = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(content);
        if (m.find()) {
            String v = m.group(1);
            return v.replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "").replace("\\u", " ");
        }
        return null;
    }

    /** 把 AI 结果与原始文本合并，保证 detailAddress 完整、空字段可回填 */
    private Map<String, String> fillResult(Map<String, String> ai, String text) {
        Map<String, String> r = new LinkedHashMap<>();
        r.put("province", ai.getOrDefault("province", ""));
        r.put("city", ai.getOrDefault("city", ""));
        r.put("district", ai.getOrDefault("district", ""));
        r.put("detailAddress", ai.getOrDefault("detailAddress", ""));
        // 若 AI 未拆分出详细地址，则用原始文本剩余内容兜底
        if (r.get("detailAddress").isEmpty()) {
            String head = r.get("province") + r.get("city") + r.get("district");
            String rest = text;
            for (String seg : new String[]{r.get("province"), r.get("city"), r.get("district")}) {
                if (seg != null && !seg.isEmpty()) rest = rest.replace(seg, "");
            }
            r.put("detailAddress", rest.replace(head, "").trim());
        }
        return r;
    }

    /** 本地规则兜底：用正则按“省 → 市 → 区/县 → 剩余”的顺序拆分地址 */
    private Map<String, String> localParseAddress(String text) {
        Map<String, String> r = new LinkedHashMap<>();
        r.put("province", "");
        r.put("city", "");
        r.put("district", "");
        r.put("detailAddress", "");

        String rest = text.trim();

        // 直辖市（省/市同为一个）
        Matcher mu = Pattern.compile("^(北京|上海|天津|重庆)市?").matcher(rest);
        if (mu.find()) {
            r.put("province", mu.group(1));
            r.put("city", mu.group(1));
            rest = rest.substring(mu.end());
        } else {
            // 常规省份：自治区 / 特别行政区 / xx省
            Matcher mp = Pattern.compile("^(内蒙古自治区|广西壮族自治区|宁夏回族自治区|新疆维吾尔自治区|西藏自治区|香港特别行政区|澳门特别行政区|[^省市区]+?省)").matcher(rest);
            if (mp.find()) {
                r.put("province", mp.group(1));
                rest = rest.substring(mp.end());
            }
            // 市级
            Matcher mc = Pattern.compile("^([^省市区]+?市)").matcher(rest);
            if (mc.find()) {
                r.put("city", mc.group(1));
                rest = rest.substring(mc.end());
            }
        }
        // 区 / 县 / 旗 / 县级市
        Matcher md = Pattern.compile("^([^省市区]+?(?:区|县|自治县|自治旗|旗|县级市|新区))").matcher(rest);
        if (md.find()) {
            r.put("district", md.group(1));
            rest = rest.substring(md.end());
        }
        r.put("detailAddress", rest.trim());
        return r;
    }
}