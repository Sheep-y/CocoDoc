package sheepy.cocodoc.worker.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.CocoUtils;
import static sheepy.util.collection.CollectionPredicate.noDuplicate;
import static sheepy.util.collection.CollectionPredicate.onlyContains;
import sheepy.util.concurrent.ObjectPoolMap;
import sheepy.util.text.Text;

public class TaskJS extends Task {
   @Override public Action getAction () { return Action.JS; }

   private static final String[] validParams = new String[]{ "es5", "babel", "minify", "uglyifyjs", "consolidate","-mangle" };
   private static final Predicate<List<String>> validate = nonEmpty.and( onlyContains( Arrays.asList( validParams ) ) ).and( noDuplicate() );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "js() task parameters must be 'es5', 'minify', 'consolidate','-mangle'."; }

   private Consumer<List<String>> action;
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
            action.accept( action_param );
         } catch ( CocoRunError ex ) {
            throwOrWarn( ex );
         } finally {
            action = null;
            action_param.clear();
         }
      } else {
         if ( ! action_param.isEmpty() ) {
            throwOrWarn( new CocoRunError( "Parameters missing action: " + Text.toString( action_param ) ) );
            action_param.clear();
         }
      }
   }


   private void babel ( List<String> params ) {
      String txt = getBlock().getText().toString();
      log( Level.FINER, "Convert JS {1} to ES5 using Babel", Text.ellipsisWithin( txt, 8 ) );
      Object get = enginePool.get( "babel" );
      try {
         if ( get instanceof RuntimeException ) {
            throwOrWarn( (RuntimeException) get );
            return;
         }

         ScriptEngine js = (ScriptEngine) get;
         js.put( "code", txt );

         log( Level.FINEST, "Babel JS: Loaded, now transforming" );
         String result = js.eval( "babel.transform( code, {nonStandard:false,ast:false,comments:true}).code" ).toString();

         log( Level.FINEST, "Babel JS: {0} -> {1}", txt.length(), result.length() );
         getBlock().setText( result );

      } catch ( ScriptException ex ) {
         throwOrWarn( new CocoRunError( "Cannot convert data to ES5", ex ) );

      } finally {
         enginePool.recycle( "js", get );
      }
   }

   private void uglifyJS( List<String> params ) {
      String txt = getBlock().getText().toString();
      log( Level.FINER, "Uglifying JS {1}", Text.ellipsisWithin( txt, 8 ) );
      Object get = enginePool.get( "uglifyjs" );
      try {
         if ( get instanceof RuntimeException ) {
            throwOrWarn( (RuntimeException) get );
            return;
         }

         ScriptEngine js = (ScriptEngine) get;
         js.put( "code", txt );

         log( Level.FINEST, "Uglify JS: Loaded, now parsing" );
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
         getBlock().setText( result );

      } catch ( ScriptException ex ) {
         throwOrWarn( new CocoRunError( "Cannot compress data as js", ex ) );

      } finally {
         enginePool.recycle( "js", get );
      }
   }

   public static final ObjectPoolMap<String, Object> enginePool = ObjectPoolMap.create(
      ( key ) -> {
         ScriptEngine js = new ScriptEngineManager().getEngineByName( "nashorn" );
         try {
            if ( key.equals( "babel" ) ) {
               js.eval( CocoUtils.getText( "res/babel/babel.min.js" ) );
            } else if ( key.equals( "uglifyjs" ) ) {
               js.eval( CocoUtils.getText( "res/uglifyjs/lib/parse-js.js" ) );
               js.eval( CocoUtils.getText( "res/uglifyjs/lib/process.js" ) );
               js.eval( CocoUtils.getText( "res/uglifyjs/lib/squeeze-more.js" ) );
               js.eval( CocoUtils.getText( "res/uglifyjs/lib/consolidator.js" ) );
               js.eval( "var jsp = uglifyjs.jsp, pro = uglifyjs.pro;" );
            }
            return js;
         } catch ( ScriptException | IOException ex ) {
            return new CocoRunError( "Cannot load " + key , ex );
         }
      }
   );
}