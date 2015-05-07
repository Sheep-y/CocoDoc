/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sheepy.cocodoc.worker;

import java.time.ZonedDateTime;

public class BlockStats {

   private final Block block;

   private ZonedDateTime btime = null;
   private ZonedDateTime mtime = null;       // Last modified time of this block
   private ZonedDateTime child_mtime = null; // Last modified time of this block and all children

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

   public ZonedDateTime getBtime() {
      return btime;
   }

   public ZonedDateTime getMtime() {
      return mtime;
   }

   public ZonedDateTime getChildMtime() {
      return child_mtime;
   }

   public void setMTime ( ZonedDateTime mtime ) {
      if ( mtime == null ) return;
      if ( this.mtime == null || mtime.isAfter( this.mtime ) )
         this.mtime = mtime;
      setChildMTime( mtime );
   }

   private void setChildMTime ( ZonedDateTime mtime ) {
      if ( mtime == null ) return;
      if ( child_mtime == null || mtime.isAfter( child_mtime ) )
         child_mtime = mtime;
      if ( getParent() == null ) return;
      getParent().setChildMTime( mtime );
   }

   public ZonedDateTime getBuildTime() {
      return getParent() == null
         ? btime
         : block.getRoot().stats().getBuildTime();
   }

   public ZonedDateTime getModifiedTime() {
      return child_mtime;
   }

   public ZonedDateTime getLocalModifiedTime() {
      return mtime;
   }

}