package com.yeafel.priceget.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author Yeafel
 * 2021/3/14 1:52
 * Do or Die,To be a better man!
 */
@Data
@Table(name = "transact_record")
@Entity
public class TransactRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Long goodsId;
    private String goodsName;
    private BigDecimal price;

    /** 武器磨损 */
    private String paintwear;

    /** 贴纸信息以及磨损度(多个贴纸记录以逗号隔开) */
    private String stickers;

    /** 贴纸是否造成价格影响(0、没有影响  1、有影响) */
    private Integer stickerIsInfluence;

    /** 交易时间 */
    private Date transactTime;


    /**  购买平台  */
    private String platform;


    /** igx平台对于记录的唯一标识 */
    private Long igxId;




}
