USE eureka_user;

INSERT INTO user_info (nickname) VALUES
('张三'),
('李四'),
('王五'),
('赵六');

INSERT INTO t_user (username, password, phone, info_id) VALUES
('user001', '$2a$12$Xds.rVevtmFivL9fKlLWTuidrnDP5wgOHNffjrnOABOP9pKPmpSvS', '13800138000', 1),
('user002', '$2a$12$Xds.rVevtmFivL9fKlLWTuidrnDP5wgOHNffjrnOABOP9pKPmpSvS', '13900139000', 2)
ON DUPLICATE KEY UPDATE username = username;

USE eureka_merchant;

INSERT INTO t_merchant (username, password, phone, email, info_id) VALUES
('merchant001', '$2a$12$Xds.rVevtmFivL9fKlLWTuidrnDP5wgOHNffjrnOABOP9pKPmpSvS', '13700137000', 'merchant001@example.com', 3),
('merchant002', '$2a$12$Xds.rVevtmFivL9fKlLWTuidrnDP5wgOHNffjrnOABOP9pKPmpSvS', '13600136000', 'merchant002@example.com', 4)
ON DUPLICATE KEY UPDATE username = username;