package com.example.mhproject.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 地理编码接口：把「省市区 + 详细地址」转为经纬度，供前端地图定位显示。
 * 使用高德开放平台 Web 服务地理编码 API，Key 通过环境变量 AMAP_KEY 注入（未配置时返回友好提示）。
 */
@RestController
@RequestMapping("/api/geo")
@CrossOrigin
public class GeoController {

    @Value("${amap.api-key:}")
    private String amapKey;

    private static final String GEOCODE_URL = "https://restapi.amap.com/v3/geocode/geo";
    private static final String REGEO_URL = "https://restapi.amap.com/v3/geocode/regeo";

    private final RestTemplate restTemplate;

    public GeoController() {
        // 加超时，避免地图服务异常时挂死
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * 高德地理编码：地址 → 经纬度。
     *
     * @param address 完整地址文本，如 "北京市海淀区中关村大街27号中关村大厦A座801室"
     * @return {ok, lng, lat, formattedAddress, message}
     */
    @GetMapping("/geocode")
    public Map<String, Object> geocode(@RequestParam String address) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", false);
        if (amapKey == null || amapKey.trim().isEmpty()) {
            result.put("message", "地图服务未配置：请设置环境变量 AMAP_KEY（高德开放平台 Web 服务 Key）。");
            return result;
        }
        if (address == null || address.trim().isEmpty()) {
            result.put("message", "请先填写省市区和详细地址。");
            return result;
        }
        try {
            String url = GEOCODE_URL + "?key=" + amapKey
                    + "&address=" + URLEncoder.encode(address.trim(), StandardCharsets.UTF_8)
                    + "&output=json";
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            Map body = resp.getBody();
            if (body != null && "1".equals(String.valueOf(body.get("status")))) {
                List geocodes = (List) body.get("geocodes");
                if (geocodes != null && !geocodes.isEmpty()) {
                    Map geo = (Map) geocodes.get(0);
                    String location = (String) geo.get("location");
                    if (location != null && location.contains(",")) {
                        String[] parts = location.split(",");
                        result.put("ok", true);
                        result.put("lng", Double.parseDouble(parts[0]));
                        result.put("lat", Double.parseDouble(parts[1]));
                        result.put("formattedAddress", geo.get("formatted_address"));
                        return result;
                    }
                }
            }
            result.put("message", "未能定位该地址，请确认省市区和详细地址填写正确。");
        } catch (Exception e) {
            result.put("message", "地图服务调用失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 高德逆地理编码：经纬度 → 省市区 + 街道门牌，用于地图上点选位置后回填表单。
     *
     * @param lng 经度
     * @param lat 纬度
     * @return {ok, province, city, district, formattedAddress, township, street, number, message}
     */
    @GetMapping("/regeo")
    public Map<String, Object> regeo(@RequestParam Double lng, @RequestParam Double lat) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ok", false);
        if (amapKey == null || amapKey.trim().isEmpty()) {
            result.put("message", "地图服务未配置：请设置环境变量 AMAP_KEY（高德开放平台 Web 服务 Key）。");
            return result;
        }
        if (lng == null || lat == null) {
            result.put("message", "缺少经纬度参数。");
            return result;
        }
        try {
            String url = REGEO_URL + "?key=" + amapKey
                    + "&location=" + lng + "," + lat
                    + "&output=json&extensions=all";
            ResponseEntity<Map> resp = restTemplate.getForEntity(url, Map.class);
            Map body = resp.getBody();
            if (body != null && "1".equals(String.valueOf(body.get("status")))) {
                Map regeocode = (Map) body.get("regeocode");
                if (regeocode != null) {
                    Map comp = (Map) regeocode.get("addressComponent");
                    Map streetNumber = comp != null ? (Map) comp.get("streetNumber") : null;
                    String township = comp != null && comp.get("township") != null ? String.valueOf(comp.get("township")) : "";
                    String street = streetNumber != null && streetNumber.get("street") != null ? String.valueOf(streetNumber.get("street")) : "";
                    String number = streetNumber != null && streetNumber.get("number") != null ? String.valueOf(streetNumber.get("number")) : "";
                    result.put("ok", true);
                    result.put("province", comp != null && comp.get("province") != null ? String.valueOf(comp.get("province")) : "");
                    result.put("city", comp != null && comp.get("city") != null ? String.valueOf(comp.get("city")) : "");
                    result.put("district", comp != null && comp.get("district") != null ? String.valueOf(comp.get("district")) : "");
                    result.put("formattedAddress", regeocode.get("formatted_address") != null ? String.valueOf(regeocode.get("formatted_address")) : "");
                    result.put("township", township);
                    result.put("street", street);
                    result.put("number", number);
                    return result;
                }
            }
            result.put("message", "未能反查该位置，请换一个点再试。");
        } catch (Exception e) {
            result.put("message", "地图服务调用失败：" + e.getMessage());
        }
        return result;
    }
}
