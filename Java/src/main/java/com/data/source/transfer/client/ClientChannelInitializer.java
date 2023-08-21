package com.data.source.transfer.client;

import com.data.source.transfer.entity.MarketData;
import com.data.source.transfer.handler.QuotaClientHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;



/**
 * @DATE 2023/5/16
 * @AUTHOR xiaoshuwen
 */
@Component
@ConditionalOnBean(NettyClient.class)
public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Autowired
    private QuotaClientHandler quotaClientHandler;

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ch.pipeline()
                .addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4))
                .addLast(new LengthFieldPrepender(4))
                .addLast(new IdleStateHandler(0, 30, 0))
                .addLast(new ProtobufEncoder())
                .addLast(new ProtobufDecoder(MarketData.Message.getDefaultInstance()))
                .addLast(quotaClientHandler);
    }
}
