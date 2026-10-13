-- 广告专区（首页轮播图下方 zone-strip）广告数据
-- 4 条 slot='zone' 广告，图片为重新生成的专区横幅图
INSERT INTO advertisement (title, description, image_url, link_url, sort_order, status, slot, click_count, view_count, create_time, update_time) VALUES
('龙年手办 · 新品首发', '国潮龙纹手办，雕工精致，镇宅纳福', '/uploads/ad-zone-1.png', 'index.html?category=手办模型', 100, 1, 'zone', 0, 0, NOW(), NOW()),
('水墨帆布包系列', '泼墨山水，雅致出行，背上都是风景', '/uploads/ad-zone-2.png', 'index.html?category=帆布包', 90, 1, 'zone', 0, 0, NOW(), NOW()),
('青瓷茶器雅集', '温润如玉，茶香四溢，静享慢生活', '/uploads/ad-zone-3.png', 'index.html?category=生活用品', 80, 1, 'zone', 0, 0, NOW(), NOW()),
('文房雅物精选', '笔墨纸砚，书香满室，添一缕书卷气', '/uploads/ad-zone-4.png', 'index.html?category=文具', 70, 1, 'zone', 0, 0, NOW(), NOW());
