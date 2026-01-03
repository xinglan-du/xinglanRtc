package cn.duxinglan.ice.attribute;

import io.netty.buffer.ByteBuf;
import lombok.Data;
import lombok.NoArgsConstructor;

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
@NoArgsConstructor
@Data
public abstract class Attribute {


    public static final int HEAD_LENGTH = 4;


    /**
     * 表示当前 STUN 扩展的类型。
     * 该属性用于标识具体的 STUN 功能，例如用户名验证、优先级设置等。
     * 类型定义为 {@link AttributeType} 枚举。
     */
    private AttributeType type;

    /**
     * 表示 STUN 扩展的长度信息。该属性存储当前扩展的原始数据长度，
     * 此长度可能用于协议解析和校验操作。
     */
    private int leng;

    /**
     * 表示STUN扩展在数据结构中的索引位置。
     * 该字段主要用于标识或访问特定的STUN扩展实例在原始数据中的位置，
     * 以便在处理和解析过程中快速定位对应字段的起始位置。
     */
    private int index;

    /**
     * 表示 STUN 扩展中的填充数据长度。
     * 该字段通常用于协议的对齐要求，将数据长度补齐至特定的倍数以满足网络传输的约束。
     * 填充数据本身通常未包含有效信息，仅作为占位符存在。
     */
    private int padding;

    public Attribute(AttributeType type) {
        this.type = type;
    }

    /**
     * 设置当前 STUN 扩展的长度，并计算所需的填充字节数。
     * 填充字节数根据协议对齐要求计算，使数据长度对齐至 4 的倍数。
     *
     * @param leng 表示当前 STUN 扩展的实际长度，单位为字节。
     */
    public void setLeng(int leng) {
        this.leng = leng;
        this.padding = (4 - (leng % 4)) % 4;
    }

    /**
     * 获取当前 STUN 扩展的总长度。
     * 总长度由 STUN 扩展的头部长度和扩展数据长度之和组成。
     *
     * @return 当前 STUN 扩展的总长度，单位为字节。
     */
    public int getTotalLength() {
        return HEAD_LENGTH + leng + this.padding;
    }

    public void writeTo(ByteBuf byteBuf) {
        byteBuf.writeShort(type.value);
        byteBuf.writeShort(leng);
        write(byteBuf);
        byteBuf.writeZero(padding);
    }

    abstract void write(ByteBuf byteBuf);


    public abstract void decodeAttribute(ByteBuf data, int offset, int length);

}
