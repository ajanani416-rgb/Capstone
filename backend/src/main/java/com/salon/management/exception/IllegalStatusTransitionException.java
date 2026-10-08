package com.salon.management.exception;

import com.salon.management.entity.AppointmentStatus;

/** Illegal status move → 409. The message names the legal targets so the
 * admin UI can surface the remedy without guessing. */
public class IllegalStatusTransitionException extends RuntimeException {

    public IllegalStatusTransitionException(AppointmentStatus from, AppointmentStatus to) {
        super("Cannot move appointment from " + from + " to " + to + ".");
    }
}
