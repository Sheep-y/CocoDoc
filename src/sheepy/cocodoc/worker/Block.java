package sheepy.cocodoc.worker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import sheepy.cocodoc.CocoConfig;
import sheepy.cocodoc.CocoObserver;
import sheepy.cocodoc.CocoRunError;
import sheepy.cocodoc.worker.directive.Directive;
import sheepy.cocodoc.worker.parser.Parser;
import sheepy.cocodoc.worker.parser.coco.ParserCoco;
import sheepy.cocodoc.worker.task.Task;
import sheepy.util.collection.NullData;
import sheepy.util.concurrent.AbstractFuture;
import sheepy.util.text.I18n;
import sheepy.util.text.Text;

/**
 * A document processing block.
 */
public class Block extends AbstractFuture<Block> {
   private final Block parent;
   private final Directive directive;
   private final BlockStats stats;
   private String name = "";
   private File basePath;
   private Task outputTarget;

   private static final String VAR_OUTPUT_LIST   = "__io.output__";
   private static final String VAR_ONDONE        = "__block.ondone__";

   public Block ( Block parent, Directive directive ) {
      this.parent = parent;
      this.directive = directive;
      stats = new BlockStats( this );
      directive.setBlock( this ); // Do throw NPE if null
      if ( directive.getContent() != null ) {
         setText( directive.getContent() );
         directive.setContent( null );
      }
   }

   @Override protected Block implRun () {
      if ( hasObserver() ) getObserver().start( (Long) stats().getVar( BlockStats.NANO_BUILD ) );

      try {
         log( Level.FINEST, "Initialising" );
         for ( Task task : getTasks() ) task.init();

         for ( Task task : getTasks() ) {
            if ( Thread.currentThread().isInterrupted() ) return this;
            log( CocoConfig.MICRO, "Running {0}", task );
            task.process();
         }

         if ( hasData() ) {
            if ( getOutputTarget() != null ) {
               String fname = getOutputTarget().getParam( 0 );
               log( CocoConfig.MICRO, "Outputting to {0}", fname );
               if ( ! fname.equals( "NUL" ) && ! fname.equals( "/dev/null" ) ) {
                  postprocess();
                  outputToFile(fname);
                  log( Level.FINEST, "Outputted to {0}", fname );
               }
               setText( null );
            } else if ( getParent() == null ) {
               log( Level.FINEST, "Outputting to stdout" );
               postprocess();
            }
         }

         if ( getParent() == null ) {
            if ( stats().hasVar( VAR_ONDONE ) ) {
               log( Level.FINER, "Dispatching ondone" );
               List<Consumer<? super Block>> list = (List<Consumer<? super Block>>) stats().getVar( VAR_ONDONE );
               for ( Consumer<? super Block> func : list )
                  func.accept( this );
            }
            double time = Math.round( ( System.nanoTime() - (Long) stats().getVar( BlockStats.NANO_BUILD ) ) / 1000_000l ) / 1000.0;
            log( Level.FINE, "Finished ({0} s)", time );
         }

      } finally {
         if ( hasObserver() ) getObserver().done();
      }
      return this;
   }

   public void postprocess() {
      if ( hasText() && getText().indexOf( "<?coco-postprocess " ) >= 0 ) {
         log( Level.FINEST, "Post processing" );
         Parser postprocessor = new ParserCoco( true );
         postprocessor.start( this );
         setText( postprocessor.get() );
      }
   }

   public void outputToFile(String fname) throws CocoRunError {
      File f = new File( getBasePath(), fname );
      if ( f.getParentFile() != null )
         f.getParentFile().mkdirs();
      byte[] data = getBinary();
      log( Level.FINE, "Writing {1} bytes to {0}.", f, data.length );

      try ( FileOutputStream out = new FileOutputStream( f, false ) ) {
         out.write( data );
         stats().createVar( VAR_OUTPUT_LIST, ArrayList::new ).add( f );
      } catch ( IOException ex ) {
         if ( getOutputTarget().isThrowError() ) throw new CocoRunError( ex );
      }
   }

   /**************************************************************************************************/
   // Block data

   // There are three output modes:
   //  1. Initial (no data): both null
   //  2. Text mode  : textResult is non-null, binaryResult is null. call toBinary() to exit text mode.
   //  3. Binary mode: textResult is null, binaryResult is non-null. call toText()   to exit binary mode.
   private ByteArrayOutputStream binaryResult;
   private StringBuilder textResult;

   private List<Charset> toBinaryCharset;
   private List<Charset> toTextCharset;
   private Charset currentCharset;

   public boolean hasData()   { return hasText() || hasBinary(); }
   public boolean hasText()   { return textResult   != null && textResult.length() > 0; }
   public boolean hasBinary() { return binaryResult != null && binaryResult.size() > 0; }

   public byte[] getBinary() {
      toBinary( null );
      return hasBinary() ? binaryResult.toByteArray() : new byte[0];
   }

   public StringBuilder getText() {
      toText( null );
      if ( ! hasText() ) textResult = new StringBuilder();
      return textResult;
   }

   public Block appendBinary( byte[] data ) {
      try {
         toBinary( null );
         if ( ! hasBinary() ) setBinary( data );
         else binaryResult.write( data );
         return this;
      } catch ( IOException ex ) {
         throw new CocoRunError( ex );
      }
   }

   public void setBinary( byte[] data ) {
      if ( data == null ) {
         setText( null );
      } else try {
         if ( textResult   != null ) textResult.setLength( 0 );
         binaryResult = new ByteArrayOutputStream( data.length );
         binaryResult.write( data );
      } catch ( IOException ex ) {
         throw new CocoRunError( ex );
      }
   }

   public void setBinary( ByteArrayOutputStream data ) {
      if ( data == null ) {
         setText( null );
      } else {
         if ( data == binaryResult ) return;
         if ( textResult != null ) textResult.setLength( 0 );
         binaryResult = data;
      }
   }

   public void setText( CharSequence text ) {
      if ( text == null ) {
         binaryResult = null;
         textResult = null;
      } else {
         if ( text == textResult ) return;
         if ( binaryResult != null ) binaryResult.reset();
         if ( text instanceof StringBuilder ) {
            textResult = (StringBuilder) text;
         } else {
            textResult = new StringBuilder( text );
         }
      }
   }

   public void toBinary ( List<Charset> encoding ) {
      if ( encoding != null ) toBinaryCharset = new ArrayList<>( encoding );
      if ( hasBinary() || ! hasText() ) return;
      if ( toBinaryCharset == null || toBinaryCharset.isEmpty() ) toBinaryCharset = Collections.singletonList( Text.UTF8 );

      IOException error = null;
      int textLen = textResult.length();
      for ( Charset charset : toBinaryCharset ) try {
         setBinary( I18n.encode(textResult, I18n.strictEncoder( charset ) ) );
         currentCharset = charset;
         log( CocoConfig.MICRO, "Converted {0} characters to {1} binary.", textLen, charset );
         return;
      } catch ( IOException ex ) {
         error = ex;
      }
      String[] list = toBinaryCharset.stream().map( Charset::name ).toArray( String[]::new );
      throw new CocoRunError( "Cannot encode text in " + String.join( ", ", list ) , error );
   }

   public void toText ( List<Charset> encoding ) {
      if ( encoding != null ) toTextCharset = new ArrayList<>( encoding );
      if ( hasText() || ! hasBinary() ) return;
      if ( toTextCharset == null || toTextCharset.isEmpty() ) toTextCharset = Arrays.asList( Text.UTF8, Text.UTF16 );

      byte[] buf = binaryResult.toByteArray();
      IOException error = null;
      for ( Charset charset : toTextCharset ) try {
         setText( I18n.decode(buf, I18n.strictDecoder( charset ) ) );
         currentCharset = charset;
         log( CocoConfig.MICRO, "Converted {0} bytes of {1} to text.", buf.length, charset );
         return;
      } catch ( CharacterCodingException ex ) {
         error = ex;
      }
      String[] list = toTextCharset.stream().map( Charset::name ).toArray( String[]::new );
      throw new CocoRunError( "Cannot decode text in " + String.join( ", ", list ) , error );
   }

   /** Return last used binary / text encoding. */
   public Charset getCurrentCharset() {
      return currentCharset;
   }

   /**************************************************************************************************/
   // Attributes

   public Block getParent() {
      return this.parent;
   }

   public Block getRoot() {
      Block pos = this;
      while ( pos.parent != null )
         pos = pos.parent;
      return pos;
   }

   public BlockStats stats() { return stats; }
   public Directive getDirective () { return directive; }
   public List<Task> getTasks ()    { return directive.getTasks(); }
   public boolean hasObserver ()     { return directive.getObserver() != null; }
   public CocoObserver getObserver () { return directive.getObserver(); }
   public void log ( Level level, String message, Object ... parameter ) {
      getDirective().log( level, message, this, parameter );
   }

   public Block setName ( CharSequence name ) {
      if ( name == null || name.length() <= 0 ) return this;
      if ( this.name.isEmpty() )
         this.name = name.toString();
      else
         this.name += ',' + name.toString();
      if ( hasObserver() )
         getObserver().setName( this.name );
      return this;
   }

   public File getParentBasePath() {
      return parent == null ? null : parent.getBasePath();
   }
   public File getBasePath() {
      return basePath == null ? getParentBasePath() : basePath;
   }
   public Block setBasePath( File basePath ) {
      if ( basePath != null && this.basePath == null ) {
         log( Level.FINER, "Block base path set to {0}", basePath );
         this.basePath = basePath;
      }
      return this;
   }

   public Task getOutputTarget() { return outputTarget; }
   public void setOutputTarget( Task output ) {
      this.outputTarget = output;
   }
   public List<File> getOutputList() {
      return new ArrayList<>( NullData.nonNull( (List<File>) stats().getVar( VAR_OUTPUT_LIST ) ) );
   }

   public Block addOnDone( Consumer<? super Block> task ) {
      stats.createVar( VAR_ONDONE, ArrayList::new ).add( task );
      return this;
   }

   private Parser parser;
   public Parser getParser () { return parser; }
   public void setParser ( Parser currentParser ) { this.parser = currentParser; }

   @Override public String toString() {
      if ( hasText() )
         return "Block " + name + " (" + textResult.length()+ " characters)";
      else if ( hasBinary() )
         return "Block " + name + " (" + binaryResult.size() + " bytes)";
      else
         return "Block " + name + " (no data)";
   }
}