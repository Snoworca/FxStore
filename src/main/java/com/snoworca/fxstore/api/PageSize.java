package com.snoworca.fxstore.api;

/**
 * Page size options.
 */
public enum PageSize {
    PAGE_4K(4096),
    PAGE_8K(8192),
    PAGE_16K(16384);

    private final int bytes;

    PageSize(int bytes) {
        this.bytes = bytes;
    }

    public int bytes() {
        return bytes;
    }
}
