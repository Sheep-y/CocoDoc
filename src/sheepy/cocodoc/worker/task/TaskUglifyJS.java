package sheepy.cocodoc.worker.task;

import java.io.File;
import java.io.FileInputStream;
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
import sheepy.util.concurrent.ObjectPoolMap;
import sheepy.util.text.Text;

public class TaskUglifyJS extends Task {
   @Override public Action getAction () { return Action.FILE; }

   @Override protected Predicate<List<String>> validParam() { return isEmpty; }
   @Override protected String invalidParamMessage() { return "uglifyjs() task parameters is not implemented."; }

   @Override protected void run () {
      String txt = getBlock().getText().toString();
      if ( txt == null || txt.isEmpty() ) {
         log( Level.INFO, "Skipping, no content" );
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
         log( Level.FINEST, "Uglifyer initiated" );

         String result = js.eval( "pro.gen_code( pro.ast_squeeze( pro.ast_mangle( jsp.parse( code ) ) ) )" ).toString();
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
         ScriptEngine js = new ScriptEngineManager().getEngineByName( "nashorn" );
         try {
            js.eval( CocoUtils.getText( "uglifyjs/lib/parse-js.js" ) );
            js.eval( CocoUtils.getText( "uglifyjs/lib/process.js" ) );
            js.eval( CocoUtils.getText( "uglifyjs/lib/squeeze-more.js" ) );
            js.eval( "var jsp = uglifyjs.jsp, pro = uglifyjs.pro;" );
            return js;
         } catch ( ScriptException | IOException ex ) {
            return new CocoRunError( "Cannot load UglifyJS", ex );
         }
      }
   );
}