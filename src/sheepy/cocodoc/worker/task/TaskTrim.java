package sheepy.cocodoc.worker.task;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.regex.Matcher;
import sheepy.cocodoc.CocoParseError;
import sheepy.cocodoc.CocoUtils;
import static sheepy.util.collection.CollectionPredicate.onlyContains;

public class TaskTrim extends Task {

   @Override public Action getAction () { return Action.TRIM; }

   private static final String[] validParams = new String[]{ "css","html","js","xml","ws","line","crlf","lf","oneline" };
   private static final Predicate<List<String>> validate = nonEmpty.and( onlyContains( Arrays.asList( validParams ) ) );
   @Override protected Predicate<List<String>> validParam() { return validate; }
   @Override protected String invalidParamMessage() { return "trim() task should have one or more of " + String.join( ",", validParams ) + ". Actual: {0}"; }

   private static String WsTrim = "([^\\S\r\n])[^\\S\r\n]+"; // TODO: Rewrite with proper parser
   private static String WsReplace = "$1";

   private static String LineTrim = "(^|\n)\\s+|\\s+([\r\n]|$)"; // TODO: Rewrite with proper parser
   private static String LineReplace = "$1$2";

   private static String CssTrim = "/\\*.*?\\*/"; // I don't think we need rewrite to avoid values and filenames.
   private static String JsTrim = "/\\*.*?\\*/|(?<=^|[\\s{};])(?<!:)//[^\n]*(?=\n|$)"; // TODO: Replace with a JS parser
   private static String HtmlTrim = "<!--.*?-->"; // TODO: Replace with HTML parser to avoid script, template etc

   @Override protected void run () {
      if ( ! hasParams() ) {
         log( Level.INFO, "Skipping, no parameter" );
         return;
      }
      log( Level.FINER, "Trimming text" );

      String text = getBlock().getText().toString();
      int startLen = text.length();
      for ( String e : getParams() ) { // TODO: Rewrite and use this as general parser
         log( Level.FINEST, "Trimming {0} text", e );
         switch ( e.toLowerCase() ) {
            case "css"  : text = replace( text, CssTrim , "" ); break;
            case "js"   : text = replace( text, JsTrim  , "" ); break;
            case "html" : // Fallthrough
            case "xml"  : text = replace( text, HtmlTrim, "" ); break;
            case "ws"   : text = replace( text, WsTrim  , WsReplace   ); break; // See oneline
            case "line" : text = replace( text, LineTrim, LineReplace ); break; // See oneline
            case "crlf" : text = replace( text, "\r?\n" , "" ); break; // See oneline
            case "lf"   : text = replace( text, "\n"    , "" ); break;
            case "oneline" :
               text = replace( text, LineTrim, LineReplace ); // Line
               text = replace( text, WsTrim  , WsReplace   ); // WS
               text = replace( text, "\r?\n" , "" ); // CRLF
               break;
            default :
               throwOrWarn( new CocoParseError( "Unknown trim parameter: " + e ) );
         }
      }
      getBlock().setText( text );
      log( Level.FINEST, "Trimmed {0} characters to {1}", startLen, text.length() );
   }

   private static String replace ( String text, String pattern, String replacement ) {
      Matcher m = CocoUtils.tagPool.get( pattern ).reset( text );
      String result = m.replaceAll( replacement );
      CocoUtils.tagPool.recycle( pattern, m );
      return result;
   }

}