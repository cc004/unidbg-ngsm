package com.github.unidbg.linux.thread;

import com.github.unidbg.Emulator;
import com.github.unidbg.arm.backend.Backend;
import com.github.unidbg.file.FileIO;
import com.github.unidbg.linux.file.PipedSocketIO;
import com.github.unidbg.linux.file.SocketIO;
import com.github.unidbg.thread.AbstractWaiter;
import com.github.unidbg.thread.Waiter;
import com.github.unidbg.unix.UnixEmulator;
import com.sun.jna.Pointer;
import unicorn.Arm64Const;
import unicorn.ArmConst;

public class ReceiveWaiter extends AbstractWaiter {
    private final Emulator<?> emulator;
    private final Backend backend;
    private final SocketIO file;
    private final Pointer buf;
    private final int len;
    private final Pointer src_addr;
    private final Pointer addrlen;
    private final long startWaitTimeInMillis;
    private final int flags;
    private final Thread thread;
    private int ret;

    public ReceiveWaiter(Emulator<?> emulator, SocketIO file, Backend backend, Pointer buf, int len, int flags,
                         Pointer src_addr, Pointer addrlen) {
        this.emulator = emulator;
        this.file = file;
        this.backend = backend;
        this.buf = buf;
        this.flags = flags;
        this.len = len;
        this.src_addr = src_addr;
        this.addrlen = addrlen;
        this.startWaitTimeInMillis = System.currentTimeMillis();
        this.thread = new Thread(() -> {
            ret = file.recvfrom(backend, buf, len, flags, src_addr, addrlen);
        });
        this.thread.start();
    }

    @Override
    public void onContinueRun(Emulator<?> emulator) {
        emulator.getBackend().reg_write(emulator.is32Bit() ? ArmConst.UC_ARM_REG_R0 : Arm64Const.UC_ARM64_REG_X0,
                ret);
    }

    @Override
    public boolean canDispatch() {
        if (this.thread.getState() == Thread.State.TERMINATED)
            return true;
        Thread.yield();
        return false;
    }
}
