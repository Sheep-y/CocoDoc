package sheepy.cocodoc.worker.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

public class TaskJS extends JSTask {
   @Override public Action getAction () { return Action.JS; }

   private static final String[] validParams = new String[]{ "es5", "babel", "minify", "uglyifyjs", "consolidate", "-mangle" };
   private static final Predicate<List<String>> validate = nonEmpty.and( onlyContains( Arrays.asList( validParams ) ) ).and( noDuplicate() );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "js() task parameters must be 'es5', 'minify', 'consolidate','-mangle'."; }

   @Override protected void run () {
      super.run();
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

   public String babel ( List<String> params ) {
      return run( "Babel", params, context -> {
         ScriptEngine js = context.js;
         try {
            return js.eval( "babel.transform( code, {ast:false,compact:false,comments:true,nonStandard:false}).code" ).toString();

         } catch ( ScriptException ex ) {
            throw new CocoRunError( "Cannot convert data to ES5", ex );
         }
      });
   }

   public String uglifyJS( List<String> params ) {
      return run( "UglifyJS", params, context -> {
         ScriptEngine js = context.js;
         try {
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
            return js.eval( "pro.gen_code( pro.ast_squeeze( ast ) )" ).toString();

         } catch ( ScriptException ex ) {
            throw new CocoRunError( "Cannot compress data as js", ex );
         }
      } );
   }

   @Override protected ObjectPoolMap<String, Object> getPool() { return enginePool; }
   protected static final ObjectPoolMap<String, Object> enginePool = ObjectPoolMap.create(
      ( key ) -> {
         ScriptEngine js = new ScriptEngineManager().getEngineByName( "nashorn" );
         try {
            switch ( key ) {
               case "Babel":
                  loadBabel( js );
                  break;

               case "UglifyJS":
                  loadJS( js, "res/uglifyjs/lib/parse-js.js" );
                  loadJS( js, "res/uglifyjs/lib/process.js"  );
                  loadJS( js, "res/uglifyjs/lib/squeeze-more.js" );
                  loadJS( js, "res/uglifyjs/lib/consolidator.js" );
                  js.eval( "var jsp = uglifyjs.jsp, pro = uglifyjs.pro;" );
                  break;
            }
            return js;
         } catch ( ScriptException | IOException ex ) {
            return new CocoRunError( "Cannot load " + key, ex );
         }
      } );

   /*******************************************************************************************************************/
   // Babel support.  Babel is difficult to support.  Really.

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