package com.salon.management.exception;

/** Requested barber+date+time is already taken → 409. Kept separate from the
 * DB-constraint path so the message can name the remedy ("pick another time"). */
public class SlotTakenException extends RuntimeException {

    public SlotTakenException() {
        super("That time slot is already booked for this barber. Please pick another time.");
    }
}
