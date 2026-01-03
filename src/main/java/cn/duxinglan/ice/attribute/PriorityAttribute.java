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
public class PriorityAttribute extends Attribute {

    /**
     * 表示当前属性的优先级值。
     * 优先级用于在网络通信过程中对不同候选项进行排序和选择。
     * 其值通常为一个整数，用于标识候选项的优先级高低。
     * 数值越高，优先级越高。
     */
    private int priority;

    public PriorityAttribute() {
        super(AttributeType.PRIORITY);
    }

    /*   @Override
        protected void onValueChange(ByteBuf value) {
            this.priority = value.readInt();
        }
    */
    @Override
    void write(ByteBuf byteBuf) {
        byteBuf.writeInt(this.priority);
    }

    @Override
    public void decodeAttribute(ByteBuf data, int offset, int length) {
        this.priority = data.getInt(offset);
    }
}
