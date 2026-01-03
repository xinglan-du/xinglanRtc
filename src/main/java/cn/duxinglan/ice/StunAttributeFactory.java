package cn.duxinglan.ice;

import cn.duxinglan.ice.attribute.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import lombok.extern.slf4j.Slf4j;

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
public class StunAttributeFactory {

    public static Attribute createStunAttribute(int type, int length, int offest, int index, ByteBuf byteBuf) {
        AttributeType attributeType = AttributeType.fromValue(type);
        Attribute attribute = switch (attributeType) {
            case USERNAME -> new UsernameAttribute();
            case GOOG_NETWORK_INFO -> new GoogNetworkInfoAttribute();
            case ICE_CONTROLLING -> new IceControllingAttribute();
            case PRIORITY -> new PriorityAttribute();
            case MESSAGE_INTEGRITY -> new MessageIntegrityAttribute();
            case FINGERPRINT -> new FingerprintAttribute();
            case XOR_MAPPED_ADDRESS -> new XorMappedAddressAttribute();
            case SOFTWARE -> new SoftwareAttribute();
            case USE_CANDIDATE -> new UseCandidate();
            case NONE -> {
                log.info("未识别的ice扩展：0x{}", ByteBufUtil.hexDump(new byte[]{(byte) (type >> 8), (byte) type}));
                yield new DefaultAttribute();
            }

        };

        attribute.setType(attributeType);
        attribute.setLeng(length);
        attribute.setIndex(index);
        attribute.decodeAttribute(byteBuf, offest + Attribute.HEAD_LENGTH + index, length);
        //这里跳过一下数据，因为在内部转换的时候 我没有使用修改readIndex的方式处理
        byteBuf.skipBytes(length);

        return attribute;
    }
}
