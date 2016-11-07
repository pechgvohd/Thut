package thut.api.entity.ai;

import net.minecraft.world.World;

public interface IAIRunnable
{
    /** called to execute the needed non-threadsafe tasks on the main thread. */
    void doMainThreadTick(World world);

    /** Will only run an AI if it is higher priority (ie lower number) or a
     * bitwise AND of the two mutex is 0.
     * 
     * @return */
    int getMutex();

    /** @return the priority of this AIRunnable. Lower numbers run first. */
    int getPriority();

    /** Resets the task. */
    void reset();

    /** runs the task */
    void run();

    /** Sets the mutex.
     * 
     * @param mutex
     * @return */
    IAIRunnable setMutex(int mutex);

    /** Sets the priority.
     * 
     * @param prior
     * @return */
    IAIRunnable setPriority(int prior);

    /** Should the task start running. if true, will call run next.
     * 
     * @return */
    boolean shouldRun();
}