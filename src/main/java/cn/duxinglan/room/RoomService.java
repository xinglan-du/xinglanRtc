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
 package cn.duxinglan.room;

 import cn.duxinglan.media.core.IConsumer;
 import cn.duxinglan.media.core.IMediaNode;
 import cn.duxinglan.media.core.MediaSession;
 import cn.duxinglan.media.core.endpoint.Endpoint;
 import cn.duxinglan.media.impl.DefaultRouter;
 import cn.duxinglan.media.impl.webrtc.GlobalProducerMediaRouter;
 import lombok.extern.slf4j.Slf4j;

 import java.util.ArrayList;
 import java.util.List;


 @Slf4j
 public class RoomService {

     private static volatile RoomService instance;

     private List<IMediaNode> mediaNodeList = new ArrayList<>();

     private List<Endpoint> endpointList = new ArrayList<>();

     private final DefaultRouter defaultRouter;

     private RoomService() {
         this.defaultRouter = new DefaultRouter();
     }


     public static RoomService getInstance() {
         if (instance == null) {
             synchronized (RoomService.class) {
                 if (instance == null) {
                     instance = new RoomService();
                 }
             }
         }
         return instance;
     }

     public void addEndpoint(Endpoint endpoint) {
         endpointList.add(endpoint);
         MediaSession mediaSession = endpoint.getMediaSession();
         mediaSession.addRouter(defaultRouter);
     }

     public void removeEndpoint(Endpoint endpoint) {

     }


     public void removeMediaNode(IMediaNode removeMediaNode) {
         if (mediaNodeList.remove(removeMediaNode)) {
             GlobalProducerMediaRouter globalMediaRouter = removeMediaNode.getGlobalMediaRouter();
             List<IConsumer> consumerList = globalMediaRouter.getConsumer();
             for (IConsumer consumer : consumerList) {
                 for (IMediaNode mediaNode : mediaNodeList) {
                     boolean b = mediaNode.removeConsumer(consumer);
                     if (b) {
                         mediaNode.updateOfferInfo();
                     }
                 }
             }

         }

     }

 }
