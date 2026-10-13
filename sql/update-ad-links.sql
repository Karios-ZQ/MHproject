-- 将广告跳转链接更新为站内有效地址
USE mhproject;
UPDATE advertisement SET link_url = 'index.html?category=手办模型' WHERE id = 1;
UPDATE advertisement SET link_url = 'detail.html?id=1' WHERE id = 2;
UPDATE advertisement SET link_url = 'index.html?category=生活用品' WHERE id = 3;
