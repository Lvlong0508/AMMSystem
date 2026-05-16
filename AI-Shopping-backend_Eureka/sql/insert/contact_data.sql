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