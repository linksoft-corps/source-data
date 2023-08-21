package com.data.source.transfer.handler;

import com.data.source.transfer.entity.MarketData;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * @DATE 2023/5/15
 * @AUTHOR xiaoshuwen
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class QuotaClientHandler extends SimpleChannelInboundHandler<MarketData.Message> {

    public ChannelHandlerContext ctx;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, MarketData.Message message) {
        MarketData.Message.DataType dataType = message.getDataType();

        switch (dataType) {
            case NewsType:
                MarketData.News news = message.getNews();

                break;
            case TradeType:
                MarketData.Trade trade = message.getTrade();
                break;
            // 省略其他类型...
            default:
                break;
        }
        /**
         * 重要的事情，重要的事情，重要的事情，讲了三遍。
         * 此处为接收到数据
         * 1. 切记不要在此打印数据
         * 2. 切记做任何IO操作
         * 以上操作都会影响接收速率，切记。
         */
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt){

        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.WRITER_IDLE) {
                // 发送心跳消息
                MarketData.Message build = MarketData.Message.newBuilder().setDataType(MarketData.Message.DataType.CertificationMsgType)
                        .setCertificationMsg(MarketData.CertificationMsg.newBuilder().setActionType(MarketData.ActionType.Heartbeat).build()).build();
                ctx.writeAndFlush(build);
            }
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx){
        //连接上订阅
        MarketData.Message.Builder ceshishouquan = MarketData.Message.newBuilder().setDataType(MarketData.Message.DataType.CertificationMsgType)
                .setCertificationMsg(MarketData.CertificationMsg.newBuilder().setActionType(MarketData.ActionType.Login)
                        .setAuth("aaaaa").build());
        ctx.channel().writeAndFlush(ceshishouquan.build());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
    }

}
