package sheepy.cocodoc.worker.task;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.CocoUtils;
import static sheepy.util.collection.CollectionPredicate.onlyContains;
import sheepy.util.concurrent.ObjectPoolMap;
import sheepy.util.text.Text;

public class TaskUglifyJS extends Task {
   @Override public Action getAction () { return Action.UGLIFYJS; }

   private static final String[] validParams = new String[]{ "consolidate","-mangle" };
   private static final Predicate<List<String>> validate = onlyContains( Arrays.asList( validParams ) );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "uglifyjs() task parameters must be 'consolidate','-mangle'."; }

   @Override protected void run () {
      List<String> params = getParams();
      String txt = getBlock().getText().toString();
      if ( txt == null || txt.isEmpty() ) {
         log( Level.INFO, "Skipping uglify js, no content" );
         return;
      }

      log( Level.FINER, "Uglifying JS {1}", Text.ellipsisWithin( txt, 8 ) );
      Object get = enginePool.get( "js" );
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
         throwOrWarn( new CocoRunError( "Cannot uglify js", ex ) );

      } finally {
         enginePool.recycle( "js", get );
      }
   }

   public static final ObjectPoolMap<String, Object> enginePool = ObjectPoolMap.create(
      ( key ) -> {
         if ( Thread.currentThread().getPriority() < 6 ) // Uglify JS needs heavy computation
            Thread.currentThread().setPriority( 6 );
         ScriptEngine js = new ScriptEngineManager().getEngineByName( "nashorn" );
         try {
            js.eval( CocoUtils.getText( "js/uglifyjs/lib/parse-js.js" ) );
            js.eval( CocoUtils.getText( "js/uglifyjs/lib/process.js" ) );
            js.eval( CocoUtils.getText( "js/uglifyjs/lib/squeeze-more.js" ) );
            js.eval( CocoUtils.getText( "js/uglifyjs/lib/consolidator.js" ) );
            js.eval( "var jsp = uglifyjs.jsp, pro = uglifyjs.pro;" );
            return js;
         } catch ( ScriptException | IOException ex ) {
            return new CocoRunError( "Cannot load UglifyJS", ex );
         }
      }
   );
}