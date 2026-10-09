# SMS Ledger v1.151

## Source corrections
- Normalize ISO, year-first slash, and Indian day-first slash dates before applying period boundaries. This targets records whose date strings were previously unparseable and consequently excluded from Yearly filtering.
- Preserve selected year state when entering Yearly mode and render the year-scoped sections from the shared period bounds.
- Subscription actual-payment totals now tolerate legacy amount/share record shapes while still counting only linked transactions inside the chosen period.
- Home Family Hub has a single top-level Manage Family and Share control; removed repeated section-level Share buttons/toggles from Home cards.
- Non-Home Family Hub summaries no longer show Manage Family.
- Align Subscriptions & Recurring share/expand controls, improve legend category labels, allow full-section Ledger scrolling, and show Protection expiry as month/year in legacy warranty rows.

## Validation
- JavaScript syntax checked with Node.js.
- Targeted date-normalization and year-boundary tests executed.
- ZIP integrity checked.
- Android APK compilation and device UI tests were not available in this environment.
