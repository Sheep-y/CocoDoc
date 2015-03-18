package sheepy.cocodoc.worker.task;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.UNICODE_CHARACTER_CLASS;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.Directive;
import sheepy.cocodoc.worker.error.CocoParseError;
import sheepy.util.Text;
import sheepy.util.collection.CollectionPredicate;
import sheepy.util.collection.NullData;
import sheepy.util.concurrent.CacheMap;
import sheepy.util.concurrent.ObjectPoolMap;

/* A normalised action, e.g. prefix({auto-datauri}), encode(base64), or position(src of the img before) */
public abstract class Task {
   protected static final Logger log = Logger.getLogger( Task.class.getSimpleName() );
   static {
      log.setLevel( Level.ALL );
   }

   protected static final Predicate<List<String>> isEmpty = CollectionPredicate.isEmpty();
   protected static final Predicate<List<String>> nonEmpty = CollectionPredicate.hasItem();
   public static final Charset UTF16 = Charset.forName("UTF-16");
   public static final Charset UTF8 = Charset.forName("UTF-8");

   public enum Action {
      FILE,
      DELETE,
      POSITION,

      BINARY,
      TEXT,
      COCO,
      DEFLATE,
      DECODE,
      ENCODE,
      PREFIX,
      POSTFIX,
      TRIM,
      WRAP
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
         case COCO    : result = new TaskCoco(); break;
         case DEFLATE : result = new TaskDeflate(); break;
         case ENCODE  : result = new TaskEncode(); break;
         case FILE    : result = new TaskFile(); break;
         case PREFIX  : result = new TaskPrefix(); break;
         case TEXT    : result = new TaskText(); break;
         case TRIM    : result = new TaskTrim(); break;
         default      : throw new UnsupportedOperationException( "Unimplemented task: " + task );
      }
      return result.addParam( params );
   }

   public static boolean isQuoted ( String param ) {
      if ( param == null || param.isEmpty() ) return false;
      return param.charAt(0) == '"' || param.charAt(0) == '\'';
   }

   public static String unquote ( String param ) {
      if ( ! isQuoted( param ) ) return param;
      if ( param.charAt( 0 ) == '"'  ) return Text.unquote( param, '"' , str -> str.replaceAll( "\"\"", "\"" ) );
      else                             return Text.unquote( param, '\'', str -> str.replaceAll( "''"  , "'"  ) );
   }

   /** Cache Pattern and Matcher for task reuse. */
   private static final CacheMap<String, Pattern> patternPool = CacheMap.create(
         pattern -> Pattern.compile( pattern, UNICODE_CHARACTER_CLASS | DOTALL )
      );
   public static final ObjectPoolMap<String, Matcher> tagPool = ObjectPoolMap.create(
         key -> patternPool.get( key ).matcher(""),
         v   -> v.reset("")
      );

   /*****************************************************************************************************/

   private Directive owner;
   protected boolean throwError = true;

   public void process () {
      try {
         run();
      } catch ( RuntimeException ex ) {
         if ( throwError ) throw ex;
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
         if ( ! validate.test(params ) )
            log.log( Level.WARNING, invalidParamMessage(), this );
   }
   protected abstract Predicate<List<String>> validParam();
   protected String invalidParamMessage() { return "Incorrect or non-effective parameters: {0}"; };

   List<String> params;
   public boolean hasParams () { return ! NullData.isEmpty(params ); }
   public List<String> getParams () { return NullData.copy(params ); }
   public String getParam ( int index ) { return NullData.get(params, index ); }
   public String getParamText () {
      return hasParams() ? '"' + String.join(",", params ) + '"' : "";
   }
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