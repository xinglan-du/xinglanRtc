package cn.duxinglan.ice.message;

import cn.duxinglan.ice.StunAttributeFactory;
import cn.duxinglan.ice.StunUtils;
import cn.duxinglan.ice.attribute.*;
import cn.duxinglan.media.core.INetworkPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.CRC32;

/**
 *
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
public abstract class StunMessage implements INetworkPacket {

    /**
     * 定义了 STUN 数据报文的头部长度。
     * <p>
     * 在 STUN 协议中，数据报文的头部固定长度为 20 字节，其中包含以下字段：
     * - 消息类型：2 字节，用于标识消息的类型，例如绑定请求或响应。
     * - 消息长度：2 字节，表示消息体（不包含头部）的长度。
     * - 魔法字（Magic Cookie）：4 字节，固定值，用于验证 STUN 消息的来源和一致性。
     * - 事务 ID（Transaction ID）：12 字节，用于唯一标识每个 STUN 消息，确保消息不会被重复或混淆。
     * <p>
     * 该常量用于明确 STUN 消息头部在编码、解码或内存对齐中的长度要求。
     */
    public static final int STUN_DATA_HEADER_LENGTH = 20;

    /**
     * 表示 STUN 协议中消息完整性校验（Message Integrity）的固定长度。
     * <p>
     * MESSAGE_INTEGRITY 属性用于验证消息的完整性，确保消息在传输过程中未被篡改。
     * 它的固定长度为 24 字节，依据 STUN 协议规范，为 HMAC-SHA1 签名值。
     * <p>
     * 该常量通常用于计算和校验 STUN 消息的 MESSAGE_INTEGRITY 属性。
     */
    public static final int MESSAGE_INTEGRITY_LENGTH = 24;

    public static final int STUN_COOKIE = 0x2112A442;

    public static final int FINGERPRINT_LENGTH = 8;

    public static final int FINGERPRINT_XOR = 0x5354554e;

    public static final byte TRANSACTION_ID_LENGTH = 12;


    /**
     * 原始文件，用于对数据进行校验使用
     */
    @Getter
    protected ByteBuf byteBuf;

    /**
     * 数据类型
     */
    @Getter
    @Setter
    protected StunMessageType type;


    /**
     * 数据长度，不包含20字节的数据头
     */
    @Getter
    @Setter
    protected int length;

    /**
     * stun固定值：0x2112A442
     */
    @Getter
    @Setter
    protected int cookie;

    /**
     * rfc3489的id,12字节
     */
    @Getter
    @Setter
    protected byte[] transactionId;

    /**
     * 表示当前 STUN 消息的属性集合。
     * 该变量是一个 Map 结构，键为 {@link AttributeType} 表示的属性类型，
     * 值为对应的 {@link Attribute} 实例，表示特定类型的 STUN 扩展属性。
     * 使用 {@link LinkedHashMap} 实现的 Map 确保了插入顺序的保留。
     * <p>
     * 属性集合主要用于存储和管理 STUN 消息中包含的所有扩展字段，例如身份认证、优先级等信息。
     * 可以通过键值对方式快速访问特定类型的属性，同时便于扩展和迭代操作。
     */
    protected Map<AttributeType, Attribute> attributes = new LinkedHashMap<>();


    public StunMessage() {
    }



    /*public StunMessage(ByteBuf byteBuf) {
        byte[] raw = new byte[byteBuf.readableBytes()];
        byteBuf.getBytes(byteBuf.readerIndex(), raw);
        this.byteBuf = Unpooled.wrappedBuffer(raw);
        this.byteBuf.markReaderIndex();
        this.type = StunMessageType.fromValue(this.byteBuf.readUnsignedShort());
        this.length = this.byteBuf.readUnsignedShort();
        this.cookie = this.byteBuf.readInt();
//        this.transactionId = Unpooled.buffer(12);
        this.byteBuf.readBytes(this.transactionId);
        int index = 0;
        ByteBuf attributeBytebuf = this.byteBuf.readBytes(length);
        while (attributeBytebuf.readableBytes() > 0) {
            int attributeType = attributeBytebuf.readUnsignedShort();
            int attributeLength = attributeBytebuf.readUnsignedShort();
            ByteBuf valuesBuf = Unpooled.buffer(attributeLength);  // 创建堆内存 ByteBuf
            attributeBytebuf.readBytes(valuesBuf);
            Attribute attribute = StunAttributeFactory.createStunAttribute(attributeType, attributeLength,StunMessage.STUN_DATA_HEADER_LENGTH, index, valuesBuf);
            addAttribute(attribute);
            int padding = attribute.getPadding();
            attributeBytebuf.skipBytes(padding);
            index += 4 + attributeLength + padding;
        }
        this.byteBuf.resetReaderIndex();


    }*/

    public void addAttribute(Attribute attribute) {
        if (attribute.getType() == AttributeType.NONE) {
            return;
        }
        attributes.put(attribute.getType(), attribute);
    }

    public Attribute getAttributes(AttributeType attributeType) {
        return attributes.get(attributeType);
    }


    /**
     * 计算消息完整性校验值 (Message Integrity) 的 HMAC-SHA1 签名。
     * 该方法通过给定的密码和消息内容，生成用于 STUN 消息中 MESSAGE_INTEGRITY 属性的校验值。
     *
     * @param pwd     用于生成校验值的共享密钥（通常为 STUN 认证中的密码）。
     * @param byteBuf 包含 STUN 消息内容的 ByteBuf 对象，消息内容用于计算 HMAC 校验。
     * @return 返回计算出的 HMAC-SHA1 校验值字节数组。
     * @throws NoSuchAlgorithmException 当指定的算法 "HmacSHA1" 不被支持时抛出。
     * @throws InvalidKeyException      当初始化 HMAC 时提供的密钥无效时抛出。
     */
    public byte[] calculateMessageIntegrity(String pwd, ByteBuf byteBuf) throws NoSuchAlgorithmException, InvalidKeyException {
        //获取messageIntegrity属性在attribute包的位置
        int expandLength = getAttributeIndex(AttributeType.MESSAGE_INTEGRITY);
        //计算从数据头开始到messageIntegrity的位置
        int length = expandLength + STUN_DATA_HEADER_LENGTH;

        //这里是计算从数据头到messageIntegrity一共有多少长度，这部分要注意 需要提前将长度加上去 但是不要参与计算
        int miLength = expandLength + MESSAGE_INTEGRITY_LENGTH;
        byte[] raw = new byte[length];
        byteBuf.getBytes(0, raw);
        log.debug("参与计算的原始数据:{}", ByteBufUtil.hexDump(raw));
        raw[2] = (byte) ((miLength) >> 8);
        raw[3] = (byte) (miLength);

        log.debug("参与计算修改长度数据:{}", ByteBufUtil.hexDump(raw));

        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec keySpec = new SecretKeySpec(pwd.getBytes(StandardCharsets.UTF_8), "HmacSHA1");
        mac.init(keySpec);
        return mac.doFinal(raw);
    }

    /**
     * 计算 STUN 消息的 FINGERPRINT 属性值。
     * 使用 CRC32 校验算法对消息内容进行校验计算，并结合特定的 STUN XOR 常量，生成校验值。
     *
     * @param byteBuf 包含 STUN 消息内容的 ByteBuf 对象，用于计算 FINGERPRINT 校验值。
     * @return 返回计算出的 FINGERPRINT 属性值，类型为 long。
     */
    public long calculateFingerprint(ByteBuf byteBuf) {

        int expandLength = getAttributeIndex(AttributeType.FINGERPRINT);
        CRC32 crc32 = new CRC32();
        ByteBuffer buf = byteBuf.nioBuffer(0, expandLength + STUN_DATA_HEADER_LENGTH);
        crc32.update(buf);
        // 转换成 4 字节大端序
        return crc32.getValue() ^ StunUtils.FINGERPRINT_XOR;
    }

    /**
     * 根据指定的 {@link AttributeType} 计算该类型在 STUN 消息中的索引位置。
     * 若指定类型的扩展不存在，则返回当前消息中所有已存在扩展的长度总和。
     *
     * @param attributeType 指定的扩展类型，用于计算其在消息中的索引位置。
     * @return 目标扩展的索引位置，或所有扩展总长度（包括对齐填充）之和。
     */
    private int getAttributeIndex(AttributeType attributeType) {
        int expandLength = 0;
        Attribute attribute = attributes.get(attributeType);
        if (attribute == null) {
            for (Map.Entry<AttributeType, Attribute> attributeTypeStunAttributeEntry : attributes.entrySet()) {
                Attribute value = attributeTypeStunAttributeEntry.getValue();
                expandLength += 4 + value.getLeng() + value.getPadding();
            }
        } else {
            expandLength = attribute.getIndex();
        }
        return expandLength;
    }


    public int getTotalLength() {
        return STUN_DATA_HEADER_LENGTH + length + MESSAGE_INTEGRITY_LENGTH + FINGERPRINT_LENGTH;
    }

    public void writeTo(ByteBuf byteBuf) {
        byteBuf.writeShort(this.type.value);
        byteBuf.writeShort(this.length + MESSAGE_INTEGRITY_LENGTH + FINGERPRINT_LENGTH);
        byteBuf.writeInt(this.cookie);
        byteBuf.writeBytes(this.transactionId);
        for (Map.Entry<AttributeType, Attribute> attributeTypeStunAttributeEntry : attributes.entrySet()) {
            Attribute value = attributeTypeStunAttributeEntry.getValue();
            value.writeTo(byteBuf);

        }
    }



    public static StunMessage createBindingSuccessMessage(
            String pwd,
            StunMessage requestMessage,
            InetSocketAddress remoteAddress
    ) {
        BindingSuccessResponseStunMessage stunMessage = new BindingSuccessResponseStunMessage(requestMessage.getTransactionId());
        stunMessage.setPwd(pwd);

        XorMappedAddressAttribute xorMappedAddressAttribute = XorMappedAddressAttribute.decode(remoteAddress, requestMessage.getTransactionId());
        stunMessage.addAttribute(xorMappedAddressAttribute);
        UsernameAttribute attributes = (UsernameAttribute) requestMessage.getAttributes(AttributeType.USERNAME);
        UsernameAttribute usernameAttribute = UsernameAttribute.decode(attributes.getUsername());
        stunMessage.addAttribute(usernameAttribute);

        SoftwareAttribute softwareAttribute = SoftwareAttribute.decode("xinglan.cn");
        stunMessage.addAttribute(softwareAttribute);


        return stunMessage;
    }

    /**
     * 解码给定的 ByteBuf 数据为 STUN 消息对象。
     *
     * @param byteBuf 包含 STUN 消息数据的 ByteBuf 对象。
     * @return 解码后的 STUN 消息对象。如果无法解码或消息类型无效，则返回 null。
     */
    public static StunMessage decode(ByteBuf byteBuf) {
        //读取类型
        StunMessageType stunMessageType = StunMessageType.fromValue(byteBuf.getUnsignedShort(0));
        StunMessage stunMessage;
        switch (stunMessageType) {
            case BINDING_REQUEST -> {
                stunMessage = new BindingRequestStunMessage();
            }
            case null, default -> {
                return null;
            }
        }
        //设置类型
        stunMessage.setType(stunMessageType);
        //保留原始数据
        stunMessage.setOriginalData(byteBuf);

        byteBuf.markReaderIndex();

        //消耗掉type类型
        byteBuf.readUnsignedShort();

        //读取长度
        stunMessage.setLength(byteBuf.readUnsignedShort());

        //读取cookie
        stunMessage.setCookie(byteBuf.readInt());

        //读取transactionId
        byte[] transactionId = new byte[TRANSACTION_ID_LENGTH];
        byteBuf.readBytes(transactionId);
        stunMessage.setTransactionId(transactionId);

        //读取attributeType
        int index = 0;
        while (byteBuf.readableBytes() > 0) {
            int attributeType = byteBuf.readUnsignedShort();
            int attributeLength = byteBuf.readUnsignedShort();
            Attribute attribute = StunAttributeFactory.createStunAttribute(attributeType, attributeLength, StunMessage.STUN_DATA_HEADER_LENGTH, index, byteBuf);
            stunMessage.addAttribute(attribute);
            int padding = attribute.getPadding();
            byteBuf.skipBytes(padding);
            index += 4 + attributeLength + padding;
        }


        byteBuf.resetReaderIndex();
        return stunMessage;
    }

    /**
     * 保留原始数据
     *
     * @param byteBuf
     */
    private void setOriginalData(ByteBuf byteBuf) {
        this.byteBuf = byteBuf;
    }


}
