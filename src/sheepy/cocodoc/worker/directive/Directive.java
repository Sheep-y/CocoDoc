package sheepy.cocodoc.worker.directive;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import sheepy.cocodoc.CocoObserver;
import sheepy.cocodoc.CocoOption;
import sheepy.cocodoc.CocoParseError;
import sheepy.cocodoc.worker.Block;
import static sheepy.cocodoc.worker.directive.Directive.Action.END;
import static sheepy.cocodoc.worker.directive.Directive.Action.INLINE;
import static sheepy.cocodoc.worker.directive.Directive.Action.OUTPUT;
import static sheepy.cocodoc.worker.directive.Directive.Action.START;
import sheepy.cocodoc.worker.task.Task;
import sheepy.util.collection.NullData;
import sheepy.util.text.Text;

public abstract class Directive {
   private static final Logger log = Logger.getLogger( Directive.class.getName() );
   static {
      log.setLevel( Level.ALL );
   }

   public enum Action {
      INLINE,
      POSTPROCESS,
      OUTPUT,
      START,
      END,
   };

   public static Directive create ( String action, List<Task> tasks ) {
      if ( action == null ) action = "";
      action = action.trim().toUpperCase();
      if ( action.isEmpty() ) action = "INLINE";
      try {
         return create( Action.valueOf( action ), tasks );
      } catch ( IllegalArgumentException ex ) {
         return createMapped( action.toLowerCase(), tasks );
      }
   }

   public static Directive create ( Action action, List<Task> tasks ) {
      switch ( action ) {
         case INLINE:
            return new DirInline( action, tasks );
         case POSTPROCESS:
            return new DirPostProcess( action, tasks );
         case OUTPUT:
            return new DirOutput( action, tasks );
         case START:
            return new DirStart( action, tasks );
         case END:
            return new DirEnd( action, tasks );
         default:
            throw new UnsupportedOperationException( "Not implemented" );
      }
   }

   private static Directive createMapped ( String action, List<Task> tasks ) {
      if ( tasks == null ) tasks = new ArrayList<>(4);
      switch ( action ) {
         case "image"  :
            tasks.add( Task.create( Task.Action.POSITION, "noerr", "replace src of the <img> before this" ) );
            // Fall through
         case "datauri":
            tasks.add( Task.create( Task.Action.ENCODE, "base64" ) );
            tasks.add( Task.create( Task.Action.PREFIX, "${auto-datauri}" ) );
            break;
         case "script" :
            tasks.add( Task.create( Task.Action.DELETE, "noerr", "src of this" ) );
            break;
         case "css" :
            tasks.add( Task.create( Task.Action.DELETE, "noerr", "the <link>[href][rel$=stylesheet] before this" ) );
            break;
         default:
            throw new CocoParseError( "Unknown coco process: " + action );
      }
      return create( "INLINE", NullData.nullIfEmpty( tasks ) );
   }

   /*****************************************************************************************************/

   private Action action;
   private Block block;
   private List<Task> tasks;
   private CocoObserver observer;
   private CharSequence content;
   private static final Formatter logFormatter = new SimpleFormatter();

   public Directive ( Action action, List<Task> tasks ) {
      this.action = action;
      this.tasks = tasks;
      if ( tasks != null )
         for ( Task e : tasks )
            e.setDirective( this );
   }

   public Action getAction () { return action; }

   public List<Task> getTasks () { return NullData.copy( tasks ); }
   public Block getBlock () { return block; }
   public void setBlock ( Block block ) { this.block = block; }

   public CharSequence getContent () { return content; }
   public Directive setContent ( CharSequence content ) { this.content = content; return this; }

   public CocoObserver getObserver () { return observer; }
   public Directive setObserver ( CocoObserver observer ) { this.observer = observer; return this; }

   public LogRecord observe ( Level level, String message, Object subject, Object ... parameter ) {
      if ( subject instanceof String || subject instanceof Number )
         throw new IllegalArgumentException( "Wrong subject. Expected Directive, Block, Task, etc." );
      int len = parameter.length;
      Object[] param = Arrays.copyOf( parameter, len + 2 );
      param[ len   ] = subject;
      param[ len+1 ] = Thread.currentThread().getName();

      LogRecord rec = new LogRecord( level, message );
      rec.setParameters( param );
      if ( getObserver() != null && level.intValue() >= CocoOption.log_level_gui ) {
         if ( rec.getLevel().intValue() < Level.WARNING.intValue() )
            getObserver().log( logFormatter.formatMessage( rec ) );
         else
            getObserver().error( logFormatter.formatMessage( rec ) );
      }
      rec.setMessage( message + " << {" + len + "} @ {" + (len+1) + "}" );
      return rec;
   }

   public void log ( Level level, String message, Object subject, Object ... parameter ) {
      log.log( observe( level, message, subject, parameter ) );
   }

   protected CocoObserver branchObserver ( Block parent, String name ) {
      if ( parent != null && getObserver() == null ) {
         setObserver( parent.getDirective().getObserver().newNode( name ) );
      }
      return getObserver();
   }

   @Override public String toString() {
      return "<?coco-" + getAction().name().toLowerCase() + " " + Text.toString( " ", getTasks() ) + " ?>";
   }

   /*****************************************************************************************************/

   public abstract Directive start( Block parent );

   public abstract Block get() throws InterruptedException;

}