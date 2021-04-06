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
 * 定时获取C5平台csgo商品价格以及部分属性
 * 2021/3/13 23:53
 * Do or Die,To be a better man!
 */
@Component
@Slf4j
public class GetC5CSGOPriceDataScheduled {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TransactRecordRepository transactRecordRepository;

    @Autowired
    private NeedGetGoodsRepository needGetGoodsRepository;


    @Value(value = "${yeafel.c5cookie}")
    private String cookie;

    /**
     * 每个小时第30分钟获取
     */
    @Scheduled(cron = "0 30 * * * ?")
//    @Scheduled(cron = "0/3 * * * * ?")
    public void getPricesData() {
//        String url = "https://www.c5game.com/gw/steamtrade/sga/store/v2/recent-deal?itemId=553370868&reqId=1615956839";
        List<NeedGetGoods> needGetGoodsList = needGetGoodsRepository.findAll();
        String[] names = new String[needGetGoodsList.size()];
        long[] goodsIdList = new long[needGetGoodsList.size()];
        for (int i = 0; i < needGetGoodsList.size(); i++) {
            names[i] = needGetGoodsList.get(i).getGoodsName();
            goodsIdList[i] = needGetGoodsList.get(i).getC5GoodsId();
        }
        System.out.println("\r\n \r\n C5:本次获取任务启动.........................");
        for (int i = 0; i < needGetGoodsList.size(); i++) {
            String goodsName = names[i];
            long currentTimeMillis = System.currentTimeMillis();
            String url = "https://www.c5game.com/gw/steamtrade/sga/store/v2/recent-deal?" +
                    "itemId=" + goodsIdList[i] + "&reqId=" + currentTimeMillis + "5864132031";

            HttpHeaders headers = new HttpHeaders();
            String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.190 Safari/537.36";
            List<String> cookies = new ArrayList<>();
            cookies.add(cookie);
            headers.set(HttpHeaders.USER_AGENT, userAgent);
            headers.put(HttpHeaders.COOKIE, cookies);
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

            MultiValueMap<String, String> paramMap = new LinkedMultiValueMap<>();
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(paramMap, headers);

            ResponseEntity<String> result = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            JSONObject body = JSONObject.parseObject(result.getBody());
            if (body == null) {
                System.out.println("本次【" + goodsName + "】未获取到内容，进入下一个商品获取");
            } else if ("请登录".equals(body.getString("errorMsg"))) {
                System.err.println("C5的令牌已经过期，请及时更新令牌");
                //直接终止本次C5的程序
                break;
            } else {
                JSONArray data = body.getJSONArray("data");
                for (Object o : data) {
                    JSONObject item = (JSONObject) o;
                    Long goodsId = Long.valueOf(item.getString("itemId"));
                    BigDecimal price = item.getBigDecimal("price");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    //交易时间 ，但小时数一直是00,
                    String transactTimeStr = sdf.format(new Date(Long.parseLong(item.getLong("updateTime") + "000")));
                    Date transactTime = null;
                    try {
                        transactTime = sdf.parse(transactTimeStr);
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    //为了取磨损度和印花信息 ----------------start
                    String detailUrl = "https://www.c5game.com/steam/item/detail.html?id=" + item.getString("productId");
                    headers.set(HttpHeaders.USER_AGENT, userAgent);
                    headers.put(HttpHeaders.COOKIE, cookies);
                    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));

                    MultiValueMap<String, String> paramMapForDetail = new LinkedMultiValueMap<>();
                    HttpEntity<MultiValueMap<String, String>> entityForDetail = new HttpEntity<>(paramMapForDetail, headers);

                    ResponseEntity<String> resultForDetail = restTemplate.exchange(detailUrl, HttpMethod.GET, entityForDetail, String.class);
                    int i3 = resultForDetail.toString().indexOf("磨损：");
                    String paintwear = null;
                    if (i3 != -1) {
                        paintwear = resultForDetail.toString().substring(resultForDetail.toString().indexOf("磨损："), resultForDetail.toString().indexOf("磨损：") + 16).replace("磨损：", "");
                    }
                    int i1 = resultForDetail.toString().indexOf("印花:");
                    String stickers = null;
                    //如果印花项存在
                    if (i1 != -1) {
                        stickers = resultForDetail.toString().substring(resultForDetail.toString().indexOf("印花:"), resultForDetail.toString().indexOf("</center>")).replace("印花:", "");
                    }
                    //为了取磨损度和印花信息 ----------------end


                    //入库之前当磨损度为空时，交易时间和商品Id是否重复
                    if (paintwear == null) {
                        List<TransactRecord> byGoodsIdAndTransactTime = transactRecordRepository.findByGoodsIdAndTransactTime(goodsId, transactTime);
                        if (byGoodsIdAndTransactTime != null && byGoodsIdAndTransactTime.size() > 0) {
                            System.out.println("\r\n" + byGoodsIdAndTransactTime.toString());
                            //说明已经存在了该条记录，跳过该次存储
                            System.out.println("C5: 该条记录已存在，跳过");
                            continue;
                        }
                    }

                    //开始存入数据
                    TransactRecord transactRecord = new TransactRecord();
                    transactRecord.setPrice(price);
                    transactRecord.setGoodsId(goodsId);
                    transactRecord.setPaintwear(paintwear);
                    transactRecord.setStickers(stickers);
                    transactRecord.setStickerIsInfluence(0);
                    transactRecord.setTransactTime(transactTime);
                    transactRecord.setGoodsName(goodsName);
                    transactRecord.setPlatform("C5");
                    try {
                        System.out.println("C5:开始存储----------------------------------------");
                        String curTime = sdf.format(new Date());
                        System.out.println("C5:当前时间" + curTime);
                        transactRecordRepository.save(transactRecord);
                        System.out.println(transactRecord.toString());
                        System.out.println("C5:存储成功----------------------------------------\r\n \r\n");
                        //存一条数据睡眠10秒
                        try {
                            System.out.println("C5:已获取一条记录，睡眠10秒···············\r\n \r\n \r\n");
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        System.out.println("C5:该记录已经存在，不予存储");
                        System.out.println("-----------------------------------------------\r\n \r\n");
                    }
                }
            }
        }
        System.out.println("C5:本次获取任务终止.........................");

    }
}
