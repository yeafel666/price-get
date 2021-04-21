# price-get
对网易buff、igxe、C5等饰品交易平台csgo饰品价格与其他属性进行定时爬取

# 重要声明
网易buff封号很严格，谨慎使用,注意爬取频率和次数。目前每个小时执行一次，3天后被封(摊手)

## 详情
网易buff和C5平台需要在用户登录后，看控制台，抓到自己的cookie，贴到yml配置文件下。
Igxe平台无需任何配置。


## 使用说明
找到项目的src --> main -->java -->com.yeafel.priceget -->scheduled 目录，以下3个定时任务，可通过@Scheduled注解修改定时任务表达式。
例如：  @Scheduled(cron = "0 30 * * * ?")   就是每个小时的第30分钟执行。




## 项目架构
本项目使用java语言开发，使用springBoot框架作为骨架。使用mysql5.7版本作为数据库。




## 库表说明
本项目涉及两张库表  need_get_goods 表  、  transact_record
简要说明一下， need_get_goods是维护了一个自己要爬取的商品的各个平台的id ，可以去各个平台打开控制台进行查看。部分平台直接暴露在url中。
transact_record表则表示爬取的数据。


#创建表结构脚本
transact_record：
```
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for transact_record
-- ----------------------------
DROP TABLE IF EXISTS `transact_record`;
CREATE TABLE `transact_record`  (
  `id` bigint(32) NOT NULL AUTO_INCREMENT,
  `goods_id` bigint(16) NULL DEFAULT NULL COMMENT '商品id',
  `goods_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '商品名',
  `price` decimal(10, 2) NULL DEFAULT NULL COMMENT '成交价格',
  `paintwear` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '武器磨损',
  `stickers` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '贴纸信息以及磨损度(多个贴纸记录以逗号隔开)',
  `sticker_is_influence` smallint(8) NULL DEFAULT NULL COMMENT '贴纸是否造成价格影响(0、没有影响  1、有影响)',
  `transact_time` datetime(0) NULL DEFAULT NULL COMMENT '交易时间',
  `platform` varchar(16) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '数据平台',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  `igx_id` bigint(32) NULL DEFAULT NULL COMMENT 'igx平台对于记录的唯一标识',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `idx_unique`(`goods_id`, `paintwear`, `price`, `transact_time`) USING BTREE,
  INDEX `idx_query`(`goods_id`, `transact_time`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 623014 CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '交易成交记录表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
```

need_get_goods表：
```
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for need_get_goods
-- ----------------------------
DROP TABLE IF EXISTS `need_get_goods`;
CREATE TABLE `need_get_goods`  (
  `id` int(32) NOT NULL AUTO_INCREMENT,
  `goods_id` int(16) NULL DEFAULT NULL COMMENT '网易buff平台的商品id',
  `igxe_goods_id` int(16) NULL DEFAULT NULL COMMENT 'igxe平台的商品id',
  `c5_goods_id` bigint(32) NULL DEFAULT NULL COMMENT 'c5平台商品id',
  `goods_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '商品名称',
  `create_time` datetime(0) NULL DEFAULT CURRENT_TIMESTAMP(0) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 24 CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
```

## 关于cookie
在application-dev.yml(开发环境)文件或者application-pro.yml(线上环境)下的yeafel.cookie和yeafel.c5cookie配置自己账号登录之后的网易buff的cookie和c5cookie (igxe不需要配置)
,如您有服务器可修改 application-pro.yml的mysql地址为您线上服务器，application.yml下active切换为prod即可。
cookie配置例如:
```
yeafel:
  cookie: yourBuffCookie
  c5cookie: yourC5Cookie
```



## 致谢
**希望玩CSGO的程序员兄弟们**能发现问题，一起维护该项目。真诚感谢。时刻知晓CSGO各个平台市场波动，交易东西不亏钱。
也希望大家能够发财，Go!Go!Go!
