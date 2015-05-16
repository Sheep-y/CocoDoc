package sheepy.cocodoc.worker;

import java.io.Closeable;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class BlockStats {

   private final Block block;
   private final BlockStats root;

   private final Map<String,Object> variables;
   private final ReentrantLock varLock;
   public static final String IO_BYTES_IN   = "io.in_bytes";
   public static final String IO_BYTES_OUT  = "io.out_bytes";
   public static final String TIME_BUILD    = "time.build";
   public static final String TIME_COCO     = "time.coco";
   public static final String TIME_LAST_MOD = "time.last_modified";
   public static final String TIME_NOW      = "time.now";

   public static final List<String> predefined = Arrays.asList(
         IO_BYTES_IN, IO_BYTES_OUT, TIME_BUILD, TIME_COCO, TIME_LAST_MOD, TIME_NOW );

   private final AtomicLong in_bytes = new AtomicLong(0);
   private final AtomicLong out_bytes = new AtomicLong(0);

   public BlockStats( Block block ) {
      this.block = block;
      if ( block.getParent() == null ) {
         root = this;
         variables = new HashMap<>();
         variables.put( TIME_BUILD   , ZonedDateTime.now() );
         variables.put( TIME_LAST_MOD, ZonedDateTime.now() );
         variables.put( IO_BYTES_IN  , new AtomicLong(0) );
         variables.put( IO_BYTES_OUT , new AtomicLong(0) );
         varLock = new ReentrantLock();
      } else {
         root = block.getParent().stats().root;
         variables = null;
         varLock = null;
      }
   }

   public Block getBlock () { return block; }

   public Closeable lockVar () {
      root.varLock.lock();
      return () -> root.varLock.unlock();
   }

   public boolean hasVar( String name ) {
      try ( Closeable lock = lockVar() ) {
         return root.variables.containsKey( name );
      } catch ( IOException ignore ) { return false; }
   }

   public Object getVar( String name ) {
      try ( Closeable lock = lockVar() ) {
         return root.variables.get( name );
      } catch ( IOException ignore ) { return null; }
   }

   public void setVar( String name, Object value ) {
      try ( Closeable lock = lockVar() ) {
         if ( value != null )
            root.variables.put( name, value );
         else
            root.variables.remove( name );
      } catch ( IOException ignore ) { }
   }

   private AtomicLong getLong( String name ) { return (AtomicLong) getVar( name ); }
   private ZonedDateTime getTime( String name ) { return (ZonedDateTime) getVar( name ); }

   public long getInBytes () { return in_bytes.longValue(); }
   public void addInBytes ( long bytes ) {
      in_bytes.addAndGet( bytes );
      getLong( IO_BYTES_IN ).addAndGet( bytes );
   }

   public long getOutBytes () { return out_bytes.longValue(); }
   public void addOutBytes ( long bytes ) {
      out_bytes.addAndGet( bytes );
      getLong( IO_BYTES_OUT ).addAndGet( bytes );
   }

   public void setMTime ( ZonedDateTime time ) {
      if ( time == null ) return;
      synchronized ( root.variables ) {
         ZonedDateTime gtime = getTime( TIME_LAST_MOD );
         if ( gtime == null || gtime.isBefore( time ) )
            setVar( TIME_LAST_MOD, time );
      }
   }

   public ZonedDateTime getBuildTime() { return getTime( TIME_BUILD ); }
   public ZonedDateTime getModifiedTime() { return getTime( TIME_LAST_MOD ); }
}