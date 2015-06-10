package sheepy.cocodoc.worker.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import sheepy.cocodoc.CocoParseError;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.CocoUtils;
import static sheepy.util.collection.CollectionPredicate.noDuplicate;
import static sheepy.util.collection.CollectionPredicate.onlyContains;
import sheepy.util.concurrent.ObjectPoolMap;
import sheepy.util.text.Text;

public class TaskJS extends Task {
   @Override public Action getAction () { return Action.JS; }

   private static final String[] validParams = new String[]{ "es5", "babel", "minify", "uglyifyjs", "consolidate", "-mangle" };
   private static final Predicate<List<String>> validate = nonEmpty.and( onlyContains( Arrays.asList( validParams ) ) ).and( noDuplicate() );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "js() task parameters must be 'es5', 'minify', 'consolidate','-mangle'."; }

   private Function<List<String>,String> action;
   private List<String> action_param = new ArrayList<>();

   @Override protected void run () {
      if ( ! hasParams() ) {
         log( Level.INFO, "Skipping js(), no parameter" );
         return;
      }
      if ( ! getBlock().hasData() ) {
         log( Level.INFO, "Skipping js(), no content" );
         return;
      }

      List<String> params = new ArrayList( getParams() );
      while ( ! params.isEmpty() ) {
         String p = params.remove( 0 ).toLowerCase();
         switch ( p ) {
            case "es5":
            case "babel":
               action();
               action = this::babel;
               break;

            case "minify":
            case "uglyifyjs":
               action();
               action = this::uglifyJS;
               break;

            default:
               action_param.add( p );
         }
      }
      action();
   }

   private void action() {
      if ( action != null ) {
         try {
            action_param.add( 0, getBlock().getText().toString() );
            String result = action.apply( action_param );
            if ( result != null )
               getBlock().setText( result );
         } catch ( CocoRunError | CocoParseError ex ) {
            throwOrWarn( ex );
         } finally {
            action = null;
            action_param.clear();
         }
      } else {
         if ( ! action_param.isEmpty() ) {
            throwOrWarn( new CocoRunError( "js() parameters missing action: " + Text.toString( action_param ) ) );
            action_param.clear();
         }
      }
   }

   public String babel ( List<String> params ) {
      params = new ArrayList<>( params );
      final String txt = params.remove( 0 );
      log( Level.FINER, "Convert JS to ES5: {0}", Text.ellipsisWithin( txt, 8 ) );

      final String key = "babel";
      final Object get = enginePool.get( key );
      try {
         if ( get instanceof RuntimeException ) {
            throwOrWarn( (RuntimeException) get );
            return null;
         }

         final ScriptEngine js = (ScriptEngine) get;
         //js.put( "out", System.out );
         js.put( "console", new Console() );
         js.put( "code", txt );

         log( Level.FINEST, "Babel JS: Loaded, now transforming {0} chars", txt.length() );
         String result = js.eval( "babel.transform( code, {ast:false,compact:false,comments:true,nonStandard:false}).code" ).toString();

         log( Level.FINEST, "Babel JS: {0} -> {1}", txt.length(), result.length() );
         return result;

      } catch ( ScriptException ex ) {
         throwOrWarn( new CocoRunError( "Cannot convert data to ES5", ex ) );
         return null;

      } finally {
         enginePool.recycle( key, get );
      }
   }

   public String uglifyJS( List<String> params ) {
      params = new ArrayList<>( params );
      final String txt = params.remove( 0 );
      log( Level.FINER, "Uglifying JS: {0}", Text.ellipsisWithin( txt, 8 ) );

      final String key = "uglifyjs";
      final Object get = enginePool.get( key );
      try {
         if ( get instanceof RuntimeException ) {
            throwOrWarn( (RuntimeException) get );
            return null;
         }

         final ScriptEngine js = (ScriptEngine) get;
         js.put( "code", txt );

         log( Level.FINEST, "Uglify JS: Loaded, now parsing {0} chars", txt.length() );
         js.eval( "var ast = jsp.parse( code );" );

         if ( ! params.contains( "-mangle" ) ) {
            log( Level.FINEST, "UglifyJS: Mangling identifiers" );
            js.eval( "ast = pro.ast_mangle( ast );" );
         }

         if ( params.contains( "consolidate" ) ) {
            log( Level.FINEST, "UglifyJS: Consolidate constants" );
            js.eval( "ast = pro.ast_consolidate( ast );" );
         }

         log( Level.FINEST, "UglifyJS: Compressing" );
         String result = js.eval( "pro.gen_code( pro.ast_squeeze( ast ) )" ).toString();

         log( Level.FINEST, "Uglified: {0} -> {1}", txt.length(), result.length() );
         return result;

      } catch ( ScriptException ex ) {
         throwOrWarn( new CocoRunError( "Cannot compress data as js", ex ) );
         return null;

      } finally {
         enginePool.recycle( key, get );
      }
   }

   private static final ObjectPoolMap<String, Object> enginePool = ObjectPoolMap.create(
      ( key ) -> {
         ScriptEngine js = new ScriptEngineManager().getEngineByName( "nashorn" );
         try {
            if ( key.equals( "babel" ) ) {
               loadBabel( js );
            } else if ( key.equals( "uglifyjs" ) )
               loadUglifyJS( js );
            js.put( "console", null );
            return js;
         } catch ( CocoRunError | CocoParseError ex ) {
            return ex;
         } catch ( ScriptException | IOException ex ) {
            return new CocoRunError( "Cannot load " + key , ex );
         }
      },
      ( js ) -> {
         ( (ScriptEngine) js ).put( "console", null );
         return js;
      }
   );

   private static void loadUglifyJS ( ScriptEngine js ) throws ScriptException, IOException {
      js.eval( CocoUtils.getText( "res/uglifyjs/lib/parse-js.js" ) );
      js.eval( CocoUtils.getText( "res/uglifyjs/lib/process.js" ) );
      js.eval( CocoUtils.getText( "res/uglifyjs/lib/squeeze-more.js" ) );
      js.eval( CocoUtils.getText( "res/uglifyjs/lib/consolidator.js" ) );
      js.eval( "var jsp = uglifyjs.jsp, pro = uglifyjs.pro;" );
   }

   private static void loadBabel ( ScriptEngine js ) throws ScriptException, IOException {
      js.eval( "var modules = {};" );
      String src = CocoUtils.getText( "res/babel/babel.formatted.js" ).replaceAll( "\n[\t ]+", "\n" );

      // Extract each module
      Matcher func = Pattern.compile( "(\\d+):\\s*\\[function\\(\\w+,\\s*\\w+,\\s*\\w[^)]*\\)\\s*\\{" ).matcher( src );
      int previous = 0;
      int prevId = 0;
      while ( func.find() ) {
         // Check that we have't reached a new block (core-js)
         int thisId = Integer.parseInt( func.group( 1 ) );
         if ( thisId < prevId ) break;
         // Import last module, if any
         if ( previous != 0 )
            loadBabelFunc( js, prevId, src.substring( previous, func.start() ).trim() );
         // Current module will be imported in next loop
         previous = func.start();
         prevId = thisId;
      }
      // Last module is delimited by a manual blank line
      int next = src.indexOf( "\n\n", previous );
      if ( next < 0 ) throw new CocoParseError( "Cannot find end of last Babel module" );
      loadBabelFunc( js, prevId, src.substring( previous, next ).trim() + "," );
      // Cleanup parsed sources
      next = src.indexOf( "\n\n", next+2 );
      if ( next < 0 ) throw new CocoParseError( "Cannot find core-js module" );
      src = src.substring( next ).trim();

      // Run loader
      js.eval( CocoUtils.getText( "res/babel/babel.loader.js" ) );

      // Run dependency code
      js.eval( src );
   }

   /*******************************************************************************************************************/
   // Babel support.  Babel is difficult to support.  Really.

   /**
    * Console bridge because Babel may use console!
    */
   public class Console {
      public void log( Object args ) {
         handle( Level.FINER, args );
      }
      public void warn( Object args ) {
         handle( Level.WARNING, args );
      }
      public void error( Object args ) {
         handle( Level.SEVERE, args );
      }
      private void handle( Level level, Object args ) {
         TaskJS.this.log( level, Objects.toString( args ) );
      }
   }

   /**
    * Load babel modules.
    *
    * @param id Id of module
    * @param func Module code
    */
   private static void loadBabelFunc ( ScriptEngine js, int id, String func ) throws ScriptException {
      // Rewrite assignment and remove trailing comma
      func = "modules[" + id + "]=" + func.substring( func.indexOf( ':' )+1, func.length() - 1 ).trim();
      //System.out.println( Text.ellipsisWithin( func, 40 ) );
      if ( func.length() > 200_000 )
         loadBabelTransform( js, id, func );
      else
         js.eval( func );
   }

   /**
    * Load babel transform mapping module, the biggest function that is breaking Java's 64k method limit
    *
    * @param id Id of module
    * @param src Module code
    */
   private static void loadBabelTransform ( ScriptEngine js, int id, String src ) throws ScriptException {
      String result = src;
      js.eval( "var transforms = {}" );

      /* Caputre each transform block like the one below and put into global variable

           "abstract-expression-call": {
                   loc: null,
                   start: null,
                   range: null,
                   body: [{ ... }],
                   type: "Program",
                   end: null
           },

      */
      Matcher func = Pattern.compile( "\"?([\\w-]+)\"?\\:\\s*(\\{\\s*loc:.*?\\,\\s*type:\\s*\"Program\",[^{}]+\\})\\,?", Pattern.DOTALL ).matcher( src );
      while ( func.find() ) {
         String var = "transforms['" + func.group( 1 ) + "']";
         String code = func.group( 2 );
         js.eval( var + "=" + code );
         result = result.replaceFirst( Pattern.quote( code ), var );
      }
      if ( result.length() == src.length() ) // No match
         throw new CocoParseError( "Cannot load Babel transforms." );
      js.eval( result );
   }
}