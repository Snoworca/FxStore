package com.snoworca.fxstore.storage;

import com.snoworca.fxstore.api.FileLockMode;
import com.snoworca.fxstore.api.FxErrorCode;
import com.snoworca.fxstore.api.FxException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.Objects;

/**
 * File-based storage implementation.
 */
public class FileStorage implements Storage {

    private final Path path;
    private final FileChannel channel;
    private final FileLock lock;
    private final boolean readOnly;
    private boolean closed;

    /**
     * Open storage with default settings (read-write, no lock).
     */
    public FileStorage(Path path) {
        this(path, false, FileLockMode.NONE);
    }

    /**
     * Open storage with read-only option.
     */
    public FileStorage(Path path, boolean readOnly) {
        this(path, readOnly, FileLockMode.NONE);
    }

    /**
     * Open storage with read-only and lock mode options.
     */
    public FileStorage(Path path, boolean readOnly, FileLockMode lockMode) {
        Objects.requireNonNull(path, "path");
        this.path = path;
        this.readOnly = readOnly;

        FileLockMode effectiveLockMode = lockMode != null ? lockMode : FileLockMode.NONE;

        try {
            boolean exists = Files.exists(path);

            if (readOnly && !exists) {
                throw new FxException("Failed to open file (not found): " + path, FxErrorCode.IO);
            }

            EnumSet<StandardOpenOption> options = EnumSet.of(StandardOpenOption.READ);

            if (!readOnly) {
                options.add(StandardOpenOption.WRITE);
                if (!exists) {
                    options.add(StandardOpenOption.CREATE_NEW);
                }
            }

            this.channel = FileChannel.open(path, options);
            this.closed = false;

            if (effectiveLockMode == FileLockMode.PROCESS) {
                try {
                    FileLock acquired = readOnly ? channel.tryLock(0, Long.MAX_VALUE, true) : channel.tryLock();
                    if (acquired == null) {
                        channel.close();
                        throw new FxException("Failed to acquire file lock: " + path, FxErrorCode.LOCK_FAILED);
                    }
                    this.lock = acquired;
                } catch (java.nio.channels.OverlappingFileLockException e) {
                    try { channel.close(); } catch (IOException ignored) {}
                    throw new FxException("File lock conflict: " + path, e, FxErrorCode.LOCK_FAILED);
                }
            } else {
                this.lock = null;
            }
        } catch (IOException e) {
            throw new FxException("Failed to open file: " + path, e, FxErrorCode.IO);
        }
    }

    /**
     * Get the file path.
     */
    public Path getPath() {
        return path;
    }

    /**
     * Check if storage is read-only.
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    private void checkClosed() {
        if (closed) {
            throw new FxException("Storage is closed", FxErrorCode.CLOSED);
        }
    }

    private void checkWritable() {
        if (readOnly) {
            throw new FxException("Storage is read-only", FxErrorCode.IO);
        }
    }

    @Override
    public void read(long offset, byte[] buffer, int bufOffset, int length) {
        checkClosed();
        Objects.requireNonNull(buffer, "buffer");
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be non-negative");
        }
        if (bufOffset < 0) {
            throw new IllegalArgumentException("bufOffset must be non-negative");
        }
        if (length < 0) {
            throw new IllegalArgumentException("length must be non-negative");
        }
        if (bufOffset + length > buffer.length) {
            throw new IllegalArgumentException("buffer overflow: bufOffset + length > buffer.length");
        }
        if (length == 0) {
            return;
        }

        try {
            ByteBuffer bb = ByteBuffer.wrap(buffer, bufOffset, length);
            int totalRead = 0;
            while (totalRead < length) {
                int read = channel.read(bb, offset + totalRead);
                if (read < 0) {
                    throw new FxException("Unexpected EOF at offset " + (offset + totalRead), FxErrorCode.IO);
                }
                totalRead += read;
            }
        } catch (IOException e) {
            throw new FxException("Read failed at offset " + offset, e, FxErrorCode.IO);
        }
    }

    @Override
    public void write(long offset, byte[] buffer, int bufOffset, int length) {
        checkClosed();
        checkWritable();
        Objects.requireNonNull(buffer, "buffer");
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be non-negative");
        }
        if (bufOffset < 0) {
            throw new IllegalArgumentException("bufOffset must be non-negative");
        }
        if (length < 0) {
            throw new IllegalArgumentException("length must be non-negative");
        }
        if (bufOffset + length > buffer.length) {
            throw new IllegalArgumentException("buffer overflow: bufOffset + length > buffer.length");
        }
        if (length == 0) {
            return;
        }

        try {
            ByteBuffer bb = ByteBuffer.wrap(buffer, bufOffset, length);
            int totalWritten = 0;
            while (totalWritten < length) {
                int written = channel.write(bb, offset + totalWritten);
                totalWritten += written;
            }
        } catch (IOException e) {
            throw new FxException("Write failed at offset " + offset, e, FxErrorCode.IO);
        }
    }

    @Override
    public void force(boolean metadata) {
        checkClosed();
        try {
            channel.force(metadata);
        } catch (IOException e) {
            throw new FxException("Force failed", e, FxErrorCode.IO);
        }
    }

    @Override
    public long size() {
        checkClosed();
        try {
            return channel.size();
        } catch (IOException e) {
            throw new FxException("Failed to get file size", e, FxErrorCode.IO);
        }
    }

    @Override
    public void extend(long newSize) {
        checkClosed();
        checkWritable();
        if (newSize < 0) {
            throw new IllegalArgumentException("newSize must be non-negative");
        }
        try {
            if (channel.size() < newSize) {
                channel.position(newSize - 1);
                channel.write(ByteBuffer.wrap(new byte[1]));
            }
        } catch (IOException e) {
            throw new FxException("Extend failed to size " + newSize, e, FxErrorCode.IO);
        }
    }

    @Override
    public void truncate(long newSize) {
        checkClosed();
        checkWritable();
        try {
            channel.truncate(newSize);
        } catch (IOException e) {
            throw new FxException("Truncate failed to size " + newSize, e, FxErrorCode.IO);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        try {
            if (lock != null) {
                lock.release();
            }
            channel.close();
        } catch (IOException e) {
            // Ignore close errors
        }
    }
}
