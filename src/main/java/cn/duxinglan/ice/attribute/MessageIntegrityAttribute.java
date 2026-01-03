package cn.duxinglan.ice.attribute;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.EqualsAndHashCode;

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
public class MessageIntegrityAttribute extends Attribute {

    private byte[] messageIntegrity = new byte[20];


    public MessageIntegrityAttribute() {
        super(AttributeType.MESSAGE_INTEGRITY);
    }

    /*   @Override
        protected void onValueChange(ByteBuf value) {
            value.readBytes(this.messageIntegrity);
        }
    */
    @Override
    void write(ByteBuf byteBuf) {
        byteBuf.writeBytes(this.messageIntegrity);
    }

    @Override
    public void decodeAttribute(ByteBuf data, int offset, int length) {
        data.getBytes(offset, messageIntegrity);
    }
}
