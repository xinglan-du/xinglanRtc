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
 package cn.duxinglan.media.impl;


 import cn.duxinglan.ice.attribute.*;
 import cn.duxinglan.ice.message.StunMessage;
 import cn.duxinglan.media.core.ChannelContext;
 import cn.duxinglan.media.transport.udp.InboundPacket;
 import io.netty.buffer.ByteBuf;
 import lombok.extern.slf4j.Slf4j;

 import java.security.InvalidKeyException;
 import java.security.MessageDigest;
 import java.security.NoSuchAlgorithmException;
 import java.util.Optional;

 /**
  * IceProcessor 是一个负责处理 STUN 消息的类，主要用于解析和验证消息的完整性与认证信息。
  * 它在接收到消息后，基于用户身份标识（ufrag）查找相关的传输上下文，验证消息的合法性，
  * 并在必要时返回绑定成功消息。这在交互式连接建立（ICE）协议中用于 NAT 穿透和通信建立。
  * <p>
  * 主要功能如下：
  * 1. 解码接收到的 STUN 消息。
  * 2. 根据消息中的 USERNAME 属性解析用户名，并通过 IceProcessorEvent 接口查找传输上下文。
  * 3. 验证消息完整性，包括校验数据的消息认证码（Message Integrity）和指纹（Fingerprint）。
  * 4. 在验证通过情况下，发送绑定成功响应消息给对端。
  *
  */
 @Slf4j
 public class IceProcessor {

     /**
      * 表示通道的上下文信息，用于进行网络通信。
      * <p>
      * channelContext 是 IceProcessor 类中一个关键的不可变字段，通过其可以获取远程地址、
      * 分配缓冲区以及发送数据的能力。它与 ICE（交互式连接建立）通信密切相关。
      * <p>
      * 主要功能：
      * - 提供远程地址的访问接口。
      * - 允许分配指定长度的缓冲区（ByteBuf）。
      * - 提供向远程目标发送数据的能力。
      * <p>
      * 该字段被用作与底层网络通道的抽象接口，支持 IceProcessor 类中对数据的处理与传输。
      */
     private final ChannelContext channelContext;

     /**
      * 表示用于处理 ICE（交互式连接建立）的事件回调的接口实例。
      * <p>
      * iceProcessorEvent 是 IceProcessor 类的一个不可变（final）私有字段，
      * 它用于与外部环境交互，通过提供处理特定 ICE 事件（如查找传输上下文或处理传输上下文）的功能来协助 ICE 流程。
      * <p>
      * 依赖 IceProcessorEvent 接口的实现，相关功能包括：
      * 1. 根据 `ufrag` 查找相关的传输上下文（TransportContext）。
      * 2. 处理传输上下文的事件。
      * <p>
      * 此字段赋值在 IceProcessor 构造函数调用时传入，确保在对象创建时完成依赖注入。
      */
     private final IceProcessorEvent iceProcessorEvent;

     /**
      * IceProcessor 构造方法，用于初始化 ICE 处理器实例。
      *
      * @param channelContext    用于网络通信的上下文，负责与远程端点之间的数据交互。
      * @param iceProcessorEvent 事件回调接口，处理与 ICE 相关的业务逻辑，如上下文管理和事件触发。
      */
     public IceProcessor(ChannelContext channelContext, IceProcessorEvent iceProcessorEvent) {
         this.channelContext = channelContext;
         this.iceProcessorEvent = iceProcessorEvent;
     }

     /**
      * 处理传入的 STUN 消息数据。
      *
      * @param data 包含 STUN 消息的字节缓冲区。
      */
     public void receiveProcess(InboundPacket data) {
         StunMessage.decode(data.packet()).ifPresent(message -> {
             try {
                 UsernameAttribute attributes = (UsernameAttribute) message.getAttributes(AttributeType.USERNAME);
                 String[] username = attributes.getUsername().split(":");
                 Optional<TransportContext> byUfrag = iceProcessorEvent.findByUfrag(username[0]);
                 byUfrag.ifPresent(transportContext -> {
                     String password = transportContext.getLocal().pwd();
                     log.debug("这次stun消息使用的用户名:{};密码:{}", attributes.getUsername(), password);
                     //校验数据
                     if (verifyData(message, password)) {
                         //这里要标志当前端口已经认证成功
                         StunMessage bindingSuccessMessage = StunMessage.createBindingSuccessMessage(password, message, channelContext.getRemoteAddress());
                         channelContext.send(bindingSuccessMessage);
                         PriorityAttribute priorityAttribute = (PriorityAttribute) message.getAttributes(AttributeType.PRIORITY);
                         iceProcessorEvent.onTransportContext(transportContext);
                     }
                 });


             } catch (Exception e) {
                 log.error(e.getMessage(), e);
             }
         });
     }


     public ByteBuf sendProcess(ChannelContext channelContext,StunMessage stunMessage) {
         int totalLength = stunMessage.getTotalLength();
         ByteBuf byteBuf = channelContext.allocateByteBuf(totalLength);
         stunMessage.writeTo(byteBuf);
         return byteBuf;
     }



     /**
      * 验证 STUN 消息的完整性与指纹的正确性。
      *
      * @param message  需要验证的 STUN 消息实例。
      * @param password 用于验证消息完整性的密码。
      * @return 如果消息完整性校验和指纹校验均通过，则返回 true，否则返回 false。
      */
     private boolean verifyData(StunMessage message, String password) {
         //校验数据完整性
         boolean messageIntegrity = checkMessageIntegrity(message, password);
         //校验指纹
         boolean verifyFingerprint = verifyFingerprint(message);
         return messageIntegrity && verifyFingerprint;
     }


     /**
      * 检查给定的 STUN 消息的完整性。
      *
      * @param message  要检查完整性的 STUN 消息实例。
      * @param password 用于计算消息完整性的密码。
      * @return 如果消息完整性验证通过，则返回 true；否则返回 false。
      */
     private boolean checkMessageIntegrity(StunMessage message, String password) {
         try {
             byte[] hmac = message.calculateMessageIntegrity(password, message.getByteBuf());
             //数据请求的数据进行比对
             MessageIntegrityAttribute stunAttribute = (MessageIntegrityAttribute) message.getAttributes(AttributeType.MESSAGE_INTEGRITY);
             byte[] messageIntegrity = stunAttribute.getMessageIntegrity();
             return MessageDigest.isEqual(hmac, messageIntegrity);
         } catch (NoSuchAlgorithmException | InvalidKeyException e) {
             log.error(e.getMessage(), e);
             return false;
         }
     }

     /**
      * 验证给定的 STUN 消息的指纹是否正确。
      *
      * @param message 需要验证的 STUN 消息实例。
      * @return 如果指纹验证通过，则返回 true；否则返回 false。
      */
     private boolean verifyFingerprint(StunMessage message) {
         long calcValue = message.calculateFingerprint(message.getByteBuf());
         FingerprintAttribute stunAttribute = (FingerprintAttribute) message.getAttributes(AttributeType.FINGERPRINT);
         return stunAttribute.getFingerprint() == calcValue;
     }



     /**
      * IceProcessorEvent 接口定义了与 ICE（交互式连接建立）通信相关的事件处理方法。
      * 通过实现该接口，可以处理 ICE 过程中涉及的传输上下文以及 Ufrag（用户名片段）的查找。
      * <p>
      * 该接口的主要职责包括：
      * 1. 查找与指定 Ufrag 关联的传输上下文。
      * 2. 处理传输上下文事件。
      */
     public interface IceProcessorEvent {


         Optional<TransportContext> findByUfrag(String ufrag);

         void onTransportContext(TransportContext transportContext);

     }
 }
