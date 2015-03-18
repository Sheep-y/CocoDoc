package sheepy.cocodoc.worker;

import java.util.ArrayList;
import java.util.List;
import static sheepy.cocodoc.worker.Directive.Action.END;
import static sheepy.cocodoc.worker.Directive.Action.INLINE;
import static sheepy.cocodoc.worker.Directive.Action.OUTPUT;
import static sheepy.cocodoc.worker.Directive.Action.START;
import sheepy.cocodoc.worker.error.CocoParseError;
import sheepy.cocodoc.worker.task.Task;
import sheepy.util.collection.NullData;

public class Directive {
   // protected static final Logger log = Logger.getLogger( Directive.class.getName() );

   public enum Action {
      INLINE,
      OUTPUT,
      START,
      END,
   };

   public static Directive create ( String action, List<Task> tasks ) {
      action = action.trim().toUpperCase();
      try {
         Action act = Action.valueOf( action );
         return new Directive( act, tasks );
      } catch ( IllegalArgumentException ex ) {
         return createMapped( action.toLowerCase(), tasks );
      }
   }

   private static Directive createMapped ( String action, List<Task> tasks ) {
      Action act;
      if ( tasks == null ) tasks = new ArrayList<>(4);
      if ( action.isEmpty() ) {
         throw new UnsupportedOperationException("TODO: Auto-detect image, script, css");
      }
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
         case "" :
            throw new UnsupportedOperationException("Not implemented, please use coco-image, coco-datauri, coco-script, or coco-css");
         default:
            throw new CocoParseError( "Unknown coco process: " + action );
      }
      return create( "INLINE", NullData.nullIfEmpty( tasks ) );
   }

   /*****************************************************************************************************/

   private Action action;
   private Block block;
   private List<Task> tasks;

   public Directive () {
      this( Action.INLINE, null );
   }

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

   public Directive start( Block context ) {
      switch ( getAction() ) {
         case START:
            new Block( context, this );
            break;
         case INLINE:
         case END:
            Worker.startBlock( new Block( context, this )  );
            break;

         case OUTPUT:
            setBlock( context );
            break;
      }
      return this;
   }

   public Block get() throws InterruptedException {
      switch ( getAction() ) {
         case START:
            block.run();
            return block;

         case INLINE:
            if ( block == null ) start( new Block( this ) );
            return Worker.getBlockResult( block );

         case OUTPUT:
            if ( block == null ) throw new IllegalStateException();
            for ( Task task : getTasks() )
               task.process();
      }
      return null;
   }

   /*****************************************************************************************************/

   CharSequence content;
   public CharSequence getContent () { return content; }
   public Directive setContent ( CharSequence content ) { this.content = content; return this; }

   @Override public String toString() {
      return "Coco:" + getAction().name().toLowerCase() + "("
            + String.join( " ", getTasks().stream().map( Object::toString ).toArray( String[]::new ) )
            + ")";
   }
}