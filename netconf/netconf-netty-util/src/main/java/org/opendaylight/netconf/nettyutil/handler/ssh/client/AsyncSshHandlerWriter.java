/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.netconf.nettyutil.handler.ssh.client;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.opendaylight.netconf.shaded.sshd.common.io.IoOutputStream;
import org.opendaylight.netconf.shaded.sshd.common.io.WritePendingException;
import org.opendaylight.netconf.shaded.sshd.common.util.buffer.Buffer;
import org.opendaylight.netconf.shaded.sshd.common.util.buffer.ByteArrayBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Async Ssh writer. Takes messages(byte arrays) and sends them encrypted to remote server.
 * Also handles pending writes by caching requests until pending state is over.
 */
public final class AsyncSshHandlerWriter implements AutoCloseable {

    private static final Logger LOG = LoggerFactory
            .getLogger(AsyncSshHandlerWriter.class);

    private static final Pattern NON_ASCII = Pattern.compile("([^\\x20-\\x7E\\x0D\\x0A])+");

    // public static final int MAX_PENDING_WRITES = 1000;
    // TODO implement Limiting mechanism for pending writes
    // But there is a possible issue with limiting:
    // 1. What to do when queue is full ? Immediate Fail for every request ?
    // 2. At this level we might be dealing with Chunks of messages(not whole messages)
    // and unexpected behavior might occur when we send/queue 1 chunk and fail the other chunks

    private final Object asyncInLock = new Object();
    private volatile IoOutputStream asyncIn;

    // Order has to be preserved for queued writes
    private final Deque<PendingWriteRequest> pending = new LinkedList<>();

    public AsyncSshHandlerWriter(final IoOutputStream asyncIn) {
        this.asyncIn = asyncIn;
    }

    @GuardedBy("asyncInLock")
    private boolean isWriteExecuted = false;

    public void write(final ChannelHandlerContext ctx,
            final Object msg, final ChannelPromise promise) {
        if (asyncIn == null) {
            promise.setFailure(new IllegalStateException("Channel closed"));
            return;
        }
        // synchronized block due to deadlock that happens on ssh window resize
        // writes and pending writes would lock the underlyinch channel session
        // window resize write would try to write the message on an already locked channelSession
        // while the pending write was in progress from the write callback
        synchronized (asyncInLock) {
            // TODO check for isClosed, isClosing might be performed by mina SSH internally and is not required here
            // If we are closed/closing, set immediate fail
            if (asyncIn.isClosed() || asyncIn.isClosing()) {
                promise.setFailure(new IllegalStateException("Channel closed"));
            } else {
                final ByteBuf byteBufMsg = (ByteBuf) msg;
                if (isWriteExecuted) {
                    queueRequest(ctx, byteBufMsg, promise);
                    return;
                }

                writeWithPendingDetection(ctx, promise, byteBufMsg, false);
            }
        }
    }

    //sending message with pending
    //if resending message not succesfull, then attribute wasPending is true
    private void writeWithPendingDetection(final ChannelHandlerContext ctx, final ChannelPromise promise,
                                           final ByteBuf byteBufMsg, final boolean wasPending) {
        try {

            if (LOG.isTraceEnabled()) {
                LOG.trace("Writing request on channel: {}, message: {}", ctx.channel(), byteBufToString(byteBufMsg));
            }

            isWriteExecuted = true;

            asyncIn.writeBuffer(toBuffer(byteBufMsg)).addListener(future -> {
                // synchronized block due to deadlock that happens on ssh window resize
                // writes and pending writes would lock the underlyinch channel session
                // window resize write would try to write the message on an already locked channelSession,
                // while the pending write was in progress from the write callback
                synchronized (asyncInLock) {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace(
                            "Ssh write request finished on channel: {} with result: {}: and ex:{}, message: {}",
                            ctx.channel(), future.isWritten(), future.getException(), byteBufToString(byteBufMsg));
                    }

                    // Notify success or failure
                    if (future.isWritten()) {
                        promise.setSuccess();
                    } else {
                        LOG.warn("Ssh write request failed on channel: {} for message: {}", ctx.channel(),
                                byteBufToString(byteBufMsg), future.getException());
                        promise.setFailure(future.getException());
                    }

                    //rescheduling message from queue after successfully sent
                    if (wasPending) {
                        byteBufMsg.resetReaderIndex();
                        pending.remove();
                    }

                    // Not needed anymore, release
                    byteBufMsg.release();
                }

                // Check pending queue and schedule next
                // At this time we are guaranteed that we are not in pending state anymore
                // so the next request should succeed
                writePendingIfAny();
            });

        } catch (final IOException | WritePendingException e) {
            if (!wasPending) {
                queueRequest(ctx, byteBufMsg, promise);
            }
        }
    }

    private void writePendingIfAny() {
        synchronized (asyncInLock) {
            if (pending.peek() == null) {
                isWriteExecuted = false;
                return;
            }

            final PendingWriteRequest pendingWrite = pending.peek();
            final ByteBuf msg = pendingWrite.msg;
            if (LOG.isTraceEnabled()) {
                LOG.trace("Writing pending request on channel: {}, message: {}",
                        pendingWrite.ctx.channel(), byteBufToString(msg));
            }

            writeWithPendingDetection(pendingWrite.ctx, pendingWrite.promise, msg, true);
        }
    }

    public static String byteBufToString(final ByteBuf msg) {
        final String message = msg.toString(StandardCharsets.UTF_8);
        msg.resetReaderIndex();
        Matcher matcher = NON_ASCII.matcher(message);
        return matcher.replaceAll((data) -> {
            StringBuilder buf = new StringBuilder();
            buf.append("\"");
            for (byte b : data.group().getBytes(StandardCharsets.US_ASCII)) {
                buf.append(String.format("%02X", b));
            }
            buf.append("\"");
            return buf.toString();
        });
    }

    private void queueRequest(final ChannelHandlerContext ctx, final ByteBuf msg, final ChannelPromise promise) {
//        try {
        LOG.debug("Write pending on channel: {}, queueing, current queue size: {}", ctx.channel(), pending.size());
        if (LOG.isTraceEnabled()) {
            LOG.trace("Queueing request due to pending: {}", byteBufToString(msg));
        }
        new PendingWriteRequest(ctx, msg, promise).pend(pending);
//        } catch (final Exception ex) {
//            LOG.warn("Unable to queue write request on channel: {}. Setting fail for the request: {}",
//                    ctx.channel(), ex, byteBufToString(msg));
//            msg.release();
//            promise.setFailure(ex);
//        }
    }

    @Override
    public void close() {
        asyncIn = null;
    }

    private static Buffer toBuffer(final ByteBuf msg) {
        // TODO Buffer vs ByteBuf translate, Can we handle that better ?
        msg.resetReaderIndex();
        final byte[] temp = new byte[msg.readableBytes()];
        msg.readBytes(temp, 0, msg.readableBytes());
        return new ByteArrayBuffer(temp);
    }

    private static final class PendingWriteRequest {
        private final ChannelHandlerContext ctx;
        private final ByteBuf msg;
        private final ChannelPromise promise;

        PendingWriteRequest(final ChannelHandlerContext ctx, final ByteBuf msg, final ChannelPromise promise) {
            this.ctx = ctx;
            // Reset reader index, last write (failed) attempt moved index to the end
            msg.resetReaderIndex();
            this.msg = msg;
            this.promise = promise;
        }

        public void pend(final Queue<PendingWriteRequest> pending) {
            // Preconditions.checkState(pending.size() < MAX_PENDING_WRITES,
            // "Too much pending writes(%s) on channel: %s, remote window is not getting read or is too small",
            // pending.size(), ctx.channel());
            Preconditions.checkState(pending.offer(this),
                "Cannot pend another request write (pending count: %s) on channel: %s", pending.size(), ctx.channel());
        }
    }
}
