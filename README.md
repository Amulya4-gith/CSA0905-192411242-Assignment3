# CSA09-192411242-Assignment3

| | |
|---|---|
| Name (Student A) | K. Amulya |
| Register number | 192411242 |
| Student B | [partner name and register number] |
| Course | CSA0905 – Programming in Java (Assignment 3, CO3, pair work) |
| Java version | OpenJDK 21 |

## How to compile and run

Every program is run from inside its own folder. Compile all the files of the folder first, then run the main class:

    javac *.java
    java <MainClass>

A folder with a single main class can also be run directly with `java File.java` when that class is the first one in the file.

## Tasks

| Folder | Task | Main class | Result file |
|---|---|---|---|
| Q1 | ATM withdrawal: try, catch, multi-catch, finally | AtmDemo | atm_out.txt |
| Q1 | Uncaught exception and its stack trace | UncaughtDemo | uncaught_out.txt |
| Q1 | Broader catch before narrower (compile error expected) | BroaderFirst | broader_out.txt |
| Q1 | Single catch (Exception e) | SingleCatch | single_out.txt |
| Q1 | Error codes against exceptions, cost | ErrorCodeDemo | errcode_out.txt |
| Q2 | Exam portal: user-defined exceptions, three layers, chaining | PortalDemo | portal_out.txt |
| Q2 | Cost of stack trace and chaining | ExcBench | bench_out.txt |
| Q2/nocause | Cause not passed to the constructor | NoCauseDemo | nocause_out.txt |
| Q2/CompleteTheCode | Complete the Code, TODO 1 | StorageDemo | output_after_TODO1.txt |
| Q3 | Report generator: manual close, finally, try-with-resources | ReportDemo | report_out.txt |
| Q3 | Handles held open for f files | Scale | scale_out.txt |
| Q3 | Real "Too many open files" (run after Scale, with `ulimit -n 1024`) | OsLimit | oslimit_out.txt |
| Q4 | Scanner stations (Thread and Runnable), ten runs | ScanDemo (argument `thread` or `runnable`) | scan_first.txt, scan_runs.txt, scan_runs_thread.txt |
| Q4 | run() against start() | RunVsStart | runvsstart_out.txt |
| Q4 | Thread states | LifeCycle | lifecycle_out.txt |
| Q4 | Priorities | PriorityDemo | priority_out.txt |
| Q4 | Daemon thread | DaemonDemo | daemon_out.txt |
| Q4 | Executor against new Thread | PoolCompare | pool_out.txt |
| Q5 | Race condition, unsynchronized and synchronized versions | RaceTest | race_out.txt |
| Q5 | Static against instance synchronization | StaticDemo | static_out.txt |
| Q5 | Synchronization strategies, lock scope, memory | SyncBench | bench_out.txt |
| Q5/CompleteTheCode | Complete the Code, TODO 1 (before: no synchronized, after: synchronized) | SeatBooking | output_before_TODO1.txt, output_after_TODO1.txt |
| Q6 | Counter size over time | TraceCounter | trace_out.txt |
| Q6 | while/if and notify/notifyAll trials | BoundedTrials | trials_out.txt |
| Q6 | Deadlock, lock ordering, tryLock | WalletDeadlock | wallet_out.txt |
| Q6 | wait/notifyAll against ArrayBlockingQueue | QueueBench | queue_out.txt |
| Q6/CompleteTheCode | Complete the Code, TODO 1 and TODO 2 | TokenCounter | output_after_TODO.txt |
| Q6/CompleteTheCode/while_changed_to_if | while changed to if, 15 runs | TokenCounter | output_15_runs.txt |
| Q7 | Concurrent result processor, 10,000 files, pool sizes, alternatives | Processor | processor_out.txt |
| Q7 | Hand-checked set of 13 files | HandCheck | handcheck_out.txt |

## Notes

- The programs were run on a single-CPU machine with OpenJDK 21.0.12. Timings are the best or the median of repeated runs, as stated in the report. Your numbers will differ with the machine, and speed-ups with more threads will look different on a multi-core computer.
- Q3, Q5 and Q7 create their own input files when run (the folders data, many, scores and hand).
- Add output screenshots, and the debugger screenshots for Q3, to each folder after running the programs on your own computer.
