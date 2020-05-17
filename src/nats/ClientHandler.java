package nats;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import java.nio.charset.StandardCharsets;

public class ClientHandler extends ChannelInboundHandlerAdapter {
    public enum Status {
        PARSE_OPERATION,
        PARSE_PAYLOAD
    }

    private String lastMessage;
    private int payloadSize;
    private Status state;
    private ByteBuf buf;
    private MessageHandler handler;

    public ClientHandler(MessageHandler handler) {
        this.handler = handler;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        buf = ctx.alloc().buffer(4 * 1024 * 1024);
        state = Status.PARSE_OPERATION;
    }
    
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        buf.release(); // (1)
        buf = null;
    }

    /**  INFO Server Sent to client after initial TCP/IP connection
     *   MSG Server Delivers a message payload to a subscriber
     *   PING Both PING keep-alive message
     *   PONG Both
     *   PONG keep-alive response
     *   +OK Server Acknowledges well-formed protocol message in verbose mode
     *   -ERR Server Indicates a protocol error. May cause client disconnect.
     */
    private void parse() {
        // all messages end with \r\n (byte 13, byte 10)
        // the only content that could contain byte 13 in
        // it is the payload.
        // MSG are send as:
        // MSG <subject> <sid> [reply-to] <#bytes>\r\n[payload]\r\n
        int indexOfReturn = buf.indexOf(buf.readerIndex(), buf.writerIndex(), (byte) 13);
        if (indexOfReturn == -1) return;
        
        // we are looking for \r\n. don't do anything if we have only written up to
        // \r
        if (indexOfReturn >= buf.writerIndex()-1) return;
        
        byte newline = buf.getByte(indexOfReturn+1);
        if (newline != (byte) 10) {
            // todo: how do we handle errors in the protocol
            System.out.println("Fatal error!");
            return;
        }

        int messageSize = indexOfReturn -1 - buf.readerIndex();

        String message = buf.readCharSequence(messageSize, StandardCharsets.UTF_8).toString();
        // skip over \r\n
        buf.skipBytes(2);
        if (message.startsWith("MSG")) {
            //todo implement message handling
        } else {
            this.handler.onMessage(message);
        }
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf m = (ByteBuf) msg;
        buf.writeBytes(m);
        m.release();
        parse();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
