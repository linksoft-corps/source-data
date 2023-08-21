package com.data.source.transfer.client;

import com.data.source.transfer.config.ConnectConfig;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @DATE 2023/5/15
 * @AUTHOR xiaoshuwen
 */
@Slf4j
@Component
@EnableConfigurationProperties(ConnectConfig.class)
public class NettyClient implements CommandLineRunner , InitializingBean {


    @Autowired
    private ConnectConfig connectConfig;
    private static Bootstrap bootstrap;
    private static EventLoopGroup group;
    public static ConnectConfig reconnectConfig = new ConnectConfig();


    @Autowired
    private ClientChannelInitializer clientChannelInitializer;

    @Async
    @Override
    public void run(String... args) throws Exception {
         group = new NioEventLoopGroup();
        try {
             bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(clientChannelInitializer);
            //连接服务器
            ChannelFuture future = bootstrap.connect(connectConfig.getHost(), connectConfig.getPort()).sync();
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static void reConnect() throws Exception {
        ChannelFuture cf = bootstrap.connect(reconnectConfig.getHost(), reconnectConfig.getPort());
        cf.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    future.channel().eventLoop().schedule(() -> {
                        try {
                            reConnect();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        //可通过配置文件设置间隔时间  暂未设置
                    }, reconnectConfig.getIntervalTime(), TimeUnit.MILLISECONDS);
                }
            }
        });
        cf.channel().closeFuture().sync();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        reconnectConfig.setHost(connectConfig.getHost());
        reconnectConfig.setPort(connectConfig.getPort());
        reconnectConfig.setIntervalTime(connectConfig.getIntervalTime());
    }
}
