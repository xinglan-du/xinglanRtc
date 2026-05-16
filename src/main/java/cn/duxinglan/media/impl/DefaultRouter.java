package cn.duxinglan.media.impl;

import cn.duxinglan.media.core.stream.MediaSink;
import cn.duxinglan.media.core.stream.MediaSinkFactory;
import cn.duxinglan.media.core.stream.MediaStream;
import cn.duxinglan.media.core.stream.Router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

public class DefaultRouter implements Router {

    private final Object lock = new Object();

    private Map<String, List<MediaStream>> streams = new ConcurrentHashMap<>();

    private Map<String, MediaSinkFactory> sinkFactories = new ConcurrentHashMap<>();

    public void publish(String sessionId, MediaStream stream) {

        List<MediaStream> mediaStreams = streams.computeIfAbsent(sessionId, k -> new ArrayList<>());
        if (mediaStreams.contains(stream)) {
            return;
        }
        mediaStreams.add(stream);
        synchronized (lock) {
            for (Map.Entry<String, MediaSinkFactory> stringListEntry : sinkFactories.entrySet()) {
                if (sessionId.equals(stringListEntry.getKey())) {
                    continue;
                }
                MediaSink mediaSink = stringListEntry.getValue().createMediaSink(stream);
                stream.addSink(mediaSink);
            }

        }

    }

    @Override
    public void unpublish(String sessionId, MediaStream stream) {
        List<MediaStream> mediaStreams = streams.computeIfPresent(sessionId, (k, v) -> {
            v.remove(stream);
            return v;
        });


    }

    @Override
    public void subscribe(String sessionId, MediaSinkFactory sinkFactory) {
        sinkFactories.put(sessionId, sinkFactory);
        synchronized (lock) {
            for (Map.Entry<String, List<MediaStream>> stringListEntry : streams.entrySet()) {
                String key = stringListEntry.getKey();
                if (sessionId.equals(key)) {
                    continue;
                }

                List<MediaStream> value = stringListEntry.getValue();
                for (MediaStream mediaStream : value) {
                    MediaSink mediaSink = sinkFactory.createMediaSink(mediaStream);
                    mediaStream.addSink(mediaSink);
                }
            }

        }
    }

    @Override
    public void unSubscribe(String sessionId, MediaSink mediaSink) {
        streams.values()
                .stream()
                .flatMap((Function<List<MediaStream>, Stream<MediaStream>>) Collection::stream)
                .forEach(mediaStream -> mediaStream.removeSink(mediaSink));

    }


}
