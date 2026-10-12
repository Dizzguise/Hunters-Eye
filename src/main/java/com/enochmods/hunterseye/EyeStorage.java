package com.enochmods.hunterseye;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Bounded retries for transient Windows file locks during settings replacement. */
final class EyeStorage {
    private static final long[] RETRY_DELAYS={10,25,50};
    private EyeStorage() {}

    static void replace(Path temporary,Path destination) throws IOException {
        replace(temporary,destination,Files::move,Thread::sleep);
    }

    static void replace(Path temporary,Path destination,MoveOperation move,Sleeper sleeper) throws IOException {
        for(int attempt=0;;attempt++) {
            try {
                try { move.move(temporary,destination,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
                catch(AtomicMoveNotSupportedException error) { move.move(temporary,destination,StandardCopyOption.REPLACE_EXISTING); }
                return;
            } catch(AccessDeniedException error) {
                if(attempt==RETRY_DELAYS.length) throw error;
                try { sleeper.sleep(RETRY_DELAYS[attempt]); }
                catch(InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while replacing Hunter's Eye settings",interrupted);
                }
            }
        }
    }

    @FunctionalInterface
    interface MoveOperation { void move(Path source,Path destination,CopyOption... options) throws IOException; }
    @FunctionalInterface
    interface Sleeper { void sleep(long millis) throws InterruptedException; }
}
