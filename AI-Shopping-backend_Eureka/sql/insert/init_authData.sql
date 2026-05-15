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

USE eureka_contact;

INSERT INTO t_contact (name, phone, address) VALUES
('张三的家', '13800138000', '北京市朝阳区XX街道XX号'),
('张三的公司', '13800138001', '北京市海淀区XX路XX大厦'),
('李四的家', '13900139000', '上海市浦东新区XX路XX弄'),
('李四的公司', '13900139001', '上海市静安区XX街XX中心');

INSERT INTO user_contact (user_id, contact_id) VALUES
(1, 1),
(1, 2),
(2, 3),
(2, 4);

USE eureka_merchant;

INSERT INTO t_merchant (username, password, phone, email, info_id) VALUES
('merchant001', '$2a$12$Xds.rVevtmFivL9fKlLWTuidrnDP5wgOHNffjrnOABOP9pKPmpSvS', '13700137000', 'merchant001@example.com', 3),
('merchant002', '$2a$12$Xds.rVevtmFivL9fKlLWTuidrnDP5wgOHNffjrnOABOP9pKPmpSvS', '13600136000', 'merchant002@example.com', 4)
ON DUPLICATE KEY UPDATE username = username;