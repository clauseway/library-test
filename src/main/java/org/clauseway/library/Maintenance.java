package org.clauseway.library;

// ABOUTME: The lifecycle face the domain facade deliberately lacks: compaction
// ABOUTME: sheds closed loan clusters through the certified removal door.

import static org.clauseway.logic.unification.terms.LVar.lvar;

import org.clauseway.functional.Nothing;
import org.clauseway.functional.fibers.schedulers.BreadthFirstScheduler;
import org.clauseway.logic.unification.terms.Unifiable;
import org.clauseway.pldb.inmemory.SharedDatabase;
import org.clauseway.logic.solving.Query;
import org.clauseway.pldb.transaction.RecordingTransaction;
import org.clauseway.pldb.transaction.Transaction;
import org.clauseway.vavr.control.Try;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Administrative operations over the library's world — the doors
 * {@link Library} deliberately does not have. The domain stays
 * event-sourced and never deletes; the LIFECYCLE may shed spent
 * history. Compaction selects the clusters the domain's own closure
 * rule names ({@code closedLoan} — a loan with its return event),
 * retracts loan and return together, and commits under the certified
 * removal law: a pinned reader straddling the shed refuses, and every
 * LIVE query answers exactly as before — closed clusters are invisible
 * to liveness by definition.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Maintenance {

	public static Try<Nothing> compactClosedLoans(SharedDatabase world) throws Exception {
		try (RecordingTransaction tx = Transaction.recording(world.open("compact-closed-loans"))) {
			Rules rules = new Rules(tx);
			Unifiable<Integer> id = lvar();
			Unifiable<Integer> copy = lvar();
			Unifiable<Integer> member = lvar();
			Unifiable<LocalDate> due = lvar();
			Query closed = Query.of(rules.closedLoan(id, copy, member, due)).on(BreadthFirstScheduler::new);
			Transaction staged = tx
					.retracting(closed.select(Schema.loan(null, id, copy, member, due).facts()))
					.retracting(closed.select(Schema.returned(null, id).facts()));
			return Try.of(() -> {
				staged.commit();
				return Nothing.nothing();
			});
		}
	}
}
