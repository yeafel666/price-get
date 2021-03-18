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
 * 定时获取csgo商品价格以及部分属性
 * 2021/3/13 23:53
 * Do or Die,To be a better man!
 */
@Component
@Slf4j
public class GetCSGOPriceDataScheduled {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TransactRecordRepository transactRecordRepository;

    @Autowired
    private NeedGetGoodsRepository needGetGoodsRepository;


    @Value(value = "${yeafel.cookie}")
    private String cookie;

    /**
     * 每个小时第10分钟获取
     */
//    @Scheduled(cron = "0 10 * * * ?")
//    @Scheduled(cron = "0/3 * * * * ?")
    public void getPricesData() {
        //https://buff.163.com/api/market/goods/bill_order?game=csgo&goods_id=42192&_=1615643464574
        //https://buff.163.com/api/market/goods/bill_order?game=csgo&goods_id=768729&_=1615651248793
//        String url = "https://buff.163.com/api/market/goods/bill_order";
        List<NeedGetGoods> needGetGoodsList = needGetGoodsRepository.findAll();
        String[] names = new String[needGetGoodsList.size()];
        int[] goodsIdList = new int[needGetGoodsList.size()];
        for (int i = 0; i < needGetGoodsList.size(); i++) {
            names[i] = needGetGoodsList.get(i).getGoodsName();
            goodsIdList[i] = needGetGoodsList.get(i).getGoodsId();
        }
        System.out.println("\r\n \r\n 本次获取任务启动.........................");
//        for (int i = 0; i < goodsIdList.length; i++) {
        for (int i = 0; i < 8; i++) {
            String goodsName = names[i];
            long currentTimeMillis = System.currentTimeMillis();
            String url = "https://buff.163.com/api/market/goods/bill_order?game=csgo" +
                    "&goods_id="+ goodsIdList[i] + "&_=" + currentTimeMillis;

            HttpHeaders headers = new HttpHeaders();
            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.190 Safari/537.36";
            List<String> cookies = new ArrayList<>();
            cookies.add("_ntes_nnid=" + cookie);
            headers.set(HttpHeaders.USER_AGENT,userAgent);
            headers.put(HttpHeaders.COOKIE,cookies);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
            HttpEntity<MultiValueMap<String, String>>  entity = new HttpEntity<>(paramMap, headers);

            ResponseEntity<String> result = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JSONObject body = JSONObject.parseObject(result.getBody());
            JSONObject data = body.getJSONObject("data");
            JSONArray items = data.getJSONArray("items");
            for (Object o : items) {
                JSONObject item = (JSONObject) o;
                Long goodsId = item.getLong("goods_id");
                BigDecimal price = item.getBigDecimal("price");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                //交易时间 ，但小时数一直是00,
                JSONObject assetInfo = item.getJSONObject("asset_info");
                //我需要从商品解冻时间 -7天获得真正交易时间
                String transactTimeStr = sdf.format(new Date(Long.parseLong(item.getLong("transact_time")+"000")));
                Date transactTime = null;
                try {
                    transactTime = sdf.parse(transactTimeStr);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                String paintwear = assetInfo.getString("paintwear");
                JSONObject info = assetInfo.getJSONObject("info");
                JSONArray stickers = info.getJSONArray("stickers");
                //最终入库的某武器的一条或多条贴纸情况
                List<String> stickerList = new ArrayList<>();
                //如果贴纸不为空那么将贴纸信息记录
                if (stickers.size() > 0) {
                    for (int j = 0; j < stickers.size(); j++) {
                        JSONObject sticker = (JSONObject) stickers.get(j);
                        String stickerName = sticker.getString("name");
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
                transactRecord.setPlatform("网易BUFF");
                try {
                    System.out.println("开始存储----------------------------------------");
                    String curTime = sdf.format(new Date());
                    System.out.println("当前时间" + curTime);
                    transactRecordRepository.save(transactRecord);
                    System.out.println(transactRecord.toString());
                    System.out.println("存储成功----------------------------------------\r\n \r\n");
                    //存一条数据睡眠10秒
                    try {
                        System.out.println("已获取一条记录，睡眠10秒···············\r\n \r\n \r\n");
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } catch (Exception e) {
                    System.out.println("该记录已经存在，不予存储");
                    System.out.println("-----------------------------------------------\r\n \r\n");
                }
            }
        }
        System.out.println("本次获取任务终止.........................");

    }
}
