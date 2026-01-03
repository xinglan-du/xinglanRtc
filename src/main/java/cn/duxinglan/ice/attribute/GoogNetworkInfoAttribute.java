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
public class GoogNetworkInfoAttribute extends Attribute {

    private int networkId;

    private int networkCost;

/*
    @Override
    protected void onValueChange(ByteBuf value) {
        this.networkId = value.readShort();
        this.networkCost = value.readShort();
    }
*/

    public GoogNetworkInfoAttribute() {
        super(AttributeType.GOOG_NETWORK_INFO);
    }

    @Override
    void write(ByteBuf byteBuf) {
        byteBuf.writeShort(this.networkId);
        byteBuf.writeShort(this.networkCost);
    }

    @Override
    public void decodeAttribute(ByteBuf data, int offset, int length) {
        this.networkId = data.getShort(offset);
        this.networkCost = data.getShort(offset + 2);
    }
}
