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
import javax.script.ScriptException;
import sheepy.cocodoc.CocoParseError;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.CocoUtils;
import static sheepy.util.collection.CollectionPredicate.noDuplicate;
import static sheepy.util.collection.CollectionPredicate.onlyContains;
import sheepy.util.concurrent.ObjectPoolMap;

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
         ScriptEngine js = newJS();
         try {
            switch ( key ) {
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
}