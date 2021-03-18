package com.yeafel.priceget.entity;

import lombok.Data;

import javax.persistence.*;

/**
 * @author Yeafel
 * 2021/3/14 18:05
 * Do or Die,To be a better man!
 */
@Table(name = "need_get_goods")
@Entity
@Data
public class NeedGetGoods {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer goodsId;

    private String goodsName;

    private Integer igxeGoodsId;

    @Column(name = "c5_goods_id")
    private Long c5GoodsId;

}
