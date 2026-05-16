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
 package cn.duxinglan.media.impl.v1;

 import cn.duxinglan.ice.IceEndpoint;
 import cn.duxinglan.media.core.MediaSession;
 import cn.duxinglan.media.core.endpoint.Endpoint;
 import cn.duxinglan.media.core.signaling.EndpointMessage;
 import cn.duxinglan.media.core.signaling.SignalingConnection;
 import cn.duxinglan.media.impl.TransportContext;
 import cn.duxinglan.media.impl.webrtc.MediaLineInfo;
 import cn.duxinglan.media.impl.webrtc.WebrtcNode;
 import cn.duxinglan.media.impl.webrtc.WebrtcNodeDataType;
 import cn.duxinglan.media.impl.webrtc.WebrtcProcessor;
 import cn.duxinglan.media.module.CacheModel;
 import cn.duxinglan.media.signaling.sdp.RTCSessionDescriptionInit;
 import cn.duxinglan.media.util.UUIDUtils;
 import cn.duxinglan.sdp.entity.IceInfo;
 import cn.duxinglan.sdp.entity.type.MediaInfoType;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import lombok.extern.slf4j.Slf4j;

 import java.util.Collection;

 @Slf4j
 public class WebRtcEndpoint implements Endpoint, WebrtcProcessor.IWebrtcProcessorEvent, WebrtcSession.SessionEvent {


     private final String nodeId;

     private final WebrtcProcessor webrtcProcessor;

     private final WebrtcSession webrtcSession;

     private SignalingConnection connection;

     private final WebrtcProcessorEvent webrtcProcessorEvent;

     private final ObjectMapper objectMapper = CacheModel.getObjectMapper();

     private final TransportContext transportContext;


     public WebRtcEndpoint(WebrtcProcessorEvent webrtcProcessorEvent) throws Exception {
         this.nodeId = UUIDUtils.createUUID();
         this.webrtcSession = new WebrtcSession(this);
         this.transportContext = new TransportContext(webrtcSession);
         this.webrtcProcessorEvent = webrtcProcessorEvent;
         webrtcProcessor = new WebrtcProcessor(this.transportContext.getDtlsKeyMaterial(), this);
     }

     @Override
     public void bindConnection(SignalingConnection connection) {
         this.connection = connection;
         init();
     }

     @Override
     public void unbindConnection(SignalingConnection connection) {
         this.connection = null;
     }


     private void init() {
         //这里要发送offer给客户端
         webrtcProcessor.init();
     }


     @Override
     public String getId() {
         return this.nodeId;
     }

     @Override
     public void handleSignal(Object msg) {
         WebrtcNodeData webrtcNodeData = objectMapper.convertValue(msg, WebrtcNodeData.class);

         switch (webrtcNodeData.type()) {
             case NODE_OFFER:
             case NODE_ANSWER:
                 RTCSessionDescriptionInit rtcSessionDescriptionInit = objectMapper.convertValue(webrtcNodeData.data(), RTCSessionDescriptionInit.class);
                 webrtcProcessor.setRemoteDescription(rtcSessionDescriptionInit);
         }

     }


     @Override
     public void close() {

     }

     @Override
     public MediaSession getMediaSession() {
         return this.webrtcSession;
     }



     @Override
     public void onAnswer(RTCSessionDescriptionInit answer) {
         //发送应答
         log.debug("发送answer:{}", answer.sdp());
         WebrtcNodeData webrtcNodeData = new WebrtcNodeData(WebrtcNodeDataType.NODE_ANSWER, answer);
         this.connection.sendEndpointMessage(new EndpointMessage(getId(), webrtcNodeData));
     }

     @Override
     public void onAddWebrtcProducer(MediaLineInfo mediaInfo) {
         //添加生产者

     }

     @Override
     public WebrtcNode getWebrtcNode() {
         return null;
     }

     @Override
     public void onIceInfo(IceInfo localIceInfo, IceInfo remoteIceInfo) {
         IceEndpoint local = new IceEndpoint(localIceInfo.getUfrag(), localIceInfo.getPwd());
         IceEndpoint remote = new IceEndpoint(remoteIceInfo.getUfrag(), remoteIceInfo.getPwd());
         transportContext.setLocal(local);
         transportContext.setRemote(remote);
         webrtcProcessorEvent.onIceInfo(transportContext);
     }

     @Override
     public void onSendOffer(RTCSessionDescriptionInit offer) {
         WebrtcNodeData webrtcNodeData = new WebrtcNodeData(WebrtcNodeDataType.NODE_OFFER, offer);
         this.connection.sendEndpointMessage(new EndpointMessage(getId(), webrtcNodeData));
     }


     @Override
     public void onRemoteMediaLinesParsed(Collection<MediaLineInfo> mediaLines) {
         log.info("接收到全量的媒体行更新");
         webrtcSession.onMediaLinesParsed(mediaLines);
     }


     @Override
     public MediaLineInfo onAddSendMediaLineInfo(MediaInfoType type, MediaLineInfo.Info info) {
         return webrtcProcessor.onAddSendMediaLineInfo(type, info);
     }


     public interface WebrtcProcessorEvent {

         void onIceInfo(TransportContext context);
     }


 }
