package com.snoworca.fxstore.api;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Integrity verification result.
 */
public final class VerifyResult {
    private final boolean ok;
    private final List<VerifyError> errors;

    public VerifyResult(boolean ok, List<VerifyError> errors) {
        this.ok = ok;
        this.errors = errors == null ? Collections.emptyList() : Collections.unmodifiableList(errors);
    }

    /**
     * Whether all checks passed
     */
    public boolean ok() {
        return ok;
    }

    /**
     * List of discovered errors
     */
    public List<VerifyError> errors() {
        return errors;
    }

    @Override
    public String toString() {
        return "VerifyResult{" +
                "ok=" + ok +
                ", errors=" + errors +
                '}';
    }
}
