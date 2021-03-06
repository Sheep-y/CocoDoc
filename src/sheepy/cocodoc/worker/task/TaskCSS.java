package sheepy.cocodoc.worker.task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import sheepy.cocodoc.CocoRunError;
import static sheepy.cocodoc.worker.task.Task.nonEmpty;
import static sheepy.util.collection.CollectionPredicate.noDuplicate;
import static sheepy.util.collection.CollectionPredicate.onlyContains;
import sheepy.util.concurrent.ObjectPoolMap;

public class TaskCSS extends JSTask {
   @Override public Action getAction () { return Action.CSS; }

   private static final String[] validParams = new String[]{ "less", "minify", "uglyifycss" };
   private static final Predicate<List<String>> validate = nonEmpty.and( onlyContains( Arrays.asList( validParams ) ) ).and( noDuplicate() );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "css() task takes only 'less' or 'minify' as parameter."; }

   @Override protected void run () {
      super.run();
      List<String> params = new ArrayList( getParams() );
      while ( ! params.isEmpty() ) {
         String p = params.remove( 0 ).toLowerCase();
         switch ( p ) {
            case "less":
               action();
               action = this::less;
               break;
            /*case "sass":
               action();
               action = this::sass;
               break;*/
            case "minify":
            case "uglyifycss":
               action();
               action = this::uglifyCSS;
               break;
            default:
               action_param.add( p );
         }
      }
      action();
   }

   public String less ( List<String> params ) {
      return run( "Less", params, context -> {
         ScriptEngine js = context.js;
         try {
            js.eval( "var result; less.render( code, { async: false }, function(err,data){ result = err || data.css; } );" );
            String result = js.get("result").toString();
            if ( result.startsWith( "Error" ) ) throw new CocoRunError( "Less " + result );
            return result;
         } catch ( ScriptException ex ) {
            throw new CocoRunError( "Cannot run Less CSS", ex );
         }
      } );
   }

   /*
   public String sass ( List<String> params ) {
      return run( "Sass", params, context -> {
         ScriptEngine js = context.js;
         try {
            js.eval( "var result;\nSass.compile( code, {}, function(data){ console.log( data ); } );" );
            //Object result = js.eval( "result.message" );
            //if ( result != null ) throw new CocoRunError( result.toString() );
            //return js.eval( "result.text" ).toString();
            return "";
         } catch ( ScriptException ex ) {
            throw new CocoRunError( "Cannot run Sass CSS", ex );
         }
      } );
   }
   */

   public String uglifyCSS ( List<String> params ) {
      return run( "UglifyCSS", params, context -> {
         try {
            return context.js.eval( "uglifycss.processString( code )" ).toString();
         } catch ( ScriptException ex ) {
            throw new CocoRunError( "Cannot Uglify CSS", ex );
         }
      } );
   }

   @Override protected ObjectPoolMap<String, Object> getPool() { return enginePool; }
   protected static final ObjectPoolMap<String, Object> enginePool = ObjectPoolMap.create(
      ( key ) -> {
         ScriptEngine js = newJS();
         try {
            switch ( key ) {
               case "Less" :
                  // Dummy browser objects to get Less working
                  js.eval( "var window = this;" );
                  js.eval( "var location = { protocol: '' }" );
                  js.eval( "var document = { getElementsByTagName: function( tag ){ return tag == 'script' ? [ { dataset: {} } ] : []; } }; " );
                  // Real library code
                  loadJS( js, "res/less/less.js" );
                  break;

               case "Sass" :
                  // Does not work. Requires read(), XmlHttpRequest, setTimeout, and perhaps other things that Nashorn does not have
                  loadJS( js, "res/sass/sass.sync.js" );
                  break;

               case "UglifyCSS" :
                  loadJS( js, "res/uglifycss/uglifycss-lib.js" );
                  break;
            }
            return js;
         } catch ( ScriptException | IOException ex ) {
            return new CocoRunError( "Cannot load " + key, ex );
         }
      } );
}