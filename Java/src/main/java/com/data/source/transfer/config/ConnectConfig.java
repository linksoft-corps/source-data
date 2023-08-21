package com.data.source.transfer.config;

import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "netty.connect")
public class ConnectConfig {
    private String host;

    private int port;

    /**重连时间 应该从配置文件读取暂未使用*/
    private Long intervalTime = 3000l;

    /**最大重连限制 按照道理来说应该通过配置文件设置 暂未使用*/
    private int maxReConnectNum = 1000;


}