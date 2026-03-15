package com.tutr.backend.model;

public enum ConnectionStatus {
    PENDING,
    NEGOTIATING,
    CONFIRMED,    // When either party accepts, it becomes CONFIRMED
    CANCELLED,
    DISCONNECTED  // When either party disconnects
}