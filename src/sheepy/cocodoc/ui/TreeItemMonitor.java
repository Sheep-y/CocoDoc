package sheepy.cocodoc.ui;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TreeItem;
import sheepy.cocodoc.CocoMonitor;

/**
 * Bridge between CocoMonitor and TreeTableView
 */
public abstract class TreeItemMonitor implements CocoMonitor {

   private final StringProperty status = new SimpleStringProperty( this, "Waiting" );
   public StringProperty statusProperty() { return status; }
   private void setStatus( String value ) { statusProperty().set( value ); }

   private final StringProperty name = new SimpleStringProperty( this, "" );
   public StringProperty nameProperty() { return name; }
   @Override public CocoMonitor setName( String value ) { nameProperty().set( value ); return this; }

   protected final TreeItem<TreeItemMonitor> node = new TreeItem( this );
   protected volatile long startTime;
   protected volatile long endTime;

   protected boolean isStarted() { return startTime != 0; }
   protected boolean isDone() { return endTime != 0; }

   public TreeItemMonitor ( String name ) {
      this.name.set( name );
   }

   @Override public void start () {
      if ( isStarted() ) return;
      startTime = System.nanoTime();
      setStatus( "Running" );
   }

   private static final NumberFormat timeFormatter = new DecimalFormat("#0.00");
   @Override public void done () {
      if ( isDone() ) return;
      this.endTime = System.nanoTime();
      setStatus( "Done (" + timeFormatter.format( ( endTime-startTime ) / 1000_000_000.00 ) + " s)" );
   }

}
