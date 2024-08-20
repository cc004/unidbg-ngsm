package com.github.unidbg.linux.file;

import com.alibaba.fastjson.util.IOUtils;
import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.file.FileIO;
import com.github.unidbg.file.linux.AndroidFileIO;
import com.github.unidbg.unix.UnixEmulator;
import com.github.unidbg.utils.Inspector;
import com.sun.jna.Pointer;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TcpSocket extends SocketIO implements FileIO {

    private static final Log log = LogFactory.getLog(TcpSocket.class);

    private final Socket socket;
    private ServerSocket serverSocket;

    private final Emulator<?> emulator;

    public TcpSocket(Emulator<?> emulator) {
        this(emulator, new Socket());
    }

    private TcpSocket(Emulator<?> emulator, Socket socket) {
        this.emulator = emulator;
        this.socket = socket;
        try {
            socket.setSoTimeout(5000);
        } catch (SocketException ignored) {

        }
        if (emulator.getSyscallHandler().isVerbose()) {
            // System.out.printf("Tcp opened '%s' from %s%n", this, emulator.getContext().getLRPointer());
        }
    }

    public boolean canReceive() {
        try {
            outputStream.flush();
            // System.out.println("poll: " + inputStream.available());
            return inputStream != null && inputStream.available() > 0;
        } catch (IOException ignored) {
            return false;
        }
    }

    protected OutputStream outputStream;
    protected InputStream inputStream;

    @Override
    public void close() {
        IOUtils.close(outputStream);
        IOUtils.close(inputStream);
        IOUtils.close(socket);
        IOUtils.close(serverSocket);
    }

    @Override
    public int write(byte[] data) {
        try {
            if (log.isDebugEnabled()) {
                Inspector.inspect(data, "write hex=" + Hex.encodeHexString(data));
            }
            outputStream.write(data);
            return data.length;
        } catch (IOException e) {
            log.debug("write failed", e);
            return -1;
        }
    }

    @Override
    public int recvfrom(Backend backend, Pointer buf, int len, int flags, Pointer src_addr, Pointer addrlen) {
        boolean peek = (flags & MSG_PEEK) != 0;
        if (peek &&
                (flags & ~MSG_PEEK) == 0 &&
                inputStream.markSupported() &&
                src_addr == null && addrlen == null) {
            try {
                inputStream.mark(len);
                return readInternal(buf, len, false);
            } finally {
                try {
                    inputStream.reset();
                } catch (IOException e) {
                    log.warn("recvfrom", e);
                }
            }
        }

        return super.recvfrom(backend, buf, len, flags, src_addr, addrlen);
    }

    private byte[] receiveBuf;

    @Override
    public int read(Backend backend, Pointer buffer, int count) {
        return readInternal(buffer, count, true);
    }

    protected int readInternal(Pointer buffer, int count, boolean logRead) {
        try {
            if (receiveBuf == null) {
                receiveBuf = new byte[socket.getReceiveBufferSize()];
            }
            int read = inputStream.read(receiveBuf, 0, Math.min(count, receiveBuf.length));
            if (read <= 0) {
                return read;
            }

            byte[] data = Arrays.copyOf(receiveBuf, read);
            buffer.write(0, data, 0, data.length);
            if (logRead && log.isDebugEnabled()) {
                Inspector.inspect(data, "readInternal socket=" + socket);
            }
            return data.length;
        } catch (IOException e) {
            log.debug("readInternal", e);
            return -1;
        }
    }

    @Override
    public int listen(int backlog) {
        try {
            serverSocket = new ServerSocket();
            com.alibaba.fastjson.util.IOUtils.close(socket);
            serverSocket.bind(socket.getLocalSocketAddress(), backlog);
            return 0;
        } catch (IOException e) {
            log.debug("listen failed", e);
            emulator.getMemory().setErrno(UnixEmulator.EOPNOTSUPP);
            return -1;
        }
    }

    @Override
    public AndroidFileIO accept(Pointer addr, Pointer addrlen) {
        try {
            Socket socket = serverSocket.accept();
            TcpSocket io = new TcpSocket(emulator, socket);
            io.inputStream = new BufferedInputStream(socket.getInputStream());
            io.outputStream = socket.getOutputStream();
            if (addr != null) {
                io.getpeername(addr, addrlen);
            }
            return io;
        } catch (IOException e) {
            log.debug("accept failed", e);
            emulator.getMemory().setErrno(UnixEmulator.EAGAIN);
            return null;
        }
    }

    @Override
    protected int bind_ipv4(Pointer addr, int addrlen) {
        int sa_family = addr.getShort(0);
        if (sa_family != AF_INET) {
            throw new AbstractMethodError("sa_family=" + sa_family);
        }

        try {
            int port = Short.reverseBytes(addr.getShort(2)) & 0xffff;
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByAddress(addr.getByteArray(4, 4)), port);
            if (log.isDebugEnabled()) {
                byte[] data = addr.getByteArray(0, addrlen);
                Inspector.inspect(data, "address=" + address);
            }
            socket.bind(address);
            return 0;
        } catch (IOException e) {
            log.debug("bind ipv4 failed", e);
            emulator.getMemory().setErrno(UnixEmulator.EADDRINUSE);
            return -1;
        }
    }

    private InetSocketAddress peer;

    @Override
    protected int connect_ipv4(Pointer addr, int addrlen) {
        if (log.isDebugEnabled()) {
            byte[] data = addr.getByteArray(0, addrlen);
            Inspector.inspect(data, "addr");
        }

        int sa_family = addr.getShort(0);
        if (sa_family != AF_INET) {
            throw new AbstractMethodError("sa_family=" + sa_family);
        }

        try {
            int port = Short.reverseBytes(addr.getShort(2)) & 0xffff;
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByAddress(addr.getByteArray(4, 4)), port);
            peer = address;
            // if (port == 443) address = new InetSocketAddress(InetAddress.getByName("localhost"), 443);

            socket.connect(new InetSocketAddress(InetAddress.getByName("localhost"), 8888));
            outputStream = socket.getOutputStream();
            inputStream = new BufferedInputStream(socket.getInputStream());

            String addressString = address.getAddress().toString();

            if (addressString.equals("/54.150.7.255") || addressString.equals("/54.168.225.241") )
                addressString = "api.ngsm.nexon.com";
            else if (addressString.equals("/54.250.170.11") || addressString.equals("/54.150.202.113") ||
                    addressString.equals("/57.181.246.96") || addressString.equals("/13.113.199.152"))
                addressString = "log.ngsm.nexon.com";
            else if (addressString.equals("/57.181.145.8") || addressString.equals("/52.193.109.153"))
                addressString = "config.ngsm.nexon.com";
            else
                addressString = addressString.replace("/", "");

            addressString = addressString + ":" + address.getPort();

            System.err.println("socket connect: " + addressString);

            String str = "" +
                    "CONNECT " + addressString + " HTTP/1.1\n" +
                    "User-Agent: Java/1.8.0_192\n" +
                    "Host: " + addressString + "\n" +
                    "Accept: */*\n" +
                    "Proxy-Connection: keep-alive\n" +
                    "\n";

            outputStream.write(str.getBytes(StandardCharsets.UTF_8));

            int state = 0;

            while (true) {
                int c = inputStream.read();
                if (c == '\r' || c == '\n') {
                    ++state;
                    if (state == 4) {
                        break;
                    }
                }
                else {
                    state = 0;
                }
            }

            /*
            socket.connect(address);
            outputStream = socket.getOutputStream();
            inputStream = new BufferedInputStream(socket.getInputStream());*/
            return 0;
        } catch (IOException e) {
            log.debug("connect ipv4 failed", e);
            emulator.getMemory().setErrno(UnixEmulator.ECONNREFUSED);
            return -1;
        }
    }

    @Override
    protected int connect_ipv6(Pointer addr, int addrlen) {
        if (log.isDebugEnabled()) {
            byte[] data = addr.getByteArray(0, addrlen);
            Inspector.inspect(data, "addr");
        }

        int sa_family = addr.getShort(0);
        if (sa_family != AF_INET6) {
            throw new AbstractMethodError("sa_family=" + sa_family);
        }

        try {
            int port = Short.reverseBytes(addr.getShort(2)) & 0xffff;
            InetSocketAddress address = new InetSocketAddress(InetAddress.getByAddress(addr.getByteArray(8, 16)), port);
            socket.connect(address);
            outputStream = socket.getOutputStream();
            inputStream = new BufferedInputStream(socket.getInputStream());
            return 0;
        } catch (IOException e) {
            log.debug("connect ipv6 failed", e);
            emulator.getMemory().setErrno(UnixEmulator.ECONNREFUSED);
            return -1;
        }
    }

    @Override
    public int getpeername(Pointer addr, Pointer addrlen) {
        InetSocketAddress remote = (InetSocketAddress) socket.getRemoteSocketAddress();
        remote = peer;
        fillAddress(remote, addr, addrlen);
        return 0;
    }

    @Override
    protected InetSocketAddress getLocalSocketAddress() {
        return (InetSocketAddress) socket.getLocalSocketAddress();
    }

    @Override
    protected void setKeepAlive(int keepAlive) throws SocketException {
        socket.setKeepAlive(keepAlive != 0);
    }

    @Override
    protected void setSendBufferSize(int size) throws SocketException {
        socket.setSendBufferSize(size);

    }

    @Override
    protected void setReceiveBufferSize(int size) throws SocketException {
        socket.setReceiveBufferSize(size);
    }

    @Override
    protected void setReuseAddress(int reuseAddress) throws SocketException {
        socket.setReuseAddress(reuseAddress != 0);
    }

    @Override
    protected void setTcpNoDelay(int tcpNoDelay) throws SocketException {
        socket.setTcpNoDelay(tcpNoDelay != 0);
    }

    @Override
    protected int getTcpNoDelay() throws SocketException {
        return socket.getTcpNoDelay() ? 1 : 0;
    }

    @Override
    protected void setSoTimeout(int timeout) throws SocketException {
        socket.setSoTimeout(timeout);
    }

    @Override
    public int shutdown(int how) {
        switch (how) {
            case SHUT_RD:
            case SHUT_WR:
                com.alibaba.fastjson.util.IOUtils.close(outputStream);
                outputStream = null;
                return 0;
            case SHUT_RDWR:
                com.alibaba.fastjson.util.IOUtils.close(outputStream);
                IOUtils.close(inputStream);
                outputStream = null;
                inputStream = null;
                return 0;
        }

        return super.shutdown(how);
    }

    @Override
    public String toString() {
        return socket.toString();
    }
}
