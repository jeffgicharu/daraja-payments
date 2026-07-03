package com.jeffgicharu.daraja.domain;

public enum PaymentStatus {
    /** STK push accepted by Daraja; awaiting the customer PIN and the result callback. */
    PENDING,
    /** Result callback received with ResultCode 0. */
    COMPLETED,
    /** Result callback received with a non-zero ResultCode (cancelled, timeout, insufficient funds, etc.). */
    FAILED
}
