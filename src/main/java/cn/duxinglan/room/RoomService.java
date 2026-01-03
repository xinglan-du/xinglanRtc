package cn.duxinglan.room;

import cn.duxinglan.media.core.IConsumer;
import cn.duxinglan.media.core.IMediaNode;
import cn.duxinglan.media.core.IProducer;
import cn.duxinglan.media.impl.webrtc.GlobalIProducerMediaRouter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


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
public class RoomService {

    private static volatile RoomService instance;

    private List<IMediaNode> mediaNodeList = new ArrayList<>();


    public static RoomService getInstance() {
        if (instance == null) {
            synchronized (RoomService.class) {
                if (instance == null) {
                    instance = new RoomService();
                }
            }
        }
        return instance;
    }


    public void addMediaNode(IMediaNode newMediaNode) {
        mediaNodeList.add(newMediaNode);
        GlobalIProducerMediaRouter globalMediaRouter = newMediaNode.getGlobalMediaRouter();
        globalMediaRouter.setMediaNodeMediaProducerListener(new GlobalIProducerMediaRouter.IMediaNodeMediaProducerListener() {
            @Override
            public void onAddMediaProducer(IMediaNode currentMediaNode, GlobalIProducerMediaRouter globalMediaRouter, IProducer producer) {
                for (IMediaNode mediaNode : mediaNodeList) {
                    if (mediaNode == currentMediaNode) {
                        continue;
                    }
                    IConsumer consumer = mediaNode.createConsumer(producer);
                    globalMediaRouter.addConsumer(producer, consumer);
                    mediaNode.updateOfferInfo();
                }
            }
        });

        for (IMediaNode mediaNode : mediaNodeList) {
            if (mediaNode == newMediaNode) {
                continue;
            }
            globalMediaRouter = mediaNode.getGlobalMediaRouter();
            Set<IProducer> producerList = globalMediaRouter.getProducers();
            for (IProducer producer : producerList) {
                IConsumer consumer = newMediaNode.createConsumer(producer);
                globalMediaRouter.addConsumer(producer, consumer);
            }
        }
    }

    public void removeMediaNode(IMediaNode removeMediaNode) {
        if (mediaNodeList.remove(removeMediaNode)) {
            GlobalIProducerMediaRouter globalMediaRouter = removeMediaNode.getGlobalMediaRouter();
            List<IConsumer> consumerList = globalMediaRouter.getConsumer();
            for (IConsumer consumer : consumerList) {
                for (IMediaNode mediaNode : mediaNodeList) {
                    boolean b = mediaNode.removeConsumer(consumer);
                    if (b) {
                        mediaNode.updateOfferInfo();
                    }
                }
            }

        }

    }

}
