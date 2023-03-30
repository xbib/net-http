package org.xbib.net.http.server.netty.buffer;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.IntPredicate;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.ByteBufUtil;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xbib.net.buffer.DataBuffer;
import org.xbib.net.buffer.PooledDataBuffer;

/**
 * Implementation of the {@code DataBuffer} interface that wraps a Netty
 * {@link ByteBuf}. Typically constructed with {@link NettyDataBufferFactory}.
 */
public class NettyDataBuffer implements PooledDataBuffer {

	private static final Logger logger = Logger.getLogger(NettyDataBuffer.class.getName());

	private final ByteBuf byteBuf;

	private final NettyDataBufferFactory dataBufferFactory;

	/**
	 * Create a new {@code NettyDataBuffer} based on the given {@code ByteBuff}.
	 * @param byteBuf the buffer to base this buffer on
	 */
	NettyDataBuffer(ByteBuf byteBuf, NettyDataBufferFactory dataBufferFactory) {
		Objects.requireNonNull(byteBuf, "ByteBuf must not be null");
		Objects.requireNonNull(dataBufferFactory, "NettyDataBufferFactory must not be null");
		this.byteBuf = byteBuf;
		this.dataBufferFactory = dataBufferFactory;
	}

	/**
	 * Directly exposes the native {@code ByteBuf} that this buffer is based on.
	 * @return the wrapped byte buffer
	 */
	public ByteBuf getNativeBuffer() {
		return byteBuf;
	}

	@Override
	public NettyDataBufferFactory factory() {
		return dataBufferFactory;
	}

	@Override
	public int indexOf(IntPredicate predicate, int fromIndex) {
		Objects.requireNonNull(predicate, "IntPredicate must not be null");
		if (fromIndex < 0) {
			fromIndex = 0;
		}
		else if (fromIndex >= byteBuf.writerIndex()) {
			return -1;
		}
		int length = byteBuf.writerIndex() - fromIndex;
		return byteBuf.forEachByte(fromIndex, length, predicate.negate()::test);
	}

	@Override
	public int lastIndexOf(IntPredicate predicate, int fromIndex) {
		Objects.requireNonNull(predicate, "IntPredicate must not be null");
		if (fromIndex < 0) {
			return -1;
		}
		fromIndex = Math.min(fromIndex, byteBuf.writerIndex() - 1);
		return byteBuf.forEachByteDesc(0, fromIndex + 1, predicate.negate()::test);
	}

	@Override
	public int readableByteCount() {
		return byteBuf.readableBytes();
	}

	@Override
	public int writableByteCount() {
		return byteBuf.writableBytes();
	}

	@Override
	public int readPosition() {
		return byteBuf.readerIndex();
	}

	@Override
	public NettyDataBuffer readPosition(int readPosition) {
		byteBuf.readerIndex(readPosition);
		return this;
	}

	@Override
	public int writePosition() {
		return byteBuf.writerIndex();
	}

	@Override
	public NettyDataBuffer writePosition(int writePosition) {
		byteBuf.writerIndex(writePosition);
		return this;
	}

	@Override
	public byte getByte(int index) {
		return byteBuf.getByte(index);
	}

	@Override
	public int capacity() {
		return byteBuf.capacity();
	}

	@Override
	public NettyDataBuffer capacity(int capacity) {
		byteBuf.capacity(capacity);
		return this;
	}

	@Override
	public DataBuffer ensureCapacity(int capacity) {
		byteBuf.ensureWritable(capacity);
		return this;
	}

	@Override
	public byte read() {
		return byteBuf.readByte();
	}

	@Override
	public NettyDataBuffer read(byte[] destination) {
		byteBuf.readBytes(destination);
		return this;
	}

	@Override
	public NettyDataBuffer read(byte[] destination, int offset, int length) {
		byteBuf.readBytes(destination, offset, length);
		return this;
	}

	@Override
	public NettyDataBuffer write(byte b) {
		byteBuf.writeByte(b);
		return this;
	}

	@Override
	public NettyDataBuffer write(byte[] source) {
		byteBuf.writeBytes(source);
		return this;
	}

	@Override
	public NettyDataBuffer write(byte[] source, int offset, int length) {
		byteBuf.writeBytes(source, offset, length);
		return this;
	}

	@Override
	public NettyDataBuffer write(DataBuffer... buffers) {
		if (buffers != null && buffers.length > 0) {
			if (hasNettyDataBuffers(buffers)) {
				ByteBuf[] nativeBuffers = new ByteBuf[buffers.length];
				for (int i = 0; i < buffers.length; i++) {
					nativeBuffers[i] = ((NettyDataBuffer) buffers[i]).getNativeBuffer();
				}
				write(nativeBuffers);
			}
			else {
				ByteBuffer[] byteBuffers = new ByteBuffer[buffers.length];
				for (int i = 0; i < buffers.length; i++) {
					byteBuffers[i] = buffers[i].asByteBuffer();
				}
				write(byteBuffers);
			}
		}
		return this;
	}

	private static boolean hasNettyDataBuffers(DataBuffer[] buffers) {
		for (DataBuffer buffer : buffers) {
			if (!(buffer instanceof NettyDataBuffer)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public NettyDataBuffer write(ByteBuffer... buffers) {
		if (buffers != null) {
			for (ByteBuffer buffer : buffers) {
				byteBuf.writeBytes(buffer);
			}
		}
		return this;
	}

	/**
	 * Writes one or more Netty {@link ByteBuf ByteBufs} to this buffer,
	 * starting at the current writing position.
	 * @param byteBufs the buffers to write into this buffer
	 * @return this buffer
	 */
	public NettyDataBuffer write(ByteBuf... byteBufs) {
		if (byteBufs != null) {
			for (ByteBuf byteBuf : byteBufs) {
				byteBuf.writeBytes(byteBuf);
			}
		}
		return this;
	}

	@Override
	public DataBuffer write(CharSequence charSequence, Charset charset) {
		Objects.requireNonNull(charSequence, "CharSequence must not be null");
		Objects.requireNonNull(charset, "Charset must not be null");
		if (StandardCharsets.UTF_8.equals(charset)) {
			ByteBufUtil.writeUtf8(byteBuf, charSequence);
		}
		else if (StandardCharsets.US_ASCII.equals(charset)) {
			ByteBufUtil.writeAscii(byteBuf, charSequence);
		}
		else {
			return PooledDataBuffer.super.write(charSequence, charset);
		}
		return this;
	}

	@Override
	public NettyDataBuffer slice(int index, int length) {
		ByteBuf slice = byteBuf.slice(index, length);
		return new NettyDataBuffer(slice, dataBufferFactory);
	}

	@Override
	public NettyDataBuffer retainedSlice(int index, int length) {
		ByteBuf slice = byteBuf.retainedSlice(index, length);
		return new NettyDataBuffer(slice, dataBufferFactory);
	}

	@Override
	public ByteBuffer asByteBuffer() {
		return byteBuf.nioBuffer();
	}

	@Override
	public ByteBuffer asByteBuffer(int index, int length) {
		return byteBuf.nioBuffer(index, length);
	}

	@Override
	public InputStream asInputStream() {
		return new ByteBufInputStream(byteBuf);
	}

	@Override
	public InputStream asInputStream(boolean releaseOnClose) {
		return new ByteBufInputStream(byteBuf, releaseOnClose);
	}

	@Override
	public OutputStream asOutputStream() {
		return new ByteBufOutputStream(byteBuf);
	}

	@Override
	public String toString(Charset charset) {
		Objects.requireNonNull(charset, "Charset must not be null");
		return byteBuf.toString(charset);
	}

	@Override
	public String toString(int index, int length, Charset charset) {
		Objects.requireNonNull(charset, "Charset must not be null");
		return byteBuf.toString(index, length, charset);
	}

	@Override
	public boolean isAllocated() {
		return byteBuf.refCnt() > 0;
	}

	@Override
	public PooledDataBuffer retain() {
		return new NettyDataBuffer(byteBuf.retain(), dataBufferFactory);
	}

	@Override
	public PooledDataBuffer touch(Object hint) {
		byteBuf.touch(hint);
		return this;
	}

	@Override
	public void release() {
		boolean deallocated = byteBuf.release();
		logger.log(Level.FINEST, "released " + byteBuf + " deallocated = " + deallocated);
	}

	@Override
	public boolean equals(Object other) {
		return (this == other || (other instanceof NettyDataBuffer &&
				byteBuf.equals(((NettyDataBuffer) other).byteBuf)));
	}

	@Override
	public int hashCode() {
		return byteBuf.hashCode();
	}

	@Override
	public String toString() {
		return byteBuf.toString();
	}
}
