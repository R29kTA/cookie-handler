package com.r29kta.cookiehandler.io;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
public class Handler {
    @Resource
    private AmqpTemplate rabbitTemplate;
    private final String currentQueue = "cookie";
    private final String nextQueue = "payload";
    private final String logQueue = "log";
    private static final Logger logger = LoggerFactory.getLogger(Handler.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    public Handler(){
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    private static final String url = "http://jwc.scnucas.com";
    private static final WebClient webClient = org.springframework.web.reactive.function.client.WebClient.builder()
            .defaultHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0")
            .defaultHeader("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .defaultHeader("Accept-Language","zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
            .defaultHeader("Accept-Encoding","gzip, deflate")
            .defaultHeader("Connection","keep-alive")
            .defaultHeader("Upgrade-Insecure-Requests","1")
            .defaultHeader("Pragma","no-cache")
            .defaultHeader("Cache-Control","max-age=0")
            .defaultHeader("Host","jwc.scnucas.com")
            .baseUrl(url)
            .build();
    @RabbitListener(queues = currentQueue)
    public void handleMessage(Message message, Channel channel ) {
        handle(message,channel);
    }
    public void handle(Message message,Channel channel){
        try {
            Data dat = byteToData(message.getBody());
            if (dat.getCookie()!=null){
                // 确认消息
                channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
                // 消息进入下个队列payload
                rabbitTemplate.convertAndSend(nextQueue,message);
                logger.info("重新进入队列消息："+ new String(message.getBody()));
            }else {
                logger.info("新的进入队列消息");
                webClient
                        .get()
                        .retrieve()
                        .toEntity(String.class)
                        .timeout(Duration.ofMinutes(1))
                        .subscribe(resp -> {
                            List<String> cookie = resp.getHeaders().get("Set-Cookie");
                            if (cookie!=null && cookie.size()>2){
                                String useCookie = null;
                                try {
                                    useCookie = parseCookie(cookie.get(2));
                                    Data data = byteToData(message.getBody());
                                    data.setCookie(useCookie);
                                    // false 只确认该条消息
                                    sendData(data);
                                    channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
                                    sendLog(data);
                                } catch (Exception e) {
                                    logger.error(e.getMessage());
                                    // true 消息重回队列
                                }
                                logger.info(useCookie);
                            }else {
                                // 重新返回队列
                                try {
                                    channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        },throwable -> {
                            logger.error(throwable.getMessage());
                            try {
                                // true 消息重回队列
                                channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
                            } catch (IOException ex) {
                                logger.error("出现IO异常，放弃消息："+message);
                            }
                        });
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private String parseCookie(String string) throws Exception {
        Pattern pattern = Pattern.compile("ASP.NET_SessionId=(\\w+);");
        Matcher matcher = pattern.matcher(string);
        if (matcher.find()){
            return matcher.group();
        }
        throw new Exception("Cookie parse failed!");
    }
    private Data byteToData(byte[] bytes) throws IOException {
        return mapper.readValue(bytes, Data.class);
    }
    private String dataToString(Data data) throws IOException {;
        return mapper.writeValueAsString(data);
    }
    private void sendData(Data data) throws IOException {
        rabbitTemplate.convertAndSend(nextQueue,dataToString(data));
    }
    private void sendLog(Data data){
        String msg = data.getUsername()+"," + currentQueue+",";
        rabbitTemplate.convertAndSend(logQueue,msg);
    }
}
