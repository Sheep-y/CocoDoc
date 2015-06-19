package sheepy.cocodoc.worker.task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.logging.Level;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.CocoUtils;
import sheepy.util.Net;
import sheepy.util.Net.Console;
import sheepy.util.concurrent.ObjectPoolMap;
import sheepy.util.text.Text;

public abstract class JSTask extends Task {
   protected Function<List<String>,String> action;
   protected List<String> action_param = new ArrayList<>();

   @Override protected void run () {
      if ( ! hasParams() ) {
         log( Level.INFO, "Skipping " + getName() + "(), no parameter" );
         return;
      }
      if ( ! getBlock().hasData() ) {
         log( Level.INFO, "Skipping " + getName() + "(), no content" );
         return;
      }
   }

   protected void action() {
      if ( action != null ) {
         try {
            action_param.add( 0, getBlock().getText().toString() );
            String result = action.apply( action_param );
            if ( result != null )
               getBlock().setText( result );
         } catch ( CocoRunError ex ) {
            throwOrWarn( ex );
         } finally {
            action = null;
            action_param.clear();
         }
      } else {
         if ( ! action_param.isEmpty() ) {
            throwOrWarn( new CocoRunError( getName() + "() parameters missing action: " + Text.toString( action_param ) ) );
            action_param.clear();
         }
      }
   }

   /**
    * Console bridge
    */
   public static class LogConsole implements Console {
      private final Task owner;
      public LogConsole ( Task owner ) {
         this.owner = owner;
      }
      @Override public void handle ( Level level, Object args ) {
         owner.log( level, Objects.toString( args ) );
      }
   }

   protected static class Context {
      String input;
      ScriptEngine js;
      List<String> params;
      public Context(String input, ScriptEngine js, List<String> params) {
         this.input = input;
         this.js = js;
         this.params = params;
      }
   }

   protected String run ( String key, List<String> params, Function<Context,String> action ) {
      params = new ArrayList<>( params );
      final String txt = params.remove( 0 );
      log( Level.FINER, "{0} {1}", key, Text.ellipsisWithin( txt, 8 ) );

      final Object get = getPool().get( key );
      if ( get instanceof RuntimeException ) {
         throwOrWarn( (RuntimeException) get );
         return null;
      }

      ScriptEngine js = (ScriptEngine) get;
      try {
         log( Level.FINEST, "{0}: Loaded, now processing {1} chars", key, txt.length() );
         js.put( "console", new LogConsole( this ) );
         js.put( "code", txt );
         String result = action.apply( new Context( txt, js, params ) );
         log( Level.FINEST, "{0}: {1} -> {2}", key, txt.length(), result.length() );
         return result;

      } catch ( CocoRunError ex ) {
         throwOrWarn( ex );
         return null;

      } finally {
         getPool().recycle( key, get );
      }
   }

   protected static ScriptEngine newJS () {
      ScriptEngine js = new ScriptEngineManager().getEngineByName( "nashorn" );
      js.put( "console", Net.defaultConsole() );
      return js;
   }

   protected static void loadJS ( ScriptEngine js, String path ) throws ScriptException, IOException {
      if ( new File( path ).canRead() )
         js.eval( "load('" + path + "')" );
      else {
         String txt = CocoUtils.getText( path );
         if ( txt == null ) throw new CocoRunError( "Cannot load " + path + "." );
         js.eval( txt );
      }
   }

   protected abstract ObjectPoolMap<String, Object> getPool();
}