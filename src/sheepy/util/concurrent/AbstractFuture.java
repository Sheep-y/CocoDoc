package sheepy.util.concurrent;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Barebone implementation of <a href="https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/RunnableFuture.html">RunnableFuture</a>.
 * Just subclass and implement {@link #implRun()}.
 */
public abstract class AbstractFuture<T> implements RunnableFuture<T> {

   private enum State { INITIAL, CANCELLED, RUNNING, COMPLETED }
   private final Object stateLock = new Object();
   private State state = State.INITIAL; // Read and write should be synchronised with stateLock
   private T result;                    // ditto
   private Exception runException;      // ditto
   private Thread runningThread;      // ditto

   @Override public void run () {
      T value = null;
      synchronized( stateLock ) {
         if ( isDone() ) return;
         if ( state == State.RUNNING ) {
            try { stateLock.wait(); } catch (InterruptedException ex) {}
            return;
         }
         state = State.RUNNING;
         runningThread = Thread.currentThread();
      }
      try {
         value = implRun(); // subclass can return partial result even if interrupted (non-exception)
      } catch ( Exception ex ) {
         synchronized( stateLock ) {
            runException = ex;
         }
      } finally { synchronized( stateLock ) {
         if ( value != null ) result = value; // Assign result in synchronized block
         state = runningThread.isInterrupted() && runException == null ? State.CANCELLED : State.COMPLETED;
         runningThread = null;
         stateLock.notifyAll();
      } }
   }
   protected abstract T implRun();

             public boolean isRunning()   { synchronized( stateLock ) { return state == State.RUNNING; } }
   @Override public boolean isCancelled() { synchronized( stateLock ) { return state == State.CANCELLED; } }
   @Override public boolean isDone()      { synchronized( stateLock ) { return state == State.COMPLETED || state == State.CANCELLED; } }
   @Override public boolean cancel ( boolean mayInterruptIfRunning ) { synchronized( stateLock ) {
      if ( isDone() ) return isCancelled();
      if ( state == State.INITIAL ) {
         state = State.CANCELLED;
         stateLock.notifyAll();
         return true;
      }
      if ( mayInterruptIfRunning ) runningThread.interrupt();
      try { stateLock.wait(); } catch ( InterruptedException ex ) { throw new RuntimeException(ex); }
      return isCancelled();
   } }

   @Override public T get() throws InterruptedException, ExecutionException {
      try { return get( 0, TimeUnit.MILLISECONDS ); } catch ( TimeoutException ex ) { throw new InterruptedException(); }
   }
   @Override public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException { synchronized( stateLock ) {
      if ( ! isDone() ) {
         stateLock.wait( unit.toMillis( timeout ) );
         if ( ! isDone() ) throw new TimeoutException();
      }
      if ( isCancelled() ) throw new CancellationException();
      if ( runException != null ) throw new ExecutionException( runException );
      return result;
   } }
}