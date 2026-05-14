USE eureka_user;

INSERT INTO t_user (username, password, nickname, phone) VALUES
('user001', '$2a$12$Xds.rVevtmFivL9fKlLWTuidrnDP5wgOHNffjrnOABOP9pKPmpSvS', '张三', '13800138000'),
('user002', '$2a$12$Xds.rVevtmFivL9fKlLWTuidrnDP5wgOHNffjrnOABOP9pKPmpSvS', '李四', '13900139000')
ON DUPLICATE KEY UPDATE username = username;

USE eureka_merchant;

INSERT INTO t_merchant (username, password, shop_name, phone) VALUES
('merchant001', '$2a$12$Xds.rVevtmFivL9fKlLWTuidrnDP5wgOHNffjrnOABOP9pKPmpSvS', '数码旗舰店', '13700137000'),
('merchant002', '$2a$12$Xds.rVevtmFivL9fKlLWTuidrnDP5wgOHNffjrnOABOP9pKPmpSvS', '日用百货店', '13600136000')
ON DUPLICATE KEY UPDATE username = username;