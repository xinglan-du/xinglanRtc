package cn.duxinglan.media.transport.nio.webrtc.handler.ice;

import cn.duxinglan.ice.attribute.*;
import cn.duxinglan.ice.message.StunMessage;
import cn.duxinglan.media.core.IMediaNode;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IceHandel 类用于处理与交互式连接建立 (ICE) 相关的消息与逻辑。
 * 该类主要功能包括：
 * 1. 管理本地与远程 ICE 信息。
 * 2. 解析和处理 STUN 消息，用于验证和绑定对等端。
 * 3. 校验消息完整性及指纹以确保数据安全。
 * <p>
 * 版权所有 (c) 2025 www.duxinglan.cn
 * <p>
 * 项目名称：xinglanRtc
 * <p>
 * 本文件属于 xinglanRtc 项目的一部分。
 * <p>
 * 本软件依据 XinglanRtc 非商业许可证（XNCL）授权，仅限个人非商业使用。
 * 禁止任何形式的商业用途，包括但不限于：收费安装、收费部署、
 * 收费运维、收费技术支持等行为。
 * <p>
 * 详情请参阅项目根目录下的 LICENSE 文件。
 **/


@Slf4j
public class IceHandler {

    /**
     * 一个线程安全的映射，用于存储和管理与本地 ICE 信息相关的数据。
     * <p>
     * key: 表示唯一标识符（通常是字符串，可能对应连接或会话）。
     * value: {@link LocalIceInfo} 对象，包含本地及远程的 ICE 信息。
     * <p>
     * 此变量使用 {@link ConcurrentHashMap} 实现，确保在多线程访问场景下，
     * 对映射的操作具有高效的并发安全性。
     */
    private static final Map<String, LocalIceInfo> ICE_INFO_MAP = new ConcurrentHashMap<>();

    /**
     * 用于存储处理 ICE 协商相关回调的接口实例。
     * 该对象实现了 {@link IceHandlerCallback} 接口，用来在接收到 STUN 消息时触发回调逻辑。
     * <p>
     * 功能说明：
     * 1. 回调机制：在 ICE 协商过程中，当收到 STUN 消息时，通过该接口实现具体的处理逻辑。
     * 2. 提供 STUN 消息与本地 ICE 信息的交互，增强消息处理灵活性。
     * <p>
     * 关联类说明：
     * - {@link IceHandlerCallback} 定义的接口，包含处理 STUN 消息的核心方法 {@code callback(StunMessage message, LocalIceInfo localIceInfo)}。
     * - {@link LocalIceInfo} 提供本地与远程的 ICE 信息以及 WebRTC 节点信息，用于在回调中供用户处理。
     * <p>
     * 使用场景：
     * 该变量通常用于存储用户自定义的回调实现，适配不同场景下的 ICE 信令处理需求。
     * <p>
     * 访问权限：
     * 该变量被声明为 private final，确保其在实例化后不可被修改。
     */
    private final IceHandlerCallback iceHandlerCallback;

    public IceHandler(IceHandlerCallback iceHandlerCallback) {
        this.iceHandlerCallback = iceHandlerCallback;
    }

    /**
     * 创建一个包含本地 ICE 信息的 LocalIceInfo 对象，并将其与指定的 WebRTC 节点关联。
     * 同时将该 LocalIceInfo 对象存入全局映射中，使用其 ufrag 作为键。
     *
     * @param mediaNode 用于关联的 WebRTC 节点信息
     * @return 包含本地 ICE 信息并与指定 WebRTC 节点关联的 LocalIceInfo 对象
     */
    public static LocalIceInfo craterLocalIceInfo(IMediaNode mediaNode) {
        LocalIceInfo localIceInfo = new LocalIceInfo();
        localIceInfo.setMediaNode(mediaNode);
        ICE_INFO_MAP.put(localIceInfo.getLocalIceInfo().ufrag(), localIceInfo);
        return localIceInfo;
    }

    /**
     * 处理 ICE 协议相关的网络数据。解析传入的 ByteBuf 数据并根据 STUN 消息的内容进行处理。
     * 如果消息包含有效的认证信息，则触发回调逻辑以完成进一步操作。
     *
     * @param buf           包含 STUN 消息的缓冲区
     * @param remoteAddress 消息发送方的远程地址信息
     */
    public void processIce(ByteBuf buf, InetSocketAddress remoteAddress) {
        //先将数据转换一下
        StunMessage message = StunMessage.decode(buf);
        if (message == null) {
            return;
        }
        try {
            UsernameAttribute attributes = (UsernameAttribute) message.getAttributes(AttributeType.USERNAME);
            String[] username = attributes.getUsername().split(":");
            LocalIceInfo localIceInfo = ICE_INFO_MAP.get(username[0]);
            if (localIceInfo == null) {
                return;
            }
            String password = localIceInfo.getLocalIceInfo().pwd();
            log.debug("这次stun消息使用的用户名:{};密码:{}", attributes.getUsername(), password);
            //校验数据
            if (verifyData(message, password)) {
                //这里要标志当前端口已经认证成功
                StunMessage bindingSuccessMessage = StunMessage.createBindingSuccessMessage(password, message, remoteAddress);
                PriorityAttribute priorityAttribute = (PriorityAttribute) message.getAttributes(AttributeType.PRIORITY);
                iceHandlerCallback.callback(bindingSuccessMessage, localIceInfo, priorityAttribute.getPriority());
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

    }


    /**
     * 验证指定的 STUN 消息的完整性和指纹。
     * <p>
     * 通过分别调用 checkMessageIntegrity 和 verifyFingerprint 方法，
     * 校验消息的 MESSAGE-INTEGRITY 和指纹的正确性。
     *
     * @param message  待验证的 STUN 消息对象
     * @param password 用于校验消息完整性的密码
     * @return 如果消息的完整性和指纹均验证通过，返回 true；否则返回 false
     */
    private boolean verifyData(StunMessage message, String password) {
        //校验数据完整性
        boolean messageIntegrity = checkMessageIntegrity(message, password);
        //校验指纹
        boolean verifyFingerprint = verifyFingerprint(message);
        return messageIntegrity && verifyFingerprint;
    }


    /**
     * 验证消息的完整性。通过计算消息的 HMAC 值并与消息中携带的 MESSAGE-INTEGRITY 属性值进行校验。
     *
     * @param message  待校验的 Stun 消息对象
     * @param password 用于生成 HMAC 值的密钥
     * @return 如果消息完整性校验通过，返回 true；否则返回 false
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
     * 验证消息的指纹值是否正确。
     * 指纹是通过计算消息的 CRC32 校验值，并与消息中包含的指纹属性的值进行比较来验证的。
     *
     * @return 如果指纹校验通过，返回 true；否则返回 false。
     */
    private boolean verifyFingerprint(StunMessage message) {
        long calcValue = message.calculateFingerprint(message.getByteBuf());
        FingerprintAttribute stunAttribute = (FingerprintAttribute) message.getAttributes(AttributeType.FINGERPRINT);
        return stunAttribute.getFingerprint() == calcValue;
    }


    /**
     * IceHandlerCallback 接口用于定义一个回调机制，以处理 STUN 消息和本地 ICE 信息相关的逻辑。
     * 实现此接口的类可以定义具体的回调行为，用于响应特定的网络事件。
     */
    public interface IceHandlerCallback {

        void callback(StunMessage message, LocalIceInfo localIceInfo, int priorityAttribute);
    }
}
