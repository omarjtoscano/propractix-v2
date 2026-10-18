package com.propractix.academicinstitution.adapter.out.persistence;

final class ReconciliationConflictException extends RuntimeException {

    ReconciliationConflictException() {
        super("catalog reconciliation conflict");
    }
}
