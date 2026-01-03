package cn.duxinglan.ice.attribute;

import cn.duxinglan.ice.StunUtils;
import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;

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
public class XorMappedAddressAttribute extends Attribute {

    //分配空间： 1字节 (0x00) + 1字节 family + 2字节 port + 地址
    public static final int HEAD_LENGTH = 1 + 1 + 2;

    private int family;

    private int xPort;

    private byte[] xAddress;

    public XorMappedAddressAttribute() {
        super(AttributeType.XOR_MAPPED_ADDRESS);
    }


    /**
     * 解码给定的远程地址和事务 ID，生成一个对应的 {@code XorMappedAddressAttribute} 实例。
     *
     * @param remoteAddress 表示远程对等点的 {@code InetSocketAddress}，包含 IP 地址和端口信息。
     * @param transactionId 表示当前事务的 ID，用于解码过程中进行 XOR 运算的字节数组。
     * @return 解码后的 {@code XorMappedAddressAttribute} 对象，包含 XOR 处理后的地址和端口信息。
     * @throws IllegalArgumentException 如果传入的地址类型既不是 IPv4 也不是 IPv6。
     */
    public static XorMappedAddressAttribute decode(InetSocketAddress remoteAddress, byte[] transactionId) {
        XorMappedAddressAttribute xorMappedAddressAttribute = new XorMappedAddressAttribute();
        InetAddress address = remoteAddress.getAddress();
        int port = remoteAddress.getPort();

        byte family;
        byte[] xAddress;

        if (address instanceof Inet4Address) {
            family = 0x01;
            byte[] ipBytes = address.getAddress(); // IPv4 4字节
            xAddress = new byte[4];
            for (int i = 0; i < 4; i++) {
                xAddress[i] = (byte) (ipBytes[i] ^ ((StunUtils.STUN_COOKIE >>> (24 - i * 8)) & 0xFF));
            }
        } else if (address instanceof Inet6Address) {
            family = 0x02;
            byte[] ipBytes = address.getAddress(); // IPv6 16字节
            xAddress = new byte[16];
            // 前4字节 XOR Magic Cookie
            for (int i = 0; i < 4; i++) {
                xAddress[i] = (byte) (ipBytes[i] ^ ((StunUtils.STUN_COOKIE >>> (24 - i * 8)) & 0xFF));
            }
            // 后12字节 XOR Transaction ID
            for (int i = 4; i < 16; i++) {
                xAddress[i] = (byte) (ipBytes[i] ^ transactionId[i - 4]);
            }
        } else {
            throw new IllegalArgumentException("不支持的IP地址类型: " + address);
        }
        xorMappedAddressAttribute.setLeng(HEAD_LENGTH + xAddress.length);
        xorMappedAddressAttribute.setFamily(family);
        xorMappedAddressAttribute.setXPort(port ^ 0x2112);
        xorMappedAddressAttribute.setXAddress(xAddress);
        return xorMappedAddressAttribute;

    }


/*    @Override
    protected void onValueChange(ByteBuf value) {
        value.readByte();
        this.family = value.readByte();
        this.xPort = value.readShort();
        this.xAddress = new byte[value.readableBytes()];
        value.readBytes(this.xAddress);
    }*/

    @Override
    void write(ByteBuf byteBuf) {
        byteBuf.writeByte(0x00);
        byteBuf.writeByte(family);
        byteBuf.writeShort(xPort & 0xFFFF);
        byteBuf.writeBytes(xAddress);
    }

    @Override
    public void decodeAttribute(ByteBuf data, int offset, int length) {
        //这个数据默认有个占位 第一个字节 这里直接忽略啦
        this.family = data.getByte(offset + 1);
        this.xPort = data.getShort(offset + 2);
        this.xAddress = new byte[length - 4];
        data.getBytes(offset + 4, this.xAddress);
    }
}
