package sheepy.cocodoc.ui;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TreeItem;
import sheepy.cocodoc.CocoObserver;

/**
 * CocoObserver entity for JavFX (e.g. TreeTableView)
 */
public abstract class ObserverEntity implements CocoObserver {

   public class Log {
      public final long time;
      public final String message;
      public Log ( String message ) {
         time = System.nanoTime();
         this.message = message;
      }
      public long getTime() { return Math.round( time - baseTime ) / 1000_000; }
      public String getMessage() { return message; }
   }

   private final List<Log> logList = new ArrayList<>();

   private final StringProperty status = new SimpleStringProperty( this, "Waiting" );
   public StringProperty statusProperty() { return status; }
   private void setStatus( String value ) { statusProperty().set( value ); }

   private final StringProperty name = new SimpleStringProperty( this, "" );
   public StringProperty nameProperty() { return name; }
   @Override public CocoObserver setName( String value ) { nameProperty().set( value ); return this; }

   private String error;
   private final StringProperty message = new SimpleStringProperty( this, "" );
   public StringProperty messageProperty() { return message; }

   public List<Log> getLogs () { return new ArrayList<>( logList ); }

   @Override public CocoObserver log ( String value ) {
      Log log = new Log( value );
      synchronized ( logList ) {
         logList.add( log );
         if ( error != null ) return this; // Do not set message if has error
      }
      messageProperty().set( value );
      return this;
   }

   @Override public CocoObserver error ( String value ) {
      Log log = new Log( value );
      synchronized ( logList ) {
         logList.add( log );
         error = value;
      }
      messageProperty().set( value );
      return this;
   }

   protected final TreeItem<ObserverEntity> node = new TreeItem( this );
   protected volatile long baseTime;
   protected volatile long startTime;
   protected volatile long endTime;

   protected boolean isStarted() { return startTime != 0; }
   protected boolean isDone() { return endTime != 0; }

   public ObserverEntity ( String name ) {
      this.name.set( name );
   }

   @Override public void start ( long baseTime ) {
      if ( isStarted() ) return;
      startTime = System.nanoTime();
      this.baseTime = baseTime;
      setStatus( "Running" );
   }

   private static final NumberFormat timeFormatter = new DecimalFormat("#0.00");
   @Override public void done () {
      if ( isDone() ) return;
      this.endTime = System.nanoTime();
      setStatus( "Done (" + timeFormatter.format( ( endTime-startTime ) / 1000_000_000.00 ) + " s)" );
   }
}