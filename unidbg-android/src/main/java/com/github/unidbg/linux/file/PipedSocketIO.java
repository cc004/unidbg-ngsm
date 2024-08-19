package com.github.unidbg.linux.file;

import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.file.FileIO;
import com.sun.jna.Pointer;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.channels.Pipe;

public class PipedSocketIO extends TcpSocket implements FileIO {

    private final PipedInputStream pipedInputStream = new PipedInputStream();
    private int bytesToRead;
    private PipedSocketIO other;

    @Override
    public boolean canReceive() {
        return bytesToRead > 0;
    }

    public PipedSocketIO(Emulator<?> emulator) {
        super(emulator);
        this.inputStream = new BufferedInputStream(pipedInputStream);
        this.outputStream = new PipedOutputStream();
    }

    public void connectPeer(PipedSocketIO io) {
        try {
            ((PipedOutputStream) this.outputStream).connect(io.pipedInputStream);
            ((PipedOutputStream) io.outputStream).connect(this.pipedInputStream);

            this.other = io;
            io.other = this;

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public int recvfrom(Backend backend, Pointer buf, int len, int flags, Pointer src_addr, Pointer addrlen) {

        int result = super.recvfrom(backend, buf, len, flags, src_addr, addrlen);
        if (result > 0)
            this.bytesToRead -= result;
        return result;
    }

    @Override
    public int sendto(byte[] data, int flags, Pointer dest_addr, int addrlen) {
        other.bytesToRead += data.length;
        flags &= ~MSG_NOSIGNAL;
        final int MSG_EOR = 0x80;
        if (flags == MSG_EOR && dest_addr == null && addrlen == 0) {
            return write(data);
        }

        return super.sendto(data, flags, dest_addr, addrlen);
    }

}
