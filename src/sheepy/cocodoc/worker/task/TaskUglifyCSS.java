package sheepy.cocodoc.worker.task;

import java.io.IOException;
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

public class TaskUglifyCSS extends Task {
   @Override public Action getAction () { return Action.UGLIFYCSS; }

   @Override protected Predicate<List<String>> validParam() { return isEmpty; }
   @Override protected String invalidParamMessage() { return "uglifycss() task has no parameters."; }

   @Override protected void run () {
      String txt = getBlock().getText().toString();
      if ( txt == null || txt.isEmpty() ) {
         log( Level.INFO, "Skipping uglify css, no content" );
         return;
      }

      log( Level.FINER, "Uglifying CSS {1}", Text.ellipsisWithin( txt, 8 ) );
      Object get = enginePool.get( "css" );
      try {
         if ( get instanceof RuntimeException ) {
            throwOrWarn( (RuntimeException) get );
            return;
         }

         ScriptEngine js = (ScriptEngine) get;
         js.put( "code", txt );
         log( Level.FINEST, "Running Uglify JS" );

         String result = js.eval( "uglifycss.processString( code )" ).toString();
         log( Level.FINEST, "Uglified: {0} -> {1}", txt.length(), result.length() );

         getBlock().setText( result );

      } catch ( ScriptException ex ) {
         throwOrWarn( new CocoRunError( "Cannot uglify css", ex ) );

      } finally {
         enginePool.recycle( "css", get );
      }
   }

   public static final ObjectPoolMap<String, Object> enginePool = ObjectPoolMap.create(
      ( key ) -> {
         ScriptEngine js = new ScriptEngineManager().getEngineByName( "nashorn" );
         try {
            js.eval( CocoUtils.getText( "js/uglifycss/uglifycss-lib.js" ) );
            return js;
         } catch ( ScriptException | IOException ex ) {
            return new CocoRunError( "Cannot load UglifyCSS", ex );
         }
      }
   );
}