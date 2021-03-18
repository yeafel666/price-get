package com.yeafel.priceget.scheduled;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yeafel.priceget.entity.NeedGetGoods;
import com.yeafel.priceget.entity.TransactRecord;
import com.yeafel.priceget.repository.NeedGetGoodsRepository;
import com.yeafel.priceget.repository.TransactRecordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author Yeafel
 * 定时获取igxecsgo商品价格以及部分属性
 * 2021/3/13 23:53
 * Do or Die,To be a better man!
 */
@Component
@Slf4j
public class GetIgxeCSGOPriceDataScheduled {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TransactRecordRepository transactRecordRepository;

    @Autowired
    private NeedGetGoodsRepository needGetGoodsRepository;

    /**
     * 每个小时第1分钟获取
     */
    @Scheduled(cron = "0 1 * * * ?")
//    @Scheduled(cron = "0/3 * * * * ?")
    public void getPricesData() {
        //https://www.igxe.cn/product/get_product_sales_history/730/571769
        List<NeedGetGoods> needGetGoodsList = needGetGoodsRepository.findAll();
        String[] names = new String[needGetGoodsList.size()];
        int[] goodsIdList = new int[needGetGoodsList.size()];
        for (int i = 0; i < needGetGoodsList.size(); i++) {
            names[i] = needGetGoodsList.get(i).getGoodsName();
            goodsIdList[i] = needGetGoodsList.get(i).getIgxeGoodsId();
        }
        System.out.println("\r\n \r\n igxe:本次获取任务启动.........................");
        for (int i = 0; i < goodsIdList.length; i++) {
            String goodsName = names[i];
            String url = "https://www.igxe.cn/product/get_product_sales_history/730/" + goodsIdList[i];

            HttpHeaders headers = new HttpHeaders();
            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.190 Safari/537.36";
            headers.set(HttpHeaders.USER_AGENT,userAgent);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
            HttpEntity<MultiValueMap<String, String>>  entity = new HttpEntity<>(paramMap, headers);

            ResponseEntity<String> result = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JSONObject body = JSONObject.parseObject(result.getBody());
            JSONArray data = body.getJSONArray("data");
            for (Object o : data) {
                JSONObject item = (JSONObject) o;
                Long goodsId = item.getLong("product_id");
                BigDecimal price = item.getBigDecimal("unit_price");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                SimpleDateFormat sdfForIgxTransactTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                //交易时间 ，但小时数一直是00,
                String transactTimeStr = item.getString("last_updated").
                        replace("年", "-").
                        replace("月", "-").
                        replace("日", "") + " 00:00:00";
                Date transactTime = null;
                try {
                    transactTime = sdfForIgxTransactTime.parse(transactTimeStr);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                String paintwear = item.getString("exterior_wear");

                Long igxId = item.getLong("id");

                //当磨损度为空时，igxId是否重复
                if (paintwear == null) {
                    TransactRecord byIgxId = transactRecordRepository.findByIgxId(igxId);
                    if (byIgxId != null) {
                        System.out.println("\r\n" + byIgxId.toString());
                        //说明已经存在了该条记录，跳过该次存储
                        System.out.println("igxe: 该条记录已存在，跳过");
                        continue;
                    }
                }

                JSONArray stickers = item.getJSONArray("sticker");
                //最终入库的某武器的一条或多条贴纸情况
                List<String> stickerList = new ArrayList<>();
                //如果贴纸不为空那么将贴纸信息记录
                if (stickers != null && stickers.size() > 0) {
                    for (int j = 0; j < stickers.size(); j++) {
                        JSONObject sticker = (JSONObject) stickers.get(j);
                        String stickerName = sticker.getString("sticker_title");
                        BigDecimal stickerWear = sticker.getBigDecimal("wear");
                        String stickerItem = stickerName + "|" + stickerWear;
                        stickerList.add(stickerItem);
                    }
                }




                //开始存入数据
                TransactRecord transactRecord = new TransactRecord();
                transactRecord.setPrice(price);
                transactRecord.setGoodsId(goodsId);
                transactRecord.setPaintwear(paintwear);
                transactRecord.setStickers(stickerList.toString());
                transactRecord.setStickerIsInfluence(0);
                transactRecord.setTransactTime(transactTime);
                transactRecord.setGoodsName(goodsName);
                transactRecord.setIgxId(igxId);
                transactRecord.setPlatform("IGXE");
                try {
                    System.out.println("igxe:开始存储----------------------------------------");
                    String curTime = sdf.format(new Date());
                    System.out.println("当前时间" + curTime);
                    transactRecordRepository.save(transactRecord);
                    System.out.println(transactRecord.toString());
                    System.out.println("igxe:存储成功----------------------------------------\r\n \r\n");
                    //存一条数据睡眠2秒
                    try {
                        System.out.println("igxe:已获取一条记录，睡眠2秒···············\r\n \r\n \r\n");
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    System.out.println("igxe:该记录已经存在，不予存储");
                    System.out.println("-----------------------------------------------\r\n \r\n");
                }
            }
        }
        System.out.println("igxe:本次获取任务终止.........................");

    }
}
