-- 为新增商品（id 11-35）更新为各自独立的商品图片
-- 图片文件已放置于 uploads/ 目录（product-11-phoenix.png ~ product-35-hat.png）
USE mhproject;

UPDATE product SET image_url='/uploads/product-11-phoenix.png'   WHERE id=11;
UPDATE product SET image_url='/uploads/product-12-guanyu.png'    WHERE id=12;
UPDATE product SET image_url='/uploads/product-13-flying.png'    WHERE id=13;
UPDATE product SET image_url='/uploads/product-14-lion.png'      WHERE id=14;
UPDATE product SET image_url='/uploads/product-15-bag-xiang.png' WHERE id=15;
UPDATE product SET image_url='/uploads/product-16-bag-lianpu.png'  WHERE id=16;
UPDATE product SET image_url='/uploads/product-17-bag-shanhai.png' WHERE id=17;
UPDATE product SET image_url='/uploads/product-18-bookmark.png'  WHERE id=18;
UPDATE product SET image_url='/uploads/product-19-frame.png'     WHERE id=19;
UPDATE product SET image_url='/uploads/product-20-piying.png'    WHERE id=20;
UPDATE product SET image_url='/uploads/product-21-sticker.png'   WHERE id=21;
UPDATE product SET image_url='/uploads/product-22-teapot.png'    WHERE id=22;
UPDATE product SET image_url='/uploads/product-23-incense.png'   WHERE id=23;
UPDATE product SET image_url='/uploads/product-24-mat.png'       WHERE id=24;
UPDATE product SET image_url='/uploads/product-25-pen.png'       WHERE id=25;
UPDATE product SET image_url='/uploads/product-26-notebook.png'  WHERE id=26;
UPDATE product SET image_url='/uploads/product-27-tape.png'      WHERE id=27;
UPDATE product SET image_url='/uploads/product-28-phonecase.png' WHERE id=28;
UPDATE product SET image_url='/uploads/product-29-earcase.png'   WHERE id=29;
UPDATE product SET image_url='/uploads/product-30-cable.png'     WHERE id=30;
UPDATE product SET image_url='/uploads/product-31-stand.png'     WHERE id=31;
UPDATE product SET image_url='/uploads/product-32-necklace.png'  WHERE id=32;
UPDATE product SET image_url='/uploads/product-33-earring.png'   WHERE id=33;
UPDATE product SET image_url='/uploads/product-34-bracelet.png'  WHERE id=34;
UPDATE product SET image_url='/uploads/product-35-hat.png'       WHERE id=35;
