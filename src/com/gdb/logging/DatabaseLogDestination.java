package com.gdb.logging;

import com.gdb.command.TransactionCommand;
import com.gdb.db.SimulatedDatabase;
import java.util.*;

public class DatabaseLogDestination implements LogDestination {
    private static final String TABLE = "transaction_log";

    // ============================================================
    // 📝 STEP 11: Declare Field
    // ============================================================
    // TODO: declare private final SimulatedDatabase db;
    private final SimulatedDatabase db;

    // ============================================================
    // 📝 STEP 12: Constructor
    // ============================================================
    // TODO: implement constructor accepting SimulatedDatabase
    public DatabaseLogDestination(SimulatedDatabase db) {
        this.db = db;
    }

    // ============================================================
    // 📝 STEP 13: write(cmd)
    // ============================================================
    // TODO: insert command into database table
    @Override
    public void write(TransactionCommand cmd) {
        db.insert(TABLE, cmd);
    }

    // ============================================================
    // 📝 STEP 14: readAll()
    //
    // INSTRUCTIONS:
    //   1. Call db.selectAll(TABLE).
    //   2. Map/cast each Object to TransactionCommand.
    //   3. Collect and return List<TransactionCommand>.
    // ============================================================
    // TODO: retrieve and return all commands from database
    @Override
    public List<TransactionCommand> readAll() {
        List<TransactionCommand> commands = new ArrayList<>();
        for (Object obj : db.selectAll(TABLE)) {
            if (obj instanceof TransactionCommand) {
                commands.add((TransactionCommand) obj);
            }
        }
        return commands;
    }

    // ============================================================
    // 📝 STEP 15: clear()
    // ============================================================
    // TODO: delete all records from database table
    @Override
    public void clear() {
        db.deleteAll(TABLE);
    }

    // ============================================================
    // 📝 STEP 16: getDestinationName()
    // ============================================================
    // TODO: return "DATABASE"
    @Override
    public String getDestinationName() {
        return "DATABASE";
    }
}
