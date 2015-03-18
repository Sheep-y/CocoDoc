/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sheepy.cocodoc.worker.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.Directive;
import sheepy.cocodoc.worker.error.CocoParseError;
import sheepy.cocodoc.worker.task.Task;
import static sheepy.cocodoc.worker.task.Task.tagPool;
import sheepy.cocodoc.worker.task.TaskFile;
import sheepy.util.Text;
import sheepy.util.collection.NullData;

public class ParserCoco {
   protected static final Logger log = Logger.getLogger( ParserCoco.class.getSimpleName() );

   private String startTag = "<\\?coco(?:-(\\w*))?"; // Must clear start_matcher when set
   private String endTag = "\\?>"; // Must clear end_matcher when set

   private static final String firstTagRegx = "<[^!>][^>]*>";
   private static final String paramRegx = "\"([^\"]|\"\")*\"|'([^']|'')*'|[^\t\r\n ,()'\"]+"; // double quoted | single quoted | plain parameter
   private static final String attrRegx = ("(\\w+)\\s* ( \\(\\s* (?:" + paramRegx + ") (?:\\s*,\\s*(?:"+paramRegx+") )* \\s* \\))?").replaceAll( " +", "" );
                                //   directive     \(          first param             , more params                   \)
   private Matcher startMatcher;
   private Matcher endMatcher;
   private Matcher attributeMatcher;
   private Matcher parameterMatcher;
   private int tagCount;

   private List<Object> resultStack;
   private String unparsed;

   public ParserCoco () {}

   public ParserCoco ( String startTag, String endTag ) {
      this.setTags( startTag, endTag );
   }

   public StringBuilder parse ( Block context ) {
      String text = context.getText().toString();
      if ( text.isEmpty() ) return null;
      log.log( Level.INFO, "Parsing coco tag in {0} characters. {1}", new Object[]{ text.length(), findFirstTag( text ) } ) ;

      parseDocument( context, text );
      clearMatchers();
      if ( resultStack == null ) return null; // No tag found

      return getTextResult();
   }

   /**************************************************************************************************************/

   /**
    * Match coco tags from given context and text.
    *
    * @param context Context of current text; used for recursive parsing.
    * @param text Text to be parsed.
    * @return Parsed result which is a mix of String and Directive.
    */
   private void parseDocument ( Block context, String text ) {
      tagCount = 0;
      Matcher start = startMatcher = tagPool.get( startTag );
      Matcher end   = endMatcher   = tagPool.get( endTag   );

      while ( start.reset( text ).find() ) {
         if ( shouldStop() ) return;
         if ( ! end.reset( text ).find( start.end() ) ) break;
         String tag = text.substring( start.start(), end.start() );

         try {
            Directive dir = parseDirective( tag, start );
            if ( dir != null ) {
               ++tagCount;
               addToResult( text.subSequence( 0, start.start() ) );
               switch ( dir.getAction() ) {
                  case START:
                     ParserCoco parser = new ParserCoco( startTag, endTag );
                     dir.setContent( text.substring( end.end() ) ).start( context );
                     dir.getBlock().setText( parser.parse( dir.getBlock() ) );
                     text = parser.unparsed;
                     addToResult( dir );
                     if ( text == null ) throw new CocoParseError( "Coco:start without Coco:end" );
                     break;
                  case END:
                     if ( context.getParent() == null ) throw new CocoParseError( "Coco:end without Coco:start" );
                     unparsed = text.substring( end.end() );
                     return; // Terminate
                  default:
                     log.log( Level.FINE, "Found coco tag {0}", dir );
                     dir.start( context );
                     addToResult( dir );
                     text = text.substring( end.end() );
               }
            }
         } catch ( CocoParseError ex ) {
            log.log( Level.WARNING, "Cannot parse coco tag {1}: {0}", new Object[]{ ex, tag } );
            addToResult( text.subSequence( 0, end.end() ) );
            text = text.substring( end.end() );
         }
         if ( text.isEmpty() ) break;
      }
      if ( tagCount <= 0 ) resultStack = null;
      else if ( ! text.isEmpty() ) addToResult( text );
   }

   private Directive parseDirective ( String tag, Matcher start ) {
      String action = start.group( 1 );
      String attrTxt = tag.substring( start.group().length() ).trim();
      List<Task> tasks = null;

      log.log( Level.FINER, "Parsing coco directive: {0} {1}", new Object[]{ action, attrTxt } );
      if ( ! attrTxt.isEmpty() ) {
         tasks = new ArrayList<>();
         String[] defaultCheck = checkDefaultParameter( attrTxt );
         if ( defaultCheck != null ) {
            log.log( Level.FINEST, "Matched parameter {1} for default task {0}", new Object[]{ action, defaultCheck[0] } );
            tasks.add( new TaskFile().addParam( defaultCheck[0] ) );
            attrTxt = defaultCheck[1];
         }
         if ( ! attrTxt.isEmpty() ) { // Match remaining parameters
            Matcher attr = attributeMatcher;
            if ( attr == null ) attr = attributeMatcher = tagPool.get( attrRegx );
            while ( ! attrTxt.isEmpty() ) {
               if ( ! attr.reset( attrTxt ).find() || attr.start() != 0 )
                  throw new CocoParseError( "Cannot parse parameters " + attrTxt );
               tasks.add( parseTask( attr.group(1), attr.group(2) ) );
               attrTxt = attrTxt.substring( attr.end() ).trim();
            }
         }
      }
      return Directive.create( action, tasks );
   }

   private Task parseTask ( String taskname, String attr ) {
      List<String> params = new ArrayList<>(8);
      Matcher para = parameterMatcher;
      log.log( Level.FINEST, "Parsing coco task: {0} {1}", new Object[]{ taskname, attr } );
      if ( attr != null ) {
         attr = Text.unquote( attr, '(', ')' ).trim();
         while ( ! attr.isEmpty() ) {
            if ( shouldStop() ) return null;
            if ( ! para.reset( attr ).find() || para.start() != 0 )
               throw new UnsupportedOperationException( "Bug: parameterMatcher failed to match attributeMatcher result: " + attr );
            params.add( Task.unquote( para.group() ) );
            attr = attr.substring( para.end() ).trim();
            if ( attr.startsWith( "," ) ) attr = attr.substring( 1 ).trim();
         }
      }
      return Task.create( taskname, NullData.nullIfEmpty( params ) );
   }

   /************************************************************************************************************/
   // Helpers

   /**
    * Try to match a default parameter from a cocotag text.
    *
    * @param tagBody CocoTag content.
    * @return null or { parameter, remaining text }
    */
   private String[] checkDefaultParameter( String tagBody ) {
      Matcher para = parameterMatcher;
      if ( para == null ) para = parameterMatcher = tagPool.get( paramRegx );
      if ( ! para.reset( tagBody ).find() || para.start() != 0 ) return null;
      // Found a candidate. Test whether it should be parsed as parameter or task.
      String defaultParam = para.group();
      String remaining = tagBody.substring( para.end() ).trim();
      boolean isParam = Task.isQuoted( defaultParam );
      if ( ! isParam ) try {
         Task.create( defaultParam ); // Not quoted, not followed by bracket, so try parse as task.
      } catch ( CocoParseError ex ) {
         isParam = true;              // Failed to parse as task means it is a parameter not a task.
      }
      if ( isParam && remaining.startsWith( "(" ) ) return null; // Unless it is followed by a bracket
      if ( isParam ) return new String[]{ Task.unquote( defaultParam ), remaining };
      return null;
   }

   private static boolean shouldStop() {
      return Thread.currentThread().isInterrupted();
   }

   /************************************************************************************************************/
   // Results

   private void addToResult ( CharSequence text ) {
      if ( text.length() <= 0 ) return;
      if ( resultStack == null ) resultStack = new ArrayList<>();
      resultStack.add( text );
   }

   private void addToResult ( Directive dir ) {
      if ( resultStack == null ) resultStack = new ArrayList<>();
      resultStack.add( dir );
   }

   private StringBuilder getTextResult () {
      StringBuilder result = new StringBuilder( 1024 );
      try {
         for ( Object e : resultStack ) {
            if ( shouldStop() ) throw new InterruptedException();
            if ( e instanceof CharSequence ) {
               result.append( (CharSequence) e );
            } else {
               Block block = ( (Directive) e ).get();
               if ( block != null && block.hasData() ) // Output directive has no block
                  result.append( block.getText() );
            }
         }
         log.log( Level.FINE, "Parsed {0} coco tags.", tagCount );
      } catch ( InterruptedException ex ) {
         Thread.currentThread().interrupt();
         return null;
      }
      return result;
   }

   /************************************************************************************************************/
   // Tag and matcher management

   private static String findFirstTag( CharSequence text ) {
      Matcher firstTagMatcher = tagPool.get( firstTagRegx ).reset( text );
      String firstTag = firstTagMatcher.find() ? firstTagMatcher.group() : "";
      if ( firstTag.length() > 30 ) firstTag = Text.toString( firstTag.codePoints().limit(27) ) + "...";
      tagPool.recycle( firstTagRegx, firstTagMatcher );
      return firstTag;
   }

   private void clearMatchers() {
      if ( startMatcher != null ) tagPool.recycle( startTag, startMatcher );
      if (   endMatcher != null ) tagPool.recycle( endTag, endMatcher );
      if ( attributeMatcher != null ) tagPool.recycle( attrRegx, attributeMatcher );
      if ( parameterMatcher != null ) tagPool.recycle( paramRegx, parameterMatcher );
   }

   public String getStartTag() { return startTag; }
   public void setStartTag(String start_tag) { assert( start_tag != null ); this.startTag = start_tag; }

   public String getEndTag() { return endTag; }
   public void setEndTag(String end_tag) { assert( end_tag != null ); this.endTag = end_tag; }

   public void setTags( String start, String end ) {
      if ( start != null ) setStartTag( start );
      if ( end != null ) setEndTag( end );
   }
}