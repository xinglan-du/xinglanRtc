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
 package cn.duxinglan.media.service.webrtc;


 import cn.duxinglan.media.core.IMediaNode;
 import cn.duxinglan.media.core.signaling.ISignalingChannelContext;
 import cn.duxinglan.media.impl.webrtc.WebrtcNode;
 import cn.duxinglan.media.module.CacheModel;
 import cn.duxinglan.media.signaling.data.CreateNodeData;
 import cn.duxinglan.media.signaling.data.NodeSignalingData;
 import cn.duxinglan.media.signaling.data.SignalingData;
 import cn.duxinglan.media.signaling.type.RtpTransportType;
 import cn.duxinglan.media.signaling.type.SignalingType;
 import cn.duxinglan.room.RoomService;
 import com.fasterxml.jackson.core.JsonProcessingException;
 import com.fasterxml.jackson.databind.ObjectMapper;
 import lombok.extern.slf4j.Slf4j;
 import org.jspecify.annotations.NonNull;

 import java.util.Map;
 import java.util.concurrent.ConcurrentHashMap;

 @Slf4j
 public class WebrtcNodeServer implements WebrtcNode.IWebrtcNodeEvent {

     private final ObjectMapper objectMapper = CacheModel.getObjectMapper();

     private final Map<String, IMediaNode> mediaNodeMap = new ConcurrentHashMap<>();

     private final ISignalingChannelContext signalingChannelContext;

     public WebrtcNodeServer(ISignalingChannelContext signalingChannelContext) {
         this.signalingChannelContext = signalingChannelContext;
     }


     public void handleSignalingData(@NonNull SignalingData signalingData) {
         SignalingType type = signalingData.type();
         if (type == SignalingType.INIT) {
             CreateNodeData createNodeData = objectMapper.convertValue(signalingData.data(), CreateNodeData.class);
             IMediaNode node = createNode(createNodeData.transportType(), createNodeData.version());
             mediaNodeMap.put(node.getNodeId(), node);
             RoomService.getInstance().addMediaNode(node);

             try {
                 sendMessage(new SignalingData(SignalingType.INIT, new CreateNodeData(RtpTransportType.WEBRTC, node.getNodeVersion(), node.getNodeId())));
                 node.init();
             } catch (Exception e) {
                 log.error(e.getMessage(), e);
             }

         } else if (type == SignalingType.NODE) {
             NodeSignalingData nodeSignalingData = objectMapper.convertValue(signalingData.data(), NodeSignalingData.class);
             IMediaNode mediaNode = mediaNodeMap.get(nodeSignalingData.nodeId());
             if (mediaNode != null) {
                 mediaNode.handleNodeData(nodeSignalingData);
             }
         }
     }

     private IMediaNode createNode(RtpTransportType rtpTransportType, int version) {
         if (rtpTransportType == RtpTransportType.WEBRTC) {
             try {
                 return new WebrtcNode(this, version);
             } catch (Exception e) {
                 log.error(e.getMessage(), e);
                 return null;
             }
         }
         return null;
     }

     private void sendMessage(SignalingData signalingData) throws JsonProcessingException {
         String text = CacheModel.getObjectMapper().writeValueAsString(signalingData);
         signalingChannelContext.writeAndFlush(text);
     }

     @Override
     public void onMessage(IMediaNode node, Object data) {
         NodeSignalingData nodeSignalingData = new NodeSignalingData(node.getNodeId(), data);
         try {
             sendMessage(new SignalingData(SignalingType.NODE, nodeSignalingData));
         } catch (JsonProcessingException e) {
             log.error(e.getMessage(), e);
         }
     }

     public void close() {

     }
 }
