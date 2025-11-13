package com.example;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2MultiplexHandler;

public class NettyHttp2Server {

    public static void main(String[] args) throws Exception {
        int port = 80;

        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap()
                    .group(boss, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();

                            // Prior Knowledge: assume HTTP/2 without TLS or upgrade
                            Http2FrameCodecBuilder codecBuilder = Http2FrameCodecBuilder.forServer();
                            p.addLast(codecBuilder.build());
                            p.addLast(new Http2MultiplexHandler(new HelloHttp2Handler()));
                        }
                    });

            Channel ch = bootstrap.bind(port).sync().channel();
            System.out.println("HTTP/2 cleartext server started on http://localhost:" + port);
            ch.closeFuture().sync();
        } finally {
            boss.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    @Sharable
    static class HelloHttp2Handler extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            if (msg instanceof Http2HeadersFrame) {
                Http2HeadersFrame headersFrame = (Http2HeadersFrame) msg;

                if (headersFrame.isEndStream()) {
                    // Delay execution by 10 milliseconds
                    ctx.executor().schedule(() -> {
                        Http2Headers headers = new DefaultHttp2Headers();
                        headers.status("200");

                        ctx.write(new DefaultHttp2HeadersFrame(headers, false));
                        ctx.write(new DefaultHttp2DataFrame(
                                ctx.alloc().buffer().writeBytes("hello netty".getBytes()), true));
                        ctx.flush();
                        //try{
                        //Write "1" to the file /tmp/netty.
                        //Path filePath = Paths.get("/tmp/netty");
                        //Files.write(filePath, "1".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        //} catch (IOException e) {
                        //    e.printStackTrace();
                        //}
                    }, 50, java.util.concurrent.TimeUnit.MILLISECONDS);
                }
            }
        }
    }

}
