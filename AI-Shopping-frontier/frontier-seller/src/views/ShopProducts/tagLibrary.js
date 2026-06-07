export const TAG_LIBRARY = [
  {
    value: 'clothing', label: '服饰鞋包',
    children: [
      { value: 'men', label: '男装', children: [
        { value: 'T恤', label: 'T恤' }, { value: '衬衫', label: '衬衫' },
        { value: '外套', label: '外套' }, { value: '裤子', label: '裤子' },
        { value: '卫衣', label: '卫衣' }
      ]},
      { value: 'women', label: '女装', children: [
        { value: '连衣裙', label: '连衣裙' }, { value: '上衣', label: '上衣' },
        { value: '半身裙', label: '半身裙' }, { value: '外套', label: '外套' },
        { value: '裤子', label: '裤子' }
      ]},
      { value: 'shoes', label: '鞋靴', children: [
        { value: '运动鞋', label: '运动鞋' }, { value: '皮鞋', label: '皮鞋' },
        { value: '靴子', label: '靴子' }, { value: '凉鞋', label: '凉鞋' }
      ]},
      { value: 'bags', label: '箱包', children: [
        { value: '双肩包', label: '双肩包' }, { value: '单肩包', label: '单肩包' },
        { value: '钱包', label: '钱包' }, { value: '行李箱', label: '行李箱' }
      ]}
    ]
  },
  {
    value: 'digital', label: '手机数码',
    children: [
      { value: 'phone', label: '手机配件', children: [
        { value: '手机壳', label: '手机壳' }, { value: '贴膜', label: '贴膜' },
        { value: '充电器', label: '充电器' }, { value: '数据线', label: '数据线' },
        { value: '耳机', label: '耳机' }
      ]},
      { value: 'computer', label: '电脑配件', children: [
        { value: '鼠标', label: '鼠标' }, { value: '键盘', label: '键盘' },
        { value: '硬盘', label: '硬盘' }, { value: '内存', label: '内存' }
      ]},
      { value: 'accessories', label: '数码配件', children: [
        { value: '充电宝', label: '充电宝' }, { value: '智能手表', label: '智能手表' },
        { value: '蓝牙音箱', label: '蓝牙音箱' }
      ]}
    ]
  },
  {
    value: 'appliance', label: '家用电器',
    children: [
      { value: 'kitchen', label: '厨房电器', children: [
        { value: '电饭煲', label: '电饭煲' }, { value: '电磁炉', label: '电磁炉' },
        { value: '微波炉', label: '微波炉' }, { value: '烤箱', label: '烤箱' }
      ]},
      { value: 'living', label: '生活电器', children: [
        { value: '电风扇', label: '电风扇' }, { value: '吸尘器', label: '吸尘器' },
        { value: '加湿器', label: '加湿器' }
      ]},
      { value: 'personal', label: '个护电器', children: [
        { value: '吹风机', label: '吹风机' }, { value: '剃须刀', label: '剃须刀' }
      ]}
    ]
  },
  {
    value: 'food', label: '食品饮料',
    children: [
      { value: 'snacks', label: '休闲零食', children: [
        { value: '坚果', label: '坚果' }, { value: '饼干', label: '饼干' },
        { value: '巧克力', label: '巧克力' }, { value: '肉干', label: '肉干' }
      ]},
      { value: 'drinks', label: '冲调饮品', children: [
        { value: '咖啡', label: '咖啡' }, { value: '茶叶', label: '茶叶' },
        { value: '麦片', label: '麦片' }, { value: '蜂蜜', label: '蜂蜜' }
      ]},
      { value: 'oil', label: '粮油调味', children: [
        { value: '大米', label: '大米' }, { value: '食用油', label: '食用油' },
        { value: '酱油', label: '酱油' }
      ]}
    ]
  },
  {
    value: 'beauty', label: '美妆个护',
    children: [
      { value: 'skincare', label: '面部护肤', children: [
        { value: '洗面奶', label: '洗面奶' }, { value: '爽肤水', label: '爽肤水' },
        { value: '面霜', label: '面霜' }, { value: '面膜', label: '面膜' }
      ]},
      { value: 'cosmetics', label: '彩妆', children: [
        { value: '口红', label: '口红' }, { value: '粉底', label: '粉底' },
        { value: '眼影', label: '眼影' }, { value: '眉笔', label: '眉笔' }
      ]},
      { value: 'hair', label: '个人护理', children: [
        { value: '洗发水', label: '洗发水' }, { value: '沐浴露', label: '沐浴露' },
        { value: '护手霜', label: '护手霜' }, { value: '牙膏', label: '牙膏' }
      ]}
    ]
  },
  {
    value: 'home', label: '家居生活',
    children: [
      { value: 'textile', label: '家纺', children: [
        { value: '床单', label: '床单' }, { value: '被套', label: '被套' },
        { value: '枕头', label: '枕头' }, { value: '毛毯', label: '毛毯' }
      ]},
      { value: 'storage', label: '收纳用品', children: [
        { value: '收纳箱', label: '收纳箱' }, { value: '衣架', label: '衣架' },
        { value: '置物架', label: '置物架' }
      ]},
      { value: 'kitchenware', label: '厨房用品', children: [
        { value: '餐具', label: '餐具' }, { value: '锅具', label: '锅具' },
        { value: '水杯', label: '水杯' }, { value: '保鲜盒', label: '保鲜盒' }
      ]}
    ]
  },
  {
    value: 'sports', label: '运动户外',
    children: [
      { value: 'fitness', label: '运动装备', children: [
        { value: '跑步鞋', label: '跑步鞋' }, { value: '瑜伽垫', label: '瑜伽垫' },
        { value: '健身服', label: '健身服' }, { value: '护具', label: '护具' }
      ]},
      { value: 'outdoor', label: '户外用品', children: [
        { value: '帐篷', label: '帐篷' }, { value: '登山包', label: '登山包' },
        { value: '水壶', label: '水壶' }
      ]},
      { value: 'gear', label: '运动器材', children: [
        { value: '哑铃', label: '哑铃' }, { value: '跳绳', label: '跳绳' },
        { value: '球拍', label: '球拍' }
      ]}
    ]
  },
  {
    value: 'baby', label: '母婴用品',
    children: [
      { value: 'baby_clothes', label: '婴儿服饰', children: [
        { value: '连体衣', label: '连体衣' }, { value: '睡袋', label: '睡袋' },
        { value: '帽子', label: '帽子' }, { value: '袜子', label: '袜子' }
      ]},
      { value: 'feeding', label: '喂养用品', children: [
        { value: '奶瓶', label: '奶瓶' }, { value: '辅食碗', label: '辅食碗' },
        { value: '保温杯', label: '保温杯' }
      ]},
      { value: 'toys', label: '玩具早教', children: [
        { value: '积木', label: '积木' }, { value: '绘本', label: '绘本' },
        { value: '益智玩具', label: '益智玩具' }
      ]}
    ]
  }
]
