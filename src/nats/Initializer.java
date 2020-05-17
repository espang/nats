package nats;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelHandler;
import io.netty.channel.socket.SocketChannel;

public class Initializer extends ChannelInitializer<SocketChannel> {
    private ChannelHandler handler;

    public Initializer(ChannelHandler handler) {
        this.handler = handler;
    }
    
    @Override
    public void initChannel(SocketChannel channel) {
         channel.pipeline().addLast("myHandler", this.handler);
    }
 }
