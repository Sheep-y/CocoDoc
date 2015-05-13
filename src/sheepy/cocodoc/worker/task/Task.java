package sheepy.cocodoc.worker.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import sheepy.cocodoc.CocoParseError;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.directive.Directive;
import sheepy.util.Text;
import sheepy.util.collection.CollectionPredicate;
import sheepy.util.collection.NullData;

/* A normalised action, e.g. prefix({auto-datauri}), encode(base64), or position(src of the img before) */
public abstract class Task {
   private static final Logger log = Logger.getLogger( Task.class.getSimpleName() );
   static {
      log.setLevel( Level.ALL );
   }
   public void log ( Level level, String message, Object ... parameter ) {
      log.log( getDirective().observe( level, message, this, parameter ) );
   }

   protected static final Predicate<List<String>> isEmpty = CollectionPredicate.isEmpty();
   protected static final Predicate<List<String>> nonEmpty = CollectionPredicate.hasItem();

   public enum Action {
      FILE,
      DELETE,
      POSITION,

      BINARY,
      TEXT,
      CDATA,
      COCO,
      DEFLATE,
      DECODE,
      ENCODE,
      PREFIX,
      POSTFIX,
      TEST,
      TRIM,
      VAR
   }

   public static Task create ( String task ) {
      return create( task, null );
   }
   public static Task create ( String task, List<String> params ) {
      String[] p = NullData.nullOrArray( params, String[]::new );
      String action = task.trim().toUpperCase();
      if ( action.equals( "CHARSET" ) ) {
         return create( Action.TEXT, p ); // Alias of "text"
      } else try {
         return create( Action.valueOf( action ), p );
      } catch ( IllegalArgumentException ex ) {
         throw new CocoParseError( "Unknown task: " + task );
      }
   }

   public static Task create ( Action task, String ... params ) {
      Task result = null;
      switch ( task ) {
         case DELETE  : result = new TaskDelete(); break;
         case POSITION: result = new TaskPosition(); break;
         case COCO    : result = new TaskCoco(); break;
         case CDATA   : result = new TaskCData(); break;
         case DEFLATE : result = new TaskDeflate(); break;
         case ENCODE  : result = new TaskEncode(); break;
         case FILE    : result = new TaskFile(); break;
         case PREFIX  : result = new TaskPrefix(); break;
         case POSTFIX : result = new TaskPostfix(); break;
         case TEXT    : result = new TaskText(); break;
         case TEST    : result = new TaskTest(); break;
         case TRIM    : result = new TaskTrim(); break;
         case VAR     : result = new TaskVar(); break;
         default      : throw new UnsupportedOperationException( "Unimplemented task: " + task );
      }
      return result.addParam( params );
   }

   public static boolean isQuoted ( String param ) {
      if ( param == null || param.isEmpty() ) return false;
      return param.charAt(0) == '"' || param.charAt(0) == '\'';
   }

   public static String unquote ( String param ) {
      if ( param == null || ! isQuoted( param ) ) return param;
      return param.substring( 1, param.length()-1 ).replaceAll( "\\'" , "'" ).replaceAll( "\\\"", "\"" );
   }

   public static String quote ( String param ) {
      if ( param.indexOf( '"'  ) >= 0 ) return "'" + param.replace( "'", "\\'" ) + "'";
      return '"' + param.replace( "\"", "\\\"" ) + '"';
   }

   /*****************************************************************************************************/

   private Directive owner;
   protected boolean throwError = true;

   public void process () {
      try {
         run();
      } catch ( RuntimeException ex ) {
         throwOrWarn( ex );
      }
   }

   public abstract Action getAction ();
   protected abstract void run ();

   public Directive getDirective() { return owner; }
   public Block getBlock() { return owner == null ? null : owner.getBlock(); }
   public Task setDirective( Directive owner ) {
      if ( this.owner != null ) throw new IllegalStateException( "Direction alread set" );
      this.owner = owner;
      return this;
   }

   public boolean isThrowError () { return throwError; }
   public void setThrowError( boolean throwError ) { this.throwError = throwError; }
   public <T extends Exception> void throwOrWarn ( T ex ) throws T {
      log( Level.WARNING, ex.getMessage() );
      if ( isThrowError() ) throw ex;
      else log.warning( ex.getLocalizedMessage() );
   }

   // Errors should be thrown, warnings should be logged.
   @SuppressWarnings("empty-statement")
   public void init () {
      if ( owner == null ) throw new IllegalStateException( "Task not owned by a directive" );
      if ( hasParams() ) {
         if ( params.remove( "noerr" ) ) throwError = false;
         while ( params.remove( "noerr" ) );
         while ( params.remove( null ) );
      }
      // Validate parameters
      Predicate<List<String>> validate = validParam();
      if ( validate != null )
         if ( ! validate.test( params ) )
            log.log( Level.WARNING, invalidParamMessage(), this );
   }
   protected abstract Predicate<List<String>> validParam();
   protected String invalidParamMessage() { return "Incorrect or non-effective parameters: {0}"; };

   List<String> params;
   public boolean hasParams () { return ! NullData.isEmpty( params ); }
   public List<String> getParams () { return NullData.copy( params ); }
   public String getParam ( int index ) { return NullData.get( params, index ); }
   public String getParamText () { return Text.toString( ",", getParams(), Task::quote ); }
   public Task addParam ( String ... param ) {
      if ( param != null && param.length > 0 ) {
         if ( params == null ) params = new ArrayList<>();
         params.addAll( Arrays.asList( param ) );
      }
      return this;
   }

   @Override public String toString () {
      String task = getAction().toString().toLowerCase();
      if ( hasParams() ) task = task + "(" + getParamText() + ")";
      return task;
   }
}