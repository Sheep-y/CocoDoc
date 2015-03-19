/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sheepy.cocodoc.worker.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
import sheepy.cocodoc.worker.Block;
import sheepy.cocodoc.worker.directive.Directive;
import sheepy.cocodoc.worker.error.CocoParseError;
import sheepy.cocodoc.worker.task.Task;
import sheepy.cocodoc.worker.task.TaskFile;
import sheepy.cocodoc.worker.util.CocoUtils;
import sheepy.util.Text;
import sheepy.util.collection.NullData;

public class ParserCoco extends Parser {
   private String startTag = "<\\?coco(?:-(\\w*))?";
   private String endTag = "\\?>";

   private static final String paramRegx = "\"([^\"]|\"\")*\"|'([^']|'')*'|[^,()]+"; // double quoted | single quoted | plain parameter
   private static final String attrRegx = ("(\\w+)\\s* ( \\(\\s* (?:" + paramRegx + ") (?:\\s*,\\s*(?:"+paramRegx+") )* \\s* \\))?").replaceAll( " +", "" );
   private static final String firstParamRegx = "\"([^\"]|\"\")*\"|'([^']|'')*'|[^\r\n\t ,()]+"; // differs from paramRegx in that this stop at space
                                       //   directive     \(          first param      (      ,        more params   )*       \)
   // Only live during parsing.
   private Matcher startMatcher;
   private Matcher endMatcher;
   private Matcher attributeMatcher;
   private Matcher parameterMatcher;
   private Matcher firstParameterMatcher;
   private int tagCount;

   private List<Object> resultStack;

   public ParserCoco () {}

   public ParserCoco ( String startTag, String endTag ) {
      this.setTags( startTag, endTag );
   }

   public ParserCoco ( Parser parent ) {
      super( parent );
      ParserCoco p = (ParserCoco) parent;
      this.startTag = p.startTag;
      this.endTag = p.endTag;
   }

   @Override protected StringBuilder implParse ( Block context, String text ) {
      parseDocument( context, text );
      clearMatchers();
      if ( resultStack == null ) return null; // No tag found

      return getTextResult();
   }

   @Override public ParserCoco clone() {
      return new ParserCoco( this );
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
      Matcher start = startMatcher = CocoUtils.tagPool.get( startTag );
      Matcher end   = endMatcher   = CocoUtils.tagPool.get( endTag   );

      while ( start.reset( text ).find() ) {
         if ( shouldStop() ) return;
         if ( ! end.reset( text ).find( start.end() ) ) break;
         String tag = text.substring( start.start(), end.start() );

         try {
            Directive dir = parseDirective( tag, start );
            if ( dir != null ) {
               ++tagCount;
               addToResult( text.subSequence( 0, start.start() ) );
               log.log( Level.FINE, "Found coco tag {0}", dir );
               switch ( dir.getAction() ) {
                  case START:
                     dir.setContent( text.substring( end.end() ) ); // Pass content to directive
                     addToResult( dir.start( context ) );
                     text = dir.getContent().toString(); // Get remaining (unparsed) content
                     if ( text == null ) throw new CocoParseError( "Coco:start without Coco:end" );
                     break;
                  case END:
                     if ( context.getParent() == null ) throw new CocoParseError( "Coco:end without Coco:start" );
                     context.getDirective().setContent( text.substring( end.end() ) ); // Parse unparsed content back to upper level.
                     return; // Terminate
                  default:
                     addToResult( dir.start( context ) );
                     text = text.substring( end.end() );
               }
            }
         } catch ( CocoParseError ex ) {
            log.log( Level.WARNING, "Cannot parse coco tag {1}: {0}", new Object[]{ ex, tag } );
            if ( text == null ) return;
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
      String txt = tag.substring( start.group().length() ).trim();
      List<Task> tasks = null;

      log.log( Level.FINER, "Parsing coco directive: {0} {1}", new Object[]{ action, txt } );
      if ( ! txt.isEmpty() ) {
         tasks = new ArrayList<>();
         String[] defaultCheck = checkDefaultParameter( txt );
         if ( defaultCheck != null ) {
            log.log( Level.FINEST, "Matched parameter {1} for default task {0}", new Object[]{ action, defaultCheck[0] } );
            tasks.add( new TaskFile().addParam( defaultCheck[0] ) );
            txt = defaultCheck[1];
         }
         if ( ! txt.isEmpty() ) { // Match remaining parameters
            Matcher attr = attributeMatcher;
            if ( attr == null ) attr = attributeMatcher = CocoUtils.tagPool.get( attrRegx );
            while ( ! txt.isEmpty() ) {
               if ( ! attr.reset( txt ).find() || attr.start() != 0 )
                  throw new CocoParseError( "Cannot parse tasks " + txt );

               tasks.add( parseTask( attr.group(1), attr.group(2) ) );

               txt = txt.substring( attr.end() ).trim();
            }
         }
      }
      return Directive.create( action, tasks );
   }

   private Task parseTask ( String taskname, String txt ) {
      List<String> params = new ArrayList<>(8);
      Matcher para = parameterMatcher;
      if ( para == null ) para = parameterMatcher = CocoUtils.tagPool.get( paramRegx );
      log.log( Level.FINEST, "Parsing coco task: {0} {1}", new Object[]{ taskname, txt } );
      if ( txt != null ) {
         txt = Text.unquote( txt, '(', ')' ).trim();
         while ( ! txt.isEmpty() ) {
            if ( shouldStop() ) return null;
            if ( ! para.reset( txt ).find() || para.start() != 0 )
               throw new UnsupportedOperationException( "Bug: parameterMatcher failed to match attributeMatcher result: " + txt );

            params.add( Task.unquote( para.group() ) );

            txt = txt.substring( para.end() ).trim();
            if ( txt.startsWith( "," ) ) txt = txt.substring( 1 ).trim();
         }
      }
      return Task.create( taskname, NullData.nullIfEmpty( params ) );
   }

   /************************************************************************************************************/
   // Helpers

   /**
    * Try to match the first task as a default parameter.
    *
    * @param txt Text to match.
    * @return null or [ default parameter, remaining text ]
    */
   private String[] checkDefaultParameter ( String txt ) {
      Matcher para = firstParameterMatcher;
      if ( para == null ) para = firstParameterMatcher = CocoUtils.tagPool.get( firstParamRegx );
      if ( ! para.reset( txt ).find() || para.start() != 0 ) return null;
      // Found a candidate. Test whether it should be parsed as parameter or task.
      String defaultParam = para.group();
      String remaining = txt.substring( para.end() ).trim();
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

   private void clearMatchers () {
      if ( startMatcher != null ) CocoUtils.tagPool.recycle( startTag, startMatcher );
      if (   endMatcher != null ) CocoUtils.tagPool.recycle( endTag, endMatcher );
      if ( attributeMatcher != null ) CocoUtils.tagPool.recycle( attrRegx, attributeMatcher );
      if ( parameterMatcher != null ) CocoUtils.tagPool.recycle( paramRegx, parameterMatcher );
      if ( firstParameterMatcher != null ) CocoUtils.tagPool.recycle( firstParamRegx, firstParameterMatcher );
      startMatcher = null;
      endMatcher = null;
      attributeMatcher = null;
      parameterMatcher = null;
      firstParameterMatcher = null;
   }

   public String getStartTag () { return startTag; }
   public void setStartTag(String start_tag) { assert( start_tag != null ); this.startTag = start_tag; }

   public String getEndTag () { return endTag; }
   public void setEndTag(String end_tag) { assert( end_tag != null ); this.endTag = end_tag; }

   public void setTags ( String start, String end ) {
      if ( start != null ) setStartTag( start );
      if ( end != null ) setEndTag( end );
   }
}