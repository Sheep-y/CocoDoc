/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sheepy.cocodoc.worker;

import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicLong;

public class BlockStats {

   private final Block block;

   private ZonedDateTime btime = null;
   private ZonedDateTime mtime = null;       // Last modified time of this block
   private ZonedDateTime child_mtime = null; // Last modified time of this block and all children
   private final AtomicLong in_bytes = new AtomicLong(0);
   private final AtomicLong out_bytes = new AtomicLong(0);
   private final AtomicLong total_in_bytes = new AtomicLong(0);
   private final AtomicLong total_out_bytes = new AtomicLong(0);

   public BlockStats(Block block) {
      this.block = block;
      if ( block.getParent() == null ) btime = ZonedDateTime.now();
   }

   public Block getBlock() {
      return block;
   }

   void cloneParentStat( BlockStats stats ) {
      this.mtime = this.child_mtime = stats.mtime;
   }

   private BlockStats getParent() {
      Block parent = getBlock().getParent();
      if ( parent == null )
         return null;
      return parent.stats();
   }

   public long getInBytes () { return in_bytes.longValue(); }
   public void addInBytes ( long bytes ) { in_bytes.addAndGet( bytes ); addTotalInBytes( bytes ); }

   public long getTotalInBytes () { return total_in_bytes.longValue(); }
   private void addTotalInBytes  ( long bytes ) {
      BlockStats parent = getParent();
      if ( parent == null ) return;
      parent.in_bytes.addAndGet( bytes );
      parent.addTotalInBytes( bytes );
   }

   public long getOutBytes () { return out_bytes.longValue(); }
   public void addOutBytes ( long bytes ) { out_bytes.addAndGet( bytes ); addTotalOutBytes( bytes ); }

   public long getTotalOutBytes () { return total_out_bytes.longValue(); }
   private void addTotalOutBytes  ( long bytes ) {
      BlockStats parent = getParent();
      if ( parent == null ) return;
      parent.out_bytes.addAndGet( bytes );
      parent.addTotalOutBytes( bytes );
   }

   public synchronized ZonedDateTime getBtime() { return btime; }
   public synchronized ZonedDateTime getMtime() { return mtime; }
   public synchronized ZonedDateTime getChildMtime() { return child_mtime; }

   public void setMTime ( ZonedDateTime time ) {
      if ( time == null ) return;
      synchronized( this ) {
         if ( mtime == null || time.isAfter( mtime ) )
            mtime = time;
      }
      setChildMTime( time );
   }

   private void setChildMTime ( ZonedDateTime time ) {
      if ( time == null ) return;
      synchronized( this ) {
         if ( child_mtime == null || time.isAfter( child_mtime ) )
            child_mtime = time;
      }
      if ( getParent() == null ) return;
      getParent().setChildMTime( time );
   }

   public ZonedDateTime getBuildTime() {
      if ( getParent() == null ) synchronized( this ) {
         return btime;
      } else
         return block.getRoot().stats().getBuildTime();
   }
   public synchronized ZonedDateTime getModifiedTime() { return child_mtime; }
   public synchronized ZonedDateTime getLocalModifiedTime() { return mtime; }

}