 /*
  * 版权所有 (c) 2025 www.duxinglan.cn
  *
  * 项目名称：xinglanRtc
  *
  * 本文件属于 xinglanRtc 项目的一部分。
  *
  * 本软件依据 XinglanRtc 非商业许可证（XNCL）授权，仅限个人非商业使用。
  * 禁止任何形式的商业用途，包括但不限于：收费安装、收费部署、
  * 收费运维、收费技术支持等行为。
  *
  * 详情请参阅项目根目录下的 LICENSE 文件。
  */
 package cn.duxinglan.media.impl.signaling;


 import cn.duxinglan.media.core.signaling.SignalMessage;
 import cn.duxinglan.media.core.signaling.SignalingConnection;
 import cn.duxinglan.media.module.CacheModel;
 import com.fasterxml.jackson.core.JsonProcessingException;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import io.netty.channel.ChannelHandlerContext;
 import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
 import lombok.extern.slf4j.Slf4j;

 @Slf4j
 public class WsSignalingConnection extends SignalingConnection {

     private final ChannelHandlerContext ctx;

     private final ObjectMapper objectMapper = CacheModel.getObjectMapper();

     public WsSignalingConnection(ChannelHandlerContext ctx) {
         this.ctx = ctx;
     }


     @Override
     protected void sendSignal(SignalMessage msg) {
         String message = null;
         try {
             message = objectMapper.writeValueAsString(msg);
             ctx.writeAndFlush(new TextWebSocketFrame(message));
         } catch (JsonProcessingException e) {
             log.error(e.getMessage(), e);
         }
     }
 }
