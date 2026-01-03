package cn.duxinglan.ice.message;

import cn.duxinglan.ice.attribute.Attribute;
import cn.duxinglan.ice.attribute.AttributeType;
import cn.duxinglan.ice.attribute.FingerprintAttribute;
import cn.duxinglan.ice.attribute.MessageIntegrityAttribute;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

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
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class BindingSuccessResponseStunMessage extends StunMessage {


    private String pwd;

    private int attributeIndex = 0;


    public BindingSuccessResponseStunMessage(byte[] transactionId) {
        this.type = StunMessageType.BINDING_SUCCESS_RESPONSE;
        this.length = 0;
        this.cookie = STUN_COOKIE;
        this.transactionId = transactionId;
    }

    @Override
    public void addAttribute(Attribute attribute) {
        super.addAttribute(attribute);
        attribute.setIndex(attributeIndex);
        this.length += attribute.getTotalLength();
        attributeIndex += attribute.getTotalLength();
    }

    public void generateMessageIntegrity(ByteBuf byteBuf) throws NoSuchAlgorithmException, InvalidKeyException {
        int index = 0;
        for (Map.Entry<AttributeType, Attribute> attributeTypeStunAttributeEntry : attributes.entrySet()) {
            Attribute value = attributeTypeStunAttributeEntry.getValue();
            index += value.getTotalLength() + value.getPadding();
        }

        byte[] bytes = super.calculateMessageIntegrity(pwd, byteBuf);

        MessageIntegrityAttribute messageIntegrityAttribute = new MessageIntegrityAttribute();
        messageIntegrityAttribute.setMessageIntegrity(bytes);
        messageIntegrityAttribute.setLeng(bytes.length);
        addAttribute(messageIntegrityAttribute);
        messageIntegrityAttribute.writeTo(byteBuf);
    /*    byteBuf.writeShort(attribute.getType().value);
        byteBuf.writeShort(attribute.getLeng());
        byteBuf.writeBytes(attribute.getData());
        byteBuf.writeZero(attribute.getPadding());*/
    }

    public void generateFingerprint(ByteBuf byteBuf) {
        int index = 0;
        for (Map.Entry<AttributeType, Attribute> attributeTypeStunAttributeEntry : attributes.entrySet()) {
            Attribute value = attributeTypeStunAttributeEntry.getValue();
            index += value.getTotalLength() + value.getPadding();
        }

        long l = super.calculateFingerprint(byteBuf);

        FingerprintAttribute fingerprintAttribute = new FingerprintAttribute();
        fingerprintAttribute.setFingerprint(l);
        fingerprintAttribute.setLeng(FingerprintAttribute.length);
        addAttribute(fingerprintAttribute);
       /* Attribute attribute = StunAttributeFactory.createStunAttribute(AttributeType.FINGERPRINT.value, bytes.length, StunMessage.STUN_DATA_HEADER_LENGTH, index, Unpooled.wrappedBuffer(bytes));
        addAttribute(attribute);*/
        fingerprintAttribute.writeTo(byteBuf);
     /*   byteBuf.writeShort(attribute.getType().value);
        byteBuf.writeShort(attribute.getLeng());
        byteBuf.writeBytes(attribute.getData());
        byteBuf.writeZero(attribute.getPadding());*/
    }


    @Override
    public void writeTo(ByteBuf byteBuf) {
        super.writeTo(byteBuf);
        try {
            generateMessageIntegrity(byteBuf);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error(e.getMessage(), e);
        }
        generateFingerprint(byteBuf);

    }


}
