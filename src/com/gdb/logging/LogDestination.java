package com.gdb.logging;

import com.gdb.command.TransactionCommand;
import java.util.List;

/**
 * Implementor interface for Bridge Pattern decoupling transaction log storage backends.
 */
public interface LogDestination {
    // ============================================================
    // 📝 STEP 1: Add write(TransactionCommand cmd)
    // ============================================================

    void write(TransactionCommand cmd);

    // ============================================================
    // 📝 STEP 2: Add readAll()
    // ============================================================

    List<TransactionCommand> readAll();

    // ============================================================
    // 📝 STEP 3: Add clear()
    // ============================================================
    void clear();

    // ============================================================
    // 📝 STEP 4: Add getDestinationName()
    
    String getDestinationName();
}
