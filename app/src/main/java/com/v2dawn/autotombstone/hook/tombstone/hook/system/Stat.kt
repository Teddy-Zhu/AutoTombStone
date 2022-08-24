package com.v2dawn.autotombstone.hook.tombstone.hook.system;

import android.os.Parcel;
import android.os.Parcelable.Creator

import java.io.IOException;

class Stat : ProcFile {
    private val fields: Array<String>?

    private constructor(path: String) : super(path) {
        fields = content!!.split("\\s+").toTypedArray()
    }

    private constructor(`in`: Parcel) : super(`in`) {
        fields = `in`.createStringArray()
    }

    /** The process ID.  */
    val pid: Int
        get() = fields!![0].toInt()

    /**
     * The filename of the executable, in parentheses. This is visible whether or not the
     * executable is swapped out.
     */
    val comm: String
        get() = fields!![1].replace("(", "").replace(")", "")

    /**
     *
     * One of the following characters, indicating process state:
     *
     *
     *  * 'R'  Running
     *  * 'S'  Sleeping in an interruptible wait
     *  * 'D'  Waiting in uninterruptible disk sleep
     *  * 'Z'  Zombie
     *  * 'T'  Stopped (on a signal) or (before Linux 2.6.33) trace stopped
     *  * 't'  Tracing stop (Linux 2.6.33 onward)
     *  * 'W'  Paging (only before Linux 2.6.0)
     *  * 'X'  Dead (from Linux 2.6.0 onward)
     *  * 'x'  Dead (Linux 2.6.33 to 3.13 only)
     *  * 'K'  Wakekill (Linux 2.6.33 to 3.13 only)
     *  * 'W'  Waking (Linux 2.6.33 to 3.13 only)
     *  * 'P'  Parked (Linux 3.9 to 3.13 only)
     *
     */
    fun state(): Char {
        return fields!![2][0]
    }

    /**
     * The PID of the parent of this process.
     */
    fun ppid(): Int {
        return fields!![3].toInt()
    }

    /**
     * The process group ID of the process.
     */
    fun pgrp(): Int {
        return fields!![4].toInt()
    }

    /**
     * The session ID of the process.
     */
    fun session(): Int {
        return fields!![5].toInt()
    }

    /**
     * The controlling terminal of the process. (The minor device number is contained in the
     * combination of bits 31 to 20 and 7 to 0; the major device number is in bits 15 to 8.)
     */
    fun tty_nr(): Int {
        return fields!![6].toInt()
    }

    /**
     * The ID of the foreground process group of the controlling terminal of the process.
     */
    fun tpgid(): Int {
        return fields!![7].toInt()
    }

    /**
     *
     * The kernel flags word of the process. For bit meanings, see the PF_* defines in the Linux
     * kernel source file include/linux/sched.h. Details depend on the kernel version.
     *
     *
     * The format for this field was %lu before Linux 2.6.
     */
    fun flags(): Int {
        return fields!![8].toInt()
    }

    /**
     * The number of minor faults the process has made which have not required loading a memory
     * page from disk.
     */
    fun minflt(): Long {
        return fields!![9].toLong()
    }

    /**
     * The number of minor faults that the process's waited-for children have made.
     */
    fun cminflt(): Long {
        return fields!![10].toLong()
    }

    /**
     * The number of major faults the process has made which have required loading a memory page
     * from disk.
     */
    fun majflt(): Long {
        return fields!![11].toLong()
    }

    /**
     * The number of major faults that the process's waited-for children have made.
     */
    fun cmajflt(): Long {
        return fields!![12].toLong()
    }

    /**
     * Amount of time that this process has been scheduled in user mode, measured in clock ticks
     * (divide by sysconf(_SC_CLK_TCK)).  This includes guest time, guest_time (time spent running
     * a virtual CPU, see below), so that applications that are not aware of the guest time field
     * do not lose that time from their calculations.
     */
    fun utime(): Long {
        return fields!![13].toLong()
    }

    /**
     * Amount of time that this process has been scheduled in kernel mode, measured in clock ticks
     * (divide by sysconf(_SC_CLK_TCK)).
     */
    fun stime(): Long {
        return fields!![14].toLong()
    }

    /**
     * Amount of time that this process's waited-for children have been scheduled in user mode,
     * measured in clock ticks (divide by sysconf(_SC_CLK_TCK)). (See also times(2).)  This
     * includes guest time, cguest_time (time spent running a virtual CPU, see below).
     */
    fun cutime(): Long {
        return fields!![15].toLong()
    }

    /**
     * Amount of time that this process's waited-for children have been scheduled in kernel mode,
     * measured in clock ticks (divide by sysconf(_SC_CLK_TCK)).
     */
    fun cstime(): Long {
        return fields!![16].toLong()
    }

    /**
     *
     * (Explanation for Linux 2.6) For processes running a real-time scheduling policy (policy
     * below; see sched_setscheduler(2)), this is the negated scheduling priority, minus one; that
     * is,
     * a number in the range -2 to -100, corresponding to real-time priorities 1 to 99.  For
     * processes
     * running under a non-real-time scheduling policy, this is the raw nice value (setpriority(2))
     * as
     * represented in the kernel.  The kernel stores nice values as numbers in the range 0 (high) to
     * 39 (low), corresponding to the user-visible nice range of -20 to 19.
     *
     *
     * Before Linux 2.6, this was a scaled value based on the scheduler weighting given to this
     * process.
     */
    fun priority(): Long {
        return fields!![17].toLong()
    }

    /**
     * The nice value (see setpriority(2)), a value in the range 19 (low priority) to -20 (high
     * priority).
     */
    fun nice(): Int {
        return fields!![18].toInt()
    }

    /**
     * Number of threads in this process (since Linux 2.6). Before kernel 2.6, this field was hard
     * coded to 0 as a placeholder for an earlier removed field.
     */
    fun num_threads(): Long {
        return fields!![19].toLong()
    }

    /**
     * The time in jiffies before the next SIGALRM is sent to the process due to an interval timer.
     * Since kernel 2.6.17, this field is no longer maintained, and is hard coded as 0.
     */
    fun itrealvalue(): Long {
        return fields!![20].toLong()
    }

    /**
     *
     * The time the process started after system boot. In kernels before Linux 2.6, this value was
     * expressed in jiffies.  Since Linux 2.6, the value is expressed in clock ticks (divide by
     * sysconf(_SC_CLK_TCK)).
     *
     *
     * The format for this field was %lu before Linux 2.6.
     */
    fun starttime(): Long {
        return fields!![21].toLong()
    }

    /**
     * Virtual memory size in bytes.
     */
    fun vsize(): Long {
        return fields!![22].toLong()
    }

    /**
     * Resident Set Size: number of pages the process has in real memory.  This is just the pages
     * which count toward text, data, or stack space.  This does not include pages which have not
     * been demand-loaded in, or which are swapped out.
     */
    fun rss(): Long {
        return fields!![23].toLong()
    }

    /**
     * Current soft limit in bytes on the rss of the process; see the description of RLIMIT_RSS in
     * getrlimit(2).
     */
    fun rsslim(): Long {
        return fields!![24].toLong()
    }

    /**
     * The address above which program text can run.
     */
    fun startcode(): Long {
        return fields!![25].toLong()
    }

    /**
     * The address below which program text can run.
     */
    fun endcode(): Long {
        return fields!![26].toLong()
    }

    /**
     * The address of the start (i.e., bottom) of the stack.
     */
    fun startstack(): Long {
        return fields!![27].toLong()
    }

    /**
     * The current value of ESP (stack pointer), as found in the kernel stack page for the process.
     */
    fun kstkesp(): Long {
        return fields!![28].toLong()
    }

    /**
     * The current EIP (instruction pointer).
     */
    fun kstkeip(): Long {
        return fields!![29].toLong()
    }

    /**
     * The bitmap of pending signals, displayed as a decimal number.  Obsolete, because it does not
     * provide information on real-time signals; use /proc/[pid]/status instead.
     */
    fun signal(): Long {
        return fields!![30].toLong()
    }

    /**
     * The bitmap of blocked signals, displayed as a decimal number.  Obsolete, because it does not
     * provide information on real-time signals; use /proc/[pid]/status instead.
     */
    fun blocked(): Long {
        return fields!![31].toLong()
    }

    /**
     * The bitmap of ignored signals, displayed as a decimal number.  Obsolete, because it does not
     * provide information on real-time signals; use /proc/[pid]/status instead.
     */
    fun sigignore(): Long {
        return fields!![32].toLong()
    }

    /**
     * The bitmap of caught signals, displayed as a decimal number.  Obsolete, because it does not
     * provide information on real-time signals; use /proc/[pid]/status instead.
     */
    fun sigcatch(): Long {
        return fields!![33].toLong()
    }

    /**
     * This is the "channel" in which the process is waiting.  It is the address of a location in the
     * kernel where the process is sleeping. The corresponding symbolic name can be found in
     * /proc/[pid]/wchan.
     */
    fun wchan(): Long {
        return fields!![34].toLong()
    }

    /**
     * Number of pages swapped (not maintained).
     */
    fun nswap(): Long {
        return fields!![35].toLong()
    }

    /**
     * Cumulative nswap for child processes (not maintained).
     */
    fun cnswap(): Long {
        return fields!![36].toLong()
    }

    /**
     * (since Linux 2.1.22)
     * Signal to be sent to parent when we die.
     */
    fun exit_signal(): Int {
        return fields!![37].toInt()
    }

    /**
     * (since Linux 2.2.8)
     * CPU number last executed on.
     */
    fun processor(): Int {
        return fields!![38].toInt()
    }

    /**
     * (since Linux 2.5.19)
     * Real-time scheduling priority, a number in the range 1 to 99 for processes scheduled under a
     * real-time policy, or 0, for non-real-time processes (see sched_setscheduler(2)).
     */
    fun rt_priority(): Int {
        return fields!![39].toInt()
    }

    /**
     *
     * (since Linux 2.5.19) Scheduling policy (see sched_setscheduler(2)). Decode using the
     * SCHED_*
     * constants in linux/sched.h.
     *
     *
     * The format for this field was %lu before Linux 2.6.22.
     */
    fun policy(): Int {
        return fields!![40].toInt()
    }

    /**
     * (since Linux 2.6.18)
     * Aggregated block I/O delays, measured in clock ticks (centiseconds).
     */
    fun delayacct_blkio_ticks(): Long {
        return fields!![41].toLong()
    }

    /**
     * (since Linux 2.6.24)
     * Guest time of the process (time spent running a virtual CPU for a guest operating system),
     * measured in clock ticks (divide by sysconf(_SC_CLK_TCK)).
     */
    fun guest_time(): Long {
        return fields!![42].toLong()
    }

    /**
     * (since Linux 2.6.24)
     * Guest time of the process's children, measured in clock ticks (divide by
     * sysconf(_SC_CLK_TCK)).
     */
    fun cguest_time(): Long {
        return fields!![43].toLong()
    }

    /**
     * (since Linux 3.3)
     * Address above which program initialized and uninitialized (BSS) data are placed.
     */
    fun start_data(): Long {
        return fields!![44].toLong()
    }

    /**
     * (since Linux 3.3)
     * Address below which program initialized and uninitialized (BSS) data are placed.
     */
    fun end_data(): Long {
        return fields!![45].toLong()
    }

    /**
     * (since Linux 3.3)
     * Address above which program heap can be expanded with brk(2).
     */
    fun start_brk(): Long {
        return fields!![46].toLong()
    }

    /**
     * (since Linux 3.5)
     * Address above which program command-line arguments (argv) are placed.
     */
    fun arg_start(): Long {
        return fields!![47].toLong()
    }

    /**
     * (since Linux 3.5)
     * Address below program command-line arguments (argv) are placed.
     */
    fun arg_end(): Long {
        return fields!![48].toLong()
    }

    /**
     * (since Linux 3.5)
     * Address above which program environment is placed.
     */
    fun env_start(): Long {
        return fields!![49].toLong()
    }

    /**
     * (since Linux 3.5)
     * Address below which program environment is placed.
     */
    fun env_end(): Long {
        return fields!![50].toLong()
    }

    /**
     * (since Linux 3.5)
     * The thread's exit status in the form reported by waitpid(2).
     */
    fun exit_code(): Int {
        return fields!![51].toInt()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        super.writeToParcel(dest, flags)
        dest.writeStringArray(fields)
    }

    companion object {
        @Throws(IOException::class)
        operator fun get(pid: Int): Stat {
            return Stat(String.format("/proc/%d/stat", pid))
        }

        @JvmField
        val CREATOR: Creator<Stat?> = object : Creator<Stat?> {
            override fun createFromParcel(source: Parcel): Stat? {
                return Stat(source)
            }

            override fun newArray(size: Int): Array<Stat?> {
                return arrayOfNulls(size)
            }
        }
    }
}
